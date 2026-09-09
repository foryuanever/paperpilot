package com.paperpilot.server.repository;

import com.paperpilot.server.entity.DeletedAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeletedAccountRepository extends JpaRepository<DeletedAccountEntity, Long> {
    boolean existsByQqOpenid(String qqOpenid);
    boolean existsByEmail(String email);
    Optional<DeletedAccountEntity> findFirstByQqOpenid(String qqOpenid);
    Optional<DeletedAccountEntity> findFirstByEmail(String email);
}
