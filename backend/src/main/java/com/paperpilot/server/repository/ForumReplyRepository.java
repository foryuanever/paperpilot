package com.paperpilot.server.repository;

import com.paperpilot.server.entity.ForumReplyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumReplyRepository extends JpaRepository<ForumReplyEntity, Long> {
    List<ForumReplyEntity> findAllByPostIdOrderByCreatedAtAsc(Long postId);
    void deleteAllByPostId(Long postId);
}
