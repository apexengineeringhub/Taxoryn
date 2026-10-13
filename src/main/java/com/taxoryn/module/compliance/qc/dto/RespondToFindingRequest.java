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
@Schema(description = "Preparer response to address a review finding")
public class RespondToFindingRequest {

    @NotBlank(message = "Preparer correction response is required")
    @Schema(description = "Explanation of changes made to address finding")
    private String response;
}
