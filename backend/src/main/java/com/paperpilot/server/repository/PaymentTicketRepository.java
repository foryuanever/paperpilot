package com.paperpilot.server.repository;

import com.paperpilot.server.entity.PaymentTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentTicketRepository extends JpaRepository<PaymentTicketEntity, Long> {
    List<PaymentTicketEntity> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
