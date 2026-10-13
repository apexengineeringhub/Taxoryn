package com.taxoryn.module.compliance.qc.dto;

import com.taxoryn.module.compliance.qc.model.ComplianceQcCheckStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Individual Quality Control Check Item Evaluation")
public class ComplianceQcCheckItemDto {

    @Schema(description = "Stable check identifier", example = "CHECKLIST_COMPUTATION_PREPARED")
    private String checkKey;

    @Schema(description = "Human readable check title", example = "Tax Computation Draft Prepared")
    private String checkName;

    @Schema(description = "Category of check", example = "CHECKLIST")
    private String category;

    @Schema(description = "Detailed explanation of what is evaluated")
    private String description;

    @Schema(description = "Check outcome status: PASS, FAIL, NOT_EVALUATED, NOT_APPLICABLE", example = "PASS")
    @Builder.Default
    private ComplianceQcCheckStatus status = ComplianceQcCheckStatus.NOT_EVALUATED;

    @Schema(description = "Backward-compatible boolean: true if status is PASS or NOT_APPLICABLE", example = "true")
    private boolean passed;

    @Schema(description = "Severity of the check: BLOCKING or ADVISORY", example = "BLOCKING")
    @Builder.Default
    private String severity = "BLOCKING";

    @Schema(description = "Whether this check is a blocking requirement for progression to review", example = "true")
    private boolean required;

    @Schema(description = "Actionable corrective guidance when the check fails or cannot be evaluated")
    private String correctiveHint;

    @Schema(description = "Relevant source identifier or evidence reference (e.g. employee ID, registration ID)")
    private String sourceReference;

    @Schema(description = "Evaluation message or finding details")
    private String message;

    @Schema(description = "Timestamp when this check was evaluated")
    private Instant evaluatedAt;

    @Schema(description = "Check rule version", example = "1.0")
    @Builder.Default
    private String checkVersion = "1.0";
}
