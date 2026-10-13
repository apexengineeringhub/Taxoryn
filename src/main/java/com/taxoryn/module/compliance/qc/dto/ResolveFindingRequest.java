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
@Schema(description = "Reviewer resolution decision on a finding")
public class ResolveFindingRequest {

    @NotBlank(message = "Decision is required")
    @Schema(description = "Decision: RESOLVED, DISMISSED", example = "RESOLVED")
    private String decision;

    @Schema(description = "Reviewer closing comments")
    private String reviewerNotes;
}
