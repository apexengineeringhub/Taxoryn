package com.taxoryn.module.notice.repository;

import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxNoticeRepository extends JpaRepository<TaxNoticeEntity, UUID>, JpaSpecificationExecutor<TaxNoticeEntity> {

    Optional<TaxNoticeEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<TaxNoticeEntity> findByOrganizationIdAndNoticeNumber(UUID organizationId, String noticeNumber);

    boolean existsByOrganizationIdAndNoticeNumber(UUID organizationId, String noticeNumber);

    boolean existsByOrganizationIdAndNoticeNumberAndIdNot(UUID organizationId, String noticeNumber, UUID id);

    Page<TaxNoticeEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Page<TaxNoticeEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId, Pageable pageable);

    List<TaxNoticeEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    List<TaxNoticeEntity> findAllByOrganizationIdAndClientIdAndStatusNotIn(UUID organizationId, UUID clientId, Collection<NoticeStatus> statuses);

    long countByOrganizationIdAndClientIdAndStatusNotIn(UUID organizationId, UUID clientId, Collection<NoticeStatus> statuses);

    long countByOrganizationIdAndStatusIn(UUID organizationId, Collection<NoticeStatus> statuses);

    long countByOrganizationIdAndStatusInAndResponseDueDateBefore(UUID organizationId, Collection<NoticeStatus> statuses, LocalDate date);

    long countByOrganizationIdAndStatusInAndResponseDueDate(UUID organizationId, Collection<NoticeStatus> statuses, LocalDate date);

    long countByOrganizationIdAndStatusInAndResponseDueDateBetween(UUID organizationId, Collection<NoticeStatus> statuses, LocalDate startDate, LocalDate endDate);

    long countByOrganizationIdAndStatusInAndPriority(UUID organizationId, Collection<NoticeStatus> statuses, NoticePriority priority);

    long countByOrganizationIdAndStatus(UUID organizationId, NoticeStatus status);

    long countByOrganizationIdAndHearingDateGreaterThanEqualAndStatusNotIn(UUID organizationId, LocalDate date, Collection<NoticeStatus> closedStatuses);

    @Query("SELECT n.department, COUNT(n) FROM TaxNoticeEntity n WHERE n.organizationId = :organizationId AND n.status NOT IN :closedStatuses GROUP BY n.department")
    List<Object[]> countActiveByDepartment(@Param("organizationId") UUID organizationId, @Param("closedStatuses") Collection<NoticeStatus> closedStatuses);

    @Query("SELECT n.status, COUNT(n) FROM TaxNoticeEntity n WHERE n.organizationId = :organizationId GROUP BY n.status")
    List<Object[]> countByStatusGrouped(@Param("organizationId") UUID organizationId);

    @Query("SELECT SUM(n.demandAmount) FROM TaxNoticeEntity n WHERE n.organizationId = :organizationId AND n.status NOT IN :closedStatuses AND n.demandAmount IS NOT NULL")
    BigDecimal sumActiveDemandAmount(@Param("organizationId") UUID organizationId, @Param("closedStatuses") Collection<NoticeStatus> closedStatuses);

    @Query("SELECT n FROM TaxNoticeEntity n WHERE n.organizationId = :organizationId AND n.hearingDate >= :startDate AND n.hearingDate <= :endDate ORDER BY n.hearingDate ASC")
    List<TaxNoticeEntity> findHearingsInDateRange(@Param("organizationId") UUID organizationId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT n FROM TaxNoticeEntity n WHERE n.organizationId = :organizationId AND n.responseDueDate >= :startDate AND n.responseDueDate <= :endDate ORDER BY n.responseDueDate ASC")
    List<TaxNoticeEntity> findDeadlinesInDateRange(@Param("organizationId") UUID organizationId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<TaxNoticeEntity> findAllByOrganizationIdAndResponseDueDateBetweenAndStatusNotIn(UUID organizationId, LocalDate startDate, LocalDate endDate, Collection<NoticeStatus> statuses);

    List<TaxNoticeEntity> findAllByOrganizationIdAndResponseDueDateBeforeAndStatusNotIn(UUID organizationId, LocalDate date, Collection<NoticeStatus> statuses);
}
