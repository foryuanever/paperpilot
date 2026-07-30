package com.paperpilot.server.repository;

import com.paperpilot.server.entity.MembershipPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlanEntity, String> {
    List<MembershipPlanEntity> findAllByOrderBySortOrderAscIdAsc();
}
