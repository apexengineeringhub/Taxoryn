package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkInstanceTaskDto {

    private UUID id;
    private UUID engagementId;
    private UUID workInstanceId;
    private UUID workTemplateTaskId;
    private String title;
    private String description;
    private TaskCategory taskCategory;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private UUID assignedUserId;
    private String assignedUserName;
    private Instant completedAt;
    private String notes;
}
