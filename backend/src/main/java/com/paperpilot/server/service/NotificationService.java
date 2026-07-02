package com.paperpilot.server.service;

import com.paperpilot.server.entity.UserNotificationEntity;
import com.paperpilot.server.repository.UserNotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final UserNotificationRepository repository;

    public NotificationService(UserNotificationRepository repository) {
        this.repository = repository;
    }

    public void create(Long recipientId, Long actorId, String type, Long referenceId, String title, String description) {
        if (recipientId == null || recipientId.equals(actorId)) return;
        UserNotificationEntity notification = new UserNotificationEntity();
        notification.setUserId(recipientId);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setReadFlag(false);
        repository.save(notification);
    }
}
