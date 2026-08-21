package com.taxoryn.module.task.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Update Task Payload")
public class UpdateTaskRequest {

    private UUID clientId;
    private UUID assignedTo;
    private Boolean unassign;

    @Size(min = 3, max = 255, message = "Task title must be between 3 and 255 characters")
    private String title;

    private String description;
    private TaskCategory taskCategory;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
}
