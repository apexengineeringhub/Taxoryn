package com.taxoryn.module.gov.auth.service;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.auth.dto.GovAuthContinueRequest;
import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.dto.GovAuthStartRequest;
import com.taxoryn.module.gov.auth.entity.GovAuthSessionEntity;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.auth.model.GovAuthorizationState;
import com.taxoryn.module.gov.auth.repository.GovAuthSessionRepository;
import com.taxoryn.module.gov.auth.spi.GovernmentAuthenticationProvider;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.exception.GovIntegrationException;
import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GovernmentAuthenticationServiceImpl implements GovernmentAuthenticationService {

    private final GovAuthSessionRepository authSessionRepository;
    private final GovernmentConnectionService connectionService;
    private final GovernmentAuthenticationProvider authProvider;
    private final AuditService auditService;

    @Override
    @Transactional
    public GovAuthSessionDto startAuthentication(GovAuthStartRequest request) {
        if (request == null || request.getConnectionId() == null || request.getAuthMethod() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Connection ID and Authentication Method are mandatory");
        }

        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = connectionService.getConnection(request.getConnectionId());

        if (!authProvider.getSupportedMethods().contains(request.getAuthMethod())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Authentication method " + request.getAuthMethod() + " is not supported by provider " + connection.getProviderType());
        }

        String correlationId = StringUtils.hasText(request.getCorrelationId())
                ? request.getCorrelationId().trim()
                : UUID.randomUUID().toString();

        // Check for existing active or pending session for idempotency
        Optional<GovAuthSessionEntity> existingOpt = authSessionRepository
                .findFirstByOrganizationIdAndConnectionIdAndStatusInOrderByCreatedAtDesc(
                        tenantId,
                        connection.getId(),
                        List.of(GovAuthStatus.AUTHENTICATED, GovAuthStatus.AUTHENTICATION_PENDING, GovAuthStatus.AUTHENTICATION_STARTED)
                );

        if (existingOpt.isPresent()) {
            GovAuthSessionEntity existing = existingOpt.get();
            // If already authenticated and not expired with same method, return it idempotently
            if (existing.getStatus() == GovAuthStatus.AUTHENTICATED && !existing.isExpired() && existing.getAuthMethod() == request.getAuthMethod()) {
                log.info("[GOV_AUTH] Reusing existing valid active auth session id={} for connection id={}",
                        existing.getId(), connection.getId());
                return mapToDto(existing);
            }
        }

        auditService.logEvent(
                tenantId,
                null,
                "GOV_AUTH_SESSION_STARTED",
                "GOV_AUTH_SESSION",
                connection.getId().toString(),
                null,
                Map.of(
                        "connectionId", connection.getId().toString(),
                        "providerType", connection.getProviderType().name(),
                        "authMethod", request.getAuthMethod().name(),
                        "correlationId", correlationId
                )
        );

        GovAuthSessionDto providerResult = authProvider.startAuthentication(
                connection,
                request.getAuthMethod(),
                correlationId,
                request.getOptions() != null ? request.getOptions() : Collections.emptyMap()
        );

        GovAuthSessionEntity sessionEntity = GovAuthSessionEntity.builder()
                .connectionId(connection.getId())
                .providerType(connection.getProviderType())
                .authMethod(request.getAuthMethod())
                .status(providerResult.getStatus())
                .requiresUserAction(providerResult.isRequiresUserAction())
                .actionPrompt(providerResult.getActionPrompt())
                .expiresAt(providerResult.getExpiresAt())
                .authenticatedAt(providerResult.getAuthenticatedAt())
                .lastActivityAt(Instant.now())
                .failureCode(providerResult.getFailureCode())
                .safeFailureMessage(providerResult.getSafeFailureMessage())
                .correlationId(correlationId)
                .build();

        GovAuthSessionEntity saved = authSessionRepository.save(sessionEntity);

        if (saved.getStatus() == GovAuthStatus.AUTHENTICATED) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "GOV_AUTHENTICATED",
                    "GOV_AUTH_SESSION",
                    saved.getId().toString(),
                    null,
                    Map.of(
                            "connectionId", connection.getId().toString(),
                            "authMethod", request.getAuthMethod().name(),
                            "sessionId", saved.getId().toString()
                    )
            );
        } else if (saved.getStatus() == GovAuthStatus.AUTHENTICATION_FAILED) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "GOV_AUTH_FAILED",
                    "GOV_AUTH_SESSION",
                    saved.getId().toString(),
                    null,
                    Map.of(
                            "connectionId", connection.getId().toString(),
                            "failureCode", saved.getFailureCode() != null ? saved.getFailureCode() : "UNKNOWN",
                            "errorMessage", saved.getSafeFailureMessage() != null ? saved.getSafeFailureMessage() : ""
                    )
            );
        }

        return mapToDto(saved);
    }

    @Override
    public GovAuthSessionDto getAuthenticationStatus(UUID sessionId) {
        return getAuthenticationStatus(sessionId, Collections.emptyMap());
    }

    @Override
    @Transactional
    public GovAuthSessionDto getAuthenticationStatus(UUID sessionId, Map<String, Object> options) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is mandatory");
        }

        UUID tenantId = requireActiveTenantId();
        GovAuthSessionEntity session = authSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Government authentication session not found for id: " + sessionId));

        if (!session.getOrganizationId().equals(tenantId)) {
            throw new AppException(ErrorCode.TENANT_MISMATCH,
                    "Authentication session does not belong to current organization");
        }

        // Check if session has expired locally
        if (session.isExpired() && session.getStatus() != GovAuthStatus.EXPIRED && session.getStatus() != GovAuthStatus.REVOKED) {
            session.transitionTo(GovAuthStatus.EXPIRED);
            session.setFailureCode("SESSION_EXPIRED");
            session.setSafeFailureMessage("Government authentication session expired");
            authSessionRepository.save(session);

            auditService.logEvent(
                    tenantId,
                    null,
                    "GOV_AUTH_EXPIRED",
                    "GOV_AUTH_SESSION",
                    session.getId().toString(),
                    null,
                    Map.of("connectionId", session.getConnectionId().toString(), "sessionId", session.getId().toString())
            );

            return mapToDto(session);
        }

        GovConnectionDto connection = connectionService.getConnection(session.getConnectionId());
        GovAuthSessionDto providerResult = authProvider.checkAuthenticationStatus(connection, session, options != null ? options : Collections.emptyMap());

        if (providerResult.getStatus() != session.getStatus()) {
            if (providerResult.getStatus() == GovAuthStatus.AUTHENTICATED) {
                session.transitionTo(GovAuthStatus.AUTHENTICATED);
                session.setAuthenticatedAt(providerResult.getAuthenticatedAt() != null ? providerResult.getAuthenticatedAt() : Instant.now());
                session.setExpiresAt(providerResult.getExpiresAt());
                session.setRequiresUserAction(false);
                session.setActionPrompt(null);
                authSessionRepository.save(session);

                auditService.logEvent(
                        tenantId,
                        null,
                        "GOV_AUTHENTICATED",
                        "GOV_AUTH_SESSION",
                        session.getId().toString(),
                        null,
                        Map.of("connectionId", connection.getId().toString(), "sessionId", session.getId().toString())
                );
            } else if (providerResult.getStatus() == GovAuthStatus.AUTHENTICATION_FAILED) {
                session.transitionTo(GovAuthStatus.AUTHENTICATION_FAILED);
                session.setFailureCode(providerResult.getFailureCode());
                session.setSafeFailureMessage(providerResult.getSafeFailureMessage());
                authSessionRepository.save(session);

                auditService.logEvent(
                        tenantId,
                        null,
                        "GOV_AUTH_FAILED",
                        "GOV_AUTH_SESSION",
                        session.getId().toString(),
                        null,
                        Map.of("connectionId", connection.getId().toString(), "failureCode", providerResult.getFailureCode())
                );
            } else if (providerResult.getStatus() == GovAuthStatus.EXPIRED) {
                session.transitionTo(GovAuthStatus.EXPIRED);
                session.setFailureCode("SESSION_EXPIRED");
                session.setSafeFailureMessage("Authentication session expired on government gateway");
                authSessionRepository.save(session);

                auditService.logEvent(
                        tenantId,
                        null,
                        "GOV_AUTH_EXPIRED",
                        "GOV_AUTH_SESSION",
                        session.getId().toString(),
                        null,
                        Map.of("connectionId", connection.getId().toString(), "sessionId", session.getId().toString())
                );
            }
        }

        return mapToDto(session);
    }

    @Override
    @Transactional
    public GovAuthSessionDto continueAuthorization(UUID sessionId, GovAuthContinueRequest request) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is mandatory");
        }

        UUID tenantId = requireActiveTenantId();
        GovAuthSessionEntity session = authSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Government authentication session not found for id: " + sessionId));

        if (!session.getOrganizationId().equals(tenantId)) {
            throw new AppException(ErrorCode.TENANT_MISMATCH,
                    "Authentication session does not belong to current organization");
        }

        if (session.getStatus() == GovAuthStatus.REVOKED) {
            throw new GovIntegrationException(GovErrorCode.VALIDATION_FAILED,
                    "Cannot continue a revoked authentication session");
        }

        if (session.isExpired() || session.getStatus() == GovAuthStatus.EXPIRED) {
            if (session.getStatus() != GovAuthStatus.EXPIRED) {
                session.transitionTo(GovAuthStatus.EXPIRED);
                session.setFailureCode("SESSION_EXPIRED");
                session.setSafeFailureMessage("Government authentication session expired");
                authSessionRepository.save(session);
            }
            throw new GovIntegrationException(GovErrorCode.VALIDATION_FAILED,
                    "Cannot continue an expired authentication session");
        }

        GovConnectionDto connection = connectionService.getConnection(session.getConnectionId());

        auditService.logEvent(
                tenantId,
                null,
                "GOV_AUTH_CONTINUED",
                "GOV_AUTH_SESSION",
                session.getId().toString(),
                null,
                Map.of(
                        "connectionId", connection.getId().toString(),
                        "sessionId", session.getId().toString(),
                        "authMethod", session.getAuthMethod().name()
                )
        );

        String actionRef = request != null ? request.getActionReference() : null;
        Map<String, Object> options = request != null && request.getOptions() != null
                ? request.getOptions()
                : Collections.emptyMap();

        GovAuthSessionDto providerResult = authProvider.continueAuthorization(connection, session, actionRef, options);

        if (providerResult.getStatus() == GovAuthStatus.AUTHENTICATED) {
            session.transitionTo(GovAuthStatus.AUTHENTICATED);
            session.setAuthenticatedAt(providerResult.getAuthenticatedAt() != null ? providerResult.getAuthenticatedAt() : Instant.now());
            session.setExpiresAt(providerResult.getExpiresAt());
            session.setRequiresUserAction(false);
            session.setActionPrompt(null);
            session.setFailureCode(null);
            session.setSafeFailureMessage(null);

            auditService.logEvent(
                    tenantId,
                    null,
                    "GOV_AUTHENTICATED",
                    "GOV_AUTH_SESSION",
                    session.getId().toString(),
                    null,
                    Map.of("connectionId", connection.getId().toString(), "sessionId", session.getId().toString())
            );
        } else if (providerResult.getStatus() == GovAuthStatus.AUTHENTICATION_FAILED) {
            session.transitionTo(GovAuthStatus.AUTHENTICATION_FAILED);
            session.setFailureCode(providerResult.getFailureCode());
            session.setSafeFailureMessage(providerResult.getSafeFailureMessage());

            auditService.logEvent(
                    tenantId,
                    null,
                    "GOV_AUTH_FAILED",
                    "GOV_AUTH_SESSION",
                    session.getId().toString(),
                    null,
                    Map.of(
                            "connectionId", connection.getId().toString(),
                            "sessionId", session.getId().toString(),
                            "failureCode", providerResult.getFailureCode() != null ? providerResult.getFailureCode() : "UNKNOWN"
                    )
            );
        } else if (providerResult.getStatus() == GovAuthStatus.EXPIRED) {
            session.transitionTo(GovAuthStatus.EXPIRED);
            session.setFailureCode("SESSION_EXPIRED");
            session.setSafeFailureMessage("Authentication session expired on government gateway");

            auditService.logEvent(
                    tenantId,
                    null,
                    "GOV_AUTH_EXPIRED",
                    "GOV_AUTH_SESSION",
                    session.getId().toString(),
                    null,
                    Map.of("connectionId", connection.getId().toString(), "sessionId", session.getId().toString())
            );
        } else if (providerResult.getStatus() == GovAuthStatus.AUTHENTICATION_PENDING) {
            session.setRequiresUserAction(providerResult.isRequiresUserAction());
            session.setActionPrompt(providerResult.getActionPrompt());
        }

        GovAuthSessionEntity saved = authSessionRepository.save(session);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public GovAuthSessionDto revokeAuthentication(UUID sessionId) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is mandatory");
        }

        UUID tenantId = requireActiveTenantId();
        GovAuthSessionEntity session = authSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Government authentication session not found for id: " + sessionId));

        if (!session.getOrganizationId().equals(tenantId)) {
            throw new AppException(ErrorCode.TENANT_MISMATCH,
                    "Authentication session does not belong to current organization");
        }

        GovConnectionDto connection = connectionService.getConnection(session.getConnectionId());
        authProvider.revokeAuthentication(connection, session);

        session.revoke();
        GovAuthSessionEntity saved = authSessionRepository.save(session);

        auditService.logEvent(
                tenantId,
                null,
                "GOV_AUTH_REVOKED",
                "GOV_AUTH_SESSION",
                session.getId().toString(),
                null,
                Map.of("connectionId", session.getConnectionId().toString(), "sessionId", session.getId().toString())
        );

        return mapToDto(saved);
    }

    @Override
    public GovAuthSessionDto getActiveSessionForConnection(UUID connectionId) {
        if (connectionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Connection ID is mandatory");
        }

        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = connectionService.getConnection(connectionId);

        Optional<GovAuthSessionEntity> sessionOpt = authSessionRepository
                .findFirstByOrganizationIdAndConnectionIdAndStatusInOrderByCreatedAtDesc(
                        tenantId,
                        connection.getId(),
                        List.of(GovAuthStatus.AUTHENTICATED, GovAuthStatus.AUTHENTICATION_PENDING)
                );

        if (sessionOpt.isEmpty()) {
            return null;
        }

        GovAuthSessionEntity session = sessionOpt.get();
        if (session.isExpired()) {
            session.transitionTo(GovAuthStatus.EXPIRED);
            session.setFailureCode("SESSION_EXPIRED");
            authSessionRepository.save(session);
            return null;
        }

        return mapToDto(session);
    }

    @Override
    public GovAuthorizationState getAuthorizationState(UUID sessionId) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is mandatory");
        }

        UUID tenantId = requireActiveTenantId();
        GovAuthSessionEntity session = authSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Government authentication session not found for id: " + sessionId));

        if (!session.getOrganizationId().equals(tenantId)) {
            throw new AppException(ErrorCode.TENANT_MISMATCH,
                    "Authentication session does not belong to current organization");
        }

        return session.getAuthorizationState();
    }

    private GovAuthSessionDto mapToDto(GovAuthSessionEntity entity) {
        String safeRef = null;
        String providerRef = null;
        if (entity.getCorrelationId() != null) {
            String corr = entity.getCorrelationId();
            safeRef = switch (entity.getAuthMethod()) {
                case OAUTH2 -> "https://mock-gov-portal.taxoryn.internal/oauth/authorize?flow_ref=" + corr;
                case OTP -> "mock-otp-challenge-" + corr;
                case EVC -> "mock-evc-challenge-" + corr;
                case DSC -> "mock-dsc-challenge-" + corr;
            };
            String shortId = corr.length() > 8 ? corr.substring(0, 8) : corr;
            providerRef = "mock-gov-sess-" + shortId;
        }

        return GovAuthSessionDto.builder()
                .sessionId(entity.getId())
                .connectionId(entity.getConnectionId())
                .providerType(entity.getProviderType())
                .authMethod(entity.getAuthMethod())
                .status(entity.getStatus())
                .authorizationState(entity.getAuthorizationState())
                .requiresUserAction(entity.isRequiresUserAction())
                .actionPrompt(entity.getActionPrompt())
                .safeAuthorizationReference(safeRef)
                .providerSessionReference(providerRef)
                .expiresAt(entity.getExpiresAt())
                .authenticatedAt(entity.getAuthenticatedAt())
                .lastActivityAt(entity.getLastActivityAt())
                .failureCode(entity.getFailureCode())
                .safeFailureMessage(entity.getSafeFailureMessage())
                .correlationId(entity.getCorrelationId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant context is required for government authentication operations");
        }
        return tenantId;
    }
}
