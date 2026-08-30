package com.taxoryn.module.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Work Management, Task Analytics, and Employee Productivity Report")
public class WorkManagementReportDto {

    // Task Analytics
    @Schema(description = "Total tasks tracked")
    private long totalTasks;

    @Schema(description = "Open tasks (TODO)")
    private long openTasks;

    @Schema(description = "In Progress tasks")
    private long inProgressTasks;

    @Schema(description = "Under Review tasks")
    private long underReviewTasks;

    @Schema(description = "Blocked tasks")
    private long blockedTasks;

    @Schema(description = "Overdue tasks")
    private long overdueTasks;

    @Schema(description = "Completed tasks")
    private long completedTasks;

    @Schema(description = "Tasks distribution by Category (GST, ITR, TDS, AUDIT, ACCOUNTING, OTHER)")
    private Map<String, Long> tasksByCategory;

    @Schema(description = "Tasks distribution by Priority (LOW, MEDIUM, HIGH, URGENT)")
    private Map<String, Long> tasksByPriority;

    // Employee Workload & Productivity
    @Schema(description = "Employee workload and completion productivity breakdown")
    private List<EmployeeProductivityDto> employeeProductivity;

    @Schema(description = "Operational items needing manager attention: overdue deadlines or workload imbalance. Neutral, factual language only — never a performance label.")
    private List<AttentionItemDto> attentionRequired;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "A single operational attention item for one employee")
    public static class AttentionItemDto {
        private java.util.UUID employeeId;
        private String employeeName;
        @Schema(description = "OVERDUE (has overdue tasks) or HIGH_WORKLOAD (pending load well above the team average)")
        private String reason;
        @Schema(description = "The count backing the reason: overdue task count, or current pending task count")
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual employee workload and performance metrics")
    public static class EmployeeProductivityDto {
        private java.util.UUID employeeId;
        private String employeeCode;
        private String employeeName;
        private String email;
        private String department;
        private String designation;
        private long assignedTasks;
        private long openTasks;
        private long inProgressTasks;
        private long underReviewTasks;
        private long blockedTasks;
        @Schema(description = "Currently open/non-terminal work: open + in-progress + under-review + blocked. Excludes completed and cancelled tasks. Overdue tasks are a subset of this, not counted separately.")
        private long pendingTasks;
        private long overdueTasks;
        private long completedTasks;
        @Schema(description = "Tasks assigned but cancelled — terminal, excluded from pending/completion-rate math. Shown so assignedTasks reconciles: assigned = pending + completed + cancelled.")
        private long cancelledTasks;
        @Schema(description = "Task completion rate percentage (0-100)", example = "87.5")
        private double completionRate;
        @Schema(description = "Completed tasks that had a due date, used as the denominator for on-time rate")
        private long completedWithDueDate;
        @Schema(description = "Completed tasks finished on or before their due date")
        private long onTimeCompletedTasks;
        @Schema(description = "On-time completion rate percentage (0-100), null if no completed tasks had a due date", example = "90.0")
        private Double onTimeCompletionRate;
        @Schema(description = "Workload broken down by tax type (GST, ITR, TDS, AUDIT, COMPLIANCE, BILLING, OTHER), keyed by TaskCategory enum name. Only categories with at least one assigned task are present.")
        private Map<String, TaxCategoryProductivityDto> taxCategoryBreakdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Workload for a single tax category (GST/ITR/TDS/etc.) for one employee")
    public static class TaxCategoryProductivityDto {
        private long assigned;
        private long completed;
        private long pending;
        private long overdue;
    }
}
