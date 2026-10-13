package com.taxoryn.module.compliance.qc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to record a review finding on compliance work")
public class CreateReviewFindingRequest {

    @NotBlank(message = "Category is required")
    @Schema(description = "Category: COMPUTATION, DISCLOSURE, RECONCILIATION, STATUTORY_FORM", example = "COMPUTATION")
    private String category;

    @Schema(description = "Severity: BLOCKING or ADVISORY", example = "BLOCKING")
    @Builder.Default
    private String severity = "BLOCKING";

    @NotBlank(message = "Description is required")
    @Schema(description = "Deficiency description")
    private String description;

    @Schema(description = "Specific instructions for the preparer to fix")
    private String correctiveGuidance;

    @Schema(description = "Table, schedule or source document reference")
    private String sourceReference;
}
