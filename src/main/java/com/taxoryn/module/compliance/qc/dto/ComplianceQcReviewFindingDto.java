package com.taxoryn.module.compliance.qc.dto;

import com.taxoryn.module.compliance.qc.model.ComplianceQcFindingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Quality Control Review Finding Details")
public class ComplianceQcReviewFindingDto {

    @Schema(description = "Finding unique identifier")
    private UUID id;

    @Schema(description = "Associated workflow identifier")
    private UUID workflowId;

    @Schema(description = "Review cycle number", example = "1")
    private int reviewCycle;

    @Schema(description = "Finding category (e.g. COMPUTATION, DISCLOSURE, RECONCILIATION)", example = "COMPUTATION")
    private String category;

    @Schema(description = "Severity: BLOCKING or ADVISORY", example = "BLOCKING")
    private String severity;

    @Schema(description = "Human-readable finding description")
    private String description;

    @Schema(description = "Actionable corrective guidance for the preparer")
    private String correctiveGuidance;

    @Schema(description = "Source reference or document evidence")
    private String sourceReference;

    @Schema(description = "Current finding status: OPEN, CORRECTED, RESOLVED, DISMISSED", example = "OPEN")
    private ComplianceQcFindingStatus status;

    @Schema(description = "Preparer response or explanation")
    private String preparerResponse;

    @Schema(description = "Timestamp of preparer response")
    private Instant respondedAt;

    @Schema(description = "Preparer user ID who responded")
    private UUID respondedByUserId;

    @Schema(description = "Reviewer resolution decision (e.g. ACCEPTED, WAIVED)", example = "ACCEPTED")
    private String resolutionDecision;

    @Schema(description = "Timestamp when reviewer resolved the finding")
    private Instant resolvedAt;

    @Schema(description = "Reviewer user ID who resolved the finding")
    private UUID resolvedByUserId;

    @Schema(description = "Reviewer closing notes")
    private String reviewerNotes;

    @Schema(description = "Timestamp when finding was created")
    private Instant createdAt;

    @Schema(description = "User or email who recorded finding")
    private String createdBy;
}
