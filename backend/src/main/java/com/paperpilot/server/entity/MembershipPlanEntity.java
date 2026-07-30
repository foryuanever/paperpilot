package com.paperpilot.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "membership_plan")
public class MembershipPlanEntity {
    @Id
    @Column(length = 32)
    private String id;

    private String name;
    private String subtitle;
    private Double monthlyPrice;
    private Double originalMonthlyPrice;
    private Integer reviewQuota;
    private Integer pptQuota;
    private Integer chatQuota;
    private Integer translateQuota;
    private Integer immersiveQuota;
    private Integer teamSeats;
    private Boolean teamShared;
    private Boolean forumSpecial;
    private Integer forumTopDaily;
    private Boolean peakPriority;
    private Boolean activeFlag;
    private Integer sortOrder;
    private Boolean seckillEnabled;
    private Double seckillPrice;
    private LocalDateTime seckillStartsAt;
    private LocalDateTime seckillEndsAt;
    private String seckillLabel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        applyDefaults();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        applyDefaults();
    }

    private void applyDefaults() {
        if (monthlyPrice == null) monthlyPrice = 0D;
        if (originalMonthlyPrice == null) originalMonthlyPrice = monthlyPrice;
        if (reviewQuota == null) reviewQuota = 0;
        if (pptQuota == null) pptQuota = 0;
        if (chatQuota == null) chatQuota = 0;
        if (translateQuota == null) translateQuota = 0;
        if (immersiveQuota == null) immersiveQuota = 0;
        if (teamSeats == null) teamSeats = 0;
        if (teamShared == null) teamShared = false;
        if (forumSpecial == null) forumSpecial = false;
        if (forumTopDaily == null) forumTopDaily = 0;
        if (peakPriority == null) peakPriority = false;
        if (activeFlag == null) activeFlag = true;
        if (sortOrder == null) sortOrder = 99;
        if (seckillEnabled == null) seckillEnabled = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public Double getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(Double monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    public Double getOriginalMonthlyPrice() { return originalMonthlyPrice; }
    public void setOriginalMonthlyPrice(Double originalMonthlyPrice) { this.originalMonthlyPrice = originalMonthlyPrice; }
    public Integer getReviewQuota() { return reviewQuota; }
    public void setReviewQuota(Integer reviewQuota) { this.reviewQuota = reviewQuota; }
    public Integer getPptQuota() { return pptQuota; }
    public void setPptQuota(Integer pptQuota) { this.pptQuota = pptQuota; }
    public Integer getChatQuota() { return chatQuota; }
    public void setChatQuota(Integer chatQuota) { this.chatQuota = chatQuota; }
    public Integer getTranslateQuota() { return translateQuota; }
    public void setTranslateQuota(Integer translateQuota) { this.translateQuota = translateQuota; }
    public Integer getImmersiveQuota() { return immersiveQuota; }
    public void setImmersiveQuota(Integer immersiveQuota) { this.immersiveQuota = immersiveQuota; }
    public Integer getTeamSeats() { return teamSeats; }
    public void setTeamSeats(Integer teamSeats) { this.teamSeats = teamSeats; }
    public Boolean getTeamShared() { return teamShared; }
    public void setTeamShared(Boolean teamShared) { this.teamShared = teamShared; }
    public Boolean getForumSpecial() { return forumSpecial; }
    public void setForumSpecial(Boolean forumSpecial) { this.forumSpecial = forumSpecial; }
    public Integer getForumTopDaily() { return forumTopDaily; }
    public void setForumTopDaily(Integer forumTopDaily) { this.forumTopDaily = forumTopDaily; }
    public Boolean getPeakPriority() { return peakPriority; }
    public void setPeakPriority(Boolean peakPriority) { this.peakPriority = peakPriority; }
    public Boolean getActiveFlag() { return activeFlag; }
    public void setActiveFlag(Boolean activeFlag) { this.activeFlag = activeFlag; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getSeckillEnabled() { return seckillEnabled; }
    public void setSeckillEnabled(Boolean seckillEnabled) { this.seckillEnabled = seckillEnabled; }
    public Double getSeckillPrice() { return seckillPrice; }
    public void setSeckillPrice(Double seckillPrice) { this.seckillPrice = seckillPrice; }
    public LocalDateTime getSeckillStartsAt() { return seckillStartsAt; }
    public void setSeckillStartsAt(LocalDateTime seckillStartsAt) { this.seckillStartsAt = seckillStartsAt; }
    public LocalDateTime getSeckillEndsAt() { return seckillEndsAt; }
    public void setSeckillEndsAt(LocalDateTime seckillEndsAt) { this.seckillEndsAt = seckillEndsAt; }
    public String getSeckillLabel() { return seckillLabel; }
    public void setSeckillLabel(String seckillLabel) { this.seckillLabel = seckillLabel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
