package com.taxoryn.module.marketplace.entity;

/**
 * Lifecycle states for Customer Tax Requirements.
 * DRAFT -> SUBMITTED -> CANCELLED / CLOSED
 */
public enum TaxRequirementStatus {
    DRAFT("Draft"),
    SUBMITTED("Submitted"),
    CANCELLED("Cancelled"),
    CLOSED("Closed");

    private final String displayName;

    TaxRequirementStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEditable() {
        return this == DRAFT;
    }

    public boolean isCancellable() {
        return this == DRAFT || this == SUBMITTED;
    }
}
