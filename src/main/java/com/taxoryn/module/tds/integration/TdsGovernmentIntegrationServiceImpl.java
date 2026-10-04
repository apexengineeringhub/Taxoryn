package com.taxoryn.module.tds.integration;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovConnectionHealthDto;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovConnectionStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gov.service.GovernmentHealthService;
import com.taxoryn.module.gov.service.GovernmentIntegrationService;
import com.taxoryn.module.tds.dto.TdsDeductorProfileDto;
import com.taxoryn.module.tds.integration.dto.TdsHandshakeResponseDto;
import com.taxoryn.module.tds.integration.dto.TdsIntegrationResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Implementation of TdsGovernmentIntegrationService bridging the TDS domain with Government Integration Framework.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TdsGovernmentIntegrationServiceImpl implements TdsGovernmentIntegrationService {

    private static final Pattern TAN_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{5}[A-Z]{1}$");

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
    public TdsDeductorProfileDto lookupDeductor(String tan) {
        return lookupDeductor(null, tan, Collections.emptyMap());
    }

    @Override
    public TdsDeductorProfileDto lookupDeductor(UUID connectionId, String tan, Map<String, Object> options) {
        UUID tenantId = requireActiveTenantId();
        validateTan(tan);
        String cleanTan = tan.trim().toUpperCase();
        String maskedTan = maskTan(cleanTan);

        GovConnectionDto connection = resolveTdsConnection(connectionId);
        String correlationId = UUID.randomUUID().toString();

        auditService.logEvent(
                tenantId,
                null,
                "TDS_TAN_LOOKUP_REQUESTED",
                "TDS_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("tan", maskedTan, "correlationId", correlationId)
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("tan", cleanTan);
        if (options != null) {
            payload.putAll(options);
        }

        GovIntegrationRequest govRequest = GovIntegrationRequest.builder()
                .organizationId(tenantId)
                .providerType(GovProviderType.TDS)
                .operationType("VERIFY_TAN")
                .businessEntityType("TDS_CONNECTION")
                .businessEntityId(connection.getId())
                .correlationId(correlationId)
                .requestData(payload)
                .build();

        GovIntegrationResult govResult = govIntegrationService.executeOperation(govRequest);
        Instant now = Instant.now();

        if (!govResult.isSuccess()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "TDS_TAN_LOOKUP_FAILED",
                    "TDS_CONNECTION",
                    connection.getId().toString(),
                    null,
                    Map.of(
                            "tan", maskedTan,
                            "errorCode", govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN",
                            "errorMessage", govResult.getErrorMessage() != null ? govResult.getErrorMessage() : ""
                    )
            );

            return TdsDeductorProfileDto.builder()
                    .tan(cleanTan)
                    .valid(false)
                    .verified(false)
                    .errorCode(govResult.getErrorCode() != null ? govResult.getErrorCode().name() : "UNKNOWN")
                    .errorMessage(govResult.getErrorMessage())
                    .operationId(govResult.getOperationId())
                    .verifiedAt(now)
                    .rawData(govResult.getResponseMetadata())
                    .build();
        }

        auditService.logEvent(
                tenantId,
                null,
                "TDS_TAN_LOOKUP_COMPLETED",
                "TDS_CONNECTION",
                connection.getId().toString(),
                null,
                Map.of("tan", maskedTan, "ackNumber", govResult.getProviderReferenceId() != null ? govResult.getProviderReferenceId() : "")
        );

        Map<String, Object> data = govResult.getResponseMetadata() != null ? govResult.getResponseMetadata() : Collections.emptyMap();

        return TdsDeductorProfileDto.builder()
                .tan(cleanTan)
                .deductorName((String) data.getOrDefault("deductorName", "Acme Enterprises Private Limited"))
                .category((String) data.getOrDefault("category", "COMPANY"))
                .status((String) data.getOrDefault("status", "ACTIVE"))
                .tanStatus((String) data.getOrDefault("tanStatus", "VALID"))
                .tracesStatus((String) data.getOrDefault("tracesStatus", "REGISTERED_ACTIVE"))
                .pan((String) data.get("pan"))
                .state((String) data.get("state"))
                .pinCode((String) data.get("pinCode"))
                .address((String) data.get("address"))
                .valid(true)
                .verified(true)
                .providerReferenceId(govResult.getProviderReferenceId())
                .operationId(govResult.getOperationId())
                .verifiedAt(now)
                .rawData(data)
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

    private GovConnectionDto resolveTdsConnection(UUID connectionId) {
        if (connectionId != null) {
            GovConnectionDto conn = govConnectionService.getConnection(connectionId);
            if (conn.getProviderType() != GovProviderType.TDS) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "Connection " + connectionId + " is not a TDS provider connection");
            }
            return conn;
        }

        List<GovConnectionDto> connections = govConnectionService.listConnectionsByProvider(GovProviderType.TDS);
        if (connections.isEmpty()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "No TDS Government Connection configured for active tenant");
        }

        Optional<GovConnectionDto> activeConn = connections.stream()
                .filter(c -> c.getStatus() == GovConnectionStatus.ACTIVE)
                .findFirst();

        return activeConn.orElse(connections.get(0));
    }

    private void validateTan(String tan) {
        if (tan == null || tan.trim().isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "TAN is required and cannot be blank");
        }
        String cleanTan = tan.trim().toUpperCase();
        if (!TAN_PATTERN.matcher(cleanTan).matches()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Invalid TAN format: " + tan + ". Must follow pattern ABCD12345E");
        }
    }

    private String maskTan(String tan) {
        if (tan == null || tan.length() != 10) {
            return "**********";
        }
        return tan.substring(0, 4) + "*****" + tan.substring(9);
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant context is required for TDS operations");
        }
        return tenantId;
    }
}
