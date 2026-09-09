package com.paperpilot.server.repository;

import com.paperpilot.server.entity.PaymentTicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentTicketRepository extends JpaRepository<PaymentTicketEntity, Long> {
    List<PaymentTicketEntity> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
    List<PaymentTicketEntity> findTop80ByOrderByCreatedAtDesc();

    @Query("""
        select t from PaymentTicketEntity t
        where t.detail is not null and t.detail <> ''
          and t.orderNo is not null and t.orderNo <> ''
        """)
    Page<PaymentTicketEntity> findUsableTickets(Pageable pageable);
}
