package com.paperpilot.server.repository;

import com.paperpilot.server.entity.UserNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotificationEntity, Long> {
    List<UserNotificationEntity> findByUserIdAndReadFlagFalseOrderByCreatedAtDesc(Long userId);
    List<UserNotificationEntity> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
    List<UserNotificationEntity> findByUserIdAndTypeAndReferenceIdOrderByCreatedAtDesc(Long userId, String type, Long referenceId);
    Optional<UserNotificationEntity> findByIdAndUserId(Long id, Long userId);
}
