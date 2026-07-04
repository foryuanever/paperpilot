package com.paperpilot.server.repository;

import com.paperpilot.server.entity.PaymentOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrderEntity, String> {
    List<PaymentOrderEntity> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
    List<PaymentOrderEntity> findTop80ByOrderByCreatedAtDesc();
}
