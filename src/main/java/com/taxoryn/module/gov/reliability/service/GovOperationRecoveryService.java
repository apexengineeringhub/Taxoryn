package com.taxoryn.module.gov.reliability.service;

import com.taxoryn.module.gov.dto.GovOperationDto;
import com.taxoryn.module.gov.dto.GovIntegrationResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for scanning, recovering, and reconciling in-flight, interrupted,
 * or ambiguous government operations (e.g. after application restart, network loss, or timeout).
 */
public interface GovOperationRecoveryService {

    /**
     * Discovers in-flight or retrying operations for the active tenant created/updated before the threshold timestamp.
     */
    List<GovOperationDto> findRecoverableOperations(Instant olderThan);

    /**
     * Recovers and resumes a specific pending or interrupted government operation.
     */
    GovIntegrationResult recoverOperation(UUID operationId);

    /**
     * Reconciles an ambiguous or timeout operation outcome against provider reference or idempotency records.
     */
    GovOperationDto reconcileAmbiguousOutcome(UUID operationId);
}
