package com.paperpilot.server.dto;

import jakarta.validation.constraints.NotBlank;

public class PaperImportRequest {

    @NotBlank
    private String source;

    private String paperId;

    private String paperUrl;
    private String sourceUrl;
    private String importSource;
    private String articleType;
    private java.util.List<String> subjects;

    @NotBlank
    private String title;

    private String abstractText;
    private String authors;
    private String publishYear;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(String paperId) {
        this.paperId = paperId;
    }

    public String getPaperUrl() {
        return paperUrl;
    }

    public void setPaperUrl(String paperUrl) {
        this.paperUrl = paperUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getImportSource() {
        return importSource;
    }

    public void setImportSource(String importSource) {
        this.importSource = importSource;
    }

    public String getArticleType() {
        return articleType;
    }

    public void setArticleType(String articleType) {
        this.articleType = articleType;
    }

    public java.util.List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(java.util.List<String> subjects) {
        this.subjects = subjects;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(String publishYear) {
        this.publishYear = publishYear;
    }
}
