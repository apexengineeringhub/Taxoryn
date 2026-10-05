package com.taxoryn.module.gov.reliability.service.impl;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.reliability.dto.GovReliabilityMetricsSnapshot;
import com.taxoryn.module.gov.reliability.metrics.GovReliabilityMetrics;
import com.taxoryn.module.gov.reliability.model.GovFailureClassification;
import com.taxoryn.module.gov.reliability.model.GovOperationOutcome;
import com.taxoryn.module.gov.reliability.policy.GovRetryPolicy;
import com.taxoryn.module.gov.reliability.service.GovReliabilityService;
import com.taxoryn.module.gov.reliability.util.GovReliabilitySanitizer;
import com.taxoryn.module.gov.service.GovernmentIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GovReliabilityServiceImpl implements GovReliabilityService {

    private final GovernmentIntegrationService govIntegrationService;
    private final GovReliabilityMetrics metrics;
    private final AuditService auditService;

    @Override
    public GovIntegrationResult executeWithRetry(GovIntegrationRequest request) {
        return executeWithRetry(request, GovRetryPolicy.defaultPolicy());
    }

    @Override
    public GovIntegrationResult executeWithRetry(GovIntegrationRequest request, GovRetryPolicy policy) {
        if (request == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "GovIntegrationRequest is mandatory for reliable execution");
        }
        GovRetryPolicy activePolicy = policy != null ? policy : GovRetryPolicy.defaultPolicy();

        UUID tenantId = request.getOrganizationId() != null ? request.getOrganizationId() : TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant context required for reliable government execution");
        }
        request.setOrganizationId(tenantId);

        int maxAttempts = activePolicy.getMaxAttempts();
        GovIntegrationResult lastResult = null;
        long startTime = System.currentTimeMillis();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            log.info("[GOV_RELIABILITY_ATTEMPT] Dispatching op='{}', provider='{}', attempt={}/{}, correlationId='{}'",
                    request.getOperationType(), request.getProviderType(), attempt, maxAttempts, request.getCorrelationId());

            try {
                lastResult = govIntegrationService.executeOperation(request);
            } catch (Exception ex) {
                log.error("[GOV_RELIABILITY_UNHANDLED] Exception on attempt {} for op='{}': {}",
                        attempt, request.getOperationType(), GovReliabilitySanitizer.sanitizeString(ex.getMessage()));
                lastResult = GovIntegrationResult.failure(
                        null,
                        tenantId,
                        request.getProviderType(),
                        request.getOperationType(),
                        request.getCorrelationId(),
                        request.getIdempotencyKey(),
                        GovErrorCode.UNKNOWN,
                        ex.getMessage() != null ? ex.getMessage() : "Unhandled operation failure",
                        false,
                        Map.of("exceptionClass", ex.getClass().getSimpleName())
                );
            }

            if (lastResult.isSuccess()) {
                long duration = System.currentTimeMillis() - startTime;
                metrics.recordSuccess(request.getProviderType(), request.getOperationType(), duration);
                log.info("[GOV_RELIABILITY_SUCCESS] Operation succeeded on attempt {} (duration={}ms): id={}, ref='{}'",
                        attempt, duration, lastResult.getOperationId(), lastResult.getProviderReferenceId());
                return lastResult;
            }

            GovFailureClassification classification = classifyFailure(
                    lastResult.getErrorCode(),
                    extractHttpStatus(lastResult.getResponseMetadata()),
                    lastResult.getErrorMessage()
            );

            metrics.recordFailure(request.getProviderType(), request.getOperationType(), lastResult.getErrorCode(), classification);

            boolean retryEligible = activePolicy.shouldRetryClassification(classification, attempt);

            if (retryEligible && attempt < maxAttempts) {
                metrics.recordRetry(request.getProviderType(), request.getOperationType(), attempt);
                long backoffMs = activePolicy.calculateBackoff(attempt);
                log.warn("[GOV_RELIABILITY_RETRY] Operation transiently failed with {} ({}), scheduling retry {}/{} in {}ms",
                        lastResult.getErrorCode(), classification, attempt + 1, maxAttempts, backoffMs);

                auditService.logEvent(
                        tenantId,
                        null,
                        "GOV_OPERATION_RETRY_SCHEDULED",
                        "GOV_OPERATION",
                        lastResult.getOperationId() != null ? lastResult.getOperationId().toString() : request.getCorrelationId(),
                        null,
                        Map.of(
                                "attempt", attempt,
                                "nextAttempt", attempt + 1,
                                "backoffMs", backoffMs,
                                "errorCode", lastResult.getErrorCode() != null ? lastResult.getErrorCode().name() : "UNKNOWN",
                                "classification", classification.name()
                        )
                );

                if (backoffMs > 0) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("[GOV_RELIABILITY_INTERRUPTED] Retry sleep interrupted for correlationId='{}'", request.getCorrelationId());
                        break;
                    }
                }
            } else {
                log.warn("[GOV_RELIABILITY_TERMINAL] Operation failed non-retryably or exhausted attempts ({}/{}): errorCode={}, classification={}",
                        attempt, maxAttempts, lastResult.getErrorCode(), classification);
                break;
            }
        }

        return lastResult;
    }

    @Override
    public GovFailureClassification classifyFailure(GovErrorCode errorCode, Integer httpStatusCode, String rawMessage) {
        if (httpStatusCode != null) {
            return GovFailureClassification.fromHttpStatus(httpStatusCode);
        }
        if (errorCode != null) {
            return GovFailureClassification.fromErrorCode(errorCode);
        }
        return GovFailureClassification.UNKNOWN;
    }

    @Override
    public GovOperationOutcome normalizeOutcome(GovIntegrationResult result) {
        return GovOperationOutcome.fromResult(result);
    }

    @Override
    public GovReliabilityMetricsSnapshot getMetrics() {
        return metrics.getSnapshot();
    }

    private Integer extractHttpStatus(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object statusObj = metadata.get("httpStatus");
        if (statusObj instanceof Number num) {
            return num.intValue();
        }
        Object statusCodeObj = metadata.get("statusCode");
        if (statusCodeObj instanceof Number num) {
            return num.intValue();
        }
        return null;
    }
}
