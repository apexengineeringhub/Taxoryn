package com.taxoryn.module.workflow.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Operational lifecycle status for a client service workflow instance.
 */
@Getter
@RequiredArgsConstructor
public enum ServiceWorkflowStatus {

    NOT_STARTED("Not Started", "Workflow initialized but work has not begun"),
    IN_PROGRESS("In Progress", "Practice team is actively executing workflow steps"),
    WAITING_FOR_CLIENT("Waiting for Client", "Workflow is blocked pending client document, info, or approval"),
    READY_FOR_FILING("Ready for Filing", "All computations, reconciliations, and reviews are approved for filing"),
    FILED("Filed", "Return or response has been submitted to the statutory portal"),
    COMPLETED("Completed", "All steps, acknowledgements, and client dispatches are finished"),
    CANCELLED("Cancelled", "Workflow instance was cancelled");

    private final String displayName;
    private final String description;

    public boolean canTransitionTo(ServiceWorkflowStatus target) {
        if (target == null || target == this) {
            return true;
        }

        return switch (this) {
            case NOT_STARTED -> target == IN_PROGRESS || target == WAITING_FOR_CLIENT || target == CANCELLED;
            case IN_PROGRESS -> target == WAITING_FOR_CLIENT || target == READY_FOR_FILING || target == FILED || target == COMPLETED || target == CANCELLED;
            case WAITING_FOR_CLIENT -> target == IN_PROGRESS || target == READY_FOR_FILING || target == CANCELLED;
            case READY_FOR_FILING -> target == IN_PROGRESS || target == WAITING_FOR_CLIENT || target == FILED || target == COMPLETED || target == CANCELLED;
            case FILED -> target == COMPLETED || target == IN_PROGRESS;
            case COMPLETED, CANCELLED -> false; // Terminal states
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
