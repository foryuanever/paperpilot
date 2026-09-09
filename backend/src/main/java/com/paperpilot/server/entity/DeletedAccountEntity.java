package com.paperpilot.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deleted_account", indexes = {
    @Index(name = "idx_deleted_account_qq", columnList = "qq_openid", unique = true),
    @Index(name = "idx_deleted_account_email", columnList = "email", unique = true)
})
public class DeletedAccountEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "qq_openid", length = 128)
    private String qqOpenid;
    @Column(length = 255)
    private String email;
    @Column(nullable = false)
    private LocalDateTime deletedAt;

    public Long getId() { return id; }
    public String getQqOpenid() { return qqOpenid; }
    public void setQqOpenid(String qqOpenid) { this.qqOpenid = qqOpenid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
