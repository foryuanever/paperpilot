package com.paperpilot.server.repository;

import com.paperpilot.server.entity.VocabularyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface VocabularyRepository extends JpaRepository<VocabularyEntity, String> {

    List<VocabularyEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<VocabularyEntity> findAllByUserIdAndPaperIdOrderByCreatedAtDesc(Long userId, String paperId);

    Optional<VocabularyEntity> findFirstByUserIdAndWordIgnoreCase(Long userId, String word);

    @Modifying
    @Transactional
    @Query("DELETE FROM VocabularyEntity v WHERE v.id LIKE :prefix")
    int deleteDemoEntries(@Param("prefix") String prefix);

    long countByUserId(Long userId);

    long countByUserIdAndMasteryLevel(Long userId, Integer masteryLevel);

    @Query("SELECT v FROM VocabularyEntity v WHERE v.userId = :userId AND " +
           "(LOWER(v.word) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(v.meaningCn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(v.paperTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<VocabularyEntity> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
}
