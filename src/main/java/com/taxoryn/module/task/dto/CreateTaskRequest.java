package com.taxoryn.module.task.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Create Task Payload")
public class CreateTaskRequest {

    private UUID clientId;
    private UUID assignedTo;

    @NotBlank(message = "Task title is required")
    @Size(min = 2, max = 255, message = "Task title must be between 2 and 255 characters")
    private String title;

    private String description;

    @JsonAlias({"category", "taskCategory"})
    @Builder.Default
    private TaskCategory taskCategory = TaskCategory.OTHER;

    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    private LocalDate dueDate;
    private UUID complianceId;
    private UUID documentRequestId;
    private String blockedReason;
}
