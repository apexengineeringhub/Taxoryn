package com.taxoryn.module.gst.integration;

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
import com.taxoryn.module.gst.integration.dto.GstHandshakeResponseDto;
import com.taxoryn.module.gst.integration.dto.GstIntegrationResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enterprise implementation of GstGovernmentIntegrationService.
 * Connects the GST domain to the Government Integration Framework via DTO application boundaries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GstGovernmentIntegrationServiceImpl implements GstGovernmentIntegrationService {

    private final GovernmentIntegrationService govIntegrationService;
    private final GovernmentConnectionService govConnectionService;
    private final GovernmentHealthService govHealthService;
    private final AuditService auditService;

    @Override
    public GstHandshakeResponseDto checkGstConnectionHealth(UUID connectionId) {
        return checkGstConnectionHealth(connectionId, Collections.emptyMap());
    }

    @Override
    public GstHandshakeResponseDto checkGstConnectionHealth(UUID connectionId, Map<String, Object> directives) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = govConnectionService.getConnection(connectionId);

        if (connection.getProviderType() != GovProviderType.GST) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connectionId + " is not a GST provider connection");
        }

        auditService.logEvent(
                tenantId,
                null,
                "GST_PROVIDER_HANDSHAKE",
                "GST_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("displayName", connection.getDisplayName(), "providerType", connection.getProviderType().name())
        );

        GovConnectionHealthDto health = govHealthService.checkConnectionHealth(connectionId, directives);

        return GstHandshakeResponseDto.builder()
                .connectionId(health.getConnectionId())
                .displayName(health.getDisplayName())
                .healthStatus(health.getHealthStatus() != null ? health.getHealthStatus().name() : "UNKNOWN")
                .latencyMs(health.getLatencyMs())
                .message(health.getMessage())
                .lastHealthCheckAt(health.getLastHealthCheckAt())
                .build();
    }

    @Override
    public GstIntegrationResultDto verifyGstin(UUID connectionId, String gstin) {
        return verifyGstin(connectionId, gstin, Collections.emptyMap());
    }

    @Override
    public GstIntegrationResultDto verifyGstin(UUID connectionId, String gstin, Map<String, Object> options) {
        if (!StringUtils.hasText(gstin)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "GSTIN must not be blank");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("gstin", gstin.trim().toUpperCase());
        if (options != null) {
            payload.putAll(options);
        }

        return executeGstOperation(connectionId, "VERIFY_GSTIN", payload);
    }

    @Override
    public GstIntegrationResultDto executeGstOperation(UUID connectionId, String operationType, Map<String, Object> payload) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = govConnectionService.getConnection(connectionId);

        if (connection.getProviderType() != GovProviderType.GST) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connectionId + " is not a GST provider connection");
        }

        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "GST_INTEGRATION_REQUESTED",
                "GST_INTEGRATION",
                connection.getId().toString(),
                null,
                Map.of("operationType", operationType, "correlationId", correlationId)
        );

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.GST)
                .operationType(operationType)
                .businessEntityType("GST_CONNECTION")
                .businessEntityId(connection.getId())
                .correlationId(correlationId)
                .requestData(payload != null ? payload : Collections.emptyMap())
                .build();

        GovIntegrationResult govResult = govIntegrationService.executeOperation(govRequest);

        if (!govResult.isSuccess()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "GST_INTEGRATION_FAILED",
                    "GST_INTEGRATION",
                    connection.getId().toString(),
                    null,
                    Map.of(
                            "operationType", operationType,
                            "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                    )
            );
        }

        String gstin = payload != null && payload.containsKey("gstin") ? String.valueOf(payload.get("gstin")) : null;

        return GstIntegrationResultDto.builder()
                .success(govResult.isSuccess())
                .operationId(govResult.getOperationId())
                .connectionId(connection.getId())
                .gstin(gstin)
                .operationType(govResult.getOperationType())
                .correlationId(govResult.getCorrelationId())
                .providerReferenceId(govResult.getProviderReferenceId())
                .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : null)
                .errorMessage(govResult.getErrorMessage())
                .data(govResult.getResponseMetadata())
                .build();
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant required for GST government integration");
        }
        return tenantId;
    }
}
