package com.taxoryn.module.client.businesscontext.dto;

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
@Schema(description = "Level 2: Work & Task Context Model (What work is currently being executed?)")
public class WorkSummaryContextDto {

    @Schema(description = "Work item / instance ID")
    private UUID workInstanceId;

    @Schema(description = "Task ID")
    private UUID taskId;

    @Schema(description = "Task title / description")
    private String taskTitle;

    @Schema(description = "Task status (PENDING, IN_PROGRESS, COMPLETED, BLOCKED, etc.)")
    private String taskStatus;

    @Schema(description = "Task priority (LOW, MEDIUM, HIGH, CRITICAL)")
    private String taskPriority;

    @Schema(description = "Task category")
    private String taskCategory;

    @Schema(description = "Assigned user ID")
    private UUID assignedUserId;

    @Schema(description = "Assignee name")
    private String assigneeName;

    @Schema(description = "Assignee email")
    private String assigneeEmail;

    @Schema(description = "Start date")
    private LocalDate startDate;

    @Schema(description = "Task due date")
    private LocalDate dueDate;

    @Schema(description = "Statutory government due date if applicable")
    private LocalDate statutoryDueDate;

    @Schema(description = "Whether the task is overdue")
    private boolean overdue;

    @Schema(description = "Whether the task is due today")
    private boolean dueToday;

    @Schema(description = "Whether the task is due this week")
    private boolean dueThisWeek;

    @Schema(description = "Whether the task is blocked")
    private boolean blocked;

    @Schema(description = "Reason why task is blocked")
    private String blockedReason;
}
