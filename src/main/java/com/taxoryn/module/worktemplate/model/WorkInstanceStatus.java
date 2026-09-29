package com.taxoryn.module.worktemplate.model;

import com.taxoryn.core.exception.BusinessValidationException;

/**
 * Lifecycle statuses for concrete generated Work Instances.
 */
public enum WorkInstanceStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    ON_HOLD,
    CANCELLED;

    public boolean canTransitionTo(WorkInstanceStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case NOT_STARTED -> target == IN_PROGRESS || target == ON_HOLD || target == CANCELLED;
            case IN_PROGRESS -> target == COMPLETED || target == ON_HOLD || target == CANCELLED;
            case ON_HOLD -> target == IN_PROGRESS || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    public void validateTransition(WorkInstanceStatus target) {
        if (!canTransitionTo(target)) {
            throw new BusinessValidationException(
                    String.format("Invalid work instance status transition from %s to %s", this.name(), target.name())
            );
        }
    }
}
