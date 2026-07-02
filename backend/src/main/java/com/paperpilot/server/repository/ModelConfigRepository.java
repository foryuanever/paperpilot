package com.paperpilot.server.repository;

import com.paperpilot.server.entity.ModelConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModelConfigRepository extends JpaRepository<ModelConfigEntity, Long> {

    Optional<ModelConfigEntity> findFirstByUserIdAndActiveTrueOrderByUpdatedAtDesc(Long userId);

    Optional<ModelConfigEntity> findFirstByActiveTrueOrderByUpdatedAtDesc();

    Optional<ModelConfigEntity> findFirstBySceneAndActiveTrueOrderByUpdatedAtDesc(String scene);

    Optional<ModelConfigEntity> findFirstByProviderNameIgnoreCaseAndBaseUrlIgnoreCase(String providerName, String baseUrl);

    Optional<ModelConfigEntity> findFirstByProviderNameIgnoreCaseAndBaseUrlIgnoreCaseAndScene(String providerName, String baseUrl, String scene);

    List<ModelConfigEntity> findAllByOrderByActiveDescUpdatedAtDesc();

    List<ModelConfigEntity> findAllBySceneOrderByActiveDescUpdatedAtDesc(String scene);

    List<ModelConfigEntity> findByActiveFalse();

    List<ModelConfigEntity> findBySceneAndActiveFalse(String scene);
}
