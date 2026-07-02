package com.paperpilot.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ModelChatRequest {

    @NotNull
    @Valid
    private ModelConfigRequest config;

    @NotBlank
    private String prompt;

    public ModelConfigRequest getConfig() {
        return config;
    }

    public void setConfig(ModelConfigRequest config) {
        this.config = config;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
