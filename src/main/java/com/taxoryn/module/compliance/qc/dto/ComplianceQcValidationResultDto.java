package com.taxoryn.module.compliance.qc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Deterministic Quality Control Validation Result")
public class ComplianceQcValidationResultDto {

    @Schema(description = "Overall validation status: true if zero blocking failures or non-evaluated mandatory checks", example = "true")
    private boolean valid;

    @Schema(description = "Total number of applicable checks", example = "6")
    private int totalChecks;

    @Schema(description = "Number of checks that passed", example = "6")
    private int passedChecks;

    @Schema(description = "Number of checks that failed", example = "0")
    private int failedChecks;

    @Schema(description = "Number of checks that could not be evaluated reliably", example = "0")
    private int notEvaluatedChecks;

    @Schema(description = "Number of checks deemed not applicable under domain rules", example = "0")
    private int notApplicableChecks;

    @Schema(description = "Validation execution timestamp")
    private Instant validatedAt;

    @Schema(description = "Preparation data version or state fingerprint at validation execution time", example = "wf:1:chk:4")
    private String preparationFingerprint;

    @Schema(description = "Evaluated check list with structured findings")
    @Builder.Default
    private List<ComplianceQcCheckItemDto> checks = new ArrayList<>();

    @Schema(description = "Blocking findings preventing progression to review")
    @Builder.Default
    private List<String> blockers = new ArrayList<>();

    @Schema(description = "Advisory findings that do not prevent progression")
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Schema(description = "Prioritized list of actionable corrections for the preparer")
    @Builder.Default
    private List<String> correctiveActions = new ArrayList<>();
}
