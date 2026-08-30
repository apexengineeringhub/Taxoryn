package com.taxoryn.module.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Worklist Summary Aggregation Metrics")
public class WorklistSummaryDto {

    @Schema(description = "Count of tasks and compliance items past their due date")
    private long overdueCount;

    @Schema(description = "Count of items due today")
    private long dueTodayCount;

    @Schema(description = "Count of items due this week (next 7 days)")
    private long dueThisWeekCount;

    @Schema(description = "Count of active tasks in progress")
    private long inProgressCount;

    @Schema(description = "Count of tasks blocked waiting for client documents/clarification")
    private long blockedCount;

    @Schema(description = "Count of tasks completed today")
    private long completedTodayCount;

    @Schema(description = "Count of pending document items requested from clients")
    private long documentsWaitingCount;

    @Schema(description = "Count of tasks assigned to current practitioner/staff")
    private long myTasksCount;

    @Schema(description = "Count of total tasks across the practice firm")
    private long teamTasksCount;
}
