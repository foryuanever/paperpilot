package com.paperpilot.server.repository;

import com.paperpilot.server.entity.SiteMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteMessageRepository extends JpaRepository<SiteMessageEntity, Long> {
    List<SiteMessageEntity> findByActiveFlagTrueOrderByCreatedAtDesc();
    List<SiteMessageEntity> findAllByOrderByCreatedAtDesc();
}
