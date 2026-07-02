package com.paperpilot.server.repository;

import com.paperpilot.server.entity.AiUsageRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiUsageRecordRepository extends JpaRepository<AiUsageRecordEntity, Long> {
    List<AiUsageRecordEntity> findTop240ByUserIdOrderByCreatedAtDesc(Long userId);
    List<AiUsageRecordEntity> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime createdAt);
    List<AiUsageRecordEntity> findTop240ByOrderByCreatedAtDesc();
    List<AiUsageRecordEntity> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt);
}
