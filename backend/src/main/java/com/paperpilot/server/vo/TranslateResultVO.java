package com.paperpilot.server.vo;

public class TranslateResultVO {

    private String provider;
    private String providerLabel;
    private String sourceLang;
    private String targetLang;
    private String translatedText;
    private boolean fallback;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderLabel() {
        return providerLabel;
    }

    public void setProviderLabel(String providerLabel) {
        this.providerLabel = providerLabel;
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

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public boolean isFallback() {
        return fallback;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }
}
