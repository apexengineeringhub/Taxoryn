package com.taxoryn.module.tds.integration;

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

    TdsIntegrationResultDto executeTdsOperation(UUID connectionId, String operationType, Map<String, Object> payload);
}
