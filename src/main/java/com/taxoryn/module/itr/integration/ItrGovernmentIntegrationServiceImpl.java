package com.taxoryn.module.itr.integration;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovConnectionHealthDto;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gov.service.GovernmentHealthService;
import com.taxoryn.module.gov.service.GovernmentIntegrationService;
import com.taxoryn.module.itr.integration.dto.ItrHandshakeResponseDto;
import com.taxoryn.module.itr.integration.dto.ItrIntegrationResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Implementation of ItrGovernmentIntegrationService bridging ITR domain with Government Integration Framework.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItrGovernmentIntegrationServiceImpl implements ItrGovernmentIntegrationService {

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");

    private final GovernmentIntegrationService govIntegrationService;
    private final GovernmentConnectionService govConnectionService;
    private final GovernmentHealthService govHealthService;
    private final AuditService auditService;

    @Override
    public ItrHandshakeResponseDto checkItrConnectionHealth(UUID connectionId) {
        return checkItrConnectionHealth(connectionId, Collections.emptyMap());
    }

    @Override
    public ItrHandshakeResponseDto checkItrConnectionHealth(UUID connectionId, Map<String, Object> directives) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = govConnectionService.getConnection(connectionId);

        if (connection.getProviderType() != GovProviderType.INCOME_TAX) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connectionId + " is not an Income Tax provider connection");
        }

        auditService.logEvent(
                tenantId,
                null,
                "ITR_PROVIDER_HANDSHAKE",
                "ITR_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("displayName", connection.getDisplayName(), "providerType", connection.getProviderType().name())
        );

        GovConnectionHealthDto health = govHealthService.checkConnectionHealth(connectionId, directives);

        return ItrHandshakeResponseDto.builder()
                .connectionId(health.getConnectionId())
                .displayName(health.getDisplayName())
                .healthStatus(health.getHealthStatus() != null ? health.getHealthStatus().name() : "UNKNOWN")
                .latencyMs(health.getLatencyMs())
                .message(health.getMessage())
                .lastHealthCheckAt(health.getLastHealthCheckAt())
                .build();
    }

    @Override
    public ItrIntegrationResultDto verifyPan(UUID connectionId, String pan) {
        return verifyPan(connectionId, pan, Collections.emptyMap());
    }

    @Override
    public ItrIntegrationResultDto verifyPan(UUID connectionId, String pan, Map<String, Object> options) {
        validatePan(pan);
        String cleanPan = pan.trim().toUpperCase();

        Map<String, Object> payload = new HashMap<>();
        payload.put("pan", cleanPan);
        if (options != null) {
            payload.putAll(options);
        }

        return executeItrOperation(connectionId, "VERIFY_PAN", payload);
    }

    @Override
    public ItrIntegrationResultDto executeItrOperation(UUID connectionId, String operationType, Map<String, Object> payload) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = govConnectionService.getConnection(connectionId);

        if (connection.getProviderType() != GovProviderType.INCOME_TAX) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connectionId + " is not an Income Tax provider connection");
        }

        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "ITR_INTEGRATION_REQUESTED",
                "ITR_INTEGRATION",
                connection.getId().toString(),
                null,
                Map.of("operationType", operationType, "correlationId", correlationId)
        );

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.INCOME_TAX)
                .operationType(operationType)
                .businessEntityType("ITR_CONNECTION")
                .businessEntityId(connection.getId())
                .correlationId(correlationId)
                .requestData(payload != null ? payload : Collections.emptyMap())
                .build();

        GovIntegrationResult govResult = govIntegrationService.executeOperation(govRequest);

        if (!govResult.isSuccess()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "ITR_INTEGRATION_FAILED",
                    "ITR_INTEGRATION",
                    connection.getId().toString(),
                    null,
                    Map.of(
                            "operationType", operationType,
                            "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                    )
            );
        }

        String pan = payload != null && payload.containsKey("pan") ? String.valueOf(payload.get("pan")) : null;

        return ItrIntegrationResultDto.builder()
                .success(govResult.isSuccess())
                .operationId(govResult.getOperationId())
                .connectionId(connection.getId())
                .pan(pan)
                .operationType(govResult.getOperationType())
                .correlationId(govResult.getCorrelationId())
                .providerReferenceId(govResult.getProviderReferenceId())
                .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : null)
                .errorMessage(govResult.getErrorMessage())
                .data(govResult.getResponseMetadata())
                .build();
    }

    private void validatePan(String pan) {
        if (!StringUtils.hasText(pan) || !PAN_PATTERN.matcher(pan.trim().toUpperCase()).matches()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Invalid PAN format. Must match standard 10-character PAN format (e.g., ABCDE1234F).");
        }
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Tenant context is missing. Action requires an authenticated tenant organization.");
        }
        return tenantId;
    }
}
