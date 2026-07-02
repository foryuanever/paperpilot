package com.paperpilot.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "model_config")
public class ModelConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "provider_name", nullable = false, length = 64)
    private String providerName;

    @Column(name = "base_url", nullable = false, length = 255)
    private String baseUrl;

    @Column(name = "api_key", nullable = false, length = 255)
    private String apiKey;

    @Column(name = "api_key_masked", nullable = false, length = 128)
    private String apiKeyMasked;

    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;

    @Column(name = "api_format", length = 32)
    private String apiFormat = "openai_chat";

    @Column(name = "auth_type", length = 32)
    private String authType = "bearer";

    @Column(name = "is_full_url", nullable = false)
    private boolean fullUrl;

    @Column(name = "models_url", length = 500)
    private String modelsUrl;

    @Column(name = "custom_user_agent", length = 255)
    private String customUserAgent;

    @Column(nullable = false, length = 32)
    private String scene;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
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

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKeyMasked() { return apiKeyMasked; }
    public void setApiKeyMasked(String apiKeyMasked) { this.apiKeyMasked = apiKeyMasked; }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getApiFormat() {
        return apiFormat;
    }

    public void setApiFormat(String apiFormat) {
        this.apiFormat = apiFormat;
    }

    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public boolean isFullUrl() { return fullUrl; }
    public void setFullUrl(boolean fullUrl) { this.fullUrl = fullUrl; }
    public String getModelsUrl() { return modelsUrl; }
    public void setModelsUrl(String modelsUrl) { this.modelsUrl = modelsUrl; }
    public String getCustomUserAgent() { return customUserAgent; }
    public void setCustomUserAgent(String customUserAgent) { this.customUserAgent = customUserAgent; }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
