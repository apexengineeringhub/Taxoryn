package com.taxoryn.module.notice.repository;

import com.taxoryn.module.notice.entity.NoticeResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoticeResponseRepository extends JpaRepository<NoticeResponseEntity, UUID> {

    List<NoticeResponseEntity> findAllByOrganizationIdAndNoticeIdOrderByResponseVersionDesc(UUID organizationId, UUID noticeId);

    Optional<NoticeResponseEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<NoticeResponseEntity> findByIdAndNoticeIdAndOrganizationId(UUID id, UUID noticeId, UUID organizationId);

    Optional<NoticeResponseEntity> findByOrganizationIdAndNoticeIdAndResponseVersion(UUID organizationId, UUID noticeId, Integer responseVersion);

    @Query("SELECT MAX(r.responseVersion) FROM NoticeResponseEntity r WHERE r.organizationId = :organizationId AND r.noticeId = :noticeId")
    Integer findMaxVersionByNoticeId(@Param("organizationId") UUID organizationId, @Param("noticeId") UUID noticeId);

    long countByOrganizationIdAndNoticeId(UUID organizationId, UUID noticeId);
}
