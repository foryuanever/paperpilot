package com.paperpilot.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "paper_record")
public class PaperEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false, length = 64, unique = true)
    private String workspaceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String source;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(length = 255)
    private String authors;

    @Column(name = "paper_url", columnDefinition = "LONGTEXT")
    private String paperUrl;

    @Column(name = "source_url", columnDefinition = "LONGTEXT")
    private String sourceUrl;

    @Column(name = "import_source", length = 128)
    private String importSource;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    @Column(length = 16)
    private String progress;

    @Column(length = 8)
    private String importance;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "journal_tags", length = 512)
    private String journalTags;

    @Column(name = "venue_type", length = 32)
    private String venueType;

    @Column(name = "venue_ranking", length = 64)
    private String venueRanking;

    @Column(name = "publish_year", length = 16)
    private String publishYear;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "uploaded_at")
    private LocalDate uploadedAt;

    @Column(length = 64)
    private String folder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (uploadedAt == null) {
            uploadedAt = LocalDate.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
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

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getImportance() {
        return importance;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getJournalTags() {
        return journalTags;
    }

    public void setJournalTags(String journalTags) {
        this.journalTags = journalTags;
    }

    public String getVenueType() {
        return venueType;
    }

    public void setVenueType(String venueType) {
        this.venueType = venueType;
    }

    public String getVenueRanking() {
        return venueRanking;
    }

    public void setVenueRanking(String venueRanking) {
        this.venueRanking = venueRanking;
    }

    public String getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(String publishYear) {
        this.publishYear = publishYear;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDate getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDate uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
