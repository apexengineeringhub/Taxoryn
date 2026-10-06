package com.taxoryn.module.gov.dto;

import com.taxoryn.module.gov.model.GovConnectionHealthStatus;
import com.taxoryn.module.gov.model.GovConnectionStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Public/API DTO for connection health status.
 * Never leaks raw secrets or authorization headers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovConnectionHealthDto {

    private UUID connectionId;
    private UUID organizationId;
    private GovProviderType providerType;
    private String adapterCode;
    private String displayName;
    private GovConnectionStatus connectionStatus;
    private GovConnectionHealthStatus healthStatus;
    private long latencyMs;
    private String message;
    private Instant lastHealthCheckAt;
}
