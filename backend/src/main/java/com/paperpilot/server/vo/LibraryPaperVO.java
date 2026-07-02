package com.paperpilot.server.vo;

import java.util.List;

public class LibraryPaperVO {

    private String workspaceId;
    private String title;
    private String source;
    private String authors;
    private String progress;
    private String importance;
    private String note;
    private List<String> journalTags;
    private String venueType;
    private String venueRanking;
    private String publishYear;
    private String readAt;
    private String uploadedAt;
    private String paperUrl;
    private String sourceUrl;
    private String importSource;
    private String abstractText;

    public LibraryPaperVO(
        String workspaceId,
        String title,
        String source,
        String authors,
        String progress,
        String importance,
        String note,
        List<String> journalTags,
        String venueType,
        String venueRanking,
        String publishYear,
        String readAt,
        String uploadedAt,
        String paperUrl,
        String sourceUrl,
        String importSource,
        String abstractText
    ) {
        this.workspaceId = workspaceId;
        this.title = title;
        this.source = source;
        this.authors = authors;
        this.progress = progress;
        this.importance = importance;
        this.note = note;
        this.journalTags = journalTags;
        this.venueType = venueType;
        this.venueRanking = venueRanking;
        this.publishYear = publishYear;
        this.readAt = readAt;
        this.uploadedAt = uploadedAt;
        this.paperUrl = paperUrl;
        this.sourceUrl = sourceUrl;
        this.importSource = importSource;
        this.abstractText = abstractText;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getAuthors() {
        return authors;
    }

    public String getProgress() {
        return progress;
    }

    public String getImportance() {
        return importance;
    }

    public String getNote() {
        return note;
    }

    public List<String> getJournalTags() {
        return journalTags;
    }

    public String getVenueType() {
        return venueType;
    }

    public String getVenueRanking() {
        return venueRanking;
    }

    public String getPublishYear() {
        return publishYear;
    }

    public String getReadAt() {
        return readAt;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }

    public String getPaperUrl() {
        return paperUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getImportSource() {
        return importSource;
    }

    public String getAbstractText() {
        return abstractText;
    }
}
