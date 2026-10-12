package com.taxoryn.module.compliance.qc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Individual Quality Control Check Item Evaluation")
public class ComplianceQcCheckItemDto {

    @Schema(description = "Check key identifier", example = "CHECKLIST_COMPUTATION_PREPARED")
    private String checkKey;

    @Schema(description = "Human readable check title", example = "Tax Computation Draft Prepared")
    private String checkName;

    @Schema(description = "Category of check", example = "CHECKLIST")
    private String category;

    @Schema(description = "Whether the check evaluated to pass", example = "true")
    private boolean passed;

    @Schema(description = "Whether this check is a blocking requirement for review or filing", example = "true")
    private boolean required;

    @Schema(description = "Explanation or detail message")
    private String message;
}
