package com.paperpilot.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** A durable one-device-to-one-QQ binding, independent from a user's last device. */
@Entity
@Table(name = "qq_device_binding", uniqueConstraints = @UniqueConstraint(name = "uk_qq_device_machine", columnNames = "machine_id"))
public class QqDeviceBindingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_id", nullable = false, length = 128)
    private String machineId;

    @Column(name = "qq_openid", nullable = false, length = 128)
    private String qqOpenid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }
    public String getQqOpenid() { return qqOpenid; }
    public void setQqOpenid(String qqOpenid) { this.qqOpenid = qqOpenid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
