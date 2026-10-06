package com.taxoryn.module.gov.dto;

import com.taxoryn.module.gov.model.GovConnectionStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for government connection details returned to callers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovConnectionDto {

    private UUID id;
    private UUID organizationId;
    private GovProviderType providerType;
    private String displayName;
    private String description;
    private String environment;
    private GovConnectionStatus status;
    private UUID credentialReferenceId;
    private String metadata;
    private com.taxoryn.module.gov.model.GovConnectionHealthStatus healthStatus;
    private Instant lastHealthCheckAt;
    private String healthMessage;
    private Long healthLatencyMs;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;
}
