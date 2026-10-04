package com.taxoryn.module.gst.integration;

import com.taxoryn.module.gst.dto.GstTaxpayerProfileDto;
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

    GstTaxpayerProfileDto lookupTaxpayer(String gstin);

    GstTaxpayerProfileDto lookupTaxpayer(UUID connectionId, String gstin, Map<String, Object> options);

    com.taxoryn.module.gst.dto.GstPreparedReturnDto prepareReturn(com.taxoryn.module.gst.dto.GstPrepareReturnRequest request);

    com.taxoryn.module.gst.dto.GstReturnSubmissionResultDto submitReturn(com.taxoryn.module.gst.dto.GstSubmitReturnRequest request);

    com.taxoryn.module.gst.dto.GstReturnStatusDto checkReturnStatus(UUID filingId);

    com.taxoryn.module.gst.dto.GstReturnStatusDto checkReturnStatus(UUID filingId, UUID connectionId, Map<String, Object> options);

    GstIntegrationResultDto executeGstOperation(UUID connectionId, String operationType, Map<String, Object> payload);
}
