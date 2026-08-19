package com.taxoryn.module.gst.repository;

import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GstReturnFilingRepository extends JpaRepository<GstReturnFilingEntity, UUID>, JpaSpecificationExecutor<GstReturnFilingEntity> {

    Optional<GstReturnFilingEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<GstReturnFilingEntity> findByOrganizationIdAndGstProfileIdAndReturnTypeAndReturnPeriod(
            UUID organizationId, UUID gstProfileId, GstReturnType returnType, String returnPeriod);

    List<GstReturnFilingEntity> findAllByOrganizationIdAndGstProfileIdOrderByDueDateDesc(UUID organizationId, UUID gstProfileId);

    List<GstReturnFilingEntity> findAllByOrganizationIdAndClientIdOrderByDueDateDesc(UUID organizationId, UUID clientId);

    List<GstReturnFilingEntity> findAllByOrganizationIdAndReturnPeriod(UUID organizationId, String returnPeriod);

    long countByOrganizationIdAndReturnPeriodAndReturnTypeAndFilingStatus(
            UUID organizationId, String returnPeriod, GstReturnType returnType, GstFilingStatus filingStatus);

    boolean existsByOrganizationIdAndGstProfileIdAndReturnTypeAndReturnPeriod(
            UUID organizationId, UUID gstProfileId, GstReturnType returnType, String returnPeriod);

    /**
     * Filings due within a date window and not yet in a terminal status, for GST_DUE reminders.
     */
    java.util.List<GstReturnFilingEntity> findAllByOrganizationIdAndDueDateBetweenAndFilingStatusNotIn(
            UUID organizationId, java.time.LocalDate fromDate, java.time.LocalDate toDate, java.util.Collection<GstFilingStatus> excludedStatuses);

    @org.springframework.data.jpa.repository.Query("SELECT " +
           "SUM(CASE WHEN f.filingStatus IN (com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.PENDING, com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.PREPARED, com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.UNDER_REVIEW) THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN f.filingStatus = com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.OVERDUE OR (f.filingStatus IN (com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.PENDING, com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.PREPARED, com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.UNDER_REVIEW) AND f.dueDate < :currentDate) THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN f.filingStatus = com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus.FILED THEN 1L ELSE 0L END) " +
           "FROM GstReturnFilingEntity f WHERE f.organizationId = :organizationId")
    List<Object[]> getGstDashboardStats(@org.springframework.data.repository.query.Param("organizationId") UUID organizationId, @org.springframework.data.repository.query.Param("currentDate") java.time.LocalDate currentDate);
}
