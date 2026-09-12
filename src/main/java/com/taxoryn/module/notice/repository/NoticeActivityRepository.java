package com.taxoryn.module.notice.repository;

import com.taxoryn.module.notice.entity.NoticeActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoticeActivityRepository extends JpaRepository<NoticeActivityEntity, UUID> {

    List<NoticeActivityEntity> findAllByOrganizationIdAndNoticeIdOrderByCreatedAtDesc(UUID organizationId, UUID noticeId);
}
