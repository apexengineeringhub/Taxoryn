package com.taxoryn.module.compliance.repository;

import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ComplianceWorkflowRepository extends JpaRepository<ComplianceWorkflowEntity, UUID>, JpaSpecificationExecutor<ComplianceWorkflowEntity> {

    Optional<ComplianceWorkflowEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ComplianceWorkflowEntity> findByComplianceObligationIdAndOrganizationId(UUID complianceObligationId, UUID organizationId);

    boolean existsByComplianceObligationIdAndOrganizationId(UUID complianceObligationId, UUID organizationId);

    List<ComplianceWorkflowEntity> findByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    List<ComplianceWorkflowEntity> findByOrganizationIdAndAssignedEmployeeId(UUID organizationId, UUID assignedEmployeeId);

    @Query("SELECT cw FROM ComplianceWorkflowEntity cw LEFT JOIN FETCH cw.checklistItems WHERE cw.id = :id AND cw.organizationId = :organizationId")
    Optional<ComplianceWorkflowEntity> findByIdWithChecklist(@Param("id") UUID id, @Param("organizationId") UUID organizationId);

    // Scoped metrics queries with optional accessible client IDs
    @Query("SELECT COUNT(cw) FROM ComplianceWorkflowEntity cw WHERE cw.organizationId = :orgId AND cw.workflowStatus NOT IN ('COMPLETED', 'CANCELLED') " +
           "AND (:accessibleClients IS NULL OR cw.clientId IN :accessibleClients)")
    long countActiveWorkflows(@Param("orgId") UUID orgId, @Param("accessibleClients") Set<UUID> accessibleClients);

    @Query("SELECT COUNT(cw) FROM ComplianceWorkflowEntity cw WHERE cw.organizationId = :orgId AND cw.workflowStatus NOT IN ('COMPLETED', 'CANCELLED') " +
           "AND cw.targetDate = :targetDate AND (:accessibleClients IS NULL OR cw.clientId IN :accessibleClients)")
    long countDueTodayWorkflows(@Param("orgId") UUID orgId, @Param("targetDate") LocalDate targetDate, @Param("accessibleClients") Set<UUID> accessibleClients);

    @Query("SELECT COUNT(cw) FROM ComplianceWorkflowEntity cw WHERE cw.organizationId = :orgId AND cw.workflowStatus NOT IN ('COMPLETED', 'CANCELLED') " +
           "AND cw.targetDate BETWEEN :startDate AND :endDate AND (:accessibleClients IS NULL OR cw.clientId IN :accessibleClients)")
    long countDueInRangeWorkflows(@Param("orgId") UUID orgId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("accessibleClients") Set<UUID> accessibleClients);

    @Query("SELECT COUNT(cw) FROM ComplianceWorkflowEntity cw WHERE cw.organizationId = :orgId AND cw.workflowStatus NOT IN ('COMPLETED', 'CANCELLED') " +
           "AND cw.targetDate < :cutoffDate AND (:accessibleClients IS NULL OR cw.clientId IN :accessibleClients)")
    long countOverdueWorkflows(@Param("orgId") UUID orgId, @Param("cutoffDate") LocalDate cutoffDate, @Param("accessibleClients") Set<UUID> accessibleClients);

    @Query("SELECT COUNT(cw) FROM ComplianceWorkflowEntity cw WHERE cw.organizationId = :orgId AND cw.waitingForClient = true " +
           "AND cw.workflowStatus NOT IN ('COMPLETED', 'CANCELLED') AND (:accessibleClients IS NULL OR cw.clientId IN :accessibleClients)")
    long countWaitingForClientWorkflows(@Param("orgId") UUID orgId, @Param("accessibleClients") Set<UUID> accessibleClients);

    @Query("SELECT COUNT(cw) FROM ComplianceWorkflowEntity cw WHERE cw.organizationId = :orgId AND cw.workflowStatus = :status " +
           "AND (:accessibleClients IS NULL OR cw.clientId IN :accessibleClients)")
    long countByStatus(@Param("orgId") UUID orgId, @Param("status") ComplianceWorkflowStatus status, @Param("accessibleClients") Set<UUID> accessibleClients);

    @Query("SELECT COUNT(cw) FROM ComplianceWorkflowEntity cw WHERE cw.organizationId = :orgId AND cw.workflowStatus = 'COMPLETED' " +
           "AND cw.completedAt >= :sinceDate AND (:accessibleClients IS NULL OR cw.clientId IN :accessibleClients)")
    long countCompletedSince(@Param("orgId") UUID orgId, @Param("sinceDate") Instant sinceDate, @Param("accessibleClients") Set<UUID> accessibleClients);

    @Query("SELECT COUNT(cw) FROM ComplianceWorkflowEntity cw WHERE cw.organizationId = :orgId AND cw.assignedEmployeeId = :employeeId " +
           "AND cw.workflowStatus NOT IN ('COMPLETED', 'CANCELLED') AND (:accessibleClients IS NULL OR cw.clientId IN :accessibleClients)")
    long countMyAssignedWorkflows(@Param("orgId") UUID orgId, @Param("employeeId") UUID employeeId, @Param("accessibleClients") Set<UUID> accessibleClients);

    @Query("SELECT COUNT(cw) FROM ComplianceWorkflowEntity cw WHERE cw.organizationId = :orgId AND cw.reviewerEmployeeId = :employeeId " +
           "AND cw.workflowStatus = 'UNDER_REVIEW' AND (:accessibleClients IS NULL OR cw.clientId IN :accessibleClients)")
    long countMyReviewWorkflows(@Param("orgId") UUID orgId, @Param("employeeId") UUID employeeId, @Param("accessibleClients") Set<UUID> accessibleClients);
}
