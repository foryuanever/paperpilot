package com.paperpilot.server.repository;

import com.paperpilot.server.entity.RequestMonitorRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestMonitorRecordRepository extends JpaRepository<RequestMonitorRecordEntity, Long> {
    List<RequestMonitorRecordEntity> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime start, LocalDateTime end);
}
