package com.taxoryn.module.gov.auth.infrastructure;

import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.entity.GovAuthSessionEntity;
import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.auth.model.GovAuthorizationState;
import com.taxoryn.module.gov.auth.spi.GovernmentAuthenticationProvider;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.model.GovProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic mock government authentication provider supporting GST, INCOME_TAX, and TDS.
 * Simulates OAuth 2.0, OTP, EVC, and DSC authentication flows without external network calls.
 */
@Slf4j
@Component
public class MockGovernmentAuthenticationProvider implements GovernmentAuthenticationProvider {

    private static final Set<GovAuthMethod> SUPPORTED_METHODS = EnumSet.of(
            GovAuthMethod.OAUTH2,
            GovAuthMethod.OTP,
            GovAuthMethod.EVC,
            GovAuthMethod.DSC
    );

    @Override
    public GovProviderType getProviderType() {
        return GovProviderType.GST; // Acts as generic / default fallback handler
    }

    @Override
    public Set<GovAuthMethod> getSupportedMethods() {
        return Collections.unmodifiableSet(SUPPORTED_METHODS);
    }

    @Override
    public GovAuthSessionDto startAuthentication(
            GovConnectionDto connection,
            GovAuthMethod method,
            String correlationId,
            Map<String, Object> options) {

        log.info("[MOCK_AUTH_PROVIDER] Starting auth for connection id={}, provider={}, method={}, correlationId={}",
                connection.getId(), connection.getProviderType(), method, correlationId);

        String directive = resolveDirective(options);
        Instant now = Instant.now();
        String safeRef = buildSafeAuthReference(method, correlationId);
        String providerRef = buildProviderSessionRef(correlationId, options);

        return switch (directive.toUpperCase()) {
            case "PENDING", "AUTHENTICATION_PENDING", "OTP_SENT", "REQUIRES_ACTION", "AUTHORIZATION_REQUIRED" -> GovAuthSessionDto.builder()
                    .connectionId(connection.getId())
                    .providerType(connection.getProviderType())
                    .authMethod(method)
                    .status(GovAuthStatus.AUTHENTICATION_PENDING)
                    .authorizationState(GovAuthorizationState.USER_ACTION_REQUIRED)
                    .requiresUserAction(true)
                    .actionPrompt(getActionPrompt(method))
                    .safeAuthorizationReference(safeRef)
                    .providerSessionReference(providerRef)
                    .expiresAt(now.plus(Duration.ofMinutes(15)))
                    .lastActivityAt(now)
                    .correlationId(correlationId)
                    .build();

            case "FAILED", "AUTHENTICATION_FAILED", "INVALID_CREDENTIALS" -> GovAuthSessionDto.builder()
                    .connectionId(connection.getId())
                    .providerType(connection.getProviderType())
                    .authMethod(method)
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("INVALID_CREDENTIALS")
                    .safeFailureMessage("Government portal authentication failed: Invalid credentials or rejected consent")
                    .lastActivityAt(now)
                    .correlationId(correlationId)
                    .build();

            case "EXPIRED", "SESSION_EXPIRED" -> GovAuthSessionDto.builder()
                    .connectionId(connection.getId())
                    .providerType(connection.getProviderType())
                    .authMethod(method)
                    .status(GovAuthStatus.EXPIRED)
                    .authorizationState(GovAuthorizationState.EXPIRED)
                    .failureCode("SESSION_EXPIRED")
                    .safeFailureMessage("Authentication session expired on government gateway")
                    .expiresAt(now.minusSeconds(60))
                    .lastActivityAt(now)
                    .correlationId(correlationId)
                    .build();

            case "PROVIDER_UNAVAILABLE", "UNAVAILABLE" -> GovAuthSessionDto.builder()
                    .connectionId(connection.getId())
                    .providerType(connection.getProviderType())
                    .authMethod(method)
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("PROVIDER_UNAVAILABLE")
                    .safeFailureMessage("Government authentication gateway is undergoing scheduled maintenance (HTTP 503)")
                    .lastActivityAt(now)
                    .correlationId(correlationId)
                    .build();

            case "TIMEOUT" -> GovAuthSessionDto.builder()
                    .connectionId(connection.getId())
                    .providerType(connection.getProviderType())
                    .authMethod(method)
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("TIMEOUT")
                    .safeFailureMessage("Government authentication gateway timed out")
                    .lastActivityAt(now)
                    .correlationId(correlationId)
                    .build();

            default -> GovAuthSessionDto.builder()
                    .connectionId(connection.getId())
                    .providerType(connection.getProviderType())
                    .authMethod(method)
                    .status(GovAuthStatus.AUTHENTICATED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_COMPLETED)
                    .requiresUserAction(false)
                    .providerSessionReference(providerRef)
                    .authenticatedAt(now)
                    .expiresAt(now.plus(Duration.ofHours(8)))
                    .lastActivityAt(now)
                    .correlationId(correlationId)
                    .build();
        };
    }

