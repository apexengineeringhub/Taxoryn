package com.taxoryn.module.itr.service.impl;

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
import com.taxoryn.module.itr.dto.ItrDscSignRequest;
import com.taxoryn.module.itr.dto.ItrDscVerifyRequest;
import com.taxoryn.module.itr.dto.ItrEvcStartRequest;
import com.taxoryn.module.itr.dto.ItrEvcVerifyRequest;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.service.ItrAuthenticationService;
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
public class ItrAuthenticationServiceImpl implements ItrAuthenticationService {

    private final GovernmentAuthenticationService govAuthService;
    private final GovernmentConnectionService connectionService;
    private final ItrProfileRepository profileRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public GovEvcChallengeDto startEvc(ItrEvcStartRequest request) {
        if (request == null || request.getConnectionId() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Connection ID is required for ITR EVC");
        }

        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = connectionService.getConnection(request.getConnectionId());
        validateItrConnection(connection);
        validateDomainOwnership(request.getProfileId(), request.getPan());

        Map<String, Object> options = new HashMap<>();
        if (request.getOptions() != null) {
            options.putAll(request.getOptions());
        }
        options.put("purpose", GovAuthPurpose.ITR_EVC.name());
        if (request.getReturnId() != null) {
            options.put("returnId", request.getReturnId().toString());
        }

        // Idempotency: if already active session exists for this connection with method EVC in pending state
        GovAuthSessionDto active = govAuthService.getActiveSessionForConnection(connection.getId());
        if (active != null && active.getAuthMethod() == GovAuthMethod.EVC && active.getStatus().isPending() && !active.isExpired()) {
            log.info("[ITR_AUTH] Reusing existing pending EVC challenge for connection id={}", connection.getId());
            return mapToEvcChallengeDto(active);
        }

        auditService.logEvent(
                tenantId,
                null,
                "ITR_EVC_STARTED",
                "ITR_AUTH_SESSION",
                connection.getId().toString(),
                null,
                Map.of(
                        "connectionId", connection.getId().toString(),
                        "purpose", GovAuthPurpose.ITR_EVC.name(),
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
    public GovEvcVerifyResultDto verifyEvc(UUID sessionId, ItrEvcVerifyRequest request) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is required for EVC verification");
        }
        if (request == null || request.getVerificationCode() == null || request.getVerificationCode().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Verification code is required for EVC verification");
        }

        UUID tenantId = requireActiveTenantId();
        GovAuthSessionDto existing = govAuthService.getAuthenticationStatus(sessionId);
        validateItrEvcSession(existing);

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
        options.put("purpose", GovAuthPurpose.ITR_EVC.name());

        GovAuthSessionDto result = govAuthService.continueAuthorization(sessionId, GovAuthContinueRequest.builder()
                .actionReference(request.getActionReference() != null ? request.getActionReference() : request.getVerificationCode())
                .options(options)
                .build());

        boolean verified = result.getStatus() == GovAuthStatus.AUTHENTICATED;
        if (verified) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "ITR_EVC_VERIFIED",
                    "ITR_AUTH_SESSION",
                    sessionId.toString(),
                    null,
                    Map.of("sessionId", sessionId.toString(), "purpose", GovAuthPurpose.ITR_EVC.name())
            );
        } else {
            auditService.logEvent(
                    tenantId,
                    null,
                    "ITR_EVC_FAILED",
                    "ITR_AUTH_SESSION",
                    sessionId.toString(),
                    null,
                    Map.of(
                            "sessionId", sessionId.toString(),
                            "purpose", GovAuthPurpose.ITR_EVC.name(),
                            "failureCode", result.getFailureCode() != null ? result.getFailureCode() : "VERIFICATION_FAILED"
                    )
            );
        }

