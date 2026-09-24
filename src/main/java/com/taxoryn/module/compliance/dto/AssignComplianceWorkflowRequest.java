package com.taxoryn.module.compliance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Assign Practitioner and Reviewer to Compliance Workflow")
public class AssignComplianceWorkflowRequest {

    @Schema(description = "Assigned Practitioner Employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned Reviewer/Checker Employee ID")
    private UUID reviewerEmployeeId;

    @Schema(description = "Execution Priority")
    private com.taxoryn.module.task.entity.TaskEntity.TaskPriority priority;

    @Schema(description = "Internal Target Date")
    private java.time.LocalDate targetDate;

    @Schema(description = "Optional assignment note")
    private String assignmentNote;
}
