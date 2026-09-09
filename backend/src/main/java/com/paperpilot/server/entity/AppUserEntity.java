package com.paperpilot.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "app_user")
public class AppUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 128, unique = true)
    private String email;

    @Column(name = "invite_code", length = 64)
    private String inviteCode;

    @Column(name = "password_hash", length = 255)
    @JsonIgnore
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (numericId == null) {
            numericId = 100000 + new java.util.Random().nextInt(900000);
        }
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    @Column(name = "role", length = 32)
    private String role = "普通用户";

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Column(name = "plain_password", length = 255)
    @JsonIgnore
    private String plainPassword;

    public String getPlainPassword() {
        return plainPassword;
    }

    public void setPlainPassword(String plainPassword) {
        this.plainPassword = plainPassword;
    }

    @Column(name = "token_limit")
    private Long tokenLimit = 5000000L;

    public Long getTokenLimit() {
        return tokenLimit;
    }

    public void setTokenLimit(Long tokenLimit) {
        this.tokenLimit = tokenLimit;
    }

    @Column(name = "token_used")
    private Long tokenUsed = 0L;

    public Long getTokenUsed() {
        return tokenUsed;
    }

    public void setTokenUsed(Long tokenUsed) {
        this.tokenUsed = tokenUsed;
    }

    @Column(name = "balance_amount")
    private Double balanceAmount = 0.0;

    public Double getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(Double balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    @Column(name = "membership_plan", length = 32)
    private String membershipPlan = "free";

    @Column(name = "membership_cycle", length = 16)
    private String membershipCycle = "monthly";

    @Column(name = "membership_expires_at")
    private LocalDateTime membershipExpiresAt;

    @Column(name = "review_quota")
    private Integer reviewQuota = 90;

    @Column(name = "review_used")
    private Integer reviewUsed = 0;

    @Column(name = "ppt_quota")
    private Integer pptQuota = 0;

    @Column(name = "ppt_used")
    private Integer pptUsed = 0;

    @Column(name = "chat_quota")
    private Integer chatQuota = 150;

    @Column(name = "chat_used")
    private Integer chatUsed = 0;

    @Column(name = "research_quota")
    private Integer researchQuota = 90;

    @Column(name = "research_used")
    private Integer researchUsed = 0;

    @Column(name = "report_quota")
    private Integer reportQuota = 1;

    @Column(name = "report_used")
    private Integer reportUsed = 0;

    @Column(name = "translate_quota")
    private Integer translateQuota = 5;

    @Column(name = "translate_used")
    private Integer translateUsed = 0;

    @Column(name = "immersive_quota")
    private Integer immersiveQuota = 3;

    @Column(name = "immersive_used")
    private Integer immersiveUsed = 0;

    @Column(name = "translate_one_off_quota")
    private Integer translateOneOffQuota = 0;

    @Column(name = "immersive_one_off_quota")
    private Integer immersiveOneOffQuota = 0;

    @Column(name = "translate_daily_quota_base")
    private Integer translateDailyQuotaBase;

    @Column(name = "immersive_daily_quota_base")
    private Integer immersiveDailyQuotaBase;

    @Column(name = "translate_daily_bonus")
    private Integer translateDailyBonus = 0;

    @Column(name = "translate_daily_bonus_date")
    private LocalDate translateDailyBonusDate;

    @Column(name = "immersive_daily_bonus")
    private Integer immersiveDailyBonus = 0;

    @Column(name = "immersive_daily_bonus_date")
    private LocalDate immersiveDailyBonusDate;

    @Column(name = "fruit_score")
    private Integer fruitScore = 0;

    @Column(name = "checkin_score")
    private Integer checkinScore = 0;

    public String getMembershipPlan() { return membershipPlan; }
    public void setMembershipPlan(String membershipPlan) { this.membershipPlan = membershipPlan; }
    public String getMembershipCycle() { return membershipCycle; }
    public void setMembershipCycle(String membershipCycle) { this.membershipCycle = membershipCycle; }
    public LocalDateTime getMembershipExpiresAt() { return membershipExpiresAt; }
    public void setMembershipExpiresAt(LocalDateTime membershipExpiresAt) { this.membershipExpiresAt = membershipExpiresAt; }
    public Integer getReviewQuota() { return reviewQuota; }
    public void setReviewQuota(Integer reviewQuota) { this.reviewQuota = reviewQuota; }
    public Integer getReviewUsed() { return reviewUsed; }
    public void setReviewUsed(Integer reviewUsed) { this.reviewUsed = reviewUsed; }
    public Integer getPptQuota() { return pptQuota; }
    public void setPptQuota(Integer pptQuota) { this.pptQuota = pptQuota; }
    public Integer getPptUsed() { return pptUsed; }
    public void setPptUsed(Integer pptUsed) { this.pptUsed = pptUsed; }
    public Integer getChatQuota() { return chatQuota; }
    public void setChatQuota(Integer chatQuota) { this.chatQuota = chatQuota; }
    public Integer getChatUsed() { return chatUsed; }
    public void setChatUsed(Integer chatUsed) { this.chatUsed = chatUsed; }
    public Integer getResearchQuota() { return researchQuota; }
    public void setResearchQuota(Integer researchQuota) { this.researchQuota = researchQuota; }
    public Integer getResearchUsed() { return researchUsed; }
    public void setResearchUsed(Integer researchUsed) { this.researchUsed = researchUsed; }
    public Integer getReportQuota() { return reportQuota; }
    public void setReportQuota(Integer reportQuota) { this.reportQuota = reportQuota; }
    public Integer getReportUsed() { return reportUsed; }
    public void setReportUsed(Integer reportUsed) { this.reportUsed = reportUsed; }
    public Integer getTranslateQuota() { return translateQuota; }
    public void setTranslateQuota(Integer translateQuota) { this.translateQuota = translateQuota; }
    public Integer getTranslateUsed() { return translateUsed; }
    public void setTranslateUsed(Integer translateUsed) { this.translateUsed = translateUsed; }
    public Integer getImmersiveQuota() { return immersiveQuota; }
    public void setImmersiveQuota(Integer immersiveQuota) { this.immersiveQuota = immersiveQuota; }
    public Integer getImmersiveUsed() { return immersiveUsed; }
    public void setImmersiveUsed(Integer immersiveUsed) { this.immersiveUsed = immersiveUsed; }
    public Integer getTranslateOneOffQuota() { return translateOneOffQuota; }
    public void setTranslateOneOffQuota(Integer translateOneOffQuota) { this.translateOneOffQuota = translateOneOffQuota; }
    public Integer getImmersiveOneOffQuota() { return immersiveOneOffQuota; }
    public void setImmersiveOneOffQuota(Integer immersiveOneOffQuota) { this.immersiveOneOffQuota = immersiveOneOffQuota; }
    public Integer getTranslateDailyQuotaBase() { return translateDailyQuotaBase; }
    public void setTranslateDailyQuotaBase(Integer translateDailyQuotaBase) { this.translateDailyQuotaBase = translateDailyQuotaBase; }
    public Integer getImmersiveDailyQuotaBase() { return immersiveDailyQuotaBase; }
    public void setImmersiveDailyQuotaBase(Integer immersiveDailyQuotaBase) { this.immersiveDailyQuotaBase = immersiveDailyQuotaBase; }
    public Integer getTranslateDailyBonus() { return translateDailyBonus; }
    public void setTranslateDailyBonus(Integer translateDailyBonus) { this.translateDailyBonus = translateDailyBonus; }
    public LocalDate getTranslateDailyBonusDate() { return translateDailyBonusDate; }
    public void setTranslateDailyBonusDate(LocalDate translateDailyBonusDate) { this.translateDailyBonusDate = translateDailyBonusDate; }
    public Integer getImmersiveDailyBonus() { return immersiveDailyBonus; }
    public void setImmersiveDailyBonus(Integer immersiveDailyBonus) { this.immersiveDailyBonus = immersiveDailyBonus; }
    public LocalDate getImmersiveDailyBonusDate() { return immersiveDailyBonusDate; }
    public void setImmersiveDailyBonusDate(LocalDate immersiveDailyBonusDate) { this.immersiveDailyBonusDate = immersiveDailyBonusDate; }
    public Integer getFruitScore() { return fruitScore; }
    public void setFruitScore(Integer fruitScore) { this.fruitScore = fruitScore; }
    public Integer getCheckinScore() { return checkinScore; }
    public void setCheckinScore(Integer checkinScore) { this.checkinScore = checkinScore; }

    @Column(name = "last_ip", length = 128)
    private String lastIp;

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    @Column(name = "active_time")
    private Long activeTime = 0L;

    public Long getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(Long activeTime) {
        this.activeTime = activeTime;
    }

    @Column(name = "team_id")
    private Long teamId;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    @Column(name = "avatar_url", columnDefinition = "LONGTEXT")
    private String avatarUrl;

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @Column(name = "background_url", columnDefinition = "LONGTEXT")
    private String backgroundUrl;

    public String getBackgroundUrl() {
        return backgroundUrl;
    }

    public void setBackgroundUrl(String backgroundUrl) {
        this.backgroundUrl = backgroundUrl;
    }

    @Column(name = "school_name", length = 128)
    private String schoolName;

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    @Column(name = "campus_verified")
    private Boolean campusVerified = false;

    public boolean isCampusVerified() {
        return Boolean.TRUE.equals(campusVerified);
    }

    public void setCampusVerified(boolean campusVerified) {
        this.campusVerified = campusVerified;
    }

    @Column(name = "qq_openid", length = 128, unique = true)
    private String qqOpenid;

    @Column(name = "machine_id", length = 128)
    private String machineId;

    public String getQqOpenid() {
        return qqOpenid;
    }

    public void setQqOpenid(String qqOpenid) {
        this.qqOpenid = qqOpenid;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    @Column(name = "qq", length = 32)
    private String qq;

    @Column(name = "wechat", length = 64)
    private String wechat;

    public String getQq() {
        return qq;
    }

    public void setQq(String qq) {
        this.qq = qq;
    }

    public String getWechat() {
        return wechat;
    }

    public void setWechat(String wechat) {
        this.wechat = wechat;
    }

    @Column(name = "numeric_id", unique = true)
    private Integer numericId;

    public Integer getNumericId() {
        if (numericId == null && id != null) {
            int h = id.hashCode();
            h ^= (h >>> 16);
            h *= 0x85ebca6b;
            h ^= (h >>> 13);
            h *= 0xc2b2ae35;
            h ^= (h >>> 16);
            numericId = 100000 + Math.abs(h) % 900000;
        }
        return numericId;
    }

    public void setNumericId(Integer numericId) {
        this.numericId = numericId;
    }

    @Column(name = "membership_stack", length = 2000)
    private String membershipStack;

    @Column(name = "last_membership_evaluation_time")
    private LocalDateTime lastMembershipEvaluationTime;

    public String getMembershipStack() {
        return membershipStack;
    }

    public void setMembershipStack(String membershipStack) {
        this.membershipStack = membershipStack;
    }

    public LocalDateTime getLastMembershipEvaluationTime() {
        return lastMembershipEvaluationTime;
    }

    public void setLastMembershipEvaluationTime(LocalDateTime lastMembershipEvaluationTime) {
        this.lastMembershipEvaluationTime = lastMembershipEvaluationTime;
    }
}
