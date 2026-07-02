package com.paperpilot.server.dto;

import jakarta.validation.constraints.NotBlank;

public class ModelConfigRequest {

    @NotBlank
    private String providerName;

    @NotBlank
    private String baseUrl;

    private String apiKey;

    private String modelName;

    private String apiFormat = "openai_chat";
    private String authType = "bearer";
    private boolean fullUrl;
    private String modelsUrl;
    private String customUserAgent;

    @NotBlank
    private String scene;

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
}
