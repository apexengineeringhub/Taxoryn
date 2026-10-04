package com.taxoryn.module.gst.integration;

import com.taxoryn.module.gst.integration.dto.GstHandshakeResponseDto;
import com.taxoryn.module.gst.integration.dto.GstIntegrationResultDto;

import java.util.Map;
import java.util.UUID;

/**
 * High-level GST application integration boundary.
 * Orchestrates GST domain operations with the underlying Government Integration Framework.
 */
public interface GstGovernmentIntegrationService {

    GstHandshakeResponseDto checkGstConnectionHealth(UUID connectionId);

    GstHandshakeResponseDto checkGstConnectionHealth(UUID connectionId, Map<String, Object> directives);

    GstIntegrationResultDto verifyGstin(UUID connectionId, String gstin);

    GstIntegrationResultDto verifyGstin(UUID connectionId, String gstin, Map<String, Object> options);

    GstIntegrationResultDto executeGstOperation(UUID connectionId, String operationType, Map<String, Object> payload);
}
