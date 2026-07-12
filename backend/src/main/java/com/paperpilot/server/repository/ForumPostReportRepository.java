package com.paperpilot.server.repository;

import com.paperpilot.server.entity.ForumPostReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumPostReportRepository extends JpaRepository<ForumPostReportEntity, Long> {
    List<ForumPostReportEntity> findTop160ByOrderByCreatedAtDesc();
    long countByPostIdAndStatus(Long postId, String status);
}
