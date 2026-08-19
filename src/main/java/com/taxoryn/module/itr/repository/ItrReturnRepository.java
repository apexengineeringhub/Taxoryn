package com.taxoryn.module.itr.repository;

import com.taxoryn.module.itr.entity.ItrReturnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItrReturnRepository extends JpaRepository<ItrReturnEntity, UUID>, JpaSpecificationExecutor<ItrReturnEntity> {

    Optional<ItrReturnEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ItrReturnEntity> findByOrganizationIdAndClientIdAndAssessmentYear(UUID organizationId, UUID clientId, String assessmentYear);

    boolean existsByOrganizationIdAndClientIdAndAssessmentYear(UUID organizationId, UUID clientId, String assessmentYear);

    List<ItrReturnEntity> findAllByOrganizationIdAndClientIdOrderByAssessmentYearDesc(UUID organizationId, UUID clientId);

    List<ItrReturnEntity> findAllByOrganizationIdAndAssessmentYear(UUID organizationId, String assessmentYear);

    /**
     * Returns due within a date window and not yet in a terminal status, for ITR_DUE reminders.
     */
    List<ItrReturnEntity> findAllByOrganizationIdAndDueDateBetweenAndStatusNotIn(
            UUID organizationId, java.time.LocalDate fromDate, java.time.LocalDate toDate, java.util.Collection<ItrReturnEntity.ItrStatus> excludedStatuses);

    @org.springframework.data.jpa.repository.Query("SELECT " +
           "SUM(CASE WHEN r.status IN (com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.DOCUMENTS_PENDING, com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.DATA_ENTRY, com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.UNDER_REVIEW, com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.READY_TO_FILE, com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.VERIFICATION_PENDING) THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN r.status IN (com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.FILED, com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.COMPLETED) THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN r.status IN (com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.DOCUMENTS_PENDING, com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.DATA_ENTRY, com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.UNDER_REVIEW, com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.READY_TO_FILE) AND r.dueDate < :currentDate THEN 1L ELSE 0L END) " +
           "FROM ItrReturnEntity r WHERE r.organizationId = :organizationId AND r.status != com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus.CANCELLED")
    List<Object[]> getItrDashboardStats(@org.springframework.data.repository.query.Param("organizationId") UUID organizationId, @org.springframework.data.repository.query.Param("currentDate") java.time.LocalDate currentDate);
}
