package com.paperpilot.server.repository;

import com.paperpilot.server.entity.SystemLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SystemLogRepository extends JpaRepository<SystemLogEntity, Long> {
    List<SystemLogEntity> findAllByOrderByTimestampDesc();
}