        return GovEvcVerifyResultDto.builder()
                .sessionId(result.getSessionId())
                .connectionId(result.getConnectionId())
                .purpose(GovAuthPurpose.ITR_EVC)
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
    public GovDscSigningSessionDto startDsc(ItrDscSignRequest request) {
        if (request == null || request.getConnectionId() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Connection ID is required for ITR DSC signing");
        }

        UUID tenantId = requireActiveTenantId();
        GovConnectionDto connection = connectionService.getConnection(request.getConnectionId());
        validateItrConnection(connection);
        validateDomainOwnership(request.getProfileId(), request.getPan());

        Map<String, Object> options = new HashMap<>();
        if (request.getOptions() != null) {
            options.putAll(request.getOptions());
        }
        options.put("purpose", GovAuthPurpose.ITR_DSC.name());
        if (request.getDocumentDigest() != null) {
            options.put("documentDigest", request.getDocumentDigest());
        }
        if (request.getReturnId() != null) {
            options.put("returnId", request.getReturnId().toString());
        }

        // Check for existing pending DSC session
        GovAuthSessionDto active = govAuthService.getActiveSessionForConnection(connection.getId());
        if (active != null && active.getAuthMethod() == GovAuthMethod.DSC && active.getStatus().isPending() && !active.isExpired()) {
            log.info("[ITR_AUTH] Reusing existing pending DSC signing session for connection id={}", connection.getId());
            return mapToDscSigningSessionDto(active, request.getDocumentDigest());
        }

        auditService.logEvent(
                tenantId,
                null,
                "ITR_DSC_SIGN_STARTED",
                "ITR_AUTH_SESSION",
                connection.getId().toString(),
                null,
                Map.of(
                        "connectionId", connection.getId().toString(),
                        "purpose", GovAuthPurpose.ITR_DSC.name(),
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
    public GovDscVerifyResultDto verifyDsc(UUID sessionId, ItrDscVerifyRequest request) {
        if (sessionId == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Session ID is required for DSC verification");
        }
        if (request == null || request.getSignatureReference() == null || request.getSignatureReference().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Signature reference is required for DSC verification");
        }

        UUID tenantId = requireActiveTenantId();
        GovAuthSessionDto existing = govAuthService.getAuthenticationStatus(sessionId);
        validateItrDscSession(existing);

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
        options.put("purpose", GovAuthPurpose.ITR_DSC.name());

        GovAuthSessionDto result = govAuthService.continueAuthorization(sessionId, GovAuthContinueRequest.builder()
                .actionReference(request.getActionReference() != null ? request.getActionReference() : request.getSignatureReference())
                .options(options)
                .build());

        boolean verified = result.getStatus() == GovAuthStatus.AUTHENTICATED;
        if (verified) {
            auditService.logEvent(
                    tenantId,
                    null,
                    "ITR_DSC_VERIFIED",
                    "ITR_AUTH_SESSION",
                    sessionId.toString(),
                    null,
                    Map.of("sessionId", sessionId.toString(), "purpose", GovAuthPurpose.ITR_DSC.name())
            );
        } else {
            auditService.logEvent(
                    tenantId,
                    null,
                    "ITR_DSC_FAILED",
                    "ITR_AUTH_SESSION",
                    sessionId.toString(),
                    null,
                    Map.of(
                            "sessionId", sessionId.toString(),
                            "purpose", GovAuthPurpose.ITR_DSC.name(),
                            "failureCode", result.getFailureCode() != null ? result.getFailureCode() : "DSC_VERIFICATION_FAILED"
                    )
            );
        }

        return GovDscVerifyResultDto.builder()
                .sessionId(result.getSessionId())
                .connectionId(result.getConnectionId())
                .purpose(GovAuthPurpose.ITR_DSC)
                .status(result.getStatus())
                .authorizationState(result.getAuthorizationState())
                .verified(verified)
                .signatureReference(request.getSignatureReference())
                .certificateReference(request.getCertificateReference() != null ? request.getCertificateReference() : "MOCK_DSC_CERT_" + result.getSessionId())
                .certificateSubject("CN=Authorized Signatory (ITR), O=Mock Taxpayer")
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
        if (session.getProviderType() != GovProviderType.INCOME_TAX) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "ITR authentication session not found");
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
        if (existing.getProviderType() != GovProviderType.INCOME_TAX) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "ITR authentication session not found");
        }
        return govAuthService.revokeAuthentication(sessionId);
    }

    private void validateItrConnection(GovConnectionDto connection) {
        if (connection.getProviderType() != GovProviderType.INCOME_TAX) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Income Tax provider connection is required for ITR operations");
        }
    }

    private void validateDomainOwnership(UUID profileId, String pan) {
        if (profileId != null) {
            profileRepository.findByIdAndOrganizationId(profileId, requireActiveTenantId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "ITR Profile not found"));
        }
    }

    private void validateItrEvcSession(GovAuthSessionDto session) {
        if (session.getProviderType() != GovProviderType.INCOME_TAX) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Session does not belong to Income Tax provider");
        }
        if (session.getAuthMethod() != GovAuthMethod.EVC) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Session auth method is not EVC");
        }
    }

    private void validateItrDscSession(GovAuthSessionDto session) {
        if (session.getProviderType() != GovProviderType.INCOME_TAX) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Session does not belong to Income Tax provider");
        }
        if (session.getAuthMethod() != GovAuthMethod.DSC) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Session auth method is not DSC");
        }
    }

    private GovEvcChallengeDto mapToEvcChallengeDto(GovAuthSessionDto gov) {
        return GovEvcChallengeDto.builder()
                .sessionId(gov.getSessionId())
                .connectionId(gov.getConnectionId())
                .purpose(GovAuthPurpose.ITR_EVC)
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
                .purpose(GovAuthPurpose.ITR_DSC)
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
