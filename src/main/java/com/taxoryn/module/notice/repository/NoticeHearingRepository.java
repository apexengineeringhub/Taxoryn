package com.taxoryn.module.notice.repository;

import com.taxoryn.module.notice.entity.NoticeHearingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoticeHearingRepository extends JpaRepository<NoticeHearingEntity, UUID> {

    List<NoticeHearingEntity> findAllByOrganizationIdAndNoticeIdOrderByHearingDateDesc(UUID organizationId, UUID noticeId);

    Optional<NoticeHearingEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationIdAndNoticeId(UUID organizationId, UUID noticeId);

    List<NoticeHearingEntity> findAllByOrganizationIdAndHearingDateBetweenOrderByHearingDateAsc(UUID organizationId, LocalDate startDate, LocalDate endDate);
}
