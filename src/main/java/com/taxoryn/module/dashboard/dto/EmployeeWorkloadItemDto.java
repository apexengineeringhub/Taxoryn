package com.taxoryn.module.dashboard.dto;

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
@Schema(description = "Employee Workload Item in Dashboard")
public class EmployeeWorkloadItemDto {

    @Schema(description = "Employee ID")
    private UUID employeeId;

    @Schema(description = "Employee Code", example = "EMP-001")
    private String employeeCode;

    @Schema(description = "Employee Full Name", example = "Rohan Deshmukh")
    private String employeeName;

    @Schema(description = "Employee Email", example = "rohan@taxpractice.com")
    private String email;

    @Schema(description = "Department", example = "Direct Tax")
    private String department;

    @Schema(description = "Designation", example = "Senior Associate")
    private String designation;

    @Schema(description = "Total active assigned tasks", example = "15")
    private long assignedTasks;

    @Schema(description = "Pending tasks (TODO, IN_PROGRESS, UNDER_REVIEW)", example = "10")
    private long pendingTasks;

    @Schema(description = "Overdue tasks", example = "2")
    private long overdueTasks;
}
