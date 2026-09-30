package com.taxoryn.module.engagement.model;

import com.taxoryn.core.exception.BusinessValidationException;

import java.util.Set;

/**
 * Controlled status lifecycle of a client engagement.
 */
public enum EngagementStatus {
    DRAFT,
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    CANCELLED;

    /**
     * Checks if the transition from current status to target status is valid per business rules.
     */
    public boolean canTransitionTo(EngagementStatus target) {
        if (target == null || this == target) {
            return true;
        }
        return switch (this) {
            case DRAFT -> target == ACTIVE || target == CANCELLED;
            case ACTIVE -> target == ON_HOLD || target == COMPLETED || target == CANCELLED;
            case ON_HOLD -> target == ACTIVE || target == CANCELLED;
            case COMPLETED, CANCELLED -> false; // Terminal states
        };
    }

    /**
     * Validates status transition and throws BusinessValidationException if invalid.
     */
    public void validateTransition(EngagementStatus target) {
        if (!canTransitionTo(target)) {
            throw new BusinessValidationException(
                    String.format("Invalid engagement status transition from %s to %s. Allowed transitions: %s",
                            this.name(),
                            target != null ? target.name() : "null",
                            getAllowedTransitionsDescription())
            );
        }
    }

    private String getAllowedTransitionsDescription() {
        return switch (this) {
            case DRAFT -> "ACTIVE, CANCELLED";
            case ACTIVE -> "ON_HOLD, COMPLETED, CANCELLED";
            case ON_HOLD -> "ACTIVE, CANCELLED";
            case COMPLETED -> "None (COMPLETED is terminal)";
            case CANCELLED -> "None (CANCELLED is terminal)";
        };
    }
}
