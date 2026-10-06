package com.taxoryn.module.gov.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.dto.GovOperationDto;
import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.exception.GovProviderNotFoundException;
import com.taxoryn.module.gov.exception.GovValidationException;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core implementation of GovernmentIntegrationService.
 * Handles lifecycle state transitions, idempotency resolution, audit logging, and adapter routing.
 */
@Slf4j
@Service
public class GovernmentIntegrationServiceImpl implements GovernmentIntegrationService {

    private final GovIntegrationOperationRepository operationRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final Map<GovProviderType, Map<String, GovernmentProviderAdapter>> adapterRegistry = new ConcurrentHashMap<>();
    private final List<GovernmentProviderAdapter> allAdapters = new java.util.concurrent.CopyOnWriteArrayList<>();

    @org.springframework.beans.factory.annotation.Value("${taxoryn.gov.integration.enabled:true}")
    private boolean integrationEnabled = true;

    @org.springframework.beans.factory.annotation.Value("${taxoryn.gov.integration.mock-enabled:true}")
    private boolean mockEnabled = true;

    public GovernmentIntegrationServiceImpl(
            GovIntegrationOperationRepository operationRepository,
            AuditService auditService,
            ObjectMapper objectMapper,
            List<GovernmentProviderAdapter> adapters) {
        this.operationRepository = operationRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        if (adapters != null) {
            for (GovernmentProviderAdapter adapter : adapters) {
                registerAdapter(adapter);
            }
        }
    }

    @Override
    @Transactional
    public GovIntegrationResult executeOperation(GovIntegrationRequest request) {
        if (!integrationEnabled) {
            log.warn("[GOV_INTEGRATION_DISABLED] Rejecting operation: Government integration is disabled by configuration");
            throw new com.taxoryn.module.gov.exception.GovIntegrationException(
                    GovErrorCode.PROVIDER_UNAVAILABLE,
                    "Government integration is currently disabled by system configuration"
            );
        }

        if (request == null) {
            throw new GovValidationException("GovIntegrationRequest must not be null");
        }
        if (request.getProviderType() == null) {
            throw new GovValidationException("GovProviderType is mandatory");
        }
        if (request.getOperationType() == null || request.getOperationType().trim().isEmpty()) {
            throw new GovValidationException("OperationType is mandatory");
        }

        UUID activeTenantId = resolveAndEnforceTenant(request.getOrganizationId());
        request.setOrganizationId(activeTenantId);

        // Ensure correlationId is present
        if (request.getCorrelationId() == null || request.getCorrelationId().trim().isEmpty()) {
            request.setCorrelationId(UUID.randomUUID().toString());
        }

        // Ensure idempotencyKey is present
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().trim().isEmpty()) {
            String key = GovIdempotencyKeyGenerator.generateKey(
                    activeTenantId,
                    request.getProviderType(),
                    request.getBusinessEntityType(),
                    request.getBusinessEntityId(),
                    request.getOperationType(),
                    request.getPayloadFingerprint()
            );
            request.setIdempotencyKey(key);
        }

        // Idempotency check: Look for an existing operation with the same key
        Optional<GovIntegrationOperationEntity> existingOpt = operationRepository
                .findByOrganizationIdAndIdempotencyKey(activeTenantId, request.getIdempotencyKey());

        if (existingOpt.isPresent()) {
            GovIntegrationOperationEntity existing = existingOpt.get();
            if (existing.getStatus() == GovOperationStatus.SUCCEEDED) {
                log.info("[GOV_IDEMPOTENCY_HIT] Returning cached SUCCEEDED operation: id={}, correlationId={}",
                        existing.getId(), existing.getCorrelationId());
                return mapToSuccessResult(existing);
            }
            if (existing.getStatus() == GovOperationStatus.IN_PROGRESS) {
                log.warn("[GOV_CONCURRENT_IN_PROGRESS] Operation already in progress: id={}, correlationId={}",
                        existing.getId(), existing.getCorrelationId());
                return GovIntegrationResult.inProgress(
                        existing.getId(),
                        existing.getOrganizationId(),
                        existing.getProviderType(),
                        existing.getOperationType(),
                        existing.getCorrelationId(),
                        existing.getIdempotencyKey()
                );
            }
        }

        // Initialize or reuse retryable entity
        GovIntegrationOperationEntity operation = existingOpt.orElseGet(() ->
                GovIntegrationOperationEntity.builder()
                        .providerType(request.getProviderType())
                        .operationType(request.getOperationType())
                        .businessEntityType(request.getBusinessEntityType())
                        .businessEntityId(request.getBusinessEntityId())
                        .correlationId(request.getCorrelationId())
                        .idempotencyKey(request.getIdempotencyKey())
                        .status(GovOperationStatus.CREATED)
                        .attemptCount(0)
                        .maxAttempts(3)
                        .requestMetadata(serializeMetadata(request.getRequestData()))
                        .build()
        );

