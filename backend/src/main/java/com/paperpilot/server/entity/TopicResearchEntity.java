package com.paperpilot.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "topic_research")
public class TopicResearchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(length = 64)
    private String discipline;

    @Column(length = 64)
    private String stage;

    @Column(length = 64)
    private String goal;

    @Column(length = 32)
    private String source = "官方";

    @Column(length = 1000)
    private String tags;

    @Column(name = "theme_clusters", length = 1000)
    private String themeClusters;

    @Column(name = "research_question", columnDefinition = "TEXT")
    private String researchQuestion;

    @Column(name = "research_gap", columnDefinition = "TEXT")
    private String researchGap;

    @Column(name = "method_route", columnDefinition = "TEXT")
    private String methodRoute;

    @Column(name = "risk_note", columnDefinition = "TEXT")
    private String riskNote;

    @Column(name = "representative_papers_json", columnDefinition = "LONGTEXT")
    private String representativePapersJson;

    @Column(name = "subtopics_json", columnDefinition = "LONGTEXT")
    private String subtopicsJson;

    @Column(name = "feasibility_score", nullable = false)
    private Integer feasibilityScore = 70;

    @Column(name = "innovation_score", nullable = false)
    private Integer innovationScore = 70;

    @Column(name = "difficulty_score", nullable = false)
    private Integer difficultyScore = 60;

    @Column(nullable = false)
    private Integer likes = 0;

    @Column(nullable = false)
    private Integer downloads = 0;

    @Column(name = "saved_by_user_ids", columnDefinition = "TEXT")
    private String savedByUserIds = "";

    @Column(name = "interested_by_user_ids", columnDefinition = "TEXT")
    private String interestedByUserIds = "";

    @Column(name = "model_name", length = 128)
    private String modelName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = createdAt;
        if (source == null || source.isBlank()) source = "官方";
        if (tags == null) tags = "";
        if (themeClusters == null) themeClusters = "";
        if (representativePapersJson == null) representativePapersJson = "[]";
        if (subtopicsJson == null) subtopicsJson = "[]";
        if (savedByUserIds == null) savedByUserIds = "";
        if (interestedByUserIds == null) interestedByUserIds = "";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDiscipline() { return discipline; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getThemeClusters() { return themeClusters; }
    public void setThemeClusters(String themeClusters) { this.themeClusters = themeClusters; }
    public String getResearchQuestion() { return researchQuestion; }
    public void setResearchQuestion(String researchQuestion) { this.researchQuestion = researchQuestion; }
    public String getResearchGap() { return researchGap; }
    public void setResearchGap(String researchGap) { this.researchGap = researchGap; }
    public String getMethodRoute() { return methodRoute; }
    public void setMethodRoute(String methodRoute) { this.methodRoute = methodRoute; }
    public String getRiskNote() { return riskNote; }
    public void setRiskNote(String riskNote) { this.riskNote = riskNote; }
    public String getRepresentativePapersJson() { return representativePapersJson; }
    public void setRepresentativePapersJson(String representativePapersJson) { this.representativePapersJson = representativePapersJson; }
    public String getSubtopicsJson() { return subtopicsJson; }
    public void setSubtopicsJson(String subtopicsJson) { this.subtopicsJson = subtopicsJson; }
    public Integer getFeasibilityScore() { return feasibilityScore; }
    public void setFeasibilityScore(Integer feasibilityScore) { this.feasibilityScore = feasibilityScore; }
    public Integer getInnovationScore() { return innovationScore; }
    public void setInnovationScore(Integer innovationScore) { this.innovationScore = innovationScore; }
    public Integer getDifficultyScore() { return difficultyScore; }
    public void setDifficultyScore(Integer difficultyScore) { this.difficultyScore = difficultyScore; }
    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }
    public Integer getDownloads() { return downloads; }
    public void setDownloads(Integer downloads) { this.downloads = downloads; }
    public String getSavedByUserIds() { return savedByUserIds; }
    public void setSavedByUserIds(String savedByUserIds) { this.savedByUserIds = savedByUserIds; }
    public String getInterestedByUserIds() { return interestedByUserIds; }
    public void setInterestedByUserIds(String interestedByUserIds) { this.interestedByUserIds = interestedByUserIds; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
