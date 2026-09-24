package com.taxoryn.module.compliance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Checklist Item Status and Notes")
public class UpdateChecklistItemRequest {

    @Schema(description = "Whether step is completed", example = "true")
    private Boolean isCompleted;

    @Schema(description = "Optional notes on this checklist step")
    private String notes;
}
