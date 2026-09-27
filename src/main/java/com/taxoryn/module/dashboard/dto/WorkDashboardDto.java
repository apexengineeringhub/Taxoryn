package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice Work & Task Management Dashboard Metrics")
public class WorkDashboardDto {

    // Work Item Metrics
    @Schema(description = "Total work items in scope", example = "48")
    @Builder.Default
    private long totalWorkItems = 0;

    @Schema(description = "Work items in TODO status", example = "15")
    @Builder.Default
    private long workItemsTodo = 0;

    @Schema(description = "Work items IN_PROGRESS", example = "18")
    @Builder.Default
    private long workItemsInProgress = 0;

    @Schema(description = "Work items BLOCKED", example = "2")
    @Builder.Default
    private long workItemsBlocked = 0;

    @Schema(description = "Work items COMPLETED", example = "12")
    @Builder.Default
    private long workItemsCompleted = 0;

    @Schema(description = "Work items CANCELLED", example = "1")
    @Builder.Default
    private long workItemsCancelled = 0;

    @Schema(description = "Overdue work items past due date", example = "3")
    @Builder.Default
    private long overdueWorkItems = 0;

    // Task Metrics
    @Schema(description = "Total tasks in scope", example = "75")
    @Builder.Default
    private long totalTasks = 0;

    @Schema(description = "Pending/TODO tasks", example = "30")
    @Builder.Default
    private long pendingTasks = 0;

    @Schema(description = "In progress tasks", example = "25")
    @Builder.Default
    private long inProgressTasks = 0;

    @Schema(description = "Completed tasks", example = "20")
    @Builder.Default
    private long completedTasks = 0;

    @Schema(description = "Overdue tasks", example = "5")
    @Builder.Default
    private long overdueTasks = 0;

    // Team Workload
    @Schema(description = "Workload distribution by team member")
    @Builder.Default
    private List<UserWorkloadSummaryDto> userWorkloads = new ArrayList<>();

    @Schema(description = "Timestamp when this summary was generated")
    @Builder.Default
    private Instant generatedAt = Instant.now();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Assigned user operational workload")
    public static class UserWorkloadSummaryDto {
        private UUID userId;
        private String userName;
        private String email;
        private String department;
        private String designation;
        @Builder.Default
        private long assignedWorkItems = 0;
        @Builder.Default
        private long openWorkItems = 0;
        @Builder.Default
        private long assignedTasks = 0;
        @Builder.Default
        private long pendingTasks = 0;
        @Builder.Default
        private long overdueTasks = 0;
    }
}
