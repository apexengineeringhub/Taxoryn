package com.taxoryn.module.gov.reconciliation.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.dto.GovOperationDto;
import com.taxoryn.module.gov.entity.GovIntegrationOperationEntity;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.reconciliation.dto.GovReconciliationBatchResultDto;
import com.taxoryn.module.gov.reconciliation.dto.GovReconciliationResultDto;
import com.taxoryn.module.gov.reconciliation.model.GovAuthoritativeStatus;
import com.taxoryn.module.gov.reconciliation.service.GovernmentOperationReconciliationService;
import com.taxoryn.module.gov.registry.GovernmentProviderRegistry;
import com.taxoryn.module.gov.reliability.metrics.GovReliabilityMetrics;
import com.taxoryn.module.gov.reliability.util.GovReliabilitySanitizer;
import com.taxoryn.module.gov.repository.GovIntegrationOperationRepository;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Core implementation of GovernmentOperationReconciliationService.
 * Enforces the critical invariant: SUBMITTED != FILED.
 * Translates provider-specific responses into standardized authoritative statuses
 * and executes bounded batch reconciliation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GovernmentOperationReconciliationServiceImpl implements GovernmentOperationReconciliationService {

    private final GovIntegrationOperationRepository operationRepository;
    private final GovernmentProviderRegistry providerRegistry;
    private final AuditService auditService;
    private final GovReliabilityMetrics reliabilityMetrics;
    private final ObjectMapper objectMapper;

    private static final List<GovOperationStatus> ELIGIBLE_STATUSES = List.of(
            GovOperationStatus.IN_PROGRESS,
            GovOperationStatus.CREATED,
            GovOperationStatus.RETRYING
    );

    @Override
    @Transactional
    public GovReconciliationResultDto reconcileOperation(UUID operationId) {
        if (operationId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Operation ID is mandatory for reconciliation");
        }

        UUID tenantId = TenantContext.getTenantId();
        GovIntegrationOperationEntity operation = (tenantId != null)
                ? operationRepository.findByIdAndOrganizationId(operationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Government operation not found with id: " + operationId))
                : operationRepository.findById(operationId)
                .orElseThrow(() -> new ResourceNotFoundException("Government operation not found with id: " + operationId));

        return executeReconciliation(operation);
    }

    @Override
    public GovReconciliationBatchResultDto reconcileBatch(int batchSize) {
        long startTime = System.currentTimeMillis();
        int limit = Math.max(1, Math.min(batchSize, 100));
        Instant now = Instant.now();

        UUID currentTenant = TenantContext.getTenantId();
        List<GovIntegrationOperationEntity> eligibleOperations;

        if (currentTenant != null) {
            eligibleOperations = operationRepository.findEligibleForTenantReconciliation(
                    currentTenant, ELIGIBLE_STATUSES, now, PageRequest.of(0, limit));
        } else {
            eligibleOperations = operationRepository.findEligibleForReconciliation(
                    ELIGIBLE_STATUSES, now, PageRequest.of(0, limit));
        }

        int totalEligible = eligibleOperations.size();
        int totalProcessed = 0;
        int statusChangedCount = 0;
        int succeededCount = 0;
        int failedCount = 0;
        int inProgressCount = 0;
        int skippedCount = 0;
        List<GovReconciliationResultDto> results = new ArrayList<>();

        for (GovIntegrationOperationEntity candidate : eligibleOperations) {
            Instant lockUntil = Instant.now().plus(Duration.ofMinutes(5));
            int claimed = operationRepository.claimOperationForReconciliation(
                    candidate.getId(), candidate.getStatus(), lockUntil, Instant.now());

            if (claimed > 0) {
                try {
                    GovReconciliationResultDto result = reconcileSingleInIsolatedTransaction(candidate.getId());
                    results.add(result);
                    totalProcessed++;

                    if (result.isChanged()) {
                        statusChangedCount++;
                    }
                    if (result.getNewStatus() == GovOperationStatus.SUCCEEDED) {
                        succeededCount++;
                    } else if (result.getNewStatus() == GovOperationStatus.FAILED) {
                        failedCount++;
                    } else {
                        inProgressCount++;
                    }
                } catch (Exception ex) {
                    log.error("[GOV_RECONCILIATION_BATCH_ITEM_ERROR] Failed to reconcile opId={}: {}",
                            candidate.getId(), ex.getMessage(), ex);
                    skippedCount++;
                }
            } else {
                skippedCount++;
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("[GOV_RECONCILIATION_BATCH_DONE] totalEligible={}, processed={}, changed={}, succeeded={}, failed={}, inProgress={}, skipped={}, duration={}ms",
                totalEligible, totalProcessed, statusChangedCount, succeededCount, failedCount, inProgressCount, skippedCount, durationMs);

        return GovReconciliationBatchResultDto.builder()
                .totalEligible(totalEligible)
                .totalProcessed(totalProcessed)
                .statusChangedCount(statusChangedCount)
                .succeededCount(succeededCount)
                .failedCount(failedCount)
                .inProgressCount(inProgressCount)
                .skippedCount(skippedCount)
                .durationMs(durationMs)
                .results(results)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovOperationDto> findEligibleOperations(int limit) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant context required to find eligible operations");
        }
        int max = Math.max(1, Math.min(limit, 100));
        return operationRepository.findEligibleForTenantReconciliation(
                tenantId, ELIGIBLE_STATUSES, Instant.now(), PageRequest.of(0, max))
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GovReconciliationResultDto reconcileSingleInIsolatedTransaction(UUID operationId) {
        GovIntegrationOperationEntity op = operationRepository.findById(operationId)
                .orElseThrow(() -> new ResourceNotFoundException("Operation not found: " + operationId));

        UUID prevTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(op.getOrganizationId());
            return executeReconciliation(op);
        } finally {
            if (prevTenant != null) {
                TenantContext.setTenantId(prevTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    private GovReconciliationResultDto executeReconciliation(GovIntegrationOperationEntity op) {
        GovOperationStatus previousStatus = op.getStatus();

        // INVARIANT: Terminal states (SUCCEEDED, FAILED, CANCELLED) are immutable
        if (previousStatus.isTerminal()) {
            log.debug("[GOV_RECONCILIATION_TERMINAL_SKIPPED] Operation {} is already in terminal state {}", op.getId(), previousStatus);
            return buildResult(op, previousStatus, previousStatus,
                    previousStatus == GovOperationStatus.SUCCEEDED ? GovAuthoritativeStatus.FILED : GovAuthoritativeStatus.FAILED,
                    false, null);
        }

        auditService.logEvent(
                op.getOrganizationId(),
                null,
                "GOV_RECONCILIATION_STARTED",
                "GovIntegrationOperation",
                op.getId().toString(),
                previousStatus.name(),
                "Starting reconciliation for " + op.getOperationType()
        );

        Optional<GovernmentProviderAdapter> adapterOpt = providerRegistry.getAdapter(op.getProviderType());
        if (adapterOpt.isEmpty()) {
            log.warn("[GOV_RECONCILIATION_NO_ADAPTER] No provider adapter found for provider {}", op.getProviderType());
            op.recordReconciliationAttempt(Instant.now().plus(Duration.ofMinutes(10)));
            operationRepository.save(op);
            return buildResult(op, previousStatus, previousStatus, GovAuthoritativeStatus.UNKNOWN, false, "No adapter registered for " + op.getProviderType());
        }

        GovernmentProviderAdapter adapter = adapterOpt.get();

        Map<String, Object> queryData = new HashMap<>();
        if (op.getProviderReferenceId() != null) {
            queryData.put("providerReferenceId", op.getProviderReferenceId());
        }
        if (op.getBusinessEntityType() != null) {
            queryData.put("businessEntityType", op.getBusinessEntityType());
        }
        if (op.getBusinessEntityId() != null) {
            queryData.put("businessEntityId", op.getBusinessEntityId().toString());
        }

        GovIntegrationRequest statusRequest = GovIntegrationRequest.builder()
                .organizationId(op.getOrganizationId())
                .providerType(op.getProviderType())
                .operationType("FETCH_STATUS")
                .businessEntityType(op.getBusinessEntityType())
                .businessEntityId(op.getBusinessEntityId())
                .correlationId(op.getCorrelationId())
                .idempotencyKey(op.getIdempotencyKey() + "_reconcile_" + (op.getReconciliationAttemptCount() + 1))
                .requestData(queryData)
                .build();

        GovIntegrationResult providerResult;
        try {
            providerResult = adapter.execute(statusRequest);
        } catch (Exception ex) {
            log.warn("[GOV_RECONCILIATION_ADAPTER_ERROR] Error querying provider for opId={}: {}", op.getId(), ex.getMessage());
            providerResult = GovIntegrationResult.failure(
                    op.getId(), op.getOrganizationId(), op.getProviderType(), "FETCH_STATUS",
                    op.getCorrelationId(), statusRequest.getIdempotencyKey(),
                    GovErrorCode.UNKNOWN, ex.getMessage(), true, null);
        }

        GovAuthoritativeStatus authStatus = resolveAuthoritativeStatus(providerResult);
        boolean changed = applyAuthoritativeTransition(op, authStatus, providerResult);

        // Calculate backoff if operation remains in-progress
        Instant nextReconciliation = null;
        if (!op.getStatus().isTerminal()) {
            long backoffSec = (long) Math.min(600, Math.pow(2, op.getReconciliationAttemptCount()) * 30);
            nextReconciliation = Instant.now().plus(Duration.ofSeconds(backoffSec));
        }

        op.recordReconciliationAttempt(nextReconciliation);
        if (providerResult != null && providerResult.getProviderReferenceId() != null && !providerResult.getProviderReferenceId().isBlank()) {
            op.setProviderReferenceId(providerResult.getProviderReferenceId());
        }

        GovIntegrationOperationEntity saved = operationRepository.save(op);

        if (changed) {
            auditService.logEvent(
                    saved.getOrganizationId(),
                    null,
                    "GOV_RECONCILIATION_STATUS_CHANGED",
                    "GovIntegrationOperation",
                    saved.getId().toString(),
                    previousStatus.name(),
                    saved.getStatus().name()
            );
        }

        String completionAuditAction = saved.getStatus() == GovOperationStatus.SUCCEEDED
                ? "GOV_RECONCILIATION_COMPLETED"
                : (saved.getStatus() == GovOperationStatus.FAILED ? "GOV_RECONCILIATION_FAILED" : "GOV_RECONCILIATION_IN_PROGRESS");

        auditService.logEvent(
                saved.getOrganizationId(),
                null,
                completionAuditAction,
                "GovIntegrationOperation",
                saved.getId().toString(),
                null,
                "Authoritative status: " + authStatus.name() + " final: " + saved.getStatus().name()
        );

        return buildResult(saved, previousStatus, saved.getStatus(), authStatus, changed,
                providerResult != null ? providerResult.getErrorMessage() : null);
    }

    private GovAuthoritativeStatus resolveAuthoritativeStatus(GovIntegrationResult result) {
        if (result == null) {
            return GovAuthoritativeStatus.UNKNOWN;
        }

        if (result.isSuccess()) {
            Map<String, Object> meta = result.getResponseMetadata();
            if (meta != null) {
                Object rawStatus = meta.get("authoritativeStatus");
                if (rawStatus == null) {
                    rawStatus = meta.get("status");
                }
                if (rawStatus != null) {
                    String statusStr = rawStatus.toString().toUpperCase();
                    if (statusStr.contains("FILED") || statusStr.contains("ACCEPTED") || statusStr.contains("SUCCEEDED")) {
                        return GovAuthoritativeStatus.FILED;
                    }
                    if (statusStr.contains("PROCESSING") || statusStr.contains("IN_PROGRESS") || statusStr.contains("PENDING") || statusStr.contains("SUBMITTED")) {
                        return GovAuthoritativeStatus.PROCESSING;
                    }
                    if (statusStr.contains("REJECTED")) {
                        return GovAuthoritativeStatus.REJECTED;
                    }
                }
            }

            if (result.getProviderReferenceId() != null && !result.getProviderReferenceId().isBlank()) {
                return GovAuthoritativeStatus.FILED;
            }
            return GovAuthoritativeStatus.FILED;
        } else {
            if (result.getErrorCode() == GovErrorCode.AUTH_REQUIRED) {
                return GovAuthoritativeStatus.REQUIRES_ACTION;
            }
            if (result.getErrorCode() == GovErrorCode.VALIDATION_FAILED) {
                return GovAuthoritativeStatus.REJECTED;
            }
            if (result.isTransientError() || result.getErrorCode() == GovErrorCode.TIMEOUT || result.getErrorCode() == GovErrorCode.RATE_LIMITED || result.getErrorCode() == GovErrorCode.PROVIDER_UNAVAILABLE) {
                return GovAuthoritativeStatus.UNKNOWN;
            }
            return GovAuthoritativeStatus.FAILED;
        }
    }

    private boolean applyAuthoritativeTransition(GovIntegrationOperationEntity op, GovAuthoritativeStatus authStatus, GovIntegrationResult result) {
        GovOperationStatus initialStatus = op.getStatus();

        switch (authStatus) {
            case FILED -> {
                // INVARIANT: Only authoritative FILED can transition operation to SUCCEEDED
                if (op.getStatus() != GovOperationStatus.SUCCEEDED) {
                    op.transitionTo(GovOperationStatus.SUCCEEDED);
                    op.setErrorCode(null);
                    op.setErrorMessage(null);
                    return true;
                }
            }
            case PROCESSING -> {
                if (op.getStatus() == GovOperationStatus.CREATED) {
                    op.transitionTo(GovOperationStatus.IN_PROGRESS);
                    return true;
                }
            }
            case REJECTED -> {
                if (!op.getStatus().isTerminal()) {
                    op.transitionTo(GovOperationStatus.FAILED);
                    op.setErrorCode(GovErrorCode.VALIDATION_FAILED);
                    op.setErrorMessage(result != null && result.getErrorMessage() != null
                            ? GovReliabilitySanitizer.sanitizeString(result.getErrorMessage())
                            : "Operation rejected by government provider");
                    return true;
                }
            }
            case FAILED -> {
                if (!op.getStatus().isTerminal()) {
                    op.transitionTo(GovOperationStatus.FAILED);
                    op.setErrorCode(result != null && result.getErrorCode() != null ? result.getErrorCode() : GovErrorCode.UNKNOWN);
                    op.setErrorMessage(result != null && result.getErrorMessage() != null
                            ? GovReliabilitySanitizer.sanitizeString(result.getErrorMessage())
                            : "Operation failed on government provider");
                    return true;
                }
            }
            case REQUIRES_ACTION -> {
                if (op.getReconciliationAttemptCount() >= op.getMaxAttempts() && !op.getStatus().isTerminal()) {
                    op.transitionTo(GovOperationStatus.FAILED);
                    op.setErrorCode(GovErrorCode.AUTH_REQUIRED);
                    op.setErrorMessage("Government authentication expired or required action before filing could complete");
                    return true;
                }
            }
            case UNKNOWN -> {
                // INVARIANT: Never transition an unknown or timeout outcome to SUCCEEDED/FILED
                // Preserve recoverable status (IN_PROGRESS or RETRYING)
                if (op.getStatus() == GovOperationStatus.CREATED) {
                    op.transitionTo(GovOperationStatus.IN_PROGRESS);
                    return true;
                }
            }
        }

        return op.getStatus() != initialStatus;
    }

    private GovReconciliationResultDto buildResult(
            GovIntegrationOperationEntity op,
            GovOperationStatus prev,
            GovOperationStatus curr,
            GovAuthoritativeStatus authStatus,
            boolean changed,
            String errorMsg) {
        return GovReconciliationResultDto.builder()
                .operationId(op.getId())
                .organizationId(op.getOrganizationId())
                .providerType(op.getProviderType())
                .operationType(op.getOperationType())
                .correlationId(op.getCorrelationId())
                .previousStatus(prev)
                .newStatus(curr)
                .authoritativeStatus(authStatus)
                .providerReferenceId(op.getProviderReferenceId())
                .reconciliationAttemptCount(op.getReconciliationAttemptCount())
                .changed(changed)
                .errorCode(op.getErrorCode() != null ? op.getErrorCode().name() : null)
                .errorMessage(errorMsg != null ? GovReliabilitySanitizer.sanitizeString(errorMsg) : op.getErrorMessage())
                .reconciledAt(Instant.now())
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
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .lastReconciledAt(entity.getLastReconciledAt())
                .nextReconciliationAt(entity.getNextReconciliationAt())
                .reconciliationAttemptCount(entity.getReconciliationAttemptCount())
                .build();
    }
}
