package com.paperpilot.server.repository;

import com.paperpilot.server.entity.BackendJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BackendJobRepository extends JpaRepository<BackendJobEntity, Long> {
    Optional<BackendJobEntity> findByJobKey(String jobKey);
}
