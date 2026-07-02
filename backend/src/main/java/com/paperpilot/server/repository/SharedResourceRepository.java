package com.paperpilot.server.repository;

import com.paperpilot.server.entity.SharedResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SharedResourceRepository extends JpaRepository<SharedResourceEntity, Long> {
    List<SharedResourceEntity> findAllByOrderByCreatedAtDesc();
}
