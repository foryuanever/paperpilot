package com.paperpilot.server.repository;

import com.paperpilot.server.entity.SearchSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchSessionRepository extends JpaRepository<SearchSessionEntity, Long> {
}
