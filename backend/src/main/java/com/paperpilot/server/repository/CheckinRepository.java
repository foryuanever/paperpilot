package com.paperpilot.server.repository;

import com.paperpilot.server.entity.CheckinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckinRepository extends JpaRepository<CheckinEntity, Long> {
    List<CheckinEntity> findAllByDate(String date);
    Optional<CheckinEntity> findByMemberIdAndDate(String memberId, String date);
}
