package com.paperpilot.server.repository;

import com.paperpilot.server.entity.QqDeviceBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QqDeviceBindingRepository extends JpaRepository<QqDeviceBindingEntity, Long> {
    Optional<QqDeviceBindingEntity> findByMachineId(String machineId);
}
