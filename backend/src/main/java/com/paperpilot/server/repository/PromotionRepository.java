package com.paperpilot.server.repository;

import com.paperpilot.server.entity.PromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionRepository extends JpaRepository<PromotionEntity, Long> {
    List<PromotionEntity> findAllByOrderByCreatedAtDesc();
    List<PromotionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PromotionEntity> findByStatusOrderByCreatedAtDesc(String status);
}
