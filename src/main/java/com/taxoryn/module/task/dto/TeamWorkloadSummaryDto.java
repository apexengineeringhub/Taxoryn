package com.taxoryn.module.task.dto;

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
@Schema(description = "Team Member Workload Summary")
public class TeamWorkloadSummaryDto {

    private UUID userId;
    private UUID employeeId;
    private String name;
    private String email;
    private String designation;
    private String department;
    private long totalAssigned;
    private long todoCount;
    private long inProgressCount;
    private long underReviewCount;
    private long blockedCount;
    private long completedCount;
    private long overdueCount;
    private Integer totalEstimatedMinutes;
    private Integer totalActualMinutes;
}
