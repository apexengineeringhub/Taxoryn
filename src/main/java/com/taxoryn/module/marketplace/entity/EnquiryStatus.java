package com.taxoryn.module.marketplace.entity;

/**
 * Controlled lifecycle states for Taxoryn Customer-Practice Enquiries.
 * <p>
 * Standard Flow:
 * NEW -> RECEIVED -> ACCEPTED -> IN_PROGRESS -> COMPLETED
 * <p>
 * Terminal / Alternative States:
 * REJECTED, CANCELLED, EXPIRED
 */
public enum EnquiryStatus {
    NEW("New", "Enquiry submitted by customer"),
    RECEIVED("Received", "Delivered to practice inbox"),
    ACCEPTED("Accepted", "Practice accepted the enquiry"),
    IN_PROGRESS("In Progress", "Work on tax compliance is underway"),
    COMPLETED("Completed", "Service delivered and closed"),
    REJECTED("Rejected", "Declined by practice"),
    CANCELLED("Cancelled", "Cancelled by customer"),
    EXPIRED("Expired", "Closed due to inactivity");

    private final String displayName;
    private final String description;

    EnquiryStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status can transition to the target status based on business state machine rules.
     */
    public boolean canTransitionTo(EnquiryStatus target) {
        if (target == null || this == target) {
            return false;
        }

        return switch (this) {
            case NEW -> target == RECEIVED || target == ACCEPTED || target == REJECTED || target == CANCELLED || target == EXPIRED;
            case RECEIVED -> target == ACCEPTED || target == REJECTED || target == CANCELLED || target == EXPIRED;
            case ACCEPTED -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == COMPLETED;
            case COMPLETED, REJECTED, CANCELLED, EXPIRED -> false; // Terminal states
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == REJECTED || this == CANCELLED || this == EXPIRED;
    }

    public boolean isCustomerCancellable() {
        return this == NEW || this == RECEIVED || this == ACCEPTED;
    }

    public boolean isReviewEligible() {
        return this == COMPLETED;
    }
}
