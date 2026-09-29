package com.taxoryn.module.worktemplate.model;

import com.taxoryn.core.exception.BusinessValidationException;

/**
 * Lifecycle statuses for Work Templates.
 */
public enum WorkTemplateStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    ARCHIVED;

    public boolean canTransitionTo(WorkTemplateStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case DRAFT -> target == ACTIVE || target == ARCHIVED;
            case ACTIVE -> target == INACTIVE || target == ARCHIVED;
            case INACTIVE -> target == ACTIVE || target == ARCHIVED;
            case ARCHIVED -> false;
        };
    }

    public void validateTransition(WorkTemplateStatus target) {
        if (!canTransitionTo(target)) {
            throw new BusinessValidationException(
                    String.format("Invalid work template status transition from %s to %s", this.name(), target.name())
            );
        }
    }
}
