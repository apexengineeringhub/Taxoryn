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
        private long overdueTasks;
        private long completedTasks;
        @Schema(description = "Task completion rate percentage (0-100)", example = "87.5")
        private double completionRate;
    }
}
