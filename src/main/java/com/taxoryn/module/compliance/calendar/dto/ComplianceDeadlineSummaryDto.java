package com.taxoryn.module.compliance.calendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Deterministic aggregate summary of compliance deadlines")
public class ComplianceDeadlineSummaryDto {

    @Schema(description = "Count of active obligations past statutory due date", example = "8")
    private long overdue;

    @Schema(description = "Count of active obligations due today", example = "5")
    private long dueToday;

    @Schema(description = "Count of active obligations due tomorrow", example = "7")
    private long dueTomorrow;

    @Schema(description = "Count of active obligations due within next 3 days (excluding today/tomorrow)", example = "12")
    private long dueWithin3Days;

    @Schema(description = "Count of active obligations due by end of ISO week (Sunday)", example = "21")
    private long dueThisWeek;

    @Schema(description = "Count of active obligations due in future weeks", example = "47")
    private long upcoming;

    @Schema(description = "Count of active obligations without a configured due date", example = "3")
    private long noDueDate;

    @Schema(description = "Total active tracked obligations participating in the radar", example = "103")
    private long totalActive;

    @Schema(description = "Count of completed obligations in scope (excluded from active radar)", example = "45")
    private long completed;

    @Schema(description = "Count of cancelled obligations in scope (excluded from active radar)", example = "2")
    private long cancelled;
}
