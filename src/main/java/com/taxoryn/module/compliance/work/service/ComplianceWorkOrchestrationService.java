package com.taxoryn.module.compliance.work.service;

import com.taxoryn.module.compliance.work.dto.ComplianceWorkGenerationResultDto;

import java.util.UUID;

/**
 * Compliance-side orchestration service for Obligation → Work generation.
 * <p>
 * This service validates the obligation, resolves the work template code from the rule,
 * calls {@link com.taxoryn.module.compliance.work.port.ComplianceWorkGenerationPort},
 * and updates the obligation's workInstanceId on success.
 * <p>
 * It MUST NOT directly inject work-module repositories. All work generation
 * crosses the boundary via the port.
 */
public interface ComplianceWorkOrchestrationService {

    /**
     * Generates (or idempotently retrieves) a work instance for the given compliance obligation.
     *
     * @param clientId     Client UUID
     * @param obligationId Compliance obligation UUID
     * @return             Deterministic result with status and workInstanceId
     */
    ComplianceWorkGenerationResultDto generateWorkForObligation(UUID clientId, UUID obligationId);
}
