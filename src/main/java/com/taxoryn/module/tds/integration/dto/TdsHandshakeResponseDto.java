package com.taxoryn.module.tds.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * High-level Tax Deducted at Source (TDS) domain gateway handshake response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "TDS Government Provider Handshake & Health Response")
public class TdsHandshakeResponseDto {

    @Schema(description = "Government connection ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID connectionId;

    @Schema(description = "Display name of the connection", example = "TRACES Portal TDS Gateway")
    private String displayName;

    @Schema(description = "Health status (HEALTHY, AUTH_REQUIRED, UNAVAILABLE, ERROR)", example = "HEALTHY")
    private String healthStatus;

    @Schema(description = "Network roundtrip latency in milliseconds", example = "20")
    private long latencyMs;

    @Schema(description = "Health check message or diagnostic details", example = "TDS TRACES Gateway handshake successful")
    private String message;

    @Schema(description = "Timestamp of the health check")
    private Instant lastHealthCheckAt;
}
