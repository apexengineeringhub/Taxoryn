package com.taxoryn.module.content.entity;

/**
 * Controlled Lifecycle Statuses for Taxoryn Learn content.
 * Enforces strictly valid state transitions:
 * DRAFT -> UNDER_REVIEW -> APPROVED -> PUBLISHED -> ARCHIVED
 */
public enum ContentStatus {
    DRAFT,
    UNDER_REVIEW,
    APPROVED,
    PUBLISHED,
    ARCHIVED;

    /**
     * Validates whether a transition from the current status to targetStatus is allowed.
     */
    public boolean canTransitionTo(ContentStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }
        if (this == targetStatus) {
            return true;
        }

        return switch (this) {
            case DRAFT -> targetStatus == UNDER_REVIEW || targetStatus == ARCHIVED;
            case UNDER_REVIEW -> targetStatus == APPROVED || targetStatus == DRAFT || targetStatus == ARCHIVED;
            case APPROVED -> targetStatus == PUBLISHED || targetStatus == DRAFT || targetStatus == UNDER_REVIEW || targetStatus == ARCHIVED;
            case PUBLISHED -> targetStatus == ARCHIVED || targetStatus == DRAFT;
            case ARCHIVED -> targetStatus == DRAFT;
        };
    }
}
