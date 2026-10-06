package com.taxoryn.module.gov.observability.util;

/**
 * Standard audit event action constants for Government Integration operations.
 */
public final class GovAuditEventConstants {

    private GovAuditEventConstants() {
        // Utility class
    }

    public static final String GOV_OPERATION_INITIATED = "GOV_OPERATION_INITIATED";
    public static final String GOV_OPERATION_SUCCEEDED = "GOV_OPERATION_SUCCEEDED";
    public static final String GOV_OPERATION_FAILED = "GOV_OPERATION_FAILED";
    public static final String GOV_OPERATION_RETRY = "GOV_OPERATION_RETRY";

    public static final String GOV_OUTBOX_EVENT_ENQUEUED = "GOV_OUTBOX_EVENT_ENQUEUED";
    public static final String GOV_OUTBOX_EVENT_COMPLETED = "GOV_OUTBOX_EVENT_COMPLETED";
    public static final String GOV_OUTBOX_EVENT_FAILED = "GOV_OUTBOX_EVENT_FAILED";
    public static final String GOV_OUTBOX_EVENT_RETRY_SCHEDULED = "GOV_OUTBOX_EVENT_RETRY_SCHEDULED";
    public static final String GOV_OUTBOX_EVENT_CANCELLED = "GOV_OUTBOX_EVENT_CANCELLED";

    public static final String GOV_RECONCILIATION_STARTED = "GOV_RECONCILIATION_STARTED";
    public static final String GOV_RECONCILIATION_STATUS_CHANGED = "GOV_RECONCILIATION_STATUS_CHANGED";
    public static final String GOV_RECONCILIATION_COMPLETED = "GOV_RECONCILIATION_COMPLETED";
    public static final String GOV_RECONCILIATION_FAILED = "GOV_RECONCILIATION_FAILED";
    public static final String GOV_RECONCILIATION_IN_PROGRESS = "GOV_RECONCILIATION_IN_PROGRESS";
}
