package com.paperpilot.server.vo;

import java.time.LocalDateTime;

public class ModelConfigVO {

    private String providerName;
    private String baseUrl;
    private String modelName;
    private String apiFormat;
    private String authType;
    private boolean fullUrl;
    private String modelsUrl;
    private String customUserAgent;
    private String scene;
    private LocalDateTime updatedAt;
    private boolean keyConfigured;

    public ModelConfigVO(
        String providerName,
        String baseUrl,
        String modelName,
        String apiFormat,
        String authType,
        boolean fullUrl,
        String modelsUrl,
        String customUserAgent,
        String scene,
        LocalDateTime updatedAt,
        boolean keyConfigured
    ) {
        this.providerName = providerName;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.apiFormat = apiFormat;
        this.authType = authType;
        this.fullUrl = fullUrl;
        this.modelsUrl = modelsUrl;
        this.customUserAgent = customUserAgent;
        this.scene = scene;
        this.updatedAt = updatedAt;
        this.keyConfigured = keyConfigured;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public String getApiFormat() {
        return apiFormat;
    }

    public String getAuthType() { return authType; }
    public boolean isFullUrl() { return fullUrl; }
    public String getModelsUrl() { return modelsUrl; }
    public String getCustomUserAgent() { return customUserAgent; }

    public String getScene() {
        return scene;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isKeyConfigured() {
        return keyConfigured;
    }
}
