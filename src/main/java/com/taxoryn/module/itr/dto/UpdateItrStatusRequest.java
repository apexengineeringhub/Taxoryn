package com.taxoryn.module.itr.dto;

import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update ITR Status Workflow Transition Payload")
public class UpdateItrStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Target workflow status", example = "UNDER_REVIEW")
    private ItrStatus status;

    @Schema(description = "Optional workflow transition remarks or reason")
    private String notes;
}
