package com.taxoryn.module.workflow.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Status of a compliance period for a client service.
 */
@Getter
@RequiredArgsConstructor
public enum ServicePeriodStatus {

    PLANNED("Planned", "Future compliance cycle planned on practice schedule"),
    ACTIVE("Active", "Current active compliance cycle with operational workflows"),
    COMPLETED("Completed", "Period compliance obligations completed"),
    OVERDUE("Overdue", "Period has past its statutory or internal due date"),
    CANCELLED("Cancelled", "Period tracking cancelled");

    private final String displayName;
    private final String description;
}