        if (operation.getId() == null) {
            operation = operationRepository.save(operation);
            auditService.logEvent(
                    activeTenantId,
                    null,
                    "GOV_OPERATION_CREATED",
                    "GOV_OPERATION",
                    operation.getId().toString(),
                    null,
                    Map.of("providerType", request.getProviderType().name(), "operationType", request.getOperationType())
            );
        }

        // Transition to IN_PROGRESS
        operation.transitionTo(GovOperationStatus.IN_PROGRESS);
        operation.incrementAttempt();
        operation = operationRepository.save(operation);

        auditService.logEvent(
                activeTenantId,
                null,
                "GOV_OPERATION_STARTED",
                "GOV_OPERATION",
                operation.getId().toString(),
                null,
                Map.of("attempt", operation.getAttemptCount(), "correlationId", operation.getCorrelationId())
        );

        // Lookup Provider Adapter
        GovernmentProviderAdapter adapter = getAdapter(request.getProviderType()).orElse(null);

        if (adapter == null) {
            operation.transitionTo(GovOperationStatus.FAILED);
            operation.setErrorCode(GovErrorCode.PROVIDER_UNAVAILABLE);
            operation.setErrorMessage("No provider adapter registered for type " + request.getProviderType());
            operationRepository.save(operation);
            auditService.logEvent(
                    activeTenantId,
                    null,
                    "GOV_OPERATION_FAILED",
                    "GOV_OPERATION",
                    operation.getId().toString(),
                    null,
                    Map.of("error", "PROVIDER_UNAVAILABLE")
            );
            throw new GovProviderNotFoundException(request.getProviderType());
        }

        // Execute Adapter
        GovIntegrationResult result;
        try {
            result = adapter.execute(request);
        } catch (Exception ex) {
            log.error("[GOV_ADAPTER_EXCEPTION] Unhandled exception during provider adapter execution", ex);
            result = GovIntegrationResult.failure(
                    operation.getId(),
                    activeTenantId,
                    request.getProviderType(),
                    request.getOperationType(),
                    request.getCorrelationId(),
                    request.getIdempotencyKey(),
                    GovErrorCode.UNKNOWN,
                    ex.getMessage() != null ? ex.getMessage() : "Unknown adapter error",
                    false,
                    Map.of("exceptionClass", ex.getClass().getSimpleName())
            );
        }

        // Update Operation State based on Result
        if (result.isSuccess()) {
            operation.transitionTo(GovOperationStatus.SUCCEEDED);
            operation.setProviderReferenceId(result.getProviderReferenceId());
            operation.setResponseMetadata(serializeMetadata(result.getResponseMetadata()));
            operation.setErrorCode(null);
            operation.setErrorMessage(null);
            operationRepository.save(operation);

            auditService.logEvent(
                    activeTenantId,
                    null,
                    "GOV_OPERATION_SUCCEEDED",
                    "GOV_OPERATION",
                    operation.getId().toString(),
                    null,
                    Map.of("referenceId", result.getProviderReferenceId() != null ? result.getProviderReferenceId() : "")
            );
        } else {
            if (result.isTransientError() && operation.getAttemptCount() < operation.getMaxAttempts()) {
                operation.transitionTo(GovOperationStatus.RETRYING);
            } else {
                operation.transitionTo(GovOperationStatus.FAILED);
            }
            operation.setErrorCode(result.getErrorCode());
            operation.setErrorMessage(result.getErrorMessage());
            operation.setResponseMetadata(serializeMetadata(result.getResponseMetadata()));
            operationRepository.save(operation);

            auditService.logEvent(
                    activeTenantId,
                    null,
                    "GOV_OPERATION_FAILED",
                    "GOV_OPERATION",
                    operation.getId().toString(),
                    null,
                    Map.of("errorCode", result.getErrorCode() != null ? result.getErrorCode().name() : "UNKNOWN",
                            "status", operation.getStatus().name())
            );
        }

        result.setOperationId(operation.getId());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public GovOperationDto getOperation(UUID operationId) {
        UUID activeTenantId = TenantContext.getTenantId();
        if (activeTenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant required to inspect operations");
        }
        return operationRepository.findByIdAndOrganizationId(operationId, activeTenantId)
                .map(this::mapToDto)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Government operation not found with id: " + operationId));
    }

