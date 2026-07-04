package com.paperpilot.server.repository;

import com.paperpilot.server.entity.BillingSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingSettingRepository extends JpaRepository<BillingSettingEntity, String> {
}
