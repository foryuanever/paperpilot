package com.paperpilot.server.vo;

import java.util.List;

public class SearchResultVO {

    private List<SearchPaperVO> items;
    private int total;
    private String source;
    private String message;
    private boolean official;
    private boolean requiresConfiguration;

    public SearchResultVO(List<SearchPaperVO> items, int total) {
        this.items = items;
        this.total = total;
        this.source = "";
        this.message = "";
        this.official = false;
        this.requiresConfiguration = false;
    }

    public List<SearchPaperVO> getItems() {
        return items;
    }

    public void setItems(List<SearchPaperVO> items) {
        this.items = items;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isOfficial() {
        return official;
    }

    public void setOfficial(boolean official) {
        this.official = official;
    }

    public boolean isRequiresConfiguration() {
        return requiresConfiguration;
    }

    public void setRequiresConfiguration(boolean requiresConfiguration) {
        this.requiresConfiguration = requiresConfiguration;
    }
}
