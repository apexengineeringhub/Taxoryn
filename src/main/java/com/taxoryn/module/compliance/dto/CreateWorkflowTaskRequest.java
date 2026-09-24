package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Create Subtask from Compliance Workflow")
public class CreateWorkflowTaskRequest {

    @NotBlank(message = "Task title is required")
    @Schema(description = "Task title", example = "Reconcile GSTR-2B purchase register with Tally ledger")
    private String title;

    @Schema(description = "Task description")
    private String description;

    @Schema(description = "Task category")
    private com.taxoryn.module.task.entity.TaskEntity.TaskCategory category;

    @Schema(description = "Assigned employee ID")
    private UUID assignedTo;

    @Schema(description = "Priority level", example = "HIGH")
    private TaskPriority priority;

    @Schema(description = "Task due date", example = "2026-08-15")
    private LocalDate dueDate;
}
