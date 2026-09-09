package com.paperpilot.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_vocabulary", indexes = {
    @Index(name = "idx_vocab_user_id", columnList = "user_id"),
    @Index(name = "idx_vocab_paper_id", columnList = "paper_id"),
    @Index(name = "idx_vocab_word", columnList = "word")
})
public class VocabularyEntity {

    @Id
    @Column(length = 64)
    public String id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false, length = 128)
    public String word;

    @Column(length = 128)
    public String phonetic;

    @Column(name = "audio_url", length = 512)
    public String audioUrl;

    @Column(name = "part_of_speech", length = 64)
    public String partOfSpeech;

    @Column(name = "meaning_cn", columnDefinition = "TEXT")
    public String meaningCn;

    @Column(name = "meaning_en", columnDefinition = "TEXT")
    public String meaningEn;

    @Column(name = "oxford_level", length = 32)
    public String oxfordLevel;

    @Column(name = "context_sentence", columnDefinition = "TEXT")
    public String contextSentence;

    @Column(name = "context_translation", columnDefinition = "TEXT")
    public String contextTranslation;

    @Column(name = "paper_id", length = 128)
    public String paperId;

    @Column(name = "paper_title", length = 512)
    public String paperTitle;

    @Column(name = "section_name", length = 128)
    public String sectionName;

    @Column(name = "collocations_json", columnDefinition = "TEXT")
    public String collocationsJson;

    @Column(name = "academic_examples_json", columnDefinition = "TEXT")
    public String academicExamplesJson;

    @Column(name = "synonyms_json", columnDefinition = "TEXT")
    public String synonymsJson;

    @Column(name = "antonyms_json", columnDefinition = "TEXT")
    public String antonymsJson;

    @Column(name = "etymology", columnDefinition = "TEXT")
    public String etymology;

    @Column(name = "mastery_level", nullable = false)
    public Integer masteryLevel = 0; // 0: 待学习, 1: 熟悉, 2: 已掌握

    @Column(name = "tags_json", length = 512)
    public String tagsJson;

    @Column(name = "review_count", nullable = false)
    public Integer reviewCount = 0;

    @Column(name = "last_reviewed_at")
    public LocalDateTime lastReviewedAt;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();

    public VocabularyEntity() {}
}
