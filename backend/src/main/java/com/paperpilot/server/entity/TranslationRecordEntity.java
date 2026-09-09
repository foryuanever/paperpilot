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
@Table(name = "translation_record")
public class TranslationRecordEntity {

    @Column(length = 512)
    private String paperTitle;
    @Column(length = 64)
    private String translationMode;

    public String getPaperTitle() { return paperTitle; }
    public void setPaperTitle(String value) { paperTitle = value; }
    public String getTranslationMode() { return translationMode; }
    public void setTranslationMode(String value) { translationMode = value; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(length = 128)
    private String route;

    @Column(name = "source_lang", length = 32)
    private String sourceLang;

    @Column(name = "target_lang", length = 32)
    private String targetLang;

    @Column(name = "client_type", length = 64)
    private String clientType;

    @Column(name = "ip_address", length = 128)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "network_profile", length = 512)
    private String networkProfile;

    @Column(name = "char_count", nullable = false)
    private Long charCount = 0L;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs = 0L;

    @Column(nullable = false)
    private boolean success = true;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getSourceLang() {
        return sourceLang;
    }

    public void setSourceLang(String sourceLang) {
        this.sourceLang = sourceLang;
    }

    public String getTargetLang() {
        return targetLang;
    }

    public void setTargetLang(String targetLang) {
        this.targetLang = targetLang;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getNetworkProfile() {
        return networkProfile;
    }

    public void setNetworkProfile(String networkProfile) {
        this.networkProfile = networkProfile;
    }

    public Long getCharCount() {
        return charCount;
    }

    public void setCharCount(Long charCount) {
        this.charCount = charCount;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
