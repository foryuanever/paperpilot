package com.paperpilot.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TranslateRequest {

    @NotBlank
    @Size(max = 12000)
    private String text;

    private String sourceLang = "auto";

    private String targetLang = "zh-CN";

    @NotBlank
    private String provider = "google";

    private String usageScene = "";

    private String paperTitle = "";

    private String translationMode = "";

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getUsageScene() { return usageScene; }

    public void setUsageScene(String usageScene) { this.usageScene = usageScene; }

    public String getPaperTitle() { return paperTitle; }

    public void setPaperTitle(String paperTitle) { this.paperTitle = paperTitle; }

    public String getTranslationMode() { return translationMode; }

    public void setTranslationMode(String translationMode) { this.translationMode = translationMode; }
}
