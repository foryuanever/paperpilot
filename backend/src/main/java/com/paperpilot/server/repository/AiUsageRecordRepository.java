package com.paperpilot.server.repository;

import com.paperpilot.server.entity.AiUsageRecordEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiUsageRecordRepository extends JpaRepository<AiUsageRecordEntity, Long>, JpaSpecificationExecutor<AiUsageRecordEntity> {
    void deleteAllByUserId(Long userId);
    List<AiUsageRecordEntity> findTop240ByUserIdOrderByCreatedAtDesc(Long userId);
    List<AiUsageRecordEntity> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime createdAt);
    List<AiUsageRecordEntity> findTop240ByOrderByCreatedAtDesc();
    List<AiUsageRecordEntity> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt);
    List<AiUsageRecordEntity> findTop3ByUserIdAndSceneAndActionAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
        Long userId,
        String scene,
        String action,
        String status,
        LocalDateTime start,
        LocalDateTime end
    );
    long countByUserId(Long userId);
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime createdAt);
    long countByUserIdAndSceneAndStatusAndCreatedAtAfter(Long userId, String scene, String status, LocalDateTime createdAt);
    long countByCreatedAtAfter(LocalDateTime createdAt);
    boolean existsByUserIdAndRequestKey(Long userId, String requestKey);
    boolean existsByUserIdAndSceneAndStatusAndErrorMessage(Long userId, String scene, String status, String errorMessage);
}
