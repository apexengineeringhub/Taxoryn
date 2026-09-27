package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to initialize an operational compliance workflow for an obligation")
public class CreateComplianceWorkflowRequest {

    @NotNull(message = "Compliance obligation ID is required")
    @Schema(description = "ID of the statutory compliance obligation")
    private UUID complianceObligationId;

    @Schema(description = "Optional custom workflow type code")
    private String workflowType;

    @Schema(description = "User ID assigned to prepare this compliance workflow")
    private UUID assignedUserId;

    @Schema(description = "Location / branch ID responsible for this workflow")
    private UUID locationId;

    @Schema(description = "Execution priority")
    private TaskPriority priority;

    @Schema(description = "Internal target completion date")
    private LocalDate targetDate;

    @Schema(description = "Initial internal notes")
    private String notes;
}
