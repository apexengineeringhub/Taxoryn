package com.taxoryn.module.compliance.qc.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.compliance.qc.dto.AuthorizeFilingRequest;
import com.taxoryn.module.compliance.qc.dto.ComplianceQcStatusDto;
import com.taxoryn.module.compliance.qc.dto.ComplianceQcValidationResultDto;
import com.taxoryn.module.compliance.qc.dto.RevokeAuthorizationRequest;
import com.taxoryn.module.compliance.qc.service.ComplianceQualityControlService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/compliance/workflows/{workflowId}/qc", "/api/compliance/workflows/{workflowId}/qc"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.TASKS)
@Tag(name = "Compliance Quality Control Foundation", description = "Quality control lifecycle stage management, deterministic validation, review gates, and filing authorization")
@SecurityRequirement(name = "BearerAuth")
public class ComplianceQualityControlController {

    private final ComplianceQualityControlService qcService;

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'NOTICE_VIEW', 'REVIEW_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get compliance Quality Control status", description = "Retrieves current QC stage (Preparation -> Validation -> Review -> Correction -> Approval -> Authorization -> Submission), check outcomes, and next actionable step.")
    public ResponseEntity<ApiResponse<ComplianceQcStatusDto>> getQcStatus(
            @PathVariable UUID workflowId
    ) {
        ComplianceQcStatusDto status = qcService.getQcStatus(workflowId);
        return ResponseEntity.ok(ApiResponse.success("Quality control status retrieved", status));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'REVIEW_SUBMIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Run deterministic pre-review validation", description = "Executes deterministic validation checks on checklist completion, reviewer assignment, separation of duties, and statutory registrations.")
    public ResponseEntity<ApiResponse<ComplianceQcValidationResultDto>> validateForReview(
            @PathVariable UUID workflowId
    ) {
        ComplianceQcValidationResultDto result = qcService.validateForReview(workflowId);
        return ResponseEntity.ok(ApiResponse.success("Pre-review validation completed", result));
    }

    @PostMapping("/authorize-filing")
    @PreAuthorize("hasAnyAuthority('CLIENT_EDIT', 'COMPLIANCE_EDIT', 'REVIEW_APPROVE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Authorize filing of approved compliance work", description = "Records client sign-off or practitioner authorization permitting official portal filing.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDto>> authorizeFiling(
            @PathVariable UUID workflowId,
            @Valid @RequestBody AuthorizeFilingRequest request
    ) {
        ComplianceWorkflowDto workflow = qcService.authorizeFiling(workflowId, request);
        return ResponseEntity.ok(ApiResponse.success("Filing authorization recorded successfully", workflow));
    }

    @PostMapping("/revoke-authorization")
    @PreAuthorize("hasAnyAuthority('CLIENT_EDIT', 'COMPLIANCE_EDIT', 'REVIEW_APPROVE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Revoke filing authorization", description = "Revokes previous filing authorization if data amendments or client objections occur prior to submission.")
    public ResponseEntity<ApiResponse<ComplianceWorkflowDto>> revokeFilingAuthorization(
            @PathVariable UUID workflowId,
            @Valid @RequestBody RevokeAuthorizationRequest request
    ) {
        ComplianceWorkflowDto workflow = qcService.revokeFilingAuthorization(workflowId, request);
        return ResponseEntity.ok(ApiResponse.success("Filing authorization revoked", workflow));
    }

    @PostMapping("/findings")
    @PreAuthorize("hasAnyAuthority('REVIEW_APPROVE', 'REVIEW_REJECT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Record review finding", description = "Reviewer records a specific deficiency or finding during review.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.compliance.qc.dto.ComplianceQcReviewFindingDto>> addReviewFinding(
            @PathVariable UUID workflowId,
            @Valid @RequestBody com.taxoryn.module.compliance.qc.dto.CreateReviewFindingRequest request
    ) {
        var finding = qcService.addReviewFinding(workflowId, request);
        return ResponseEntity.ok(ApiResponse.success("Review finding recorded", finding));
    }

    @GetMapping("/findings")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'REVIEW_VIEW', 'REVIEW_SUBMIT', 'REVIEW_APPROVE', 'REVIEW_REJECT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get review findings", description = "Retrieves all findings recorded across all review cycles for the workflow.")
    public ResponseEntity<ApiResponse<java.util.List<com.taxoryn.module.compliance.qc.dto.ComplianceQcReviewFindingDto>>> getReviewFindings(
            @PathVariable UUID workflowId
    ) {
        var findings = qcService.getReviewFindings(workflowId);
        return ResponseEntity.ok(ApiResponse.success("Review findings retrieved", findings));
    }

    @GetMapping("/review-cycles")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'CLIENT_VIEW', 'REVIEW_VIEW', 'REVIEW_SUBMIT', 'REVIEW_APPROVE', 'REVIEW_REJECT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get review cycle history", description = "Retrieves review cycles with structured findings history.")
    public ResponseEntity<ApiResponse<java.util.List<com.taxoryn.module.compliance.qc.dto.ComplianceQcReviewCycleDto>>> getReviewCycles(
            @PathVariable UUID workflowId
    ) {
        var cycles = qcService.getReviewCycles(workflowId);
        return ResponseEntity.ok(ApiResponse.success("Review cycles retrieved", cycles));
    }

    @PostMapping("/findings/{findingId}/respond")
    @PreAuthorize("hasAnyAuthority('TASK_EDIT', 'CLIENT_EDIT', 'REVIEW_SUBMIT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Respond to review finding", description = "Preparer records explanation and correction response for a finding.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.compliance.qc.dto.ComplianceQcReviewFindingDto>> respondToFinding(
            @PathVariable UUID workflowId,
            @PathVariable UUID findingId,
            @Valid @RequestBody com.taxoryn.module.compliance.qc.dto.RespondToFindingRequest request
    ) {
        var finding = qcService.respondToFinding(workflowId, findingId, request);
        return ResponseEntity.ok(ApiResponse.success("Correction response recorded", finding));
    }

    @PostMapping("/findings/{findingId}/resolve")
    @PreAuthorize("hasAnyAuthority('REVIEW_APPROVE', 'REVIEW_REJECT', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    @Operation(summary = "Resolve or dismiss review finding", description = "Reviewer verifies correction and marks finding resolved or dismissed.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.compliance.qc.dto.ComplianceQcReviewFindingDto>> resolveFinding(
            @PathVariable UUID workflowId,
            @PathVariable UUID findingId,
            @Valid @RequestBody com.taxoryn.module.compliance.qc.dto.ResolveFindingRequest request
    ) {
        var finding = qcService.resolveFinding(workflowId, findingId, request);
        return ResponseEntity.ok(ApiResponse.success("Finding resolution recorded", finding));
    }
}
