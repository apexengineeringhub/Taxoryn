package com.taxoryn.module.workflow.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Execution status for an individual workflow step.
 */
@Getter
@RequiredArgsConstructor
public enum StepStatus {

    PENDING("Pending", "Step is waiting to be started"),
    IN_PROGRESS("In Progress", "Step is currently being executed"),
    WAITING_FOR_CLIENT("Waiting for Client", "Step is awaiting client documents or approval"),
    COMPLETED("Completed", "Step has been completed successfully"),
    SKIPPED("Skipped", "Optional step was bypassed");

    private final String displayName;
    private final String description;
}
