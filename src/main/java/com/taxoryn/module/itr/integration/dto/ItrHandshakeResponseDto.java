package com.taxoryn.module.itr.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * High-level Income Tax (ITR) domain gateway handshake response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ITR Government Provider Handshake & Health Response")
public class ItrHandshakeResponseDto {

    @Schema(description = "Government connection ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID connectionId;

    @Schema(description = "Display name of the connection", example = "Income Tax Department E-Filing Gateway")
    private String displayName;

    @Schema(description = "Health status (HEALTHY, AUTH_REQUIRED, UNAVAILABLE, ERROR)", example = "HEALTHY")
    private String healthStatus;

    @Schema(description = "Network roundtrip latency in milliseconds", example = "25")
    private long latencyMs;

    @Schema(description = "Health check message or diagnostic details", example = "ITD Gateway handshake successful")
    private String message;

    @Schema(description = "Timestamp of the health check")
    private Instant lastHealthCheckAt;
}
