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
@Table(name = "campus_verification")
public class CampusVerificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", length = 128)
    private String userName;

    @Column(length = 128)
    private String email;

    @Column(name = "school_name", nullable = false, length = 128)
    private String schoolName;

    @Column(name = "real_name", nullable = false, length = 64)
    private String realName;

    @Column(name = "student_card_front", columnDefinition = "LONGTEXT")
    private String studentCardFront;

    @Column(name = "chsi_screenshot", columnDefinition = "LONGTEXT")
    private String chsiScreenshot;

    @Column(nullable = false, length = 32)
    private String status = "pending";

    @Column(name = "admin_note", length = 500)
    private String adminNote = "";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "pending";
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }
    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getStudentCardFront() {
        return studentCardFront;
    }

    public void setStudentCardFront(String studentCardFront) {
        this.studentCardFront = studentCardFront;
    }

    public String getChsiScreenshot() {
        return chsiScreenshot;
    }

    public void setChsiScreenshot(String chsiScreenshot) {
        this.chsiScreenshot = chsiScreenshot;
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
