package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkTemplateTaskRequest {

    @NotBlank(message = "Task name is required")
    @Size(max = 255, message = "Task name must not exceed 255 characters")
    private String name;

    private String description;

    private Integer sequenceOrder;

    private String defaultAssigneeRole;

    @Builder.Default
    private TaskPriority defaultPriority = TaskPriority.MEDIUM;

    @Builder.Default
    private int relativeDueDays = 0;

    @Builder.Default
    private boolean mandatory = true;

    @Builder.Default
    private boolean active = true;
}
