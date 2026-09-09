package com.paperpilot.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** Immutable audit trail for a user's successful promo-code redemption. */
@Entity
@Table(
    name = "promo_code_redemption",
    uniqueConstraints = @UniqueConstraint(name = "uk_promo_code_redemption_user", columnNames = {"promo_code_id", "user_id"})
)
public class PromoCodeRedemptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "promo_code_id", nullable = false)
    private Long promoCodeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPromoCodeId() { return promoCodeId; }
    public void setPromoCodeId(Long promoCodeId) { this.promoCodeId = promoCodeId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(LocalDateTime redeemedAt) { this.redeemedAt = redeemedAt; }
}
