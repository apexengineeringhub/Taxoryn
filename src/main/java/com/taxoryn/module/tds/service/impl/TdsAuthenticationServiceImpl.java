package com.taxoryn.module.tds.service.impl;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.gov.auth.dto.GovAuthContinueRequest;
import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.dto.GovAuthStartRequest;
import com.taxoryn.module.gov.auth.dto.GovDscSigningSessionDto;
import com.taxoryn.module.gov.auth.dto.GovDscVerifyResultDto;
import com.taxoryn.module.gov.auth.dto.GovEvcChallengeDto;
import com.taxoryn.module.gov.auth.dto.GovEvcVerifyResultDto;
import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.auth.model.GovAuthPurpose;
import com.taxoryn.module.gov.auth.model.GovAuthStatus;
import com.taxoryn.module.gov.auth.service.GovernmentAuthenticationService;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
import com.taxoryn.module.tds.dto.TdsDscSignRequest;
import com.taxoryn.module.tds.dto.TdsDscVerifyRequest;
import com.taxoryn.module.tds.dto.TdsEvcStartRequest;
import com.taxoryn.module.tds.dto.TdsEvcVerifyRequest;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.service.TdsAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TdsAuthenticationServiceImpl implements TdsAuthenticationService {

    private final GovernmentAuthenticationService govAuthService;
    private final GovernmentConnectionService connectionService;
    private final TdsProfileRepository profileRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public GovEvcChallengeDto startEvc(TdsEvcStartRequest request) {
        if (request == null || request.getConnectionId() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Connection ID is required for TDS EVC");
        }

        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = connectionService.getConnection(request.getConnectionId());
        validateTdsConnection(connection);
        validateDomainOwnership(request.getProfileId(), request.getTan());

        Map<String, Object> options = new HashMap<>();
        if (request.getOptions() != null) {
            options.putAll(request.getOptions());
        }
        options.put("purpose", GovAuthPurpose.TDS_EVC.name());
        if (request.getReturnId() != null) {
            options.put("returnId", request.getReturnId().toString());
        }

        // Check active pending session for idempotency
        GovAuthSessionDto active = govAuthService.getActiveSessionForConnection(connection.getId());
        if (active != null && active.getAuthMethod() == GovAuthMethod.EVC && active.getStatus().isPending() && !active.isExpired()) {
            log.info("[TDS_AUTH] Reusing existing pending EVC challenge for connection id={}", connection.getId());
            return mapToEvcChallengeDto(active);
        }

        auditService.logEvent(
                tenantId,
                null,
                "TDS_EVC_STARTED",
                "TDS_AUTH_SESSION",
                connection.getId().toString(),
                null,
                Map.of(
                        "connectionId", connection.getId().toString(),
                        "purpose", GovAuthPurpose.TDS_EVC.name(),
                        "authMethod", GovAuthMethod.EVC.name()
                )
        );

        GovAuthSessionDto govSession = govAuthService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connection.getId())
                .authMethod(GovAuthMethod.EVC)
                .correlationId(request.getCorrelationId())
                .options(options)
                .build());

        return mapToEvcChallengeDto(govSession);
    }

    @Override
    @Transactional
    public GovEvcVerifyResultDto verifyEvc(UUID sessionId, TdsEvcVerifyRequest request) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is required for TDS EVC verification");
        }
        if (request == null || request.getVerificationCode() == null || request.getVerificationCode().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Verification code is required for TDS EVC verification");
        }

        UUID tenantId = requireActiveTenantId();
        GovAuthSessionDto existing = govAuthService.getAuthenticationStatus(sessionId);
        validateTdsEvcSession(existing);

        if (existing.getStatus() == GovAuthStatus.AUTHENTICATED || existing.getStatus().isTerminal()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Cannot verify EVC on session in terminal state: " + existing.getStatus());
        }
        if (existing.isExpired()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Cannot verify expired EVC challenge");
        }

        Map<String, Object> options = new HashMap<>();
        if (request.getOptions() != null) {
            options.putAll(request.getOptions());
        }
        options.put("purpose", GovAuthPurpose.TDS_EVC.name());

        GovAuthSessionDto result = govAuthService.continueAuthorization(sessionId, GovAuthContinueRequest.builder()
                .actionReference(request.getActionReference() != null ? request.getActionReference() : request.getVerificationCode())
                .options(options)
                .build());

        boolean verified = result.getStatus() == GovAuthStatus.AUTHENTICATED;
        if (verified) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "TDS_EVC_VERIFIED",
                    "TDS_AUTH_SESSION",
                    sessionId.toString(),
                    null,
                    Map.of("sessionId", sessionId.toString(), "purpose", GovAuthPurpose.TDS_EVC.name())
            );
        } else {
            auditService.logEvent(
                    tenantId,
                    null,
                    "TDS_EVC_FAILED",
                    "TDS_AUTH_SESSION",
                    sessionId.toString(),
                    null,
                    Map.of(
                            "sessionId", sessionId.toString(),
                            "purpose", GovAuthPurpose.TDS_EVC.name(),
                            "failureCode", result.getFailureCode() != null ? result.getFailureCode() : "VERIFICATION_FAILED"
                    )
            );
        }

        return GovEvcVerifyResultDto.builder()
                .sessionId(result.getSessionId())
                .connectionId(result.getConnectionId())
                .purpose(GovAuthPurpose.TDS_EVC)
                .status(result.getStatus())
                .authorizationState(result.getAuthorizationState())
                .verified(verified)
                .attemptsRemaining(verified ? 0 : 2)
                .verificationReference(verified ? "MOCK_EVC_VERIFIED_" + result.getSessionId() : null)
                .verifiedAt(verified ? Instant.now() : null)
                .failureCode(result.getFailureCode())
                .safeFailureMessage(result.getSafeFailureMessage())
                .build();
    }

    @Override
    @Transactional
    public GovDscSigningSessionDto startDsc(TdsDscSignRequest request) {
        if (request == null || request.getConnectionId() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Connection ID is required for TDS DSC signing");
        }

        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = connectionService.getConnection(request.getConnectionId());
        validateTdsConnection(connection);
        validateDomainOwnership(request.getProfileId(), request.getTan());

        Map<String, Object> options = new HashMap<>();
        if (request.getOptions() != null) {
            options.putAll(request.getOptions());
        }
        options.put("purpose", GovAuthPurpose.TDS_DSC.name());
        if (request.getDocumentDigest() != null) {
            options.put("documentDigest", request.getDocumentDigest());
        }
        if (request.getReturnId() != null) {
            options.put("returnId", request.getReturnId().toString());
        }

        // Check active pending session for idempotency
        GovAuthSessionDto active = govAuthService.getActiveSessionForConnection(connection.getId());
        if (active != null && active.getAuthMethod() == GovAuthMethod.DSC && active.getStatus().isPending() && !active.isExpired()) {
            log.info("[TDS_AUTH] Reusing existing pending DSC signing session for connection id={}", connection.getId());
            return mapToDscSigningSessionDto(active, request.getDocumentDigest());
        }

        auditService.logEvent(
                tenantId,
                null,
                "TDS_DSC_SIGN_STARTED",
                "TDS_AUTH_SESSION",
                connection.getId().toString(),
                null,
                Map.of(
                        "connectionId", connection.getId().toString(),
                        "purpose", GovAuthPurpose.TDS_DSC.name(),
                        "authMethod", GovAuthMethod.DSC.name()
                )
        );

        GovAuthSessionDto govSession = govAuthService.startAuthentication(GovAuthStartRequest.builder()
                .connectionId(connection.getId())
                .authMethod(GovAuthMethod.DSC)
                .correlationId(request.getCorrelationId())
                .options(options)
                .build());

        return mapToDscSigningSessionDto(govSession, request.getDocumentDigest());
    }

    @Override
    @Transactional
    public GovDscVerifyResultDto verifyDsc(UUID sessionId, TdsDscVerifyRequest request) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is required for TDS DSC verification");
        }
        if (request == null || request.getSignatureReference() == null || request.getSignatureReference().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Signature reference is required for TDS DSC verification");
        }

        UUID tenantId = requireActiveTenantId();
        GovAuthSessionDto existing = govAuthService.getAuthenticationStatus(sessionId);
        validateTdsDscSession(existing);

        if (existing.getStatus() == GovAuthStatus.AUTHENTICATED || existing.getStatus().isTerminal()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Cannot verify DSC on session in terminal state: " + existing.getStatus());
        }
        if (existing.isExpired()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Cannot verify expired DSC signing session");
        }

        Map<String, Object> options = new HashMap<>();
        if (request.getOptions() != null) {
            options.putAll(request.getOptions());
        }
        options.put("purpose", GovAuthPurpose.TDS_DSC.name());

        GovAuthSessionDto result = govAuthService.continueAuthorization(sessionId, GovAuthContinueRequest.builder()
                .actionReference(request.getActionReference() != null ? request.getActionReference() : request.getSignatureReference())
                .options(options)
                .build());

        boolean verified = result.getStatus() == GovAuthStatus.AUTHENTICATED;
        if (verified) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "TDS_DSC_VERIFIED",
                    "TDS_AUTH_SESSION",
                    sessionId.toString(),
                    null,
                    Map.of("sessionId", sessionId.toString(), "purpose", GovAuthPurpose.TDS_DSC.name())
            );
        } else {
            auditService.logEvent(
                    tenantId,
                    null,
                    "TDS_DSC_FAILED",
                    "TDS_AUTH_SESSION",
                    sessionId.toString(),
                    null,
                    Map.of(
                            "sessionId", sessionId.toString(),
                            "purpose", GovAuthPurpose.TDS_DSC.name(),
                            "failureCode", result.getFailureCode() != null ? result.getFailureCode() : "DSC_VERIFICATION_FAILED"
                    )
            );
        }

        return GovDscVerifyResultDto.builder()
                .sessionId(result.getSessionId())
                .connectionId(result.getConnectionId())
                .purpose(GovAuthPurpose.TDS_DSC)
                .status(result.getStatus())
                .authorizationState(result.getAuthorizationState())
                .verified(verified)
                .signatureReference(request.getSignatureReference())
                .certificateReference(request.getCertificateReference() != null ? request.getCertificateReference() : "MOCK_DSC_CERT_" + result.getSessionId())
                .certificateSubject("CN=Authorized Deductor (TDS), O=Mock Corporate")
                .certificateSerial("MOCK_CERT_SERIAL_" + result.getSessionId())
                .signingAlgorithm("SHA256withRSA")
                .verifiedAt(verified ? Instant.now() : null)
                .failureCode(result.getFailureCode())
                .safeFailureMessage(result.getSafeFailureMessage())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GovAuthSessionDto getStatus(UUID sessionId) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is required");
        }
        GovAuthSessionDto session = govAuthService.getAuthenticationStatus(sessionId);
        if (session.getProviderType() != GovProviderType.TDS) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "TDS authentication session not found");
        }
        return session;
    }

    @Override
    @Transactional
    public GovAuthSessionDto cancel(UUID sessionId) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is required");
        }
        GovAuthSessionDto existing = govAuthService.getAuthenticationStatus(sessionId);
        if (existing.getProviderType() != GovProviderType.TDS) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "TDS authentication session not found");
        }
        return govAuthService.revokeAuthentication(sessionId);
    }

    private void validateTdsConnection(GovConnectionDto connection) {
        if (connection.getProviderType() != GovProviderType.TDS) {
            throw new AppException(ErrorCode.BAD_REQUEST, "TDS provider connection is required for TDS operations");
        }
    }

    private void validateDomainOwnership(UUID profileId, String tan) {
        if (profileId != null) {
            profileRepository.findByIdAndOrganizationId(profileId, requireActiveTenantId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "TDS Profile not found"));
        }
    }

    private void validateTdsEvcSession(GovAuthSessionDto session) {
        if (session.getProviderType() != GovProviderType.TDS) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Session does not belong to TDS provider");
        }
        if (session.getAuthMethod() != GovAuthMethod.EVC) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Session auth method is not EVC");
        }
    }

    private void validateTdsDscSession(GovAuthSessionDto session) {
        if (session.getProviderType() != GovProviderType.TDS) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Session does not belong to TDS provider");
        }
        if (session.getAuthMethod() != GovAuthMethod.DSC) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Session auth method is not DSC");
        }
    }

    private GovEvcChallengeDto mapToEvcChallengeDto(GovAuthSessionDto gov) {
        return GovEvcChallengeDto.builder()
                .sessionId(gov.getSessionId())
                .connectionId(gov.getConnectionId())
                .purpose(GovAuthPurpose.TDS_EVC)
                .status(gov.getStatus())
                .authorizationState(gov.getAuthorizationState())
                .challengeReference(gov.getSafeAuthorizationReference())
                .actionPrompt(gov.getActionPrompt() != null ? gov.getActionPrompt() : "Enter the 6-digit EVC sent to registered mobile/email")
                .attemptsRemaining(3)
                .createdAt(Instant.now())
                .expiresAt(gov.getExpiresAt())
                .correlationId(gov.getCorrelationId())
                .safeMessage(gov.getSafeFailureMessage())
                .failureCode(gov.getFailureCode())
                .build();
    }

    private GovDscSigningSessionDto mapToDscSigningSessionDto(GovAuthSessionDto gov, String documentDigest) {
        return GovDscSigningSessionDto.builder()
                .sessionId(gov.getSessionId())
                .connectionId(gov.getConnectionId())
                .purpose(GovAuthPurpose.TDS_DSC)
                .status(gov.getStatus())
                .authorizationState(gov.getAuthorizationState())
                .signingChallengeReference(gov.getSafeAuthorizationReference())
                .actionPrompt(gov.getActionPrompt() != null ? gov.getActionPrompt() : "Attach digital signature token and authorize signing request")
                .documentDigest(documentDigest)
                .createdAt(Instant.now())
                .expiresAt(gov.getExpiresAt())
                .correlationId(gov.getCorrelationId())
                .safeMessage(gov.getSafeFailureMessage())
                .failureCode(gov.getFailureCode())
                .build();
    }

    private UUID requireActiveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Active tenant context is required");
        }
        return tenantId;
    }
}
