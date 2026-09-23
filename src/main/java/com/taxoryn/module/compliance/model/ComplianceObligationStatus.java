package com.taxoryn.module.compliance.model;

import lombok.Getter;

import java.util.Set;

/**
 * Controlled lifecycle status for compliance obligations.
 * Distinct from operational ServiceWorkflowStatus and individual TaskStatus.
 */
@Getter
public enum ComplianceObligationStatus {

    UPCOMING("Upcoming", "Statutory obligation is scheduled in advance"),
    READY("Ready to Start", "Pre-requisites and data collection commenced"),
    IN_PROGRESS("In Progress", "Preparation, computation, or review underway"),
    WAITING_FOR_CLIENT("Waiting for Client", "Blocked pending client data, confirmation, or OTP"),
    READY_FOR_FILING("Ready for Filing", "Reviewed and authorized for statutory portal submission"),
    FILED("Filed", "Successfully filed on government portal, acknowledgement obtained"),
    COMPLETED("Completed", "Filing confirmed and deliverable shared with client"),
    OVERDUE("Overdue", "Statutory due date has passed without successful completion"),
    CANCELLED("Cancelled", "Obligation waived, dismissed, or cancelled");

    private final String displayName;
    private final String description;

    ComplianceObligationStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean isActive() {
        return this == UPCOMING
                || this == READY
                || this == IN_PROGRESS
                || this == WAITING_FOR_CLIENT
                || this == READY_FOR_FILING
                || this == FILED
                || this == OVERDUE;
    }

    public boolean canTransitionTo(ComplianceObligationStatus target) {
        if (this == target) {
            return true;
        }

        if (this.isTerminal()) {
            return false;
        }

        return switch (this) {
            case UPCOMING -> target == READY || target == IN_PROGRESS || target == WAITING_FOR_CLIENT || target == READY_FOR_FILING || target == FILED || target == COMPLETED || target == OVERDUE || target == CANCELLED;
            case READY -> target == IN_PROGRESS || target == WAITING_FOR_CLIENT || target == READY_FOR_FILING || target == FILED || target == COMPLETED || target == OVERDUE || target == CANCELLED;
            case IN_PROGRESS -> target == WAITING_FOR_CLIENT || target == READY_FOR_FILING || target == FILED || target == COMPLETED || target == OVERDUE || target == CANCELLED;
            case WAITING_FOR_CLIENT -> target == IN_PROGRESS || target == READY_FOR_FILING || target == FILED || target == COMPLETED || target == OVERDUE || target == CANCELLED;
            case READY_FOR_FILING -> target == IN_PROGRESS || target == WAITING_FOR_CLIENT || target == FILED || target == COMPLETED || target == OVERDUE || target == CANCELLED;
            case FILED -> target == COMPLETED || target == IN_PROGRESS || target == CANCELLED;
            case OVERDUE -> target == IN_PROGRESS || target == WAITING_FOR_CLIENT || target == READY_FOR_FILING || target == FILED || target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
