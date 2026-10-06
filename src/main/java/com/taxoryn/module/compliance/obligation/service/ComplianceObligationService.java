package com.taxoryn.module.compliance.obligation.service;

import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.UpdateObligationStatusRequest;
import com.taxoryn.module.compliance.obligation.dto.CancelObligationRequest;
import com.taxoryn.module.compliance.obligation.dto.ComplianceObligationFilterParams;
import com.taxoryn.module.compliance.obligation.dto.ComplianceObligationSummaryDto;
import com.taxoryn.module.compliance.obligation.dto.GenerateObligationsRequest;
import com.taxoryn.module.compliance.obligation.dto.GeneratedObligationsResponseDto;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;

import java.util.List;
import java.util.UUID;

/**
 * Service for deterministic creation, retrieval, and lifecycle management of client compliance obligations.
 */
public interface ComplianceObligationService {

    /**
     * Idempotently generates obligations for all applicable rules matching client facts and the requested period.
     */
    GeneratedObligationsResponseDto generateObligations(UUID clientId, GenerateObligationsRequest request);

    /**
     * Idempotently creates or retrieves an obligation for a single specific rule code and period.
     */
    ComplianceObligationDto createOrGetSingleObligation(UUID clientId, String ruleCode, CompliancePeriodType periodType, String periodKey);

    /**
     * Retrieves an obligation by ID, ensuring tenant and client isolation.
     */
    ComplianceObligationDto getObligationById(UUID clientId, UUID obligationId);

    /**
     * Lists obligations for a client with optional multi-attribute filtering.
     */
    List<ComplianceObligationDto> listClientObligations(UUID clientId, ComplianceObligationFilterParams params);

    /**
     * Transitions an obligation's lifecycle status.
     */
    ComplianceObligationDto updateObligationStatus(UUID clientId, UUID obligationId, UpdateObligationStatusRequest request);

    /**
     * Cancels an obligation with mandatory audit rationale.
     */
    ComplianceObligationDto cancelObligation(UUID clientId, UUID obligationId, CancelObligationRequest request);

    /**
     * Computes high-level obligation metrics and domain distribution for a client.
     */
    ComplianceObligationSummaryDto getClientObligationSummary(UUID clientId);
}
