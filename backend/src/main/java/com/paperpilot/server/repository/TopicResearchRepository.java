package com.paperpilot.server.repository;

import com.paperpilot.server.entity.TopicResearchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicResearchRepository extends JpaRepository<TopicResearchEntity, Long> {
    List<TopicResearchEntity> findAllByOrderByCreatedAtDesc();
}