    @Override
    @Transactional(readOnly = true)
    public GovOperationDto getOperationByCorrelationId(String correlationId) {
        UUID activeTenantId = TenantContext.getTenantId();
        if (activeTenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant required to inspect operations");
        }
        return operationRepository.findByOrganizationIdAndCorrelationId(activeTenantId, correlationId)
                .map(this::mapToDto)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Government operation not found with correlationId: " + correlationId));
    }

    @Override
    @Transactional(readOnly = true)
    public GovOperationDto getOperationByIdempotencyKey(String idempotencyKey) {
        UUID activeTenantId = TenantContext.getTenantId();
        if (activeTenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant required to inspect operations");
        }
        return operationRepository.findByOrganizationIdAndIdempotencyKey(activeTenantId, idempotencyKey)
                .map(this::mapToDto)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Government operation not found with idempotencyKey: " + idempotencyKey));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovOperationDto> getOperationsByStatus(GovOperationStatus status) {
        UUID activeTenantId = TenantContext.getTenantId();
        if (activeTenantId == null) {
            return Collections.emptyList();
        }
        return operationRepository.findByOrganizationIdAndStatus(activeTenantId, status)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovOperationDto> getOperationsByBusinessEntity(String businessEntityType, UUID businessEntityId) {
        UUID activeTenantId = TenantContext.getTenantId();
        if (activeTenantId == null) {
            return Collections.emptyList();
        }
        return operationRepository.findByOrganizationIdAndBusinessEntityTypeAndBusinessEntityId(
                        activeTenantId, businessEntityType, businessEntityId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public void registerAdapter(GovernmentProviderAdapter adapter) {
        if (adapter != null) {
            allAdapters.add(adapter);
            adapterRegistry.computeIfAbsent(adapter.getProviderType(), k -> new ConcurrentHashMap<>())
                    .put(adapter.getAdapterCode().toUpperCase(), adapter);
            log.info("[GOV_ADAPTER_REGISTERED] Registered adapter '{}' for provider type {}",
                    adapter.getAdapterCode(), adapter.getProviderType());
        }
    }

    @Override
    public List<String> getAvailableAdapters() {
        if (!integrationEnabled) {
            return List.of();
        }
        return allAdapters.stream()
                .filter(a -> mockEnabled || (!a.getAdapterCode().toUpperCase().contains("MOCK") && !a.getAdapterCode().equalsIgnoreCase("SANDBOX")))
                .map(GovernmentProviderAdapter::getAdapterCode)
                .distinct()
                .toList();
    }

    @Override
    public Optional<GovernmentProviderAdapter> getAdapter(GovProviderType providerType) {
        if (!integrationEnabled || providerType == null) {
            return Optional.empty();
        }
        Map<String, GovernmentProviderAdapter> typeMap = adapterRegistry.get(providerType);
        if (typeMap != null && !typeMap.isEmpty()) {
            Optional<GovernmentProviderAdapter> dedicated = typeMap.values().stream()
                    .filter(a -> !a.getAdapterCode().toUpperCase().contains("MOCK") && !a.getAdapterCode().equalsIgnoreCase("SANDBOX"))
                    .findFirst();
            if (dedicated.isPresent()) {
                return dedicated;
            }
            if (mockEnabled) {
                return Optional.of(typeMap.values().iterator().next());
            } else {
                log.warn("[GOV_INTEGRATION_SAFETY] Mock adapter requested for provider {} but mock providers are disabled in production", providerType);
                return Optional.empty();
            }
        }
        if (mockEnabled) {
            return allAdapters.stream()
                    .filter(a -> a.getAdapterCode().toUpperCase().contains("MOCK") || a.getAdapterCode().equalsIgnoreCase("SANDBOX"))
                    .findFirst();
        }
        return Optional.empty();
    }

    private UUID resolveAndEnforceTenant(UUID requestedTenantId) {
        UUID currentTenant = TenantContext.getTenantId();
        if (requestedTenantId != null && currentTenant != null && !requestedTenantId.equals(currentTenant)) {
            throw new AppException(ErrorCode.TENANT_MISMATCH,
                    "Cross-tenant government integration violation: Requested tenant " + requestedTenantId + " but active tenant is " + currentTenant);
        }
        UUID effective = requestedTenantId != null ? requestedTenantId : currentTenant;
        if (effective == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Cannot execute government operation without tenant context");
        }
        return effective;
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
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    private GovIntegrationResult mapToSuccessResult(GovIntegrationOperationEntity entity) {
        Map<String, Object> meta = deserializeMetadata(entity.getResponseMetadata());
        return GovIntegrationResult.success(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getProviderType(),
                entity.getOperationType(),
                entity.getCorrelationId(),
                entity.getIdempotencyKey(),
                entity.getProviderReferenceId(),
                meta
        );
    }

    private String serializeMetadata(Map<String, ?> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize government operation metadata", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeMetadata(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
