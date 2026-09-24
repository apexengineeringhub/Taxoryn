package com.taxoryn.module.compliance.dto;

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
@Schema(description = "Reviewer Request for Changes / Rework")
public class RequestWorkflowChangesRequest {

    @NotBlank(message = "Changes requested reason is required")
    @Schema(description = "Specific discrepancies or changes requested by reviewer", example = "Please reconcile ITC claimed with GSTR-2B table 4A before finalizing")
    private String reason;

    @Schema(description = "Optional reviewer notes")
    private String reviewNotes;
}
