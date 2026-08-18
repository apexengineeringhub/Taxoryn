package com.taxoryn.module.task.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @Size(min = 3, max = 255, message = "Task title must be between 3 and 255 characters")
    private String title;

    private String description;

    @NotNull(message = "Task category is required")
    private TaskCategory taskCategory;

    @NotNull(message = "Task priority is required")
    private TaskPriority priority;

    private LocalDate dueDate;
}
