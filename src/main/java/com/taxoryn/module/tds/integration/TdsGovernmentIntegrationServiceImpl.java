package com.taxoryn.module.tds.integration;

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
import com.taxoryn.module.tds.integration.dto.TdsHandshakeResponseDto;
import com.taxoryn.module.tds.integration.dto.TdsIntegrationResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of TdsGovernmentIntegrationService bridging the TDS domain with Government Integration Framework.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TdsGovernmentIntegrationServiceImpl implements TdsGovernmentIntegrationService {

    private final GovernmentIntegrationService govIntegrationService;
    private final GovernmentConnectionService govConnectionService;
    private final GovernmentHealthService govHealthService;
    private final AuditService auditService;

    @Override
    public TdsHandshakeResponseDto checkTdsConnectionHealth(UUID connectionId) {
        return checkTdsConnectionHealth(connectionId, Collections.emptyMap());
    }

    @Override
    public TdsHandshakeResponseDto checkTdsConnectionHealth(UUID connectionId, Map<String, Object> directives) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = govConnectionService.getConnection(connectionId);

        if (connection.getProviderType() != GovProviderType.TDS) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connectionId + " is not a TDS provider connection");
        }

        auditService.logEvent(
                tenantId,
                null,
                "TDS_PROVIDER_HANDSHAKE",
                "TDS_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("displayName", connection.getDisplayName(), "providerType", connection.getProviderType().name())
        );

        GovConnectionHealthDto health = govHealthService.checkConnectionHealth(connectionId, directives);

        return TdsHandshakeResponseDto.builder()
                .connectionId(health.getConnectionId())
                .displayName(health.getDisplayName())
                .healthStatus(health.getHealthStatus() != null ? health.getHealthStatus().name() : "UNKNOWN")
                .latencyMs(health.getLatencyMs())
                .message(health.getMessage())
                .lastHealthCheckAt(health.getLastHealthCheckAt())
                .build();
    }

    @Override
    public TdsIntegrationResultDto executeTdsOperation(UUID connectionId, String operationType, Map<String, Object> payload) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = govConnectionService.getConnection(connectionId);

        if (connection.getProviderType() != GovProviderType.TDS) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connectionId + " is not a TDS provider connection");
        }

        String correlationId = UUID.randomUUID().toString();
        String idempotencyKey = "TDS_OP_" + tenantId + "_" + operationType + "_" + System.currentTimeMillis();

        auditService.logEvent(
                tenantId,
                null,
                "TDS_INTEGRATION_REQUESTED",
                "TDS_INTEGRATION",
                connection.getId().toString(),
                null,
                Map.of("operationType", operationType, "correlationId", correlationId)
        );

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.TDS)
                .operationType(operationType)
                .businessEntityType("TDS_CONNECTION")
                .businessEntityId(connection.getId())
                .correlationId(correlationId)
                .idempotencyKey(idempotencyKey)
                .requestData(payload != null ? new HashMap<>(payload) : new HashMap<>())
                .build();

        GovIntegrationResult govResult = govIntegrationService.executeOperation(govRequest);

        if (!govResult.isSuccess()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "TDS_INTEGRATION_FAILED",
                    "TDS_INTEGRATION",
                    connection.getId().toString(),
                    null,
                    Map.of(
                            "operationType", operationType,
                            "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                    )
            );
        }

        String tan = null;
        if (payload != null && payload.containsKey("tan")) {
            tan = String.valueOf(payload.get("tan"));
        } else if (govResult.getResponseMetadata() != null && govResult.getResponseMetadata().containsKey("tan")) {
            tan = String.valueOf(govResult.getResponseMetadata().get("tan"));
        }

        return TdsIntegrationResultDto.builder()
                .success(govResult.isSuccess())
                .operationId(govResult.getOperationId())
                .connectionId(connectionId)
                .tan(tan)
                .operationType(govResult.getOperationType())
                .correlationId(govResult.getCorrelationId())
                .providerReferenceId(govResult.getProviderReferenceId())
                .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : null)
                .errorMessage(govResult.getErrorMessage())
                .data(govResult.getResponseMetadata() != null ? govResult.getResponseMetadata() : new HashMap<>())
                .build();
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant context is required for TDS operations");
        }
        return tenantId;
    }
}
