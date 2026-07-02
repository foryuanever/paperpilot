package com.paperpilot.server.repository;

import com.paperpilot.server.entity.TranslationRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranslationRecordRepository extends JpaRepository<TranslationRecordEntity, Long> {
    void deleteAllByUserId(Long userId);
}
