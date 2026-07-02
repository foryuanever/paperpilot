package com.paperpilot.server.repository;

import com.paperpilot.server.entity.FriendRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, Long> {
    Optional<FriendRequestEntity> findByRequesterIdAndRecipientId(Long requesterId, Long recipientId);
    List<FriendRequestEntity> findByRecipientIdAndStatusOrderByCreatedAtDesc(Long recipientId, String status);
    List<FriendRequestEntity> findByRequesterIdAndStatusOrderByCreatedAtDesc(Long requesterId, String status);

    @Query("""
        select r from FriendRequestEntity r
        where r.status = 'accepted'
          and ((r.requesterId = :userId and r.recipientId = :otherId)
            or (r.requesterId = :otherId and r.recipientId = :userId))
    """)
    Optional<FriendRequestEntity> findAccepted(@Param("userId") Long userId, @Param("otherId") Long otherId);

    @Query("""
        select r from FriendRequestEntity r
        where (r.requesterId = :userId and r.recipientId = :otherId)
           or (r.requesterId = :otherId and r.recipientId = :userId)
        order by r.createdAt desc
    """)
    List<FriendRequestEntity> findRelationship(@Param("userId") Long userId, @Param("otherId") Long otherId);
}
