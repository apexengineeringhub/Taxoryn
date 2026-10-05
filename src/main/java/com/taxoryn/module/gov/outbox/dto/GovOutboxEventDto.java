package com.taxoryn.module.gov.outbox.dto;

import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object representing a government outbox event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovOutboxEventDto {

    private UUID id;
    private UUID organizationId;
    private String eventType;
    private String aggregateType;
    private UUID aggregateId;
    private UUID operationId;
    private GovProviderType providerType;
    private String correlationId;
    private GovOutboxStatus status;
    private String payload;
    private int attemptCount;
    private int maxAttempts;
    private Instant availableAt;
    private Instant lockedAt;
    private Instant processedAt;
    private String lastErrorCode;
    private String lastErrorMessage;
    private Instant createdAt;
    private Instant updatedAt;
}
