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
@Table(name = "ai_usage_record")
public class AiUsageRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "model_name", nullable = false, length = 120)
    private String modelName = "";

    @Column(nullable = false, length = 64)
    private String scene = "";

    @Column(nullable = false, length = 64)
    private String action = "";

    @Column(name = "paper_title", length = 255)
    private String paperTitle = "";

    @Column(name = "prompt_tokens", nullable = false)
    private Long promptTokens = 0L;

    @Column(name = "completion_tokens", nullable = false)
    private Long completionTokens = 0L;

    @Column(name = "total_tokens", nullable = false)
    private Long totalTokens = 0L;

    @Column(name = "charge_amount", nullable = false)
    private Double chargeAmount = 0.0;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice = 0.02;

    @Column(name = "billing_multiplier", nullable = false)
    private Double billingMultiplier = 1.0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getPaperTitle() { return paperTitle; }
    public void setPaperTitle(String paperTitle) { this.paperTitle = paperTitle; }
    public Long getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Long promptTokens) { this.promptTokens = promptTokens; }
    public Long getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Long completionTokens) { this.completionTokens = completionTokens; }
    public Long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }
    public Double getChargeAmount() { return chargeAmount; }
    public void setChargeAmount(Double chargeAmount) { this.chargeAmount = chargeAmount; }
    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }
    public Double getBillingMultiplier() { return billingMultiplier; }
    public void setBillingMultiplier(Double billingMultiplier) { this.billingMultiplier = billingMultiplier; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
