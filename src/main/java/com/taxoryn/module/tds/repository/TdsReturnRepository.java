package com.taxoryn.module.tds.repository;

import com.taxoryn.module.tds.entity.TdsReturnEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFormType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TdsReturnRepository extends JpaRepository<TdsReturnEntity, UUID> {

    Optional<TdsReturnEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<TdsReturnEntity> findByOrganizationIdAndTdsProfileIdAndFormTypeAndQuarterAndFinancialYear(
            UUID organizationId,
            UUID tdsProfileId,
            TdsFormType formType,
            TdsQuarter quarter,
            String financialYear
    );

    List<TdsReturnEntity> findAllByOrganizationId(UUID organizationId);

    List<TdsReturnEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    List<TdsReturnEntity> findAllByOrganizationIdAndTdsProfileId(UUID organizationId, UUID tdsProfileId);

    List<TdsReturnEntity> findAllByOrganizationIdAndQuarterAndFinancialYear(
            UUID organizationId,
            TdsQuarter quarter,
            String financialYear
    );

    @Query("SELECT r FROM TdsReturnEntity r WHERE r.organizationId = :organizationId " +
           "AND (:clientId IS NULL OR r.clientId = :clientId) " +
           "AND (:tdsProfileId IS NULL OR r.tdsProfileId = :tdsProfileId) " +
           "AND (:formType IS NULL OR r.formType = :formType) " +
           "AND (:quarter IS NULL OR r.quarter = :quarter) " +
           "AND (:financialYear IS NULL OR r.financialYear = :financialYear) " +
           "AND (:filingStatus IS NULL OR r.filingStatus = :filingStatus) " +
           "AND (:assignedEmployeeId IS NULL OR r.assignedEmployeeId = :assignedEmployeeId)")
    Page<TdsReturnEntity> searchReturns(
            @Param("organizationId") UUID organizationId,
            @Param("clientId") UUID clientId,
            @Param("tdsProfileId") UUID tdsProfileId,
            @Param("formType") TdsFormType formType,
            @Param("quarter") TdsQuarter quarter,
            @Param("financialYear") String financialYear,
            @Param("filingStatus") TdsFilingStatus filingStatus,
            @Param("assignedEmployeeId") UUID assignedEmployeeId,
            Pageable pageable
    );

    @Query("SELECT r FROM TdsReturnEntity r WHERE r.organizationId = :organizationId " +
           "AND r.filingStatus NOT IN ('FILED', 'CANCELLED') " +
           "AND r.dueDate <= :thresholdDate ORDER BY r.dueDate ASC")
    List<TdsReturnEntity> findUpcomingReturns(
            @Param("organizationId") UUID organizationId,
            @Param("thresholdDate") LocalDate thresholdDate
    );

    @Query("SELECT r FROM TdsReturnEntity r WHERE r.organizationId = :organizationId " +
           "AND r.filingStatus NOT IN ('FILED', 'CANCELLED') " +
           "AND r.dueDate < :currentDate ORDER BY r.dueDate ASC")
    List<TdsReturnEntity> findOverdueReturns(
            @Param("organizationId") UUID organizationId,
            @Param("currentDate") LocalDate currentDate
    );

    long countByOrganizationIdAndFinancialYear(UUID organizationId, String financialYear);

    long countByOrganizationIdAndFilingStatus(UUID organizationId, TdsFilingStatus filingStatus);

    @Query("SELECT " +
           "COUNT(CASE WHEN r.filingStatus NOT IN ('FILED', 'CANCELLED') AND (r.dueDate IS NULL OR r.dueDate >= :today) THEN 1 END), " +
           "COUNT(CASE WHEN r.filingStatus = 'FILED' THEN 1 END), " +
           "COUNT(CASE WHEN r.filingStatus NOT IN ('FILED', 'CANCELLED') AND r.dueDate < :today THEN 1 END) " +
           "FROM TdsReturnEntity r WHERE r.organizationId = :organizationId")
    List<Object[]> getTdsDashboardStats(@Param("organizationId") UUID organizationId, @Param("today") LocalDate today);
}
