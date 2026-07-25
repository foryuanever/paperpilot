package com.paperpilot.server.repository;

import com.paperpilot.server.entity.VerificationCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCodeEntity, Long> {
    Optional<VerificationCodeEntity> findByEmailAndPurpose(String email, String purpose);
    void deleteByEmailAndPurpose(String email, String purpose);
}
