package com.paperpilot.server.repository;

import com.paperpilot.server.entity.MeetingReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MeetingReportRepository extends JpaRepository<MeetingReportEntity, Long> {
    Optional<MeetingReportEntity> findByUserIdAndWorkspaceId(Long userId, String workspaceId);
}
