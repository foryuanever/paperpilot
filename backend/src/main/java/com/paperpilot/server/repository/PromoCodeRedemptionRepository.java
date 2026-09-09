package com.paperpilot.server.repository;

import com.paperpilot.server.entity.PromoCodeRedemptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromoCodeRedemptionRepository extends JpaRepository<PromoCodeRedemptionEntity, Long> {

    /** MySQL INSERT IGNORE makes the one-code-per-user reservation race-safe. */
    @Modifying
    @Query(value = "insert ignore into promo_code_redemption (promo_code_id, user_id, redeemed_at) values (:promoCodeId, :userId, :redeemedAt)", nativeQuery = true)
    int reserve(@Param("promoCodeId") Long promoCodeId, @Param("userId") Long userId, @Param("redeemedAt") java.time.LocalDateTime redeemedAt);
}
