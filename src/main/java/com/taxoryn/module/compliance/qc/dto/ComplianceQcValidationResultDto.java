package com.taxoryn.module.compliance.qc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Deterministic Quality Control Validation Result")
public class ComplianceQcValidationResultDto {

    @Schema(description = "Overall validation status: true if zero blocking errors", example = "true")
    private boolean valid;

    @Schema(description = "Total number of evaluated checks", example = "6")
    private int totalChecks;

    @Schema(description = "Number of checks that passed", example = "6")
    private int passedChecks;

    @Schema(description = "Evaluated check list")
    @Builder.Default
    private List<ComplianceQcCheckItemDto> checks = new ArrayList<>();

    @Schema(description = "Blocking error messages preventing transition")
    @Builder.Default
    private List<String> blockers = new ArrayList<>();

    @Schema(description = "Advisory warning messages")
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
