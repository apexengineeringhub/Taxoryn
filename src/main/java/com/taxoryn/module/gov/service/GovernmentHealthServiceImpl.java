package com.taxoryn.module.gov.service;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.GovConnectionHealthDto;
import com.taxoryn.module.gov.dto.GovHandshakeRequest;
import com.taxoryn.module.gov.dto.GovHandshakeResult;
import com.taxoryn.module.gov.entity.GovConnectionEntity;
import com.taxoryn.module.gov.entity.GovCredentialReferenceEntity;
import com.taxoryn.module.gov.exception.GovConnectionNotFoundException;
import com.taxoryn.module.gov.exception.GovValidationException;
import com.taxoryn.module.gov.model.GovConnectionHealthStatus;
import com.taxoryn.module.gov.model.GovConnectionStatus;
import com.taxoryn.module.gov.model.GovCredentialStatus;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.registry.GovernmentProviderRegistry;
import com.taxoryn.module.gov.repository.GovConnectionRepository;
import com.taxoryn.module.gov.repository.GovCredentialReferenceRepository;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enterprise implementation of GovernmentHealthService.
 * Validates connection compatibility, dynamic adapter resolution, handshake execution, and telemetry tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GovernmentHealthServiceImpl implements GovernmentHealthService {

    private final GovConnectionRepository connectionRepository;
    private final GovCredentialReferenceRepository credentialRepository;
    private final GovernmentProviderRegistry providerRegistry;
    private final AuditService auditService;

    @Override
    @Transactional
    public GovConnectionHealthDto checkConnectionHealth(UUID connectionId) {
        return checkConnectionHealth(connectionId, Collections.emptyMap());
    }

    @Override
    @Transactional
    public GovConnectionHealthDto checkConnectionHealth(UUID connectionId, Map<String, Object> directives) {
        if (connectionId == null) {
            throw new GovValidationException("Connection ID must not be null");
        }
        UUID tenantId = requireActiveTenantId();

        GovConnectionEntity connection = connectionRepository.findByIdAndOrganizationId(connectionId, tenantId)
                .orElseThrow(() -> new GovConnectionNotFoundException(connectionId));

        if (connection.getStatus() == GovConnectionStatus.INACTIVE) {
            GovHandshakeResult inactiveResult = GovHandshakeResult.builder()
                    .providerType(connection.getProviderType())
                    .healthStatus(GovConnectionHealthStatus.UNKNOWN)
                    .message("Connection is deactivated by practice administrator")
                    .latencyMs(0)
                    .build();
            connection.updateHealth(inactiveResult);
            connectionRepository.save(connection);
            return mapToHealthDto(connection, null);
        }

        // Resolve Adapter via Dynamic Registry
        GovernmentProviderAdapter adapter = providerRegistry.requireAdapter(connection.getProviderType());

        // Validate credential status if attached
        GovCredentialReferenceEntity cred = null;
        if (connection.getCredentialReferenceId() != null) {
            cred = credentialRepository.findByIdAndOrganizationId(connection.getCredentialReferenceId(), tenantId)
                    .orElse(null);
        }

        GovHandshakeResult handshakeResult;
        if (cred != null && (cred.getCredentialStatus() != GovCredentialStatus.VALID || cred.isExpired())) {
            handshakeResult = GovHandshakeResult.authRequired(
                    connection.getProviderType(),
                    adapter.getAdapterCode(),
                    "Registered credential reference is expired or revoked"
            );
        } else {
            Map<String, Object> requestMeta = new HashMap<>();
            if (directives != null) {
                requestMeta.putAll(directives);
            }

            GovHandshakeRequest handshakeRequest = GovHandshakeRequest.builder()
                    .organizationId(tenantId)
                    .connectionId(connection.getId())
                    .providerType(connection.getProviderType())
                    .adapterCode(adapter.getAdapterCode())
                    .environment(connection.getEnvironment())
                    .credentialType(cred != null ? cred.getCredentialType() : null)
                    .metadata(requestMeta)
                    .build();

            try {
                handshakeResult = adapter.handshake(handshakeRequest);
            } catch (Exception ex) {
                log.error("[GOV_HANDSHAKE_ERROR] Unhandled exception during provider handshake for connection id={}", connection.getId(), ex);
                handshakeResult = GovHandshakeResult.error(
                        connection.getProviderType(),
                        adapter.getAdapterCode(),
                        GovErrorCode.UNKNOWN,
                        "Handshake failed: " + (ex.getMessage() != null ? ex.getMessage() : "Unknown gateway error")
                );
            }
        }

        GovConnectionHealthStatus previousHealth = connection.getHealthStatus();
        connection.updateHealth(handshakeResult);
        GovConnectionEntity saved = connectionRepository.save(connection);

        // Audit health change if status changed
        if (previousHealth != saved.getHealthStatus()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "GOV_CONNECTION_HEALTH_CHANGED",
                    "GOV_CONNECTION",
                    saved.getId().toString(),
                    null,
                    Map.of(
                            "previousHealth", previousHealth != null ? previousHealth.name() : "UNKNOWN",
                            "newHealth", saved.getHealthStatus().name(),
                            "providerType", saved.getProviderType().name(),
                            "adapterCode", adapter.getAdapterCode()
                    )
            );
        }

        return mapToHealthDto(saved, adapter.getAdapterCode());
    }

    @Override
    @Transactional
    public List<GovConnectionHealthDto> checkAllActiveConnections() {
        UUID tenantId = requireActiveTenantId();
        List<GovConnectionEntity> activeConnections = connectionRepository.findAllByOrganizationId(tenantId)
                .stream()
                .filter(c -> c.getStatus() == GovConnectionStatus.ACTIVE)
                .toList();

        List<GovConnectionHealthDto> results = new ArrayList<>();
        for (GovConnectionEntity conn : activeConnections) {
            results.add(checkConnectionHealth(conn.getId()));
        }
        return results;
    }

    @Override
    public GovHandshakeResult executeDirectHandshake(GovHandshakeRequest request) {
        if (request == null) {
            throw new GovValidationException("GovHandshakeRequest cannot be null");
        }
        if (request.getProviderType() == null) {
            throw new GovValidationException("GovProviderType is mandatory");
        }

        GovernmentProviderAdapter adapter = providerRegistry.requireAdapter(request.getProviderType());
        return adapter.handshake(request);
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant required for government health check");
        }
        return tenantId;
    }

    private GovConnectionHealthDto mapToHealthDto(GovConnectionEntity entity, String adapterCode) {
        return GovConnectionHealthDto.builder()
                .connectionId(entity.getId())
                .organizationId(entity.getOrganizationId())
                .providerType(entity.getProviderType())
                .adapterCode(adapterCode)
                .displayName(entity.getDisplayName())
                .connectionStatus(entity.getStatus())
                .healthStatus(entity.getHealthStatus())
                .latencyMs(entity.getHealthLatencyMs() != null ? entity.getHealthLatencyMs() : 0L)
                .message(entity.getHealthMessage())
                .lastHealthCheckAt(entity.getLastHealthCheckAt())
                .build();
    }
}
