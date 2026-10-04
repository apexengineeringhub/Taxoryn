package com.taxoryn.module.tds.integration;

import com.taxoryn.module.tds.dto.TdsDeductorProfileDto;
import com.taxoryn.module.tds.integration.dto.TdsHandshakeResponseDto;
import com.taxoryn.module.tds.integration.dto.TdsIntegrationResultDto;

import java.util.Map;
import java.util.UUID;

/**
 * High-level TDS application integration boundary.
 * Orchestrates TDS domain operations with the underlying Government Integration Framework.
 */
public interface TdsGovernmentIntegrationService {

    TdsHandshakeResponseDto checkTdsConnectionHealth(UUID connectionId);

    TdsHandshakeResponseDto checkTdsConnectionHealth(UUID connectionId, Map<String, Object> directives);

    TdsDeductorProfileDto lookupDeductor(String tan);

    TdsDeductorProfileDto lookupDeductor(UUID connectionId, String tan, Map<String, Object> options);

    TdsIntegrationResultDto executeTdsOperation(UUID connectionId, String operationType, Map<String, Object> payload);
 
    com.taxoryn.module.tds.dto.TdsPreparedReturnDto prepareReturn(com.taxoryn.module.tds.dto.TdsPrepareReturnRequest request);

    com.taxoryn.module.tds.dto.TdsReturnSubmissionResultDto submitReturn(UUID returnId, Map<String, Object> options);

    com.taxoryn.module.tds.dto.TdsReturnSubmissionResultDto submitReturn(com.taxoryn.module.tds.dto.TdsSubmitReturnRequest request);

    com.taxoryn.module.tds.dto.TdsReturnStatusDto getReturnStatus(UUID returnId);

    com.taxoryn.module.tds.dto.TdsReturnStatusDto checkReturnStatus(UUID returnId);

    com.taxoryn.module.tds.dto.TdsReturnStatusDto checkReturnStatus(UUID returnId, UUID connectionId, Map<String, Object> options);
}
