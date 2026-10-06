package com.taxoryn.module.gov.observability.service;

import com.taxoryn.module.gov.observability.dto.GovOperationsDiagnosticsDto;
import com.taxoryn.module.gov.observability.dto.GovOperationsSummaryDto;
import com.taxoryn.module.gov.reliability.dto.GovReliabilityMetricsSnapshot;

import java.util.UUID;

/**
 * Service providing operational observability, diagnostics, and metrics
 * for the Government Integration Framework.
 */
public interface GovOperationsObservabilityService {

    /**
     * Retrieves an operational summary for the specified organization.
     *
     * @param organizationId tenant organization ID
     * @return summary of operations, outbox statuses, and provider health
     */
    GovOperationsSummaryDto getOperationsSummary(UUID organizationId);

    /**
     * Retrieves detailed sanitized diagnostics for a specific government operation.
     *
     * @param operationId operation ID
     * @return diagnostics DTO
     */
    GovOperationsDiagnosticsDto getOperationDiagnostics(UUID operationId);

    /**
     * Retrieves the current snapshot of reliability and recovery metrics.
     *
     * @return metrics snapshot
     */
    GovReliabilityMetricsSnapshot getMetricsSnapshot();
}
