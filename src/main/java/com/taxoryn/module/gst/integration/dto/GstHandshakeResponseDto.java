package com.taxoryn.module.gst.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * High-level GST domain gateway handshake response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GstHandshakeResponseDto {

    private UUID connectionId;
    private String displayName;
    private String healthStatus;
    private long latencyMs;
    private String message;
    private Instant lastHealthCheckAt;
}
