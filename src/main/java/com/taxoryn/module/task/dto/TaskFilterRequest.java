package com.taxoryn.module.task.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Task Filter and Search Request")
public class TaskFilterRequest extends PageRequestDto {

    @Schema(description = "Filter by Client ID")
    private UUID clientId;

    @Schema(description = "Filter by Assignee Employee or User ID")
    private UUID assignedTo;

    @Schema(description = "Filter by Task Status")
    private TaskStatus status;

    @Schema(description = "Filter by Task Category")
    private TaskCategory taskCategory;

    @Schema(description = "Filter by Priority")
    private TaskPriority priority;

    @Schema(description = "Filter only tasks assigned to the currently authenticated user/employee")
    private Boolean myTasksOnly;

    @Schema(description = "Search query for task title or description")
    private String search;
}
