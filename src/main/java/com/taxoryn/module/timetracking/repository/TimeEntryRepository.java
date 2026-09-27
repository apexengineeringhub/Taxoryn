package com.taxoryn.module.timetracking.repository;

import com.taxoryn.module.timetracking.entity.TimeEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntryEntity, UUID>, JpaSpecificationExecutor<TimeEntryEntity> {

    Optional<TimeEntryEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<TimeEntryEntity> findAllByOrganizationIdAndClientIdOrderByEntryDateDesc(UUID organizationId, UUID clientId);

    List<TimeEntryEntity> findAllByOrganizationIdAndEngagementIdOrderByEntryDateDesc(UUID organizationId, UUID engagementId);

    List<TimeEntryEntity> findAllByOrganizationIdAndWorkItemIdOrderByEntryDateDesc(UUID organizationId, UUID workItemId);

    List<TimeEntryEntity> findAllByOrganizationIdAndTaskIdOrderByEntryDateDesc(UUID organizationId, UUID taskId);

    List<TimeEntryEntity> findAllByOrganizationIdAndUserIdAndEntryDateBetween(UUID organizationId, UUID userId, LocalDate start, LocalDate end);

    List<TimeEntryEntity> findAllByOrganizationIdAndIdIn(UUID organizationId, List<UUID> ids);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM TimeEntryEntity t WHERE t.organizationId = :organizationId " +
            "AND t.billable = true AND t.status != com.taxoryn.module.timetracking.model.TimeEntryStatus.BILLED " +
            "AND (:clientId IS NULL OR t.clientId = :clientId) " +
            "AND (:engagementId IS NULL OR t.engagementId = :engagementId) " +
            "AND (:startDate IS NULL OR t.entryDate >= :startDate) " +
            "AND (:endDate IS NULL OR t.entryDate <= :endDate) " +
            "ORDER BY t.entryDate ASC")
    List<TimeEntryEntity> findUnbilledEntries(
            @org.springframework.data.repository.query.Param("organizationId") UUID organizationId,
            @org.springframework.data.repository.query.Param("clientId") UUID clientId,
            @org.springframework.data.repository.query.Param("engagementId") UUID engagementId,
            @org.springframework.data.repository.query.Param("startDate") LocalDate startDate,
            @org.springframework.data.repository.query.Param("endDate") LocalDate endDate
    );
}
