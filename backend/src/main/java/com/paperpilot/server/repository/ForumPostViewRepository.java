package com.paperpilot.server.repository;

import com.paperpilot.server.entity.ForumPostViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumPostViewRepository extends JpaRepository<ForumPostViewEntity, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);
}
