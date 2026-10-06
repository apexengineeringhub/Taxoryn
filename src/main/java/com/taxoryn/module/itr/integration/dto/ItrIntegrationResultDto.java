package com.taxoryn.module.itr.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * High-level ITR domain integration result DTO.
 * Decoupled from low-level government provider abstractions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ITR Government Integration Result")
public class ItrIntegrationResultDto {

    @Schema(description = "Indicates whether the integration operation was successful")
    private boolean success;

    @Schema(description = "Audit ID of the underlying government operation")
    private UUID operationId;

    @Schema(description = "Associated Government Connection ID")
    private UUID connectionId;

    @Schema(description = "Permanent Account Number (PAN)", example = "ABCDE1234F")
    private String pan;

    @Schema(description = "Executed operation type", example = "VERIFY_PAN")
    private String operationType;

    @Schema(description = "Correlation ID for request tracing")
    private String correlationId;

    @Schema(description = "Provider acknowledgement or transaction reference", example = "ITD-PAN-ACK-9F8A2B")
    private String providerReferenceId;

    @Schema(description = "Normalized error code if operation failed")
    private String errorCode;

    @Schema(description = "Error description if operation failed")
    private String errorMessage;

    @Schema(description = "Operation response payload data")
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
}
