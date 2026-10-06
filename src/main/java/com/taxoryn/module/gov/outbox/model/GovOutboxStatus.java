package com.taxoryn.module.gov.outbox.model;

/**
 * Lifecycle status of transactional outbox events in the government integration subsystem.
 */
public enum GovOutboxStatus {
    /**
     * Event is enqueued and waiting to be claimed by the background processor.
     */
    PENDING,

    /**
     * Event has been claimed and is actively executing.
     */
    PROCESSING,

    /**
     * Event completed successfully.
     */
    COMPLETED,

    /**
     * Event exhausted all retry attempts and terminated in error.
     */
    FAILED,

    /**
     * Event was explicitly cancelled prior to execution or reconciliation.
     */
    CANCELLED
}
