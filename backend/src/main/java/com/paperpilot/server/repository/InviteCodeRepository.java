package com.paperpilot.server.repository;

import com.paperpilot.server.entity.InviteCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteCodeRepository extends JpaRepository<InviteCodeEntity, Long> {

    Optional<InviteCodeEntity> findByCodeAndActiveTrue(String code);
}
