package com.taxoryn.module.compliance.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Controlled lifecycle statuses for Compliance Work Items.
 */
@Getter
@RequiredArgsConstructor
public enum ComplianceWorkStatus {

    NOT_STARTED("Not Started", "Work item created and queued for preparation"),
    DOCUMENTS_PENDING("Documents Pending", "Awaiting statutory records, invoices, or client working papers"),
    IN_PREPARATION("In Preparation", "Practitioner actively preparing computations or return schedules"),
    IN_REVIEW("In Review", "Prepared draft submitted for manager/partner quality review"),
    READY_TO_FILE("Ready to File", "Approved internally, awaiting final client sign-off or portal submission"),
    FILED("Filed", "Successfully filed on tax portal, acknowledgement generated"),
    COMPLETED("Completed", "All post-filing verifications and client communications completed"),
    ON_HOLD("On Hold", "Temporarily paused due to query, missing clarification, or dispute"),
    CANCELLED("Cancelled", "Work item cancelled or deemed not applicable");

    private final String displayName;
    private final String description;

    private static final Map<ComplianceWorkStatus, Set<ComplianceWorkStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = Map.of(
                NOT_STARTED, EnumSet.of(DOCUMENTS_PENDING, IN_PREPARATION, ON_HOLD, CANCELLED),
                DOCUMENTS_PENDING, EnumSet.of(IN_PREPARATION, ON_HOLD, CANCELLED),
                IN_PREPARATION, EnumSet.of(DOCUMENTS_PENDING, IN_REVIEW, READY_TO_FILE, ON_HOLD, CANCELLED),
                IN_REVIEW, EnumSet.of(IN_PREPARATION, READY_TO_FILE, ON_HOLD, CANCELLED),
                READY_TO_FILE, EnumSet.of(IN_PREPARATION, IN_REVIEW, FILED, ON_HOLD, CANCELLED),
                FILED, EnumSet.of(COMPLETED, ON_HOLD, CANCELLED),
                ON_HOLD, EnumSet.of(NOT_STARTED, DOCUMENTS_PENDING, IN_PREPARATION, IN_REVIEW, READY_TO_FILE, CANCELLED),
                COMPLETED, Collections.emptySet(),
                CANCELLED, Collections.emptySet()
        );
    }

    /**
     * Checks if transitioning from this status to target status is valid.
     */
    public boolean canTransitionTo(ComplianceWorkStatus target) {
        if (this == target) {
            return true;
        }
        Set<ComplianceWorkStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(this, Collections.emptySet());
        return allowed.contains(target);
    }
}
