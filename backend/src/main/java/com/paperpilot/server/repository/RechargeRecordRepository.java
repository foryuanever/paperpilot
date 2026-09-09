package com.paperpilot.server.repository;

import com.paperpilot.server.entity.RechargeRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RechargeRecordRepository extends JpaRepository<RechargeRecordEntity, Long> {
    List<RechargeRecordEntity> findByEmailOrderByCreatedAtDesc(String email);
    List<RechargeRecordEntity> findAllByOrderByCreatedAtDescIdDesc();
    void deleteAllByEmail(String email);
}
