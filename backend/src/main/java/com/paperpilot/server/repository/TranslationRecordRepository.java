package com.paperpilot.server.repository;

import com.paperpilot.server.entity.TranslationRecordEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface TranslationRecordRepository extends JpaRepository<TranslationRecordEntity, Long> {
    void deleteAllByUserId(Long userId);

    List<TranslationRecordEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
        select r from TranslationRecordEntity r
        where r.success = false or r.latencyMs >= :slowMs
        order by r.createdAt desc
        """)
    List<TranslationRecordEntity> findProblemRecords(@Param("slowMs") long slowMs, Pageable pageable);

    long countBySuccessFalse();

    long countByLatencyMsGreaterThanEqual(long slowMs);

    long countByUserIdAndRouteAndSuccessTrueAndCreatedAtAfter(Long userId, String route, LocalDateTime createdAt);

    boolean existsByUserIdAndRouteAndSuccessTrueAndErrorMessage(Long userId, String route, String errorMessage);
}
