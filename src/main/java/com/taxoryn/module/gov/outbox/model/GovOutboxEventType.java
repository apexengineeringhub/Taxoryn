package com.taxoryn.module.gov.outbox.model;

/**
 * Standard event type constants for government integration outbox events.
 */
public final class GovOutboxEventType {

    private GovOutboxEventType() {
        // Prevent instantiation
    }

    /**
     * Dispatch an asynchronous government integration operation to the provider.
     */
    public static final String GOV_OPERATION_DISPATCH = "GOV_OPERATION_DISPATCH";

    /**
     * Retry an operation that encountered a recoverable or transient failure.
     */
    public static final String GOV_OPERATION_RETRY = "GOV_OPERATION_RETRY";

    /**
     * Reconcile an ambiguous or interrupted operation with the external provider.
     */
    public static final String GOV_OPERATION_RECONCILIATION = "GOV_OPERATION_RECONCILIATION";

    /**
     * Perform an asynchronous health check on a government connection.
     */
    public static final String GOV_CONNECTION_HEALTH_CHECK = "GOV_CONNECTION_HEALTH_CHECK";
}
