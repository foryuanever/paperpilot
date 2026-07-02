package com.paperpilot.server.repository;

import com.paperpilot.server.entity.PaperEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaperRepository extends JpaRepository<PaperEntity, Long> {

    List<PaperEntity> findByUserIdOrderByUploadedAtDescIdDesc(Long userId);

    Optional<PaperEntity> findByWorkspaceId(String workspaceId);

    long countByUserIdAndUploadedAt(Long userId, LocalDate uploadedAt);

    void deleteAllByUserId(Long userId);
}
