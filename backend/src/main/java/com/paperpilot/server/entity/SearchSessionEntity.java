package com.paperpilot.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_session")
public class SearchSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "engine_id", length = 64)
    private String engineId;

    @Column(name = "engine_name", length = 128)
    private String engineName;

    @Column(length = 512)
    private String url;

    @Column(length = 512)
    private String query;

    @Column(length = 255)
    private String journal;

    @Column(length = 255)
    private String author;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setEngineId(String engineId) {
        this.engineId = engineId;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
