package com.paperpilot.server.repository;

import com.paperpilot.server.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {

    Optional<AppUserEntity> findByEmail(String email);
    Optional<AppUserEntity> findByUsername(String username);
    List<AppUserEntity> findByTeamIdOrderByCreatedAtAsc(Long teamId);
    List<AppUserEntity> findAllByIdNotOrderByUsernameAsc(Long id);

    @Modifying
    @Query("update AppUserEntity u set u.fruitScore = 0")
    int resetAllFruitScores();
}
