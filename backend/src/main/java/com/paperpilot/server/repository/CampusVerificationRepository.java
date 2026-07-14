package com.paperpilot.server.repository;

import com.paperpilot.server.entity.CampusVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampusVerificationRepository extends JpaRepository<CampusVerificationEntity, Long> {
    Optional<CampusVerificationEntity> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
    List<CampusVerificationEntity> findAllByOrderByCreatedAtDesc();
}
