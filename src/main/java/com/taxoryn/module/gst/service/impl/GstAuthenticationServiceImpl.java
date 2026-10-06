package com.taxoryn.module.gst.service.impl;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.auth.dto.GovAuthContinueRequest;
import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.dto.GovAuthStartRequest;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.auth.service.GovernmentAuthenticationService;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.gst.dto.GstAuthContinueRequest;
import com.taxoryn.module.gst.dto.GstAuthSessionDto;
import com.taxoryn.module.gst.dto.GstAuthSessionRequest;
import com.taxoryn.module.gst.model.GstAuthenticationPurpose;
import com.taxoryn.module.gst.model.GstTokenStatus;
import com.taxoryn.module.gst.service.GstAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of GST domain authentication service.
 * Enforces tenant boundary, GST provider connection type, purpose isolation (GST, EWAY_BILL, E_INVOICE),
 * server-side expiration validation, token refresh, and audit tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GstAuthenticationServiceImpl implements GstAuthenticationService {

    private final GovernmentAuthenticationService govAuthService;
    private final GovernmentConnectionService connectionService;
    private final AuditService auditService;

    @Override
    @Transactional
    public GstAuthSessionDto authenticate(GstAuthSessionRequest request) {
        if (request == null || request.getConnectionId() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Connection ID is mandatory for GST authentication");
        }

        UUID tenantId = requireActiveTenantId();
        GstAuthenticationPurpose purpose = request.getPurpose() != null
                ? request.getPurpose()
                : GstAuthenticationPurpose.GST;

        // 1. Validate connection and provider type
        GovConnectionDto connection = connectionService.getConnection(request.getConnectionId());
        validateGstConnection(connection);

        // 2. Idempotent check: check for compatible active session for this exact purpose
        GstAuthSessionDto activeToken = getActiveToken(request.getConnectionId(), purpose);
        if (activeToken != null && activeToken.getTokenStatus().isActive()) {
            log.info("[GST_AUTH] Reusing existing valid token for connection id={}, purpose={}",
                    connection.getId(), purpose);
            return activeToken;
        }

        Map<String, Object> options = new HashMap<>();
        if (request.getOptions() != null) {
            options.putAll(request.getOptions());
        }
        options.put("purpose", purpose.name());

        auditService.logEvent(
                tenantId,
                null,
                "GST_AUTH_STARTED",
                "GST_AUTH_SESSION",
                connection.getId().toString(),
                null,
                Map.of(
                        "connectionId", connection.getId().toString(),
                        "purpose", purpose.name(),
                        "authMethod", request.getAuthMethod() != null ? request.getAuthMethod().name() : "DEFAULT"
                )
        );

        GovAuthSessionDto govSession = govAuthService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connection.getId())
                .authMethod(request.getAuthMethod())
                .correlationId(request.getCorrelationId())
                .options(options)
                .build());

        GstAuthSessionDto result = mapToDto(govSession, purpose);

        if (result.getTokenStatus().isActive()) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "GST_AUTHENTICATED",
                    "GST_AUTH_SESSION",
                    result.getSessionId().toString(),
                    null,
                    Map.of(
                            "connectionId", connection.getId().toString(),
                            "purpose", purpose.name(),
                            "sessionId", result.getSessionId().toString()
                    )
            );
        } else if (result.getTokenStatus() == GstTokenStatus.FAILED) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "GST_AUTH_FAILED",
                    "GST_AUTH_SESSION",
                    result.getSessionId().toString(),
                    null,
                    Map.of(
                            "connectionId", connection.getId().toString(),
                            "purpose", purpose.name(),
                            "failureCode", result.getFailureCode() != null ? result.getFailureCode() : "UNKNOWN"
                    )
            );
        }

        return result;
    }

    @Override
    @Transactional
    public GstAuthSessionDto getAuthenticationStatus(UUID sessionId, GstAuthenticationPurpose purpose, Map<String, Object> options) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is mandatory");
        }

        GstAuthenticationPurpose targetPurpose = purpose != null ? purpose : GstAuthenticationPurpose.GST;
        Map<String, Object> opts = new HashMap<>();
        if (options != null) {
            opts.putAll(options);
        }
        opts.put("purpose", targetPurpose.name());

        GovAuthSessionDto govSession = govAuthService.getAuthenticationStatus(sessionId, opts);
        return mapToDto(govSession, targetPurpose);
    }

    @Override
    @Transactional
    public GstAuthSessionDto continueAuthorization(UUID sessionId, GstAuthContinueRequest request, GstAuthenticationPurpose purpose) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is mandatory");
        }

        GstAuthenticationPurpose targetPurpose = purpose != null ? purpose : GstAuthenticationPurpose.GST;
        Map<String, Object> opts = new HashMap<>();
        if (request != null && request.getOptions() != null) {
            opts.putAll(request.getOptions());
        }
        opts.put("purpose", targetPurpose.name());

        GovAuthContinueRequest govRequest = GovAuthContinueRequest.builder()
                .actionReference(request != null ? request.getActionReference() : null)
                .options(opts)
                .build();

        GovAuthSessionDto govSession = govAuthService.continueAuthorization(sessionId, govRequest);
        GstAuthSessionDto result = mapToDto(govSession, targetPurpose);

        if (result.getTokenStatus().isActive()) {
            auditService.logEvent(
                    requireActiveTenantId(),
                    null,
                    "GST_AUTHENTICATED",
                    "GST_AUTH_SESSION",
                    result.getSessionId().toString(),
                    null,
                    Map.of(
                            "sessionId", result.getSessionId().toString(),
                            "purpose", targetPurpose.name()
                    )
            );
        }

        return result;
    }

    @Override
    @Transactional
    public GstAuthSessionDto refreshToken(UUID sessionId, GstAuthenticationPurpose purpose, Map<String, Object> options) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is mandatory");
        }

        UUID tenantId = requireActiveTenantId();
        GstAuthenticationPurpose targetPurpose = purpose != null ? purpose : GstAuthenticationPurpose.GST;

        Map<String, Object> opts = new HashMap<>();
        if (options != null) {
            opts.putAll(options);
        }
        opts.put("purpose", targetPurpose.name());
        opts.put("mockOutcome", "REFRESH_SUCCESS");

        GovAuthContinueRequest refreshRequest = GovAuthContinueRequest.builder()
                .actionReference("REFRESH")
                .options(opts)
                .build();

        GovAuthSessionDto refreshedSession = govAuthService.continueAuthorization(sessionId, refreshRequest);
        GstAuthSessionDto result = mapToDto(refreshedSession, targetPurpose);

        auditService.logEvent(
                tenantId,
                null,
                "GST_TOKEN_REFRESHED",
                "GST_AUTH_SESSION",
                result.getSessionId().toString(),
                null,
                Map.of(
                        "sessionId", result.getSessionId().toString(),
                        "purpose", targetPurpose.name(),
                        "connectionId", result.getConnectionId().toString()
                )
        );

        return result;
    }

    @Override
    @Transactional
    public GstAuthSessionDto revoke(UUID sessionId, GstAuthenticationPurpose purpose) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is mandatory");
        }

        UUID tenantId = requireActiveTenantId();
        GstAuthenticationPurpose targetPurpose = purpose != null ? purpose : GstAuthenticationPurpose.GST;

        GovAuthSessionDto revokedSession = govAuthService.revokeAuthentication(sessionId);
        GstAuthSessionDto result = mapToDto(revokedSession, targetPurpose);

        auditService.logEvent(
                tenantId,
                null,
                "GST_AUTH_REVOKED",
                "GST_AUTH_SESSION",
                result.getSessionId().toString(),
                null,
                Map.of(
                        "sessionId", result.getSessionId().toString(),
                        "purpose", targetPurpose.name()
                )
        );

        return result;
    }

    @Override
    public GstAuthSessionDto getActiveToken(UUID connectionId, GstAuthenticationPurpose purpose) {
        if (connectionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Connection ID is mandatory");
        }

        GovConnectionDto connection = connectionService.getConnection(connectionId);
        validateGstConnection(connection);

        GstAuthenticationPurpose targetPurpose = purpose != null ? purpose : GstAuthenticationPurpose.GST;
        GovAuthSessionDto activeSession = govAuthService.getActiveSessionForConnection(connectionId);

        if (activeSession == null) {
            return null;
        }

        // Verify purpose isolation: the active session must match the target purpose
        String tokenRef = activeSession.getProviderSessionReference();
        if (!isPurposeMatching(tokenRef, targetPurpose)) {
            log.debug("[GST_AUTH] Active connection session belongs to a different purpose, ignoring for purpose={}", targetPurpose);
            return null;
        }

        GstAuthSessionDto result = mapToDto(activeSession, targetPurpose);
        if (!result.getTokenStatus().isActive()) {
            return null;
        }

        return result;
    }

    private boolean isPurposeMatching(String tokenRef, GstAuthenticationPurpose purpose) {
        if (tokenRef == null) {
            return purpose == GstAuthenticationPurpose.GST;
        }
        return switch (purpose) {
            case EWAY_BILL -> tokenRef.startsWith("MOCK_EWAY_SESSION_");
            case E_INVOICE -> tokenRef.startsWith("MOCK_EINV_SESSION_");
            case GST -> tokenRef.startsWith("MOCK_GST_SESSION_") || (!tokenRef.startsWith("MOCK_EWAY_") && !tokenRef.startsWith("MOCK_EINV_"));
        };
    }

    private GstAuthSessionDto mapToDto(GovAuthSessionDto govSession, GstAuthenticationPurpose purpose) {
        if (govSession == null) {
            return null;
        }

        GstTokenStatus tokenStatus = deriveTokenStatus(govSession);

        return GstAuthSessionDto.builder()
                .sessionId(govSession.getSessionId())
                .connectionId(govSession.getConnectionId())
                .purpose(purpose)
                .authMethod(govSession.getAuthMethod())
                .status(govSession.getStatus())
                .tokenStatus(tokenStatus)
                .tokenReference(govSession.getProviderSessionReference())
                .requiresUserAction(govSession.isRequiresUserAction())
                .actionPrompt(govSession.getActionPrompt())
                .safeAuthorizationReference(govSession.getSafeAuthorizationReference())
                .issuedAt(govSession.getAuthenticatedAt() != null ? govSession.getAuthenticatedAt() : govSession.getCreatedAt())
                .expiresAt(govSession.getExpiresAt())
                .lastActivityAt(govSession.getLastActivityAt())
                .failureCode(govSession.getFailureCode())
                .safeFailureMessage(govSession.getSafeFailureMessage())
                .correlationId(govSession.getCorrelationId())
                .metadata(govSession.getMetadata())
                .build();
    }

    private GstTokenStatus deriveTokenStatus(GovAuthSessionDto session) {
        if (session.getStatus() == GovAuthStatus.REVOKED) {
            return GstTokenStatus.REVOKED;
        }
        if (session.getStatus() == GovAuthStatus.EXPIRED
                || (session.getExpiresAt() != null && Instant.now().isAfter(session.getExpiresAt()))) {
            return GstTokenStatus.EXPIRED;
        }
        if (session.getStatus() == GovAuthStatus.AUTHENTICATION_FAILED) {
            return GstTokenStatus.FAILED;
        }
        if (session.getStatus() == GovAuthStatus.AUTHENTICATION_PENDING || session.getStatus() == GovAuthStatus.AUTHENTICATION_STARTED) {
            return session.isRequiresUserAction() ? GstTokenStatus.AUTHENTICATION_REQUIRED : GstTokenStatus.AUTHENTICATION_PENDING;
        }
        if (session.getStatus() == GovAuthStatus.AUTHENTICATED) {
            if (session.getExpiresAt() != null && Duration.between(Instant.now(), session.getExpiresAt()).toMinutes() <= 30) {
                return GstTokenStatus.EXPIRING;
            }
            return GstTokenStatus.ACTIVE;
        }
        return GstTokenStatus.NOT_AUTHENTICATED;
    }

    private void validateGstConnection(GovConnectionDto connection) {
        if (connection == null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Government connection not found");
        }
        if (connection.getProviderType() != GovProviderType.GST) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Connection " + connection.getId() + " is of provider type " + connection.getProviderType() + ", but GST provider is required");
        }
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant context is required for GST authentication operations");
        }
        return tenantId;
    }
}
