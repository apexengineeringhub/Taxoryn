package com.taxoryn.module.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Employee Task Workload & Performance Summary")
public class EmployeeWorkloadDto {

    @Schema(description = "Employee ID")
    private UUID employeeId;

    @Schema(description = "Employee Code", example = "EMP-001")
    private String employeeCode;

    @Schema(description = "Employee full name", example = "Rohan Deshmukh")
    private String employeeName;

    @Schema(description = "Total assigned tasks (excluding cancelled)", example = "15")
    private long totalAssignedTasks;

    @Schema(description = "Total pending tasks (TODO, IN_PROGRESS, UNDER_REVIEW)", example = "8")
    private long pendingTasks;

    @Schema(description = "Total overdue tasks (past due date)", example = "2")
    private long overdueTasks;

    @Schema(description = "Total completed tasks", example = "5")
    private long completedTasks;
}
