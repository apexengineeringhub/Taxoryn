package com.taxoryn.module.gov.reliability.service;

import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.reliability.dto.GovReliabilityMetricsSnapshot;
import com.taxoryn.module.gov.reliability.model.GovFailureClassification;
import com.taxoryn.module.gov.reliability.model.GovOperationOutcome;
import com.taxoryn.module.gov.reliability.policy.GovRetryPolicy;

/**
 * Provider-neutral reliability service coordinating retry policies, failure classification,
 * outcome normalization, and resilience execution for government operations.
 */
public interface GovReliabilityService {

    /**
     * Executes a government operation with default retry policy (max 3 attempts, exponential backoff).
     */
    GovIntegrationResult executeWithRetry(GovIntegrationRequest request);

    /**
     * Executes a government operation with a custom retry policy.
     */
    GovIntegrationResult executeWithRetry(GovIntegrationRequest request, GovRetryPolicy policy);

    /**
     * Classifies a failure based on GovErrorCode, optional HTTP status code, and raw message.
     */
    GovFailureClassification classifyFailure(GovErrorCode errorCode, Integer httpStatusCode, String rawMessage);

    /**
     * Normalizes a GovIntegrationResult into a high-level GovOperationOutcome.
     */
    GovOperationOutcome normalizeOutcome(GovIntegrationResult result);

    /**
     * Returns a snapshot of reliability metrics.
     */
    GovReliabilityMetricsSnapshot getMetrics();
}
