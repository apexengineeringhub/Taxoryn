package com.taxoryn.module.gov.dto;

import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-model DTO representing a persisted government integration operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovOperationDto {

    private UUID id;
    private UUID organizationId;
    private GovProviderType providerType;
    private String operationType;
    private String businessEntityType;
    private UUID businessEntityId;
    private String correlationId;
    private String idempotencyKey;
    private GovOperationStatus status;
    private int attemptCount;
    private int maxAttempts;
    private GovErrorCode errorCode;
    private String errorMessage;
    private String providerReferenceId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private String createdBy;
}