    @Override
    public GovAuthSessionDto checkAuthenticationStatus(
            GovConnectionDto connection,
            GovAuthSessionEntity session,
            Map<String, Object> options) {

        log.info("[MOCK_AUTH_PROVIDER] Checking auth status for session id={}, connection id={}",
                session.getId(), connection.getId());

        String directive = resolveDirective(options);
        Instant now = Instant.now();
        String safeRef = buildSafeAuthReference(session.getAuthMethod(), session.getCorrelationId());
        String providerRef = buildProviderSessionRef(session.getCorrelationId(), options);

        if ("EXPIRED".equalsIgnoreCase(directive) || session.isExpired()) {
            return GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.EXPIRED)
                    .authorizationState(GovAuthorizationState.EXPIRED)
                    .failureCode("SESSION_EXPIRED")
                    .safeFailureMessage("Government authentication session expired")
                    .expiresAt(session.getExpiresAt() != null ? session.getExpiresAt() : now)
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();
        }

        if ("PENDING".equalsIgnoreCase(directive) || "USER_ACTION_REQUIRED".equalsIgnoreCase(directive)) {
            return GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.AUTHENTICATION_PENDING)
                    .authorizationState(GovAuthorizationState.USER_ACTION_REQUIRED)
                    .requiresUserAction(true)
                    .actionPrompt(getActionPrompt(session.getAuthMethod()))
                    .safeAuthorizationReference(safeRef)
                    .providerSessionReference(providerRef)
                    .expiresAt(session.getExpiresAt() != null ? session.getExpiresAt() : now.plus(Duration.ofMinutes(15)))
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();
        }

        if ("FAILED".equalsIgnoreCase(directive) || "AUTHENTICATION_FAILED".equalsIgnoreCase(directive)) {
            return GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("AUTHENTICATION_FAILED")
                    .safeFailureMessage("Government portal authentication failed")
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();
        }

        // Default: Authenticated
        return GovAuthSessionDto.builder()
                .sessionId(session.getId())
                .connectionId(connection.getId())
                .providerType(session.getProviderType())
                .authMethod(session.getAuthMethod())
                .status(GovAuthStatus.AUTHENTICATED)
                .authorizationState(GovAuthorizationState.AUTHORIZATION_COMPLETED)
                .requiresUserAction(false)
                .providerSessionReference(providerRef)
                .authenticatedAt(session.getAuthenticatedAt() != null ? session.getAuthenticatedAt() : now)
                .expiresAt(session.getExpiresAt() != null ? session.getExpiresAt() : now.plus(Duration.ofHours(8)))
                .lastActivityAt(now)
                .correlationId(session.getCorrelationId())
                .build();
    }

    @Override
    public GovAuthSessionDto continueAuthorization(
            GovConnectionDto connection,
            GovAuthSessionEntity session,
            String actionReference,
            Map<String, Object> options) {

        log.info("[MOCK_AUTH_PROVIDER] Continuing authorization for session id={}, connection id={}, actionRef={}",
                session.getId(), connection.getId(), actionReference);

        String directive = resolveDirective(options);
        if ("SUCCESS".equalsIgnoreCase(directive) && actionReference != null) {
            if ("FAIL".equalsIgnoreCase(actionReference) || "REJECT".equalsIgnoreCase(actionReference)) {
                directive = "FAILED";
            } else if ("EXPIRE".equalsIgnoreCase(actionReference)) {
                directive = "EXPIRED";
            }
        }

        Instant now = Instant.now();
        String providerRef = buildProviderSessionRef(session.getCorrelationId(), options);

        return switch (directive.toUpperCase()) {
            case "FAILED", "INVALID_CREDENTIALS", "REJECTED" -> GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("INVALID_CREDENTIALS")
                    .safeFailureMessage("User rejected authorization or invalid credentials")
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();

            case "EXPIRED" -> GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.EXPIRED)
                    .authorizationState(GovAuthorizationState.EXPIRED)
                    .failureCode("SESSION_EXPIRED")
                    .safeFailureMessage("Authorization session expired on government gateway")
                    .expiresAt(now.minusSeconds(60))
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();

            case "PROVIDER_UNAVAILABLE" -> GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("PROVIDER_UNAVAILABLE")
                    .safeFailureMessage("Government authentication gateway is undergoing scheduled maintenance (HTTP 503)")
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();

            case "TIMEOUT" -> GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("TIMEOUT")
                    .safeFailureMessage("Government authentication gateway timed out")
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();

            case "PENDING", "USER_ACTION_REQUIRED" -> GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.AUTHENTICATION_PENDING)
                    .authorizationState(GovAuthorizationState.USER_ACTION_REQUIRED)
                    .requiresUserAction(true)
                    .actionPrompt(getActionPrompt(session.getAuthMethod()))
                    .safeAuthorizationReference(buildSafeAuthReference(session.getAuthMethod(), session.getCorrelationId()))
                    .providerSessionReference(providerRef)
                    .expiresAt(session.getExpiresAt() != null ? session.getExpiresAt() : now.plus(Duration.ofMinutes(15)))
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();

            case "REFRESH_SUCCESS" -> GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.AUTHENTICATED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_COMPLETED)
                    .requiresUserAction(false)
                    .providerSessionReference(providerRef + "_ROTATED")
                    .authenticatedAt(now)
                    .expiresAt(now.plus(Duration.ofHours(8)))
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();

            default -> GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.AUTHENTICATED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_COMPLETED)
                    .requiresUserAction(false)
                    .providerSessionReference(providerRef)
                    .authenticatedAt(now)
                    .expiresAt(now.plus(Duration.ofHours(8)))
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();
        };
    }

    @Override
    public GovAuthSessionDto revokeAuthentication(
            GovConnectionDto connection,
            GovAuthSessionEntity session) {

        log.info("[MOCK_AUTH_PROVIDER] Revoking auth session id={}, connection id={}",
                session.getId(), connection.getId());

        Instant now = Instant.now();
        return GovAuthSessionDto.builder()
                .sessionId(session.getId())
                .connectionId(connection.getId())
                .providerType(session.getProviderType())
                .authMethod(session.getAuthMethod())
                .status(GovAuthStatus.REVOKED)
                .authorizationState(GovAuthorizationState.CANCELLED)
                .requiresUserAction(false)
                .lastActivityAt(now)
                .correlationId(session.getCorrelationId())
                .build();
    }

    private String resolveDirective(Map<String, Object> options) {
        if (options != null && options.containsKey("mockOutcome")) {
            return String.valueOf(options.get("mockOutcome"));
        }
        return "SUCCESS";
    }

    private String getActionPrompt(GovAuthMethod method) {
        return switch (method) {
            case OAUTH2 -> "Please authenticate via the government OAuth redirect portal";
            case OTP -> "Enter the 6-digit OTP sent to the registered mobile/email on the government portal";
            case EVC -> "Enter the Electronic Verification Code (EVC) generated for this session";
            case DSC -> "Attach digital signature token and authorize signing request";
        };
    }

    private String buildSafeAuthReference(GovAuthMethod method, String correlationId) {
        String corr = correlationId != null ? correlationId : "default";
        return switch (method) {
            case OAUTH2 -> "https://mock-gov-portal.taxoryn.internal/oauth/authorize?flow_ref=" + corr;
            case OTP -> "mock-otp-challenge-" + corr;
            case EVC -> "mock-evc-challenge-" + corr;
            case DSC -> "mock-dsc-challenge-" + corr;
        };
    }

    private String buildProviderSessionRef(String correlationId) {
        return buildProviderSessionRef(correlationId, Collections.emptyMap());
    }

    private String buildProviderSessionRef(String correlationId, Map<String, Object> options) {
        String purpose = options != null && options.containsKey("purpose")
                ? String.valueOf(options.get("purpose")).toUpperCase()
                : "GST";

        String shortId = (correlationId != null && correlationId.length() > 8)
                ? correlationId.substring(0, 8)
                : (correlationId != null ? correlationId : "default");

        return switch (purpose) {
            case "EWAY_BILL" -> "MOCK_EWAY_SESSION_" + shortId;
            case "E_INVOICE" -> "MOCK_EINV_SESSION_" + shortId;
            default -> "MOCK_GST_SESSION_" + shortId;
        };
    }
}
