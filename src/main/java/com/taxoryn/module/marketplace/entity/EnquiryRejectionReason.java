package com.taxoryn.module.marketplace.entity;

/**
 * Standardized reasons for a practice declining an early tax enquiry.
 */
public enum EnquiryRejectionReason {
    SERVICE_NOT_AVAILABLE("Service Not Available", "The practice does not currently offer this specific tax service"),
    OUTSIDE_SERVICE_AREA("Outside Service Area", "The practice cannot service clients in this location or jurisdiction"),
    CURRENTLY_UNAVAILABLE("Currently Unavailable", "Practice team is temporarily unavailable or out of office"),
    CAPACITY_FULL("Capacity Full", "Practice is currently operating at maximum seasonal capacity"),
    OTHER("Other", "Other administrative or operational reason");

    private final String displayName;
    private final String description;

    EnquiryRejectionReason(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
