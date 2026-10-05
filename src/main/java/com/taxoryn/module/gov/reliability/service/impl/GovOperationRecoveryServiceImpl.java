package com.taxoryn.module.gov.reliability.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.dto.GovOperationDto;
import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.reliability.policy.GovRetryPolicy;
import com.taxoryn.module.gov.reliability.service.GovOperationRecoveryService;
import com.taxoryn.module.gov.reliability.service.GovReliabilityService;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GovOperationRecoveryServiceImpl implements GovOperationRecoveryService {

    private final GovIntegrationOperationRepository operationRepository;
    private final GovReliabilityService reliabilityService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<GovOperationDto> findRecoverableOperations(Instant olderThan) {
        UUID tenantId = requireActiveTenantId();
        List<GovOperationStatus> recoverableStatuses = List.of(GovOperationStatus.IN_PROGRESS, GovOperationStatus.RETRYING);
        List<GovIntegrationOperationEntity> entities = operationRepository.findByOrganizationIdAndStatusIn(tenantId, recoverableStatuses);

        return entities.stream()
                .filter(op -> olderThan == null || op.getUpdatedAt() == null || op.getUpdatedAt().isBefore(olderThan))
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public GovIntegrationResult recoverOperation(UUID operationId) {
        if (operationId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Operation ID is mandatory for recovery");
        }
        UUID tenantId = requireActiveTenantId();

        GovIntegrationOperationEntity operation = operationRepository.findByIdAndOrganizationId(operationId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Government operation not found for id: " + operationId));

        if (operation.getStatus().isTerminal()) {
            log.info("[GOV_RECOVERY_TERMINAL] Operation id={} is already in terminal state {}", operationId, operation.getStatus());
            return mapToResult(operation);
        }

        log.info("[GOV_RECOVERY_ATTEMPT] Recovering interrupted/retrying operation id={}, status={}, attempts={}/{}",
                operation.getId(), operation.getStatus(), operation.getAttemptCount(), operation.getMaxAttempts());

        auditService.logEvent(
                tenantId,
                null,
                "GOV_OPERATION_RECOVERY_STARTED",
                "GOV_OPERATION",
                operation.getId().toString(),
                null,
                Map.of("previousStatus", operation.getStatus().name(), "attemptCount", operation.getAttemptCount())
        );

        Map<String, Object> requestData = deserializeMetadata(operation.getRequestMetadata());

        GovIntegrationRequest retryRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(operation.getProviderType())
                .operationType(operation.getOperationType())
                .businessEntityType(operation.getBusinessEntityType())
                .businessEntityId(operation.getBusinessEntityId())
                .correlationId(operation.getCorrelationId())
                .idempotencyKey(operation.getIdempotencyKey())
                .requestData(requestData)
                .build();

        // Use conservative immediate retry policy for explicit recovery trigger
        GovIntegrationResult result = reliabilityService.executeWithRetry(retryRequest, GovRetryPolicy.immediatePolicy());

        auditService.logEvent(
                tenantId,
                null,
                result.isSuccess() ? "GOV_OPERATION_RECOVERED" : "GOV_OPERATION_RECOVERY_FAILED",
                "GOV_OPERATION",
                operation.getId().toString(),
                null,
                Map.of(
                        "success", result.isSuccess(),
                        "finalStatus", result.getStatus().name(),
                        "errorCode", result.getErrorCode() != null ? result.getErrorCode().name() : "NONE"
                )
        );

        return result;
    }

    @Override
    @Transactional
    public GovOperationDto reconcileAmbiguousOutcome(UUID operationId) {
        if (operationId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Operation ID is mandatory for reconciliation");
        }
        UUID tenantId = requireActiveTenantId();

        GovIntegrationOperationEntity operation = operationRepository.findByIdAndOrganizationId(operationId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Government operation not found for id: " + operationId));

        if (operation.getStatus() == GovOperationStatus.SUCCEEDED) {
            return mapToDto(operation);
        }

        // If operation has provider reference, acknowledge success
        if (operation.getProviderReferenceId() != null && !operation.getProviderReferenceId().isBlank()) {
            if (operation.getStatus() != GovOperationStatus.SUCCEEDED) {
                operation.transitionTo(GovOperationStatus.SUCCEEDED);
                operationRepository.save(operation);
            }
            return mapToDto(operation);
        }

        // If attempts are exhausted and no reference exists, mark as FAILED
        if (operation.getAttemptCount() >= operation.getMaxAttempts()) {
            if (!operation.getStatus().isTerminal()) {
                operation.transitionTo(GovOperationStatus.FAILED);
                if (operation.getErrorCode() == null) {
                    operation.setErrorCode(GovErrorCode.TIMEOUT);
                    operation.setErrorMessage("Operation reconciled to FAILED: timeout exhausted without provider confirmation");
                }
                operationRepository.save(operation);
            }
        }

        return mapToDto(operation);
    }

    private GovIntegrationResult mapToResult(GovIntegrationOperationEntity entity) {
        return GovIntegrationResult.builder()
                .operationId(entity.getId())
                .organizationId(entity.getOrganizationId())
                .providerType(entity.getProviderType())
                .operationType(entity.getOperationType())
                .correlationId(entity.getCorrelationId())
                .idempotencyKey(entity.getIdempotencyKey())
                .providerReferenceId(entity.getProviderReferenceId())
                .status(entity.getStatus())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .success(entity.getStatus() == GovOperationStatus.SUCCEEDED)
                .transientError(entity.getStatus() == GovOperationStatus.RETRYING)
                .responseMetadata(deserializeMetadata(entity.getResponseMetadata()))
                .build();
    }

    private GovOperationDto mapToDto(GovIntegrationOperationEntity entity) {
        return GovOperationDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .providerType(entity.getProviderType())
                .operationType(entity.getOperationType())
                .businessEntityType(entity.getBusinessEntityType())
                .businessEntityId(entity.getBusinessEntityId())
                .correlationId(entity.getCorrelationId())
                .idempotencyKey(entity.getIdempotencyKey())
                .status(entity.getStatus())
                .attemptCount(entity.getAttemptCount())
                .maxAttempts(entity.getMaxAttempts())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .providerReferenceId(entity.getProviderReferenceId())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Map<String, Object> deserializeMetadata(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            log.warn("[GOV_METADATA_DESERIALIZE_FAILED] Failed to deserialize metadata json", ex);
            return new HashMap<>();
        }
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant context required for recovery operations");
        }
        return tenantId;
    }
}
