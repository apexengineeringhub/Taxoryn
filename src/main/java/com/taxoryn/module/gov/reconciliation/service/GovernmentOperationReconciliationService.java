package com.taxoryn.module.gov.reconciliation.service;

import com.taxoryn.module.gov.dto.GovOperationDto;
import com.taxoryn.module.gov.reconciliation.dto.GovReconciliationBatchResultDto;
import com.taxoryn.module.gov.reconciliation.dto.GovReconciliationResultDto;

import java.util.List;
import java.util.UUID;

/**
 * Authoritative reconciliation engine for verifying and synchronizing Taxoryn government
 * integration operations against external provider status.
 */
public interface GovernmentOperationReconciliationService {

    /**
     * Reconciles a single government operation against the authoritative provider.
     * Enforces the invariant: SUBMITTED != FILED.
     */
    GovReconciliationResultDto reconcileOperation(UUID operationId);

    /**
     * Executes a bounded batch reconciliation of eligible in-flight or ambiguous operations.
     * Guaranteed to claim operations atomically without concurrent duplicate worker processing.
     */
    GovReconciliationBatchResultDto reconcileBatch(int batchSize);

    /**
     * Discovers operations eligible for reconciliation within the active tenant context.
     */
    List<GovOperationDto> findEligibleOperations(int limit);
}
