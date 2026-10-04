package com.taxoryn.module.itr.integration;

import com.taxoryn.module.itr.integration.dto.ItrHandshakeResponseDto;
import com.taxoryn.module.itr.integration.dto.ItrIntegrationResultDto;

import java.util.Map;
import java.util.UUID;

/**
 * High-level ITR application integration boundary.
 * Orchestrates Income Tax domain operations with the underlying Government Integration Framework.
 */
public interface ItrGovernmentIntegrationService {

    ItrHandshakeResponseDto checkItrConnectionHealth(UUID connectionId);

    ItrHandshakeResponseDto checkItrConnectionHealth(UUID connectionId, Map<String, Object> directives);

    ItrIntegrationResultDto verifyPan(UUID connectionId, String pan);

    ItrIntegrationResultDto verifyPan(UUID connectionId, String pan, Map<String, Object> options);

    ItrIntegrationResultDto executeItrOperation(UUID connectionId, String operationType, Map<String, Object> payload);
}
