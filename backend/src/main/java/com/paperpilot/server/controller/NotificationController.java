package com.paperpilot.server.controller;

import com.paperpilot.server.entity.UserNotificationEntity;
import com.paperpilot.server.repository.UserNotificationRepository;
import com.paperpilot.server.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final UserNotificationRepository repository;
    private final CurrentUserService currentUserService;

    public NotificationController(UserNotificationRepository repository, CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<UserNotificationEntity> unread() {
        return repository.findByUserIdAndReadFlagFalseOrderByCreatedAtDesc(
            currentUserService.getOrCreateDefaultUserId()
        );
    }

    @PatchMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        UserNotificationEntity notification = repository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "消息不存在"));
        notification.setReadFlag(true);
        repository.save(notification);
    }
}
