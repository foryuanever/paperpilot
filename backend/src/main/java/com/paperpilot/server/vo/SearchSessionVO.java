package com.paperpilot.server.vo;

import java.util.List;

public class SearchSessionVO {

    private String engineId;
    private String engineName;
    private String url;
    private String query;
    private String journal;
    private String author;
    private List<String> suggestions;

    public SearchSessionVO(
        String engineId,
        String engineName,
        String url,
        String query,
        String journal,
        String author,
        List<String> suggestions
    ) {
        this.engineId = engineId;
        this.engineName = engineName;
        this.url = url;
        this.query = query;
        this.journal = journal;
        this.author = author;
        this.suggestions = suggestions;
    }

    public String getEngineId() {
        return engineId;
    }

    public String getEngineName() {
        return engineName;
    }

    public String getUrl() {
        return url;
    }

    public String getQuery() {
        return query;
    }

    public String getJournal() {
        return journal;
    }

    public String getAuthor() {
        return author;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }
}
