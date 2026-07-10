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
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
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
    private String role = "学生";

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Column(name = "plain_password", length = 255)
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
    private Integer reviewQuota = 0;

    @Column(name = "review_used")
    private Integer reviewUsed = 0;

    @Column(name = "ppt_quota")
    private Integer pptQuota = 0;

    @Column(name = "ppt_used")
    private Integer pptUsed = 0;

    @Column(name = "chat_quota")
    private Integer chatQuota = 0;

    @Column(name = "chat_used")
    private Integer chatUsed = 0;

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
}
