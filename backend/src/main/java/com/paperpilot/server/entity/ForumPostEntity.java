package com.paperpilot.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_post")
public class ForumPostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false, length = 128)
    private String author;

    @Column(length = 32)
    private String avatar;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "post_type", length = 32)
    private String postType;

    @Column(length = 24)
    private String visibility = "public";

    @Column(length = 64)
    private String discipline;

    @Column(name = "research_area", length = 128)
    private String researchArea;

    @Column(length = 1000)
    private String tags;

    @Column(name = "paper_title", length = 512)
    private String paperTitle;

    @Column(name = "publish_year", length = 16)
    private String publishYear;

    @Column(name = "venue_name", length = 255)
    private String venueName;

    @Column(name = "venue_level", length = 64)
    private String venueLevel;

    @Column(name = "resource_link", length = 1000)
    private String resourceLink;

    @Column(name = "images_json", columnDefinition = "LONGTEXT")
    private String imagesJson;

    @Column(name = "attachments_json", columnDefinition = "LONGTEXT")
    private String attachmentsJson;

    @Column(nullable = false)
    private Integer likes = 0;

    @Column(nullable = false)
    private Integer bookmarks = 0;

    @Column(nullable = false)
    private Integer views = 0;

    @Column(name = "has_liked", nullable = false)
    private boolean hasLiked = false;

    @Column(name = "has_bookmarked", nullable = false)
    private boolean hasBookmarked = false;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(nullable = false)
    private boolean banned = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (postType == null || postType.isBlank()) postType = "研究讨论";
        if (visibility == null || visibility.isBlank()) visibility = "public";
        if (discipline == null || discipline.isBlank()) discipline = "计算机科学";
        if (researchArea == null || researchArea.isBlank()) researchArea = "通用研究";
        if (tags == null) tags = "";
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getDiscipline() { return discipline; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }
    public String getResearchArea() { return researchArea; }
    public void setResearchArea(String researchArea) { this.researchArea = researchArea; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getPaperTitle() { return paperTitle; }
    public void setPaperTitle(String paperTitle) { this.paperTitle = paperTitle; }
    public String getPublishYear() { return publishYear; }
    public void setPublishYear(String publishYear) { this.publishYear = publishYear; }
    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    public String getVenueLevel() { return venueLevel; }
    public void setVenueLevel(String venueLevel) { this.venueLevel = venueLevel; }
    public String getResourceLink() { return resourceLink; }
    public void setResourceLink(String resourceLink) { this.resourceLink = resourceLink; }
    public String getImagesJson() { return imagesJson; }
    public void setImagesJson(String imagesJson) { this.imagesJson = imagesJson; }
    public String getAttachmentsJson() { return attachmentsJson; }
    public void setAttachmentsJson(String attachmentsJson) { this.attachmentsJson = attachmentsJson; }
    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }
    public Integer getBookmarks() { return bookmarks; }
    public void setBookmarks(Integer bookmarks) { this.bookmarks = bookmarks; }
    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }
    public boolean isHasLiked() { return hasLiked; }
    public void setHasLiked(boolean hasLiked) { this.hasLiked = hasLiked; }
    public boolean isHasBookmarked() { return hasBookmarked; }
    public void setHasBookmarked(boolean hasBookmarked) { this.hasBookmarked = hasBookmarked; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
