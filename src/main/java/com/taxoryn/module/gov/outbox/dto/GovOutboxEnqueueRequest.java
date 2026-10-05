package com.taxoryn.module.gov.outbox.dto;

import com.taxoryn.module.gov.model.GovProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.UUID;

/**
 * Request DTO for enqueuing an event into the transactional outbox.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovOutboxEnqueueRequest {

    private UUID organizationId;
    private String eventType;
    private String aggregateType;
    private UUID aggregateId;
    private UUID operationId;
    private GovProviderType providerType;
    private String correlationId;
    private String payload;
    private Integer maxAttempts;
    private Duration delay;
}
