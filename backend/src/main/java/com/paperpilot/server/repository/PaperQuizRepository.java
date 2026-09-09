package com.paperpilot.server.repository;
import com.paperpilot.server.entity.PaperQuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PaperQuizRepository extends JpaRepository<PaperQuizEntity, String> {
    Optional<PaperQuizEntity> findFirstByUserIdAndWorkspaceIdOrderByCreatedAtDesc(Long userId, String workspaceId);
}
