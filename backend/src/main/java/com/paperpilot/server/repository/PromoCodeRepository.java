package com.paperpilot.server.repository;

import com.paperpilot.server.entity.PromoCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCodeEntity, Long> {
    Optional<PromoCodeEntity> findByCode(String code);

    @Modifying
    @Query("update PromoCodeEntity p set p.usedCount = coalesce(p.usedCount, 0) + 1, p.usedByUserId = :userId, p.usedAt = :usedAt, p.used = true, p.maxUses = 0 where p.code = :code and coalesce(p.invalidated, false) = false")
    int incrementUsageIfActive(@Param("code") String code, @Param("userId") Long userId, @Param("usedAt") LocalDateTime usedAt);
}
