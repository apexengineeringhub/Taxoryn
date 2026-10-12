package com.taxoryn.module.worktemplate.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Normalized lifecycle status across unified work inventory")
public enum WorkInventoryStatus {
    @Schema(description = "Item is pending or not started")
    NOT_STARTED,

    @Schema(description = "Work is currently in progress")
    IN_PROGRESS,

    @Schema(description = "Work is actively blocked waiting on client or dependencies")
    BLOCKED,

    @Schema(description = "Work is ready for or undergoing internal review")
    UNDER_REVIEW,

    @Schema(description = "Obligation or return is ready for statutory filing")
    READY_FOR_FILING,

    @Schema(description = "Obligation or return has been filed with statutory authorities")
    FILED,

    @Schema(description = "Work item has reached completion")
    COMPLETED,

    @Schema(description = "Work item was cancelled or waived")
    CANCELLED
}
