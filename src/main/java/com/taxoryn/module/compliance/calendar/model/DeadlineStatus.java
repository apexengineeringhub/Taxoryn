package com.taxoryn.module.compliance.calendar.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Deterministic statutory deadline classification buckets for Compliance Calendar and Deadline Radar.
 */
@Schema(description = "Deterministic deadline classification status")
public enum DeadlineStatus {
    @Schema(description = "Active obligation with due date prior to reference date")
    OVERDUE,

    @Schema(description = "Active obligation due on the reference date")
    DUE_TODAY,

    @Schema(description = "Active obligation due on reference date + 1 day")
    DUE_TOMORROW,

    @Schema(description = "Active obligation due within 3 days (excluding today and tomorrow)")
    DUE_WITHIN_3_DAYS,

    @Schema(description = "Active obligation due before the end of the current ISO week (Sunday)")
    DUE_THIS_WEEK,

    @Schema(description = "Active obligation due beyond the current ISO week")
    UPCOMING,

    @Schema(description = "Active obligation without a configured or calculable due date")
    NO_DUE_DATE,

    @Schema(description = "Completed obligation (excluded from active radar)")
    COMPLETED,

    @Schema(description = "Cancelled obligation (excluded from active radar)")
    CANCELLED
}
