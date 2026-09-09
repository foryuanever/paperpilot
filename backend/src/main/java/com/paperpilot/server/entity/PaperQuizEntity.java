package com.paperpilot.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "paper_quiz")
public class PaperQuizEntity {
    @Id public String id;
    @Column(nullable = false) public Long userId;
    @Column(nullable = false) public String workspaceId;
    @Column(length = 512) public String title;
    @Lob @Column(columnDefinition = "LONGTEXT") public String questions;
    @Lob @Column(columnDefinition = "LONGTEXT") public String sources;
    @Lob @Column(columnDefinition = "LONGTEXT") public String result;
    public LocalDateTime createdAt = LocalDateTime.now();
    @Version public Long version;
}
