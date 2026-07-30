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

import java.time.LocalDateTime;
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
        Optional<FriendRequestEntity> acceptedRequest = requestRepository.findAccepted(current.getId(), userId);
        String status = relationshipStatus(current.getId(), userId);
        boolean revealContact = Objects.equals(current.getId(), userId) || "friends".equals(status);
        Map<String, Object> result = userMap(user, revealContact, acceptedRequest.map(FriendRequestEntity::getContactInfo).orElse(""));
        result.put("isSelf", Objects.equals(current.getId(), userId));
        result.put("friendshipStatus", status);
        result.put("contactStatus", status);
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
            .filter(request -> !isExpiredPending(request))
            .map(request -> requestMap(request, request.getRequesterId())).toList();
        List<Map<String, Object>> outgoing = requestRepository
            .findByRequesterIdAndStatusOrderByCreatedAtDesc(userId, "pending").stream()
            .filter(request -> !isExpiredPending(request))
            .map(request -> requestMap(request, request.getRecipientId())).toList();
        return Map.of("incoming", incoming, "outgoing", outgoing, "pendingCount", incoming.size());
    }

    @PostMapping("/requests/{recipientId}")
    public Map<String, Object> send(@PathVariable Long recipientId, @RequestBody(required = false) Map<String, Object> body) {
        AppUserEntity requester = currentUserService.getOrCreateDefaultUser();
        AppUserEntity recipient = requireUser(recipientId);
        if (Objects.equals(requester.getId(), recipientId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能申请自己的联系方式");
        }
        if (requestRepository.findAccepted(requester.getId(), recipientId).isPresent()) {
            return Map.of("status", "friends");
        }
        List<FriendRequestEntity> existing = requestRepository.findRelationship(requester.getId(), recipientId);
        FriendRequestEntity request = existing.isEmpty() ? new FriendRequestEntity() : existing.get(0);
        if ("pending".equals(request.getStatus()) && Objects.equals(request.getRecipientId(), requester.getId())) {
            if (!isExpiredPending(request)) {
                return Map.of("status", "incoming_pending");
            }
        }
        if ("pending".equals(request.getStatus()) && !isExpiredPending(request)) {
            return Map.of("status", "outgoing_pending", "id", request.getId());
        }
        request.setRequesterId(requester.getId());
        request.setRecipientId(recipientId);
        request.setStatus("pending");
        request.setContactInfo("");
        request.setMessage(body == null ? "" : text(body.get("message")));
        FriendRequestEntity saved = requestRepository.save(request);
        notificationService.create(recipientId, requester.getId(), "contact_request", saved.getId(),
            "收到联系方式申请", requester.getUsername() + " 希望获取你的联系方式");
        return Map.of("status", "outgoing_pending", "id", saved.getId());
    }

    @PatchMapping("/requests/{requestId}")
    public Map<String, Object> handle(@PathVariable Long requestId, @RequestBody Map<String, Object> body) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        FriendRequestEntity request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "联系方式申请不存在"));
        if (!Objects.equals(request.getRecipientId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权处理此联系方式申请");
        }
        String action = text(body.get("action"));
        if ("accept".equals(action)) {
            String contactInfo = text(body.get("contactInfo"));
            if (!StringUtils.hasText(contactInfo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "同意前请填写微信或 QQ 联系方式");
            }
            request.setContactInfo(limit(contactInfo, 120));
            request.setStatus("accepted");
        } else {
            request.setStatus("rejected");
            request.setContactInfo("");
        }
        requestRepository.save(request);
        AppUserEntity actor = requireUser(userId);
        String contactInfo = text(request.getContactInfo());
        boolean accepted = "accepted".equals(request.getStatus());
        String acceptedDescription = actor.getUsername() + " 已同意向你展示联系方式，微信/QQ：" + contactInfo;
        notificationService.create(request.getRequesterId(), userId, "contact_request_result", request.getId(),
            accepted ? "联系方式申请已通过" : "联系方式申请已拒绝",
            accepted ? acceptedDescription : actor.getUsername() + " 已拒绝展示联系方式，你可以稍后再次申请");
        return Map.of(
            "status", request.getStatus(),
            "requestId", request.getId(),
            "contactInfo", accepted ? contactInfo : ""
        );
    }

    private String relationshipStatus(Long userId, Long otherId) {
        if (Objects.equals(userId, otherId)) return "self";
        List<FriendRequestEntity> relationships = requestRepository.findRelationship(userId, otherId);
        if (relationships.isEmpty()) return "none";
        FriendRequestEntity request = relationships.get(0);
        if ("accepted".equals(request.getStatus())) return "friends";
        if (!"pending".equals(request.getStatus())) return "none";
        if (isExpiredPending(request)) return "none";
        return Objects.equals(request.getRequesterId(), userId) ? "outgoing_pending" : "incoming_pending";
    }

    private boolean isExpiredPending(FriendRequestEntity request) {
        return request != null
            && "pending".equals(request.getStatus())
            && request.getCreatedAt() != null
            && request.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24));
    }

    private Map<String, Object> requestMap(FriendRequestEntity request, Long otherId) {
        AppUserEntity user = requireUser(otherId);
        boolean accepted = "accepted".equals(request.getStatus());
        Map<String, Object> map = userMap(user, accepted, request.getContactInfo());
        map.put("requestId", request.getId());
        map.put("message", request.getMessage());
        map.put("time", request.getCreatedAt().format(FORMATTER));
        return map;
    }

    private Map<String, Object> userMap(AppUserEntity user) {
        return userMap(user, true, "");
    }

    private Map<String, Object> userMap(AppUserEntity user, boolean revealContact, String contactInfo) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", user.getId());
        map.put("name", user.getUsername());
        map.put("contactInfo", revealContact ? text(contactInfo) : "");
        map.put("role", user.getRole());
        map.put("membershipPlan", user.getMembershipPlan());
        map.put("fruitScore", user.getFruitScore() == null ? 0 : user.getFruitScore());
        map.put("avatar", avatar(user.getUsername()));
        map.put("avatarUrl", user.getAvatarUrl());
        map.put("backgroundUrl", user.getBackgroundUrl());
        return map;
    }

    private AppUserEntity requireUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }
    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private String limit(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
    private String avatar(String value) { return StringUtils.hasText(value) ? value.trim().substring(0, 1).toUpperCase() : "U"; }
}
