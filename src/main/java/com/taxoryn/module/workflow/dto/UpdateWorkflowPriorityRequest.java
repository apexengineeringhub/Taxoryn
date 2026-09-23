package com.taxoryn.module.workflow.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkflowPriorityRequest {

    @NotNull(message = "Priority is required")
    private TaskPriority priority;

    private String notes;
}
