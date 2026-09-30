package com.taxoryn.module.task.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
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
@Schema(description = "Update Task Priority Request")
public class UpdateTaskPriorityRequest {

    @NotNull(message = "Task priority is required")
    private TaskPriority priority;
}
