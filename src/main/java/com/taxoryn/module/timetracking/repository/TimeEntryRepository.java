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
}
