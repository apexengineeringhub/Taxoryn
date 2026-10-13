package com.taxoryn.module.compliance.qc.service;

import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.compliance.qc.dto.AuthorizeFilingRequest;
import com.taxoryn.module.compliance.qc.dto.ComplianceQcStatusDto;
import com.taxoryn.module.compliance.qc.dto.ComplianceQcValidationResultDto;
import com.taxoryn.module.compliance.qc.dto.RevokeAuthorizationRequest;

import java.util.UUID;

/**
 * Compliance Quality Control Service establishing the controlled progression:
 * Preparation -> Validation -> Review -> Correction -> Approval -> Authorization -> Submission
 */
public interface ComplianceQualityControlService {

    /**
     * Inspects the comprehensive Quality Control status, active stage, and next actionable step for a workflow.
     */
    ComplianceQcStatusDto getQcStatus(UUID workflowId);

    /**
     * Executes deterministic validation checks against preparation data, checklist, and reviewer assignment.
     */
    ComplianceQcValidationResultDto validateForReview(UUID workflowId);

    /**
     * Explicitly authorizes filing of approved compliance work.
     */
    ComplianceWorkflowDto authorizeFiling(UUID workflowId, AuthorizeFilingRequest request);

    /**
     * Revokes filing authorization (e.g. if updated client data arrives prior to submission).
     */
    ComplianceWorkflowDto revokeFilingAuthorization(UUID workflowId, RevokeAuthorizationRequest request);

    /**
     * Verifies that all approval prerequisites are met prior to approval decision / READY_FOR_FILING:
     * - Workflow is in UNDER_REVIEW status
     * - Assigned reviewer exists, is active, and possesses REVIEW_APPROVE authority
     * - Four-Eyes policy is respected (acting reviewer cannot be the preparer)
     * - Pre-review validation is fresh and passing
     * - Zero unresolved blocking review findings exist in the active cycle
     * Throws BusinessValidationException or ForbiddenException if prerequisites are not satisfied.
     */
    void verifyApprovalPrerequisites(UUID workflowId);

    /**
     * Retrieves the complete immutable authorization event history for a workflow.
     */
    java.util.List<com.taxoryn.module.compliance.qc.dto.ComplianceQcAuthorizationRecordDto> getAuthorizationHistory(UUID workflowId);

    /**
     * Verifies that all approval and authorization prerequisites are met prior to official submission.
     * Throws BusinessValidationException if prerequisites are not satisfied.
     */
    void verifySubmissionPrerequisites(UUID workflowId);

    /**
     * Verifies that preparation validation has been executed, is currently passing, and is fresh.
     * Throws BusinessValidationException if validation is missing, failing, or stale.
     */
    void verifyValidationFreshness(UUID workflowId);

    /**
     * Records a review finding on a compliance workflow during active review.
     */
    com.taxoryn.module.compliance.qc.dto.ComplianceQcReviewFindingDto addReviewFinding(
            UUID workflowId, com.taxoryn.module.compliance.qc.dto.CreateReviewFindingRequest request);

    /**
     * Retrieves all findings recorded across all review cycles for a workflow.
     */
    java.util.List<com.taxoryn.module.compliance.qc.dto.ComplianceQcReviewFindingDto> getReviewFindings(UUID workflowId);

    /**
     * Retrieves review cycles history with findings grouped by cycle.
     */
    java.util.List<com.taxoryn.module.compliance.qc.dto.ComplianceQcReviewCycleDto> getReviewCycles(UUID workflowId);

    /**
     * Preparer records a response to address a specific review finding.
     */
    com.taxoryn.module.compliance.qc.dto.ComplianceQcReviewFindingDto respondToFinding(
            UUID workflowId, UUID findingId, com.taxoryn.module.compliance.qc.dto.RespondToFindingRequest request);

    /**
     * Reviewer resolves or dismisses a specific review finding.
     */
    com.taxoryn.module.compliance.qc.dto.ComplianceQcReviewFindingDto resolveFinding(
            UUID workflowId, UUID findingId, com.taxoryn.module.compliance.qc.dto.ResolveFindingRequest request);
}
