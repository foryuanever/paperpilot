package com.paperpilot.server.repository;

import com.paperpilot.server.entity.ReferralRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReferralRecordRepository extends JpaRepository<ReferralRecordEntity, Long> {
    List<ReferralRecordEntity> findAllByReferrerIdOrderByCreatedAtDesc(Long referrerId);
    List<ReferralRecordEntity> findAllByInviteeIdOrderByCreatedAtDesc(Long inviteeId);
    long countByReferrerId(Long referrerId);
    boolean existsByInviteeId(Long inviteeId);
    void deleteAllByReferrerId(Long userId);
    void deleteAllByInviteeId(Long userId);
}
