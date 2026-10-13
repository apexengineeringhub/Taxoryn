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
@Schema(description = "Summary of review cycles and findings for a workflow")
public class ComplianceQcReviewCycleDto {

    @Schema(description = "Cycle number", example = "1")
    private int cycleNumber;

    @Schema(description = "Total findings in this cycle")
    private int totalFindings;

    @Schema(description = "Unresolved findings in this cycle")
    private int unresolvedFindings;

    @Schema(description = "List of findings recorded during this cycle")
    @Builder.Default
    private List<ComplianceQcReviewFindingDto> findings = new ArrayList<>();
}
