package com.taxoryn.module.task.dto;

import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Calendar Task Projection")
public class TaskCalendarDto {

    private UUID id;
    private String title;
    private TaskCategory taskCategory;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate startDate;
    private LocalDate dueDate;
    private UUID clientId;
    private String clientName;
    private UUID engagementId;
    private String engagementTitle;
    private String engagementCode;
    private UUID assignedTo;
    private String assigneeName;
    private UUID workInstanceId;
    private Boolean isOverdue;
}
