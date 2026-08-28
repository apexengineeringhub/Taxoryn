package com.taxoryn.module.content.entity;

/**
 * Controlled Lifecycle Statuses for Taxoryn Content & Marketing Studio.
 * Workflow:
 * DRAFT -> SUBMITTED -> IN_REVIEW -> APPROVED -> PUBLISHED
 *                                  \-> REJECTED -> DRAFT
 *                                  \-> SCHEDULED -> PUBLISHED
 * PUBLISHED -> ARCHIVED -> DRAFT (restore)
 */
public enum ContentStatus {
    DRAFT,
    SUBMITTED,
    IN_REVIEW,
    UNDER_REVIEW, // Backward-compatible alias for SUBMITTED/IN_REVIEW
    APPROVED,
    SCHEDULED,
    REJECTED,
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
            case DRAFT -> targetStatus == SUBMITTED || targetStatus == IN_REVIEW || targetStatus == UNDER_REVIEW || targetStatus == ARCHIVED;
            case SUBMITTED, UNDER_REVIEW -> targetStatus == IN_REVIEW || targetStatus == APPROVED || targetStatus == REJECTED || targetStatus == DRAFT || targetStatus == ARCHIVED;
            case IN_REVIEW -> targetStatus == APPROVED || targetStatus == REJECTED || targetStatus == DRAFT || targetStatus == ARCHIVED;
            case REJECTED -> targetStatus == DRAFT || targetStatus == SUBMITTED || targetStatus == ARCHIVED;
            case APPROVED -> targetStatus == PUBLISHED || targetStatus == SCHEDULED || targetStatus == DRAFT || targetStatus == IN_REVIEW || targetStatus == ARCHIVED;
            case SCHEDULED -> targetStatus == PUBLISHED || targetStatus == APPROVED || targetStatus == DRAFT || targetStatus == ARCHIVED;
            case PUBLISHED -> targetStatus == ARCHIVED || targetStatus == DRAFT;
            case ARCHIVED -> targetStatus == DRAFT;
        };
    }
}
