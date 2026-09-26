package com.taxoryn.module.task.dto;

import com.taxoryn.module.task.model.WorkItemStatus;
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
@Schema(description = "Request payload to update the lifecycle status of a Work Item")
public class UpdateWorkItemStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Target status", requiredMode = Schema.RequiredMode.REQUIRED)
    private WorkItemStatus status;

    @Schema(description = "Optional notes or reason for status change")
    private String notes;
}
