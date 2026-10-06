package com.taxoryn.module.gov.auth.infrastructure;

import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.entity.GovAuthSessionEntity;
import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.auth.model.GovAuthorizationState;
import com.taxoryn.module.gov.auth.spi.GovernmentAuthenticationProvider;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.model.GovErrorCode;
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

    @org.springframework.beans.factory.annotation.Value("${taxoryn.gov.integration.mock-enabled:true}")
    private boolean mockEnabled = true;

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

        if (!mockEnabled) {
            log.warn("[MOCK_AUTH_SAFETY] Mock authentication attempted while mock providers are disabled");
            throw new com.taxoryn.module.gov.exception.GovIntegrationException(
                    GovErrorCode.PROVIDER_UNAVAILABLE,
                    "Mock government authentication is disabled in production"
            );
        }

        log.info("[MOCK_AUTH_PROVIDER] Starting auth for connection id={}, provider={}, method={}, correlationId={}",
                connection.getId(), connection.getProviderType(), method, correlationId);

        String directive = resolveDirective(options);
        Instant now = Instant.now();
        String safeRef = buildSafeAuthReference(method, correlationId);
        String providerRef = buildProviderSessionRef(correlationId, options);

        return switch (directive.toUpperCase()) {
            case "PENDING", "AUTHENTICATION_PENDING", "OTP_SENT", "REQUIRES_ACTION", "AUTHORIZATION_REQUIRED",
                 "CHALLENGE_CREATED", "EVC_PENDING", "SIGNING_PENDING", "SIGN_REQUESTED", "USER_ACTION_REQUIRED" -> GovAuthSessionDto.builder()
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

            case "SIGNATURE_CREATED" -> GovAuthSessionDto.builder()
                    .connectionId(connection.getId())
                    .providerType(connection.getProviderType())
                    .authMethod(method)
                    .status(GovAuthStatus.AUTHENTICATION_PENDING)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_IN_PROGRESS)
                    .requiresUserAction(false)
                    .safeAuthorizationReference("MOCK_DSC_SIGNATURE_" + (correlationId != null && correlationId.length() > 8 ? correlationId.substring(0, 8) : correlationId))
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

            case "INVALID_EVC", "INVALID" -> GovAuthSessionDto.builder()
                    .connectionId(connection.getId())
                    .providerType(connection.getProviderType())
                    .authMethod(method)
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("INVALID_EVC")
                    .safeFailureMessage("The entered Electronic Verification Code (EVC) is invalid or incorrect")
                    .lastActivityAt(now)
                    .correlationId(correlationId)
                    .build();

            case "INVALID_SIGNATURE", "SIGNATURE_INVALID" -> GovAuthSessionDto.builder()
                    .connectionId(connection.getId())
                    .providerType(connection.getProviderType())
                    .authMethod(method)
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("INVALID_SIGNATURE")
                    .safeFailureMessage("Digital signature verification failed or signature does not match payload digest")
                    .lastActivityAt(now)
                    .correlationId(correlationId)
                    .build();

            case "CERTIFICATE_EXPIRED", "CERT_EXPIRED" -> GovAuthSessionDto.builder()
                    .connectionId(connection.getId())
                    .providerType(connection.getProviderType())
                    .authMethod(method)
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("CERTIFICATE_EXPIRED")
                    .safeFailureMessage("Digital Signature Certificate (DSC) has expired")
                    .lastActivityAt(now)
                    .correlationId(correlationId)
                    .build();

            case "MAX_ATTEMPTS", "ATTEMPTS_EXCEEDED", "MAX_ATTEMPTS_EXCEEDED" -> GovAuthSessionDto.builder()
                    .connectionId(connection.getId())
                    .providerType(connection.getProviderType())
                    .authMethod(method)
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("MAX_ATTEMPTS_EXCEEDED")
                    .safeFailureMessage("Maximum verification attempts exceeded for this challenge")
                    .lastActivityAt(now)
                    .correlationId(correlationId)
                    .build();

            case "EXPIRED", "SESSION_EXPIRED", "EVC_EXPIRED" -> GovAuthSessionDto.builder()
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

        if (!mockEnabled) {
            log.warn("[MOCK_AUTH_SAFETY] Mock auth status check attempted while mock providers are disabled");
            throw new com.taxoryn.module.gov.exception.GovIntegrationException(
                    GovErrorCode.PROVIDER_UNAVAILABLE,
                    "Mock government authentication is disabled in production"
            );
        }

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

        if ("PENDING".equalsIgnoreCase(directive) || "USER_ACTION_REQUIRED".equalsIgnoreCase(directive)
                || "CHALLENGE_CREATED".equalsIgnoreCase(directive) || "SIGNING_PENDING".equalsIgnoreCase(directive)) {
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

        // Default: If session was pending and no outcome directive given, keep pending
        if (session.getStatus() == GovAuthStatus.AUTHENTICATION_PENDING && (options == null || !options.containsKey("mockOutcome"))) {
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

        if (!mockEnabled) {
            log.warn("[MOCK_AUTH_SAFETY] Mock auth continuation attempted while mock providers are disabled");
            throw new com.taxoryn.module.gov.exception.GovIntegrationException(
                    GovErrorCode.PROVIDER_UNAVAILABLE,
                    "Mock government authentication is disabled in production"
            );
        }

        log.info("[MOCK_AUTH_PROVIDER] Continuing authorization for session id={}, connection id={}, actionRef={}",
                session.getId(), connection.getId(), actionReference);

        String directive = resolveDirective(options);
        if ("SUCCESS".equalsIgnoreCase(directive) && actionReference != null) {
            String refUpper = actionReference.toUpperCase();
            if (refUpper.contains("FAIL") || refUpper.contains("REJECT") || refUpper.equals("INVALID") || refUpper.equals("INVALID_EVC")) {
                directive = "INVALID_EVC";
            } else if (refUpper.contains("INVALID_SIGNATURE") || refUpper.equals("SIGNATURE_INVALID")) {
                directive = "INVALID_SIGNATURE";
            } else if (refUpper.contains("CERT_EXPIRED") || refUpper.contains("CERTIFICATE_EXPIRED")) {
                directive = "CERTIFICATE_EXPIRED";
            } else if (refUpper.contains("MAX_ATTEMPT") || refUpper.contains("ATTEMPTS_EXCEEDED")) {
                directive = "MAX_ATTEMPTS";
            } else if (refUpper.contains("EXPIRE")) {
                directive = "EXPIRED";
            } else if (refUpper.contains("UNAVAILABLE")) {
                directive = "PROVIDER_UNAVAILABLE";
            } else if (refUpper.contains("TIMEOUT")) {
                directive = "TIMEOUT";
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

            case "INVALID_EVC", "INVALID" -> GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("INVALID_EVC")
                    .safeFailureMessage("The entered Electronic Verification Code (EVC) is invalid or incorrect")
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();

            case "INVALID_SIGNATURE", "SIGNATURE_INVALID" -> GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("INVALID_SIGNATURE")
                    .safeFailureMessage("Digital signature verification failed or signature does not match payload digest")
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();

            case "CERTIFICATE_EXPIRED", "CERT_EXPIRED" -> GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("CERTIFICATE_EXPIRED")
                    .safeFailureMessage("Digital Signature Certificate (DSC) has expired")
                    .lastActivityAt(now)
                    .correlationId(session.getCorrelationId())
                    .build();

            case "MAX_ATTEMPTS", "ATTEMPTS_EXCEEDED", "MAX_ATTEMPTS_EXCEEDED" -> GovAuthSessionDto.builder()
                    .sessionId(session.getId())
                    .connectionId(connection.getId())
                    .providerType(session.getProviderType())
                    .authMethod(session.getAuthMethod())
                    .status(GovAuthStatus.AUTHENTICATION_FAILED)
                    .authorizationState(GovAuthorizationState.AUTHORIZATION_FAILED)
                    .failureCode("MAX_ATTEMPTS_EXCEEDED")
                    .safeFailureMessage("Maximum verification attempts exceeded for this challenge")
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

        if (!mockEnabled) {
            log.warn("[MOCK_AUTH_SAFETY] Mock auth revocation attempted while mock providers are disabled");
            throw new com.taxoryn.module.gov.exception.GovIntegrationException(
                    GovErrorCode.PROVIDER_UNAVAILABLE,
                    "Mock government authentication is disabled in production"
            );
        }

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
            case EVC -> "Enter the Electronic Verification Code (EVC) sent to the registered mobile/email on the government portal";
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
        String shortId = (correlationId != null && correlationId.length() > 8)
                ? correlationId.substring(0, 8)
                : (correlationId != null ? correlationId : "default");

        if (options != null && options.containsKey("purpose")) {
            String purpose = String.valueOf(options.get("purpose")).toUpperCase();
            return switch (purpose) {
                case "EWAY_BILL" -> "MOCK_EWAY_SESSION_" + shortId;
                case "E_INVOICE" -> "MOCK_EINV_SESSION_" + shortId;
                case "ITR_EVC" -> "MOCK_ITR_EVC_SESSION_" + shortId;
                case "ITR_DSC" -> "MOCK_ITR_DSC_SESSION_" + shortId;
                case "TDS_EVC" -> "MOCK_TDS_EVC_SESSION_" + shortId;
                case "TDS_DSC" -> "MOCK_TDS_DSC_SESSION_" + shortId;
                default -> "MOCK_GST_SESSION_" + shortId;
            };
        }
        return "mock-gov-sess-" + shortId;
    }
}
