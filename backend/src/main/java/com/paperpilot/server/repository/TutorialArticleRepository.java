package com.paperpilot.server.repository;

import com.paperpilot.server.entity.TutorialArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TutorialArticleRepository extends JpaRepository<TutorialArticleEntity, Long> {
    List<TutorialArticleEntity> findByActiveFlagTrueOrderBySortOrderAscUpdatedAtDesc();
    List<TutorialArticleEntity> findAllByOrderBySortOrderAscUpdatedAtDesc();
}
