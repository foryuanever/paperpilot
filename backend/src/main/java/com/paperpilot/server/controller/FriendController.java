package com.paperpilot.server.controller;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.FriendRequestEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.FriendRequestRepository;
import com.paperpilot.server.service.CurrentUserService;
import com.paperpilot.server.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final FriendRequestRepository requestRepository;
    private final AppUserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public FriendController(
        FriendRequestRepository requestRepository,
        AppUserRepository userRepository,
        CurrentUserService currentUserService,
        NotificationService notificationService
    ) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @GetMapping("/profile/{userId}")
    public Map<String, Object> profile(@PathVariable Long userId) {
        AppUserEntity current = currentUserService.getOrCreateDefaultUser();
        AppUserEntity user = requireUser(userId);
        Map<String, Object> result = userMap(user);
        result.put("isSelf", Objects.equals(current.getId(), userId));
        result.put("friendshipStatus", relationshipStatus(current.getId(), userId));
        return result;
    }

    @GetMapping("/profile")
    public Map<String, Object> profileByEmail(@RequestParam String email) {
        AppUserEntity user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        return profile(user.getId());
    }

    @GetMapping("/requests")
    public Map<String, Object> requests() {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        List<Map<String, Object>> incoming = requestRepository
            .findByRecipientIdAndStatusOrderByCreatedAtDesc(userId, "pending").stream()
            .map(request -> requestMap(request, request.getRequesterId())).toList();
        List<Map<String, Object>> outgoing = requestRepository
            .findByRequesterIdAndStatusOrderByCreatedAtDesc(userId, "pending").stream()
            .map(request -> requestMap(request, request.getRecipientId())).toList();
        return Map.of("incoming", incoming, "outgoing", outgoing, "pendingCount", incoming.size());
    }

    @PostMapping("/requests/{recipientId}")
    public Map<String, Object> send(@PathVariable Long recipientId, @RequestBody(required = false) Map<String, Object> body) {
        AppUserEntity requester = currentUserService.getOrCreateDefaultUser();
        AppUserEntity recipient = requireUser(recipientId);
        if (Objects.equals(requester.getId(), recipientId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能添加自己为好友");
        }
        if (requestRepository.findAccepted(requester.getId(), recipientId).isPresent()) {
            return Map.of("status", "friends");
        }
        List<FriendRequestEntity> existing = requestRepository.findRelationship(requester.getId(), recipientId);
        FriendRequestEntity request = existing.isEmpty() ? new FriendRequestEntity() : existing.get(0);
        if ("pending".equals(request.getStatus()) && Objects.equals(request.getRecipientId(), requester.getId())) {
            request.setStatus("accepted");
            requestRepository.save(request);
            return Map.of("status", "friends");
        }
        request.setRequesterId(requester.getId());
        request.setRecipientId(recipientId);
        request.setStatus("pending");
        request.setMessage(body == null ? "" : text(body.get("message")));
        FriendRequestEntity saved = requestRepository.save(request);
        notificationService.create(recipientId, requester.getId(), "friend_request", saved.getId(),
            "收到好友申请", requester.getUsername() + " 申请添加你为好友");
        return Map.of("status", "outgoing_pending", "id", saved.getId());
    }

    @PatchMapping("/requests/{requestId}")
    public void handle(@PathVariable Long requestId, @RequestBody Map<String, Object> body) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        FriendRequestEntity request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "好友申请不存在"));
        if (!Objects.equals(request.getRecipientId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权处理此好友申请");
        }
        String action = text(body.get("action"));
        request.setStatus("accept".equals(action) ? "accepted" : "rejected");
        requestRepository.save(request);
        AppUserEntity actor = requireUser(userId);
        notificationService.create(request.getRequesterId(), userId, "friend_request_result", request.getId(),
            "好友申请已处理", actor.getUsername() + ("accepted".equals(request.getStatus()) ? " 已同意你的好友申请" : " 已拒绝你的好友申请"));
    }

    private String relationshipStatus(Long userId, Long otherId) {
        if (Objects.equals(userId, otherId)) return "self";
        List<FriendRequestEntity> relationships = requestRepository.findRelationship(userId, otherId);
        if (relationships.isEmpty()) return "none";
        FriendRequestEntity request = relationships.get(0);
        if ("accepted".equals(request.getStatus())) return "friends";
        if (!"pending".equals(request.getStatus())) return "none";
        return Objects.equals(request.getRequesterId(), userId) ? "outgoing_pending" : "incoming_pending";
    }

    private Map<String, Object> requestMap(FriendRequestEntity request, Long otherId) {
        AppUserEntity user = requireUser(otherId);
        Map<String, Object> map = userMap(user);
        map.put("requestId", request.getId());
        map.put("message", request.getMessage());
        map.put("time", request.getCreatedAt().format(FORMATTER));
        return map;
    }

    private Map<String, Object> userMap(AppUserEntity user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", user.getId());
        map.put("name", user.getUsername());
        map.put("email", user.getEmail());
        map.put("role", user.getRole());
        map.put("avatar", avatar(user.getUsername()));
        map.put("registerTime", user.getCreatedAt() == null ? "" : user.getCreatedAt().toLocalDate().toString());
        map.put("activeTime", user.getActiveTime() == null ? 0 : user.getActiveTime());
        map.put("teamId", user.getTeamId());
        return map;
    }

    private AppUserEntity requireUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }
    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private String avatar(String value) { return StringUtils.hasText(value) ? value.trim().substring(0, 1).toUpperCase() : "U"; }
}
