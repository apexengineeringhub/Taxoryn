package com.taxoryn.module.task.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk Task Multi-Client Generation Payload")
public class BulkTaskCreateRequest {

    @NotEmpty(message = "At least one client must be selected")
    @Schema(description = "List of client IDs to generate tasks for")
    private List<UUID> clientIds;

    @Schema(description = "Optional practitioner employee to assign tasks to")
    private UUID assignedTo;

    @NotBlank(message = "Task title template is required")
    @Schema(description = "Task title", example = "GSTR-3B Monthly Return Filing")
    private String title;

    @Schema(description = "Task workflow description")
    private String description;

    @NotNull(message = "Task category is required")
    @Schema(description = "Category", example = "GST")
    private TaskCategory taskCategory;

    @NotNull(message = "Task priority is required")
    @Schema(description = "Priority", example = "HIGH")
    private TaskPriority priority;

    @Schema(description = "Statutory Due Date")
    private LocalDate dueDate;
}
