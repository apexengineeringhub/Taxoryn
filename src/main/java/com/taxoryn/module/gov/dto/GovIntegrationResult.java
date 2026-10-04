package com.taxoryn.module.gov.dto;

import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Normalized result model returned by government integration operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovIntegrationResult {

    private boolean success;
    private UUID operationId;
    private UUID organizationId;
    private GovProviderType providerType;
    private String operationType;
    private String correlationId;
    private String idempotencyKey;
    private String providerReferenceId;
    private GovOperationStatus status;
    private GovErrorCode errorCode;
    private String errorMessage;
    private boolean transientError;

    @Builder.Default
    private Map<String, Object> responseMetadata = new HashMap<>();

    public static GovIntegrationResult success(
            UUID operationId,
            UUID organizationId,
            GovProviderType providerType,
            String operationType,
            String correlationId,
            String idempotencyKey,
            String providerReferenceId,
            Map<String, Object> responseMetadata) {
        return GovIntegrationResult.builder()
                .success(true)
                .operationId(operationId)
                .organizationId(organizationId)
                .providerType(providerType)
                .operationType(operationType)
                .correlationId(correlationId)
                .idempotencyKey(idempotencyKey)
                .providerReferenceId(providerReferenceId)
                .status(GovOperationStatus.SUCCEEDED)
                .responseMetadata(responseMetadata != null ? responseMetadata : new HashMap<>())
                .build();
    }

    public static GovIntegrationResult failure(
            UUID operationId,
            UUID organizationId,
            GovProviderType providerType,
            String operationType,
            String correlationId,
            String idempotencyKey,
            GovErrorCode errorCode,
            String errorMessage,
            boolean isTransient,
            Map<String, Object> responseMetadata) {
        return GovIntegrationResult.builder()
                .success(false)
                .operationId(operationId)
                .organizationId(organizationId)
                .providerType(providerType)
                .operationType(operationType)
                .correlationId(correlationId)
                .idempotencyKey(idempotencyKey)
                .status(isTransient ? GovOperationStatus.RETRYING : GovOperationStatus.FAILED)
                .errorCode(errorCode != null ? errorCode : GovErrorCode.UNKNOWN)
                .errorMessage(errorMessage)
                .transientError(isTransient)
                .responseMetadata(responseMetadata != null ? responseMetadata : new HashMap<>())
                .build();
    }

    public static GovIntegrationResult inProgress(
            UUID operationId,
            UUID organizationId,
            GovProviderType providerType,
            String operationType,
            String correlationId,
            String idempotencyKey) {
        return GovIntegrationResult.builder()
                .success(false)
                .operationId(operationId)
                .organizationId(organizationId)
                .providerType(providerType)
                .operationType(operationType)
                .correlationId(correlationId)
                .idempotencyKey(idempotencyKey)
                .status(GovOperationStatus.IN_PROGRESS)
                .build();
    }
}
