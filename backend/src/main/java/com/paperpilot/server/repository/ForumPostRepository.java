package com.paperpilot.server.repository;

import com.paperpilot.server.entity.ForumPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPostEntity, Long> {
    List<ForumPostEntity> findAllByOrderByCreatedAtDesc();
    List<ForumPostEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
