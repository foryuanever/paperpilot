package com.paperpilot.server.repository;

import com.paperpilot.server.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {
    Optional<TeamEntity> findByName(String name);
    Optional<TeamEntity> findByIdentifier(String identifier);
}
