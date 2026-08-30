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
@Schema(description = "Worklist Filter and Bucket Search Request")
public class TaskWorklistFilterRequest extends PageRequestDto {

    public enum WorklistScope {
        MY_WORK,
        TEAM_WORK
    }

    public enum WorklistBucket {
        ALL,
        OVERDUE,
        DUE_TODAY,
        DUE_THIS_WEEK,
        BLOCKED,
        COMPLETED
    }

    @Schema(description = "Scope: MY_WORK (assigned to current user) or TEAM_WORK (entire practice)")
    @lombok.Builder.Default
    private WorklistScope scope = WorklistScope.MY_WORK;

    @Schema(description = "Urgency/Lifecycle bucket: ALL, OVERDUE, DUE_TODAY, DUE_THIS_WEEK, BLOCKED, COMPLETED")
    @lombok.Builder.Default
    private WorklistBucket bucket = WorklistBucket.ALL;

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

    @Schema(description = "Search query for task title, description, or client name")
    private String search;
}
