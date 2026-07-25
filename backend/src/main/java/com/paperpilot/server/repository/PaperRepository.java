package com.paperpilot.server.repository;

import com.paperpilot.server.entity.PaperEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaperRepository extends JpaRepository<PaperEntity, Long> {

    List<PaperEntity> findByUserIdOrderByUploadedAtDescIdDesc(Long userId);

    Optional<PaperEntity> findByWorkspaceId(String workspaceId);

    long countByUserIdAndUploadedAt(Long userId, LocalDate uploadedAt);

    void deleteAllByUserId(Long userId);

    @Query("""
        select p from PaperEntity p
        where p.userId = :userId
          and (
            :keyword = ''
            or lower(p.title) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(p.abstractText, '')) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(p.source, '')) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(p.journalTags, '')) like lower(concat('%', :keyword, '%'))
          )
          and (
            :author = ''
            or lower(coalesce(p.authors, '')) like lower(concat('%', :author, '%'))
          )
        order by p.uploadedAt desc, p.id desc
    """)
    List<PaperEntity> searchUserPapers(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("author") String author);
}
