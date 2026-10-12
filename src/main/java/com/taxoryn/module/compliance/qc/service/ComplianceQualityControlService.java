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
     * Verifies that all approval and authorization prerequisites are met prior to official submission.
     * Throws BusinessValidationException if prerequisites are not satisfied.
     */
    void verifySubmissionPrerequisites(UUID workflowId);
}
