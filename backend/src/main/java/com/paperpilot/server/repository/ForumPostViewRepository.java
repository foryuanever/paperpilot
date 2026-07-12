package com.paperpilot.server.repository;

import com.paperpilot.server.entity.ForumPostViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ForumPostViewRepository extends JpaRepository<ForumPostViewEntity, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    @Query("""
        select v.userId, count(v.id)
        from ForumPostViewEntity v
        where v.createdAt >= :start and v.createdAt < :end
        group by v.userId
        order by count(v.id) desc
    """)
    List<Object[]> findDailyActiveUserStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
