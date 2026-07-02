package com.paperpilot.server.repository;

import com.paperpilot.server.entity.RechargeRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RechargeRecordRepository extends JpaRepository<RechargeRecordEntity, Long> {
}
