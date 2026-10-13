package com.taxoryn.module.compliance.qc.dto;

import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.qc.model.ComplianceQcStage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Overall Compliance Quality Control Status & Lifecycle Stage Overview")
public class ComplianceQcStatusDto {

    @Schema(description = "Workflow ID")
    private UUID workflowId;

    @Schema(description = "Compliance Obligation ID")
    private UUID complianceObligationId;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client Name")
    private String clientName;

    @Schema(description = "Active Quality Control Stage", example = "AUTHORIZATION")
    private ComplianceQcStage stage;

    @Schema(description = "Underlying Operational Workflow Status", example = "READY_FOR_FILING")
    private ComplianceWorkflowStatus workflowStatus;

    // --- Stage Progression Booleans ---
    @Schema(description = "Whether preparation stage is complete", example = "true")
    private boolean isPreparationComplete;

    @Schema(description = "Whether deterministic pre-review validation passed", example = "true")
    private boolean isValidationPassed;

    @Schema(description = "Whether independent review is currently active", example = "false")
    private boolean isUnderReview;

    @Schema(description = "Whether review returned findings requiring corrections", example = "false")
    private boolean isChangesRequired;

    @Schema(description = "Whether computation has passed review and is approved", example = "true")
    private boolean isApproved;

    @Schema(description = "Whether filing has been explicitly authorized", example = "false")
    private boolean isAuthorized;

    @Schema(description = "Whether return has been submitted/filed", example = "false")
    private boolean isSubmitted;

    // --- Sign-off Metadata ---
    @Schema(description = "Approval timestamp")
    private Instant approvedAt;

    @Schema(description = "Approver user or email")
    private String approvedBy;

    @Schema(description = "Review notes recorded at approval")
    private String reviewNotes;

    @Schema(description = "Filing authorization timestamp")
    private Instant authorizedAt;

    @Schema(description = "Authorizer email or name")
    private String authorizedBy;

    @Schema(description = "Authorization method recorded", example = "CLIENT_CONSENT")
    private String authorizationMethod;

    @Schema(description = "Authorization reference identifier")
    private String authorizationReference;

    @Schema(description = "Authorization notes or instructions")
    private String authorizationNotes;

    @Schema(description = "Filing date recorded")
    private LocalDate filedDate;

    @Schema(description = "Filing submission timestamp")
    private Instant filedAt;

    @Schema(description = "Filing user")
    private String filedBy;

    @Schema(description = "Statutory acknowledgement / ARN reference")
    private String acknowledgementNumber;

    // --- Detailed Validation Summary ---
    @Schema(description = "Validation details and check outcomes")
    private ComplianceQcValidationResultDto validationSummary;

    // --- Review Cycles & Findings ---
    @Schema(description = "Current review cycle number", example = "1")
    private int reviewCycle;

    @Schema(description = "Count of unresolved blocking findings", example = "0")
    private int unresolvedFindingsCount;

    @Schema(description = "List of review findings for the current/latest cycle")
    @Builder.Default
    private List<ComplianceQcReviewFindingDto> findings = new ArrayList<>();

    @Schema(description = "Actionable next step in the quality control chain", example = "Authorize filing with client confirmation")
    private String nextActionableStep;

    @Schema(description = "List of permitted QC operations for caller", example = "[\"AUTHORIZE_FILING\", \"VALIDATE\"]")
    @Builder.Default
    private List<String> allowedActions = new ArrayList<>();
}
