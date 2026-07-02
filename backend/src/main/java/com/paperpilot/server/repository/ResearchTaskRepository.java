package com.paperpilot.server.repository;

import com.paperpilot.server.entity.ResearchTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResearchTaskRepository extends JpaRepository<ResearchTaskEntity, Long> {
    List<ResearchTaskEntity> findAllByOrderByCreatedAtDesc();
}
