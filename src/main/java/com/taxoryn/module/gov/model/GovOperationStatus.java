package com.taxoryn.module.gov.model;

/**
 * Standardized lifecycle status for government integration operations.
 */
public enum GovOperationStatus {
    /**
     * Operation record initialized and pending dispatch.
     */
    CREATED,

    /**
     * Request actively dispatched to provider adapter and awaiting response.
     */
    IN_PROGRESS,

    /**
     * Operation completed successfully with acknowledged result / reference ID.
     */
    SUCCEEDED,

    /**
     * Operation failed with terminal error or exhausted retries.
     */
    FAILED,

    /**
     * Operation encountered transient failure and is scheduled for retry.
     */
    RETRYING,

    /**
     * Operation was explicitly cancelled before completion.
     */
    CANCELLED;

    /**
     * Returns true if the operation has reached a final, immutable terminal state.
     */
    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }
}
