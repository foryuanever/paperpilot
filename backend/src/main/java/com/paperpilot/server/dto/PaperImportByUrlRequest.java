package com.paperpilot.server.dto;

import jakarta.validation.constraints.NotBlank;

public class PaperImportByUrlRequest {

    @NotBlank
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
