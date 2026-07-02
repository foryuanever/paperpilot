package com.paperpilot.server.repository;

import com.paperpilot.server.entity.DirectMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessageEntity, Long> {
    @Query("""
        select m from DirectMessageEntity m
        where (m.senderId = :userId and m.recipientId = :otherId)
           or (m.senderId = :otherId and m.recipientId = :userId)
        order by m.createdAt asc
    """)
    List<DirectMessageEntity> findConversation(@Param("userId") Long userId, @Param("otherId") Long otherId);

    long countByRecipientIdAndReadFlagFalse(Long recipientId);

    @Modifying
    @Query("""
        update DirectMessageEntity m set m.readFlag = true
        where m.recipientId = :userId and m.senderId = :otherId and m.readFlag = false
    """)
    int markRead(@Param("userId") Long userId, @Param("otherId") Long otherId);

    List<DirectMessageEntity> findByRecipientIdAndReadFlagFalseOrderByCreatedAtDesc(Long recipientId);
}
