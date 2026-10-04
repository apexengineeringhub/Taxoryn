package com.taxoryn.module.gov.service;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovCredentialReferenceDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.dto.UpdateGovConnectionRequest;
import com.taxoryn.module.gov.entity.GovConnectionEntity;
import com.taxoryn.module.gov.entity.GovCredentialReferenceEntity;
import com.taxoryn.module.gov.exception.GovConnectionNotFoundException;
import com.taxoryn.module.gov.exception.GovConnectionStateTransitionException;
import com.taxoryn.module.gov.exception.GovCredentialNotFoundException;
import com.taxoryn.module.gov.exception.GovValidationException;
import com.taxoryn.module.gov.model.GovConnectionStatus;
import com.taxoryn.module.gov.model.GovCredentialStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.repository.GovConnectionRepository;
import com.taxoryn.module.gov.repository.GovCredentialReferenceRepository;
import com.taxoryn.module.gov.spi.GovSecretStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enterprise implementation of GovernmentConnectionService.
 * Enforces strict multi-tenant isolation, state machine transitions, encrypted secret lifecycle, and auditability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GovernmentConnectionServiceImpl implements GovernmentConnectionService {

    private final GovConnectionRepository connectionRepository;
    private final GovCredentialReferenceRepository credentialReferenceRepository;
    private final GovSecretStorageProvider secretStorageProvider;
    private final AuditService auditService;

    @Override
    @Transactional
    public GovConnectionDto createConnection(CreateGovConnectionRequest request) {
        if (request == null) {
            throw new GovValidationException("CreateGovConnectionRequest cannot be null");
        }
        if (request.getProviderType() == null) {
            throw new GovValidationException("Provider type is required");
        }
        if (!StringUtils.hasText(request.getDisplayName())) {
            throw new GovValidationException("Display name is required");
        }

        UUID tenantId = requireActiveTenantId();

        GovConnectionEntity entity = GovConnectionEntity.builder()
                .providerType(request.getProviderType())
                .displayName(request.getDisplayName().trim())
                .description(request.getDescription())
                .environment(StringUtils.hasText(request.getEnvironment()) ? request.getEnvironment().trim() : "PRODUCTION")
                .status(GovConnectionStatus.CREATED)
                .metadata(request.getMetadata())
                .build();
        entity.setOrganizationId(tenantId);

        GovConnectionEntity saved = connectionRepository.save(entity);

        auditService.logEvent(
                tenantId,
                null,
                "GOVERNMENT_CONNECTION_CREATED",
                "GOV_CONNECTION",
                saved.getId().toString(),
                null,
                Map.of(
                        "providerType", saved.getProviderType().name(),
                        "displayName", saved.getDisplayName(),
                        "environment", saved.getEnvironment()
                )
        );

        log.info("[GOV_CONNECTION_CREATED] Created connection id={} for provider={}", saved.getId(), saved.getProviderType());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GovConnectionDto getConnection(UUID connectionId) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionEntity entity = connectionRepository.findByIdAndOrganizationId(connectionId, tenantId)
                .orElseThrow(() -> new GovConnectionNotFoundException(connectionId));
        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovConnectionDto> listConnections() {
        UUID tenantId = requireActiveTenantId();
        return connectionRepository.findAllByOrganizationId(tenantId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GovConnectionDto> listConnectionsByProvider(GovProviderType providerType) {
        UUID tenantId = requireActiveTenantId();
        if (providerType == null) {
            return Collections.emptyList();
        }
        return connectionRepository.findAllByOrganizationIdAndProviderType(tenantId, providerType)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public GovConnectionDto updateConnection(UUID connectionId, UpdateGovConnectionRequest request) {
        if (request == null) {
            throw new GovValidationException("UpdateGovConnectionRequest cannot be null");
        }
        UUID tenantId = requireActiveTenantId();
        GovConnectionEntity entity = connectionRepository.findByIdAndOrganizationId(connectionId, tenantId)
                .orElseThrow(() -> new GovConnectionNotFoundException(connectionId));

        if (StringUtils.hasText(request.getDisplayName())) {
            entity.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getEnvironment())) {
            entity.setEnvironment(request.getEnvironment().trim());
        }
        if (request.getMetadata() != null) {
            entity.setMetadata(request.getMetadata());
        }

        GovConnectionEntity updated = connectionRepository.save(entity);

        auditService.logEvent(
                tenantId,
                null,
                "GOVERNMENT_CONNECTION_UPDATED",
                "GOV_CONNECTION",
                updated.getId().toString(),
                null,
                Map.of(
                        "displayName", updated.getDisplayName(),
                        "environment", updated.getEnvironment()
                )
        );

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public GovConnectionDto activateConnection(UUID connectionId) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionEntity entity = connectionRepository.findByIdAndOrganizationId(connectionId, tenantId)
                .orElseThrow(() -> new GovConnectionNotFoundException(connectionId));

        if (entity.getCredentialReferenceId() == null) {
            throw new GovConnectionStateTransitionException("Cannot activate connection without a registered credential reference");
        }

        // Verify credential is VALID
        GovCredentialReferenceEntity cred = credentialReferenceRepository
                .findByIdAndOrganizationId(entity.getCredentialReferenceId(), tenantId)
                .orElseThrow(() -> new GovCredentialNotFoundException(entity.getCredentialReferenceId()));

        if (cred.getCredentialStatus() != GovCredentialStatus.VALID || cred.isExpired()) {
            throw new GovConnectionStateTransitionException("Cannot activate connection with expired or invalid credential reference");
        }

        entity.transitionTo(GovConnectionStatus.ACTIVE);
        GovConnectionEntity saved = connectionRepository.save(entity);

        auditService.logEvent(
                tenantId,
                null,
                "GOVERNMENT_CONNECTION_ACTIVATED",
                "GOV_CONNECTION",
                saved.getId().toString(),
                null,
                Map.of("status", saved.getStatus().name())
        );

        log.info("[GOV_CONNECTION_ACTIVATED] Activated connection id={}", saved.getId());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public GovConnectionDto deactivateConnection(UUID connectionId) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionEntity entity = connectionRepository.findByIdAndOrganizationId(connectionId, tenantId)
                .orElseThrow(() -> new GovConnectionNotFoundException(connectionId));

        entity.transitionTo(GovConnectionStatus.INACTIVE);
        GovConnectionEntity saved = connectionRepository.save(entity);

        auditService.logEvent(
                tenantId,
                null,
                "GOVERNMENT_CONNECTION_DEACTIVATED",
                "GOV_CONNECTION",
                saved.getId().toString(),
                null,
                Map.of("status", saved.getStatus().name())
        );

        log.info("[GOV_CONNECTION_DEACTIVATED] Deactivated connection id={}", saved.getId());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public GovConnectionDto markAuthRequired(UUID connectionId) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionEntity entity = connectionRepository.findByIdAndOrganizationId(connectionId, tenantId)
                .orElseThrow(() -> new GovConnectionNotFoundException(connectionId));

        entity.transitionTo(GovConnectionStatus.AUTH_REQUIRED);
        GovConnectionEntity saved = connectionRepository.save(entity);

        auditService.logEvent(
                tenantId,
                null,
                "GOVERNMENT_CONNECTION_AUTH_REQUIRED",
                "GOV_CONNECTION",
                saved.getId().toString(),
                null,
                Map.of("status", saved.getStatus().name())
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public GovConnectionDto markFailed(UUID connectionId, String reason) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionEntity entity = connectionRepository.findByIdAndOrganizationId(connectionId, tenantId)
                .orElseThrow(() -> new GovConnectionNotFoundException(connectionId));

        entity.transitionTo(GovConnectionStatus.FAILED);
        GovConnectionEntity saved = connectionRepository.save(entity);

        auditService.logEvent(
                tenantId,
                null,
                "GOVERNMENT_CONNECTION_FAILED",
                "GOV_CONNECTION",
                saved.getId().toString(),
                null,
                Map.of("status", saved.getStatus().name(), "reason", reason != null ? reason : "Unknown failure")
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public GovCredentialReferenceDto registerCredential(RegisterGovCredentialRequest request) {
        if (request == null) {
            throw new GovValidationException("RegisterGovCredentialRequest cannot be null");
        }
        if (request.getConnectionId() == null) {
            throw new GovValidationException("Connection ID is required");
        }
        if (request.getCredentialType() == null) {
            throw new GovValidationException("Credential type is required");
        }
        if (!StringUtils.hasText(request.getRawSecret())) {
            throw new GovValidationException("Raw secret is required");
        }

        UUID tenantId = requireActiveTenantId();

        GovConnectionEntity connection = connectionRepository.findByIdAndOrganizationId(request.getConnectionId(), tenantId)
                .orElseThrow(() -> new GovConnectionNotFoundException(request.getConnectionId()));

        // Encrypt and store secret via SPI
        String encryptedPayload = secretStorageProvider.storeSecret(
                tenantId,
                connection.getId(),
                request.getRawSecret()
        );

        String maskedIdentifier = StringUtils.hasText(request.getMaskedIdentifier())
                ? request.getMaskedIdentifier().trim()
                : maskIdentifierFallback(request.getCredentialType().name());

        GovCredentialReferenceEntity credEntity = GovCredentialReferenceEntity.builder()
                .connectionId(connection.getId())
                .credentialType(request.getCredentialType())
                .credentialStatus(GovCredentialStatus.VALID)
                .maskedIdentifier(maskedIdentifier)
                .encryptedSecret(encryptedPayload)
                .secretStorageProvider(secretStorageProvider.getStorageProviderCode())
                .lastValidatedAt(Instant.now())
                .expiresAt(request.getExpiresAt())
                .metadata(request.getMetadata())
                .build();
        credEntity.setOrganizationId(tenantId);

        GovCredentialReferenceEntity savedCred = credentialReferenceRepository.save(credEntity);

        // Update connection with active credential reference
        connection.setCredentialReferenceId(savedCred.getId());
        connectionRepository.save(connection);

        // Audit without raw secret
        auditService.logEvent(
                tenantId,
                null,
                "GOVERNMENT_CREDENTIAL_REGISTERED",
                "GOV_CREDENTIAL",
                savedCred.getId().toString(),
                null,
                Map.of(
                        "connectionId", connection.getId().toString(),
                        "credentialType", savedCred.getCredentialType().name(),
                        "maskedIdentifier", savedCred.getMaskedIdentifier(),
                        "storageProvider", savedCred.getSecretStorageProvider()
                )
        );

        log.info("[GOV_CREDENTIAL_REGISTERED] Registered credential id={} for connection id={}", savedCred.getId(), connection.getId());
        return mapToCredentialDto(savedCred);
    }

    @Override
    @Transactional
    public GovCredentialReferenceDto invalidateCredential(UUID credentialRefId) {
        UUID tenantId = requireActiveTenantId();
        GovCredentialReferenceEntity cred = credentialReferenceRepository.findByIdAndOrganizationId(credentialRefId, tenantId)
                .orElseThrow(() -> new GovCredentialNotFoundException(credentialRefId));

        cred.invalidate();
        GovCredentialReferenceEntity saved = credentialReferenceRepository.save(cred);

        // Also purge secret payload via SPI
        secretStorageProvider.deleteSecret(tenantId, credentialRefId);

        auditService.logEvent(
                tenantId,
                null,
                "GOVERNMENT_CREDENTIAL_INVALIDATED",
                "GOV_CREDENTIAL",
                saved.getId().toString(),
                null,
                Map.of("status", saved.getCredentialStatus().name())
        );

        log.info("[GOV_CREDENTIAL_INVALIDATED] Invalidated credential id={}", saved.getId());
        return mapToCredentialDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GovCredentialReferenceDto getCredentialMetadata(UUID credentialRefId) {
        UUID tenantId = requireActiveTenantId();
        GovCredentialReferenceEntity cred = credentialReferenceRepository.findByIdAndOrganizationId(credentialRefId, tenantId)
                .orElseThrow(() -> new GovCredentialNotFoundException(credentialRefId));
        return mapToCredentialDto(cred);
    }

    @Override
    @Transactional(readOnly = true)
    public String getDecryptedSecret(UUID connectionId) {
        UUID tenantId = requireActiveTenantId();
        GovConnectionEntity connection = connectionRepository.findByIdAndOrganizationId(connectionId, tenantId)
                .orElseThrow(() -> new GovConnectionNotFoundException(connectionId));

        if (connection.getCredentialReferenceId() == null) {
            throw new GovCredentialNotFoundException(connectionId);
        }

        GovCredentialReferenceEntity cred = credentialReferenceRepository
                .findByIdAndOrganizationId(connection.getCredentialReferenceId(), tenantId)
                .orElseThrow(() -> new GovCredentialNotFoundException(connection.getCredentialReferenceId()));

        if (cred.getCredentialStatus() != GovCredentialStatus.VALID || cred.isExpired()) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Government credential reference is inactive or expired");
        }

        return secretStorageProvider.retrieveSecret(tenantId, cred.getId(), cred.getEncryptedSecret());
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant required for government connection management");
        }
        return tenantId;
    }

    private String maskIdentifierFallback(String type) {
        return type + "-***";
    }

    private GovConnectionDto mapToDto(GovConnectionEntity entity) {
        return GovConnectionDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .providerType(entity.getProviderType())
                .displayName(entity.getDisplayName())
                .description(entity.getDescription())
                .environment(entity.getEnvironment())
                .status(entity.getStatus())
                .credentialReferenceId(entity.getCredentialReferenceId())
                .metadata(entity.getMetadata())
                .healthStatus(entity.getHealthStatus())
                .lastHealthCheckAt(entity.getLastHealthCheckAt())
                .healthMessage(entity.getHealthMessage())
                .healthLatencyMs(entity.getHealthLatencyMs())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .version(entity.getVersion())
                .build();
    }

    private GovCredentialReferenceDto mapToCredentialDto(GovCredentialReferenceEntity entity) {
        return GovCredentialReferenceDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .connectionId(entity.getConnectionId())
                .credentialType(entity.getCredentialType())
                .credentialStatus(entity.getCredentialStatus())
                .maskedIdentifier(entity.getMaskedIdentifier())
                .secretStorageProvider(entity.getSecretStorageProvider())
                .lastValidatedAt(entity.getLastValidatedAt())
                .expiresAt(entity.getExpiresAt())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
