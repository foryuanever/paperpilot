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
    Optional<AppUserEntity> findByQqOpenid(String qqOpenid);
    Optional<AppUserEntity> findFirstByMachineId(String machineId);
    List<AppUserEntity> findAllByOrderByCreatedAtDescIdDesc();
    List<AppUserEntity> findByTeamIdOrderByCreatedAtAsc(Long teamId);
    List<AppUserEntity> findAllByIdNotOrderByUsernameAsc(Long id);
    long countByMachineId(String machineId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AppUserEntity u set u.fruitScore = case when (coalesce(u.fruitScore, 0) - :deduction) < 0 then 0 else (coalesce(u.fruitScore, 0) - :deduction) end where u.id = :userId")
    int deductPoints(Long userId, int deduction);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AppUserEntity u set u.fruitScore = u.fruitScore - :amount where u.id = :userId and u.fruitScore >= :amount")
    int spendQuizPoints(Long userId, int amount);

    @Modifying
    @Query("update AppUserEntity u set u.fruitScore = 0")
    int resetAllFruitScores();

    @Modifying
    @Query("update AppUserEntity u set u.plainPassword = null where u.plainPassword is not null")
    int clearPlainPasswords();
}
