package com.paperpilot.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_order")
public class PaymentOrderEntity {
    @Id
    @Column(name = "order_no", length = 64)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double amount = 0.0;

    @Column(nullable = false, length = 32)
    private String provider = "";

    @Column(name = "plan_id", length = 32)
    private String planId = "custom-recharge";

    @Column(name = "plan_cycle", length = 16)
    private String planCycle = "";

    @Column(nullable = false, length = 32)
    private String status = "created";

    @Column(name = "payment_url", length = 1000)
    private String paymentUrl = "";

    @Column(name = "platform_order_no", length = 128)
    private String platformOrderNo = "";

    @Column(name = "actual_pay_amount")
    private Double actualPayAmount = 0.0;

    @Column(length = 500)
    private String message = "";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "notify_payload", length = 2000)
    private String notifyPayload = "";

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getPlanCycle() { return planCycle; }
    public void setPlanCycle(String planCycle) { this.planCycle = planCycle; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    public String getPlatformOrderNo() { return platformOrderNo; }
    public void setPlatformOrderNo(String platformOrderNo) { this.platformOrderNo = platformOrderNo; }
    public Double getActualPayAmount() { return actualPayAmount; }
    public void setActualPayAmount(Double actualPayAmount) { this.actualPayAmount = actualPayAmount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getNotifyPayload() { return notifyPayload; }
    public void setNotifyPayload(String notifyPayload) { this.notifyPayload = notifyPayload; }
}
