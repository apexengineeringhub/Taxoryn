package com.taxoryn.module.gst.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * High-level GST domain integration result DTO.
 * Decoupled from low-level government provider abstractions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GstIntegrationResultDto {

    private boolean success;
    private UUID operationId;
    private UUID connectionId;
    private String gstin;
    private String operationType;
    private String correlationId;
    private String providerReferenceId;
    private String errorCode;
    private String errorMessage;

    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
}
