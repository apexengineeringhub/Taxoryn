package com.taxoryn.module.compliance.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the operational execution lifecycle of a practitioner compliance workflow.
 * Decoupled from statutory ComplianceObligationStatus and individual TaskStatus.
 */
@Schema(description = "Compliance Workflow Operational Status")
public enum ComplianceWorkflowStatus {
    @Schema(description = "Workflow created for obligation, not yet ready or assigned")
    CREATED,

    @Schema(description = "Staff/practitioner assigned, ready to commence preparation")
    READY,

    @Schema(description = "Practitioner actively preparing computations and tax documents")
    IN_PROGRESS,

    @Schema(description = "Preparation blocked waiting for required client documents or clarifications")
    WAITING_FOR_CLIENT,

    @Schema(description = "Preparation complete, submitted for maker-checker partner/manager review")
    UNDER_REVIEW,

    @Schema(description = "Reviewer identified discrepancies; returned to practitioner for rework")
    CHANGES_REQUIRED,

    @Schema(description = "Review approved; computation verified and ready for government filing")
    READY_FOR_FILING,

    @Schema(description = "Filing executed on portal, pending final receipt/challan validation")
    FILED,

    @Schema(description = "Filing submitted, waiting for official government acknowledgement/ARN confirmation")
    ACKNOWLEDGEMENT_PENDING,

    @Schema(description = "Filing completed, verified, and acknowledgement recorded")
    COMPLETED,

    @Schema(description = "Workflow cancelled or obligation waived")
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean isActive() {
        return !isTerminal();
    }
}
