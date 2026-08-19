package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization Task Statistics")
public class TaskStatsDto {

    @Schema(description = "Total number of tasks", example = "320")
    private long total;

    @Schema(description = "Number of pending tasks (TODO, IN_PROGRESS, UNDER_REVIEW)", example = "45")
    private long pending;

    @Schema(description = "Number of overdue tasks", example = "8")
    private long overdue;

    @Schema(description = "Number of completed tasks", example = "267")
    private long completed;
}
