package com.taxoryn.module.compliance.work.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Result DTO returned from the compliance work generation boundary.
 * Provides deterministic status plus the work instance ID if created or already existing.
 */
@Getter
@Builder
public class ComplianceWorkGenerationResultDto {

    /** Generation outcome. */
    private final GenerationStatus status;

    /** ID of the created or existing work instance. Null for non-success statuses. */
    private final UUID workInstanceId;

    /** ID of the obligation this result is for. */
    private final UUID obligationId;

    /** Human-readable explanation of the result. */
    private final String message;

    /**
     * Deterministic generation outcome codes.
     */
    public enum GenerationStatus {
        /** Work instance created successfully for the first time. */
        CREATED,
        /** Work instance already existed for this obligation — idempotent return. */
        ALREADY_EXISTS,
        /** No work template code configured on the compliance rule. */
        TEMPLATE_NOT_CONFIGURED,
        /** Work template code configured but no matching active template found. */
        TEMPLATE_NOT_FOUND,
        /** No active engagement found for the client matching the required service. */
        ENGAGEMENT_NOT_CONFIGURED,
        /** Obligation status is terminal (COMPLETED/CANCELLED/FILED) — cannot generate. */
        OBLIGATION_NOT_ELIGIBLE,
        /** An unexpected error occurred during generation. */
        FAILED;

        public boolean isSuccess() {
            return this == CREATED || this == ALREADY_EXISTS;
        }
    }
}
