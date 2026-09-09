package com.paperpilot.server.repository;

import com.paperpilot.server.entity.PaperTranslationCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaperTranslationCacheRepository extends JpaRepository<PaperTranslationCacheEntity, Long> {
    Optional<PaperTranslationCacheEntity> findByUserIdAndWorkspaceIdAndMode(Long userId, String workspaceId, String mode);
}
