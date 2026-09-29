package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkTemplateTaskRequest {

    @Size(max = 255, message = "Task name must not exceed 255 characters")
    private String name;

    private String description;

    private Integer sequenceOrder;

    private String defaultAssigneeRole;

    private TaskPriority defaultPriority;

    private Integer relativeDueDays;

    private Boolean mandatory;

    private Boolean active;
}
