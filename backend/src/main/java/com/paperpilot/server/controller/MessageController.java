package com.paperpilot.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.DirectMessageEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.DirectMessageRepository;
import com.paperpilot.server.service.CurrentUserService;
import com.paperpilot.server.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final AppUserRepository userRepository;
    private final DirectMessageRepository messageRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public MessageController(
        AppUserRepository userRepository,
        DirectMessageRepository messageRepository,
        CurrentUserService currentUserService,
        NotificationService notificationService,
        ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/contacts")
    public Map<String, Object> contacts() {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        Map<Long, Long> unreadBySender = new HashMap<>();
        for (DirectMessageEntity message : messageRepository.findByRecipientIdAndReadFlagFalseOrderByCreatedAtDesc(userId)) {
            unreadBySender.merge(message.getSenderId(), 1L, Long::sum);
        }
        List<Map<String, Object>> contacts = new ArrayList<>();
        for (AppUserEntity user : userRepository.findAllByIdNotOrderByUsernameAsc(userId)) {
            List<DirectMessageEntity> thread = messageRepository.findConversation(userId, user.getId());
            DirectMessageEntity last = thread.isEmpty() ? null : thread.get(thread.size() - 1);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", user.getId());
            item.put("name", user.getUsername());
            item.put("email", user.getEmail());
            item.put("role", user.getRole());
            item.put("avatar", avatar(user.getUsername()));
            item.put("unreadCount", unreadBySender.getOrDefault(user.getId(), 0L));
            item.put("lastMessage", last == null ? "" : summarize(last.getContent()));
            item.put("lastTime", last == null ? "" : last.getCreatedAt().format(FORMATTER));
            contacts.add(item);
        }
        contacts.sort(Comparator.comparing(item -> String.valueOf(item.get("lastTime")), Comparator.reverseOrder()));
        return Map.of("contacts", contacts, "unreadCount", messageRepository.countByRecipientIdAndReadFlagFalse(userId));
    }

    @GetMapping("/thread/{otherId}")
    @Transactional
    public List<Map<String, Object>> thread(@PathVariable Long otherId) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        messageRepository.markRead(userId, otherId);
        return messageRepository.findConversation(userId, otherId).stream().map(message -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", message.getId());
            item.put("senderId", message.getSenderId());
            item.put("senderName", message.getSenderName());
            item.put("content", message.getContent());
            item.put("attachments", readAttachments(message.getAttachmentsJson()));
            item.put("time", message.getCreatedAt().format(FORMATTER));
            item.put("mine", Objects.equals(message.getSenderId(), userId));
            return item;
        }).toList();
    }

    @PostMapping("/thread/{otherId}")
    public Map<String, Object> send(@PathVariable Long otherId, @RequestBody Map<String, Object> body) {
        AppUserEntity sender = currentUserService.getOrCreateDefaultUser();
        AppUserEntity recipient = userRepository.findById(otherId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "联系人不存在"));
        String content = text(body.get("content"));
        Object attachments = body.getOrDefault("attachments", List.of());
        if (!StringUtils.hasText(content) && (!(attachments instanceof List<?> list) || list.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "私信内容和附件不能同时为空");
        }
        DirectMessageEntity message = new DirectMessageEntity();
        message.setSenderId(sender.getId());
        message.setRecipientId(recipient.getId());
        message.setSenderName(sender.getUsername());
        message.setContent(content);
        message.setAttachmentsJson(writeJson(attachments));
        DirectMessageEntity saved = messageRepository.save(message);
        notificationService.create(recipient.getId(), sender.getId(), "private_message", saved.getId(),
            "收到新的私信", sender.getUsername() + " 向你发送了一条私信");
        return Map.of("id", saved.getId());
    }

    private List<Map<String, Object>> readAttachments(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private String avatar(String value) { return StringUtils.hasText(value) ? value.trim().substring(0, 1).toUpperCase() : "U"; }
    private String summarize(String value) {
        if (!StringUtils.hasText(value)) return "[附件]";
        return value.length() > 24 ? value.substring(0, 24) + "..." : value;
    }
}
