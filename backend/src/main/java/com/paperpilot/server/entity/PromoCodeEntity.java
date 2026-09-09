package com.paperpilot.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "promo_code")
public class PromoCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(name = "plan_id", nullable = false, length = 64)
    private String planId;

    @Column(name = "plan_cycle", length = 32)
    private String planCycle = "monthly";

    @Column(nullable = false)
    private Boolean used = false;

    @Column(nullable = false)
    private Boolean invalidated = false;

    @Column(name = "max_uses", nullable = false)
    /** Zero means unlimited global redemptions; each account is still limited by the audit table. */
    private Integer maxUses = 0;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "used_by_user_id")
    private Long usedByUserId;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (planCycle == null) {
            planCycle = "monthly";
        }
        if (used == null) {
            used = false;
        }
        if (invalidated == null) {
            invalidated = false;
        }
        if (maxUses == null) {
            maxUses = 0;
        }
        if (usedCount == null) {
            usedCount = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getPlanCycle() {
        return planCycle;
    }

    public void setPlanCycle(String planCycle) {
        this.planCycle = planCycle;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    public Boolean getInvalidated() {
        return invalidated;
    }

    public void setInvalidated(Boolean invalidated) {
        this.invalidated = invalidated;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public Long getUsedByUserId() {
        return usedByUserId;
    }

    public void setUsedByUserId(Long usedByUserId) {
        this.usedByUserId = usedByUserId;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
