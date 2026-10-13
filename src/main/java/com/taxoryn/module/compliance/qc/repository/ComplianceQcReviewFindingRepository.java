package com.taxoryn.module.compliance.qc.repository;

import com.taxoryn.module.compliance.qc.entity.ComplianceQcReviewFindingEntity;
import com.taxoryn.module.compliance.qc.model.ComplianceQcFindingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceQcReviewFindingRepository extends JpaRepository<ComplianceQcReviewFindingEntity, UUID> {

    List<ComplianceQcReviewFindingEntity> findByOrganizationIdAndWorkflowIdOrderByReviewCycleAscCreatedAtAsc(
            UUID organizationId, UUID workflowId);

    List<ComplianceQcReviewFindingEntity> findByOrganizationIdAndWorkflowIdAndReviewCycleOrderByCreatedAtAsc(
            UUID organizationId, UUID workflowId, int reviewCycle);

    Optional<ComplianceQcReviewFindingEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("SELECT COUNT(f) FROM ComplianceQcReviewFindingEntity f WHERE f.organizationId = :orgId AND f.workflow.id = :workflowId AND f.severity = 'BLOCKING' AND f.status IN (:openStatuses)")
    long countUnresolvedBlockingFindings(
            @Param("orgId") UUID orgId,
            @Param("workflowId") UUID workflowId,
            @Param("openStatuses") List<ComplianceQcFindingStatus> openStatuses);
}
