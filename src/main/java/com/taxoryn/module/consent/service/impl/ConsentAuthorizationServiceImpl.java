package com.taxoryn.module.consent.service.impl;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.consent.dto.ConsentAuthorizationRequest;
import com.taxoryn.module.consent.dto.ConsentAuthorizationResult;
import com.taxoryn.module.consent.entity.TaxpayerConsentEntity;
import com.taxoryn.module.consent.model.ConsentScope;
import com.taxoryn.module.consent.model.ConsentStatus;
import com.taxoryn.module.consent.repository.TaxpayerConsentRepository;
import com.taxoryn.module.consent.service.ConsentAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentAuthorizationServiceImpl implements ConsentAuthorizationService {

    private final TaxpayerConsentRepository consentRepository;

    @Override
    @Transactional(readOnly = true)
    public ConsentAuthorizationResult evaluateConsent(ConsentAuthorizationRequest request) {
        if (request == null || request.getClientId() == null || request.getDelegateUserId() == null || request.getRequiredScope() == null) {
            return ConsentAuthorizationResult.denied(
                    request != null ? request.getClientId() : null,
                    request != null ? request.getDelegateUserId() : null,
                    request != null ? request.getRequiredScope() : null,
                    "INVALID_REQUEST",
                    "Client ID, Delegate User ID, and Required Scope are mandatory for authorization evaluation");
        }

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ConsentAuthorizationResult.denied(
                    request.getClientId(),
                    request.getDelegateUserId(),
                    request.getRequiredScope(),
                    "UNAUTHORIZED",
                    "Active tenant context is required");
        }

        Instant checkTime = request.getCheckTime() != null ? request.getCheckTime() : Instant.now();

        // 1. Fetch active consents for this client + delegate
        List<TaxpayerConsentEntity> activeConsents = consentRepository.findAllByOrganizationIdAndClientIdAndDelegateUserIdAndStatus(
                tenantId, request.getClientId(), request.getDelegateUserId(), ConsentStatus.ACTIVE);

        for (TaxpayerConsentEntity consent : activeConsents) {
            if (!consent.isUsableAt(checkTime)) {
                continue;
            }
            if (consent.hasScope(request.getRequiredScope())) {
                return ConsentAuthorizationResult.authorized(
                        consent.getId(),
                        consent.getClientId(),
                        consent.getDelegateUserId(),
                        request.getRequiredScope(),
                        consent.getValidFrom(),
                        consent.getValidUntil()
                );
            }
        }

        // 2. If no active matching consent found, evaluate diagnostic reason
        List<TaxpayerConsentEntity> allConsents = consentRepository.findAllByOrganizationIdAndClientId(tenantId, request.getClientId());
        boolean hasExpired = false;
        boolean hasRevoked = false;
        boolean hasPending = false;
        boolean hasDifferentScope = false;

        for (TaxpayerConsentEntity c : allConsents) {
            if (c.getDelegateUserId().equals(request.getDelegateUserId())) {
                if (c.getStatus() == ConsentStatus.ACTIVE && c.isExpired()) {
                    hasExpired = true;
                } else if (c.getStatus() == ConsentStatus.REVOKED) {
                    hasRevoked = true;
                } else if (c.getStatus() == ConsentStatus.PENDING) {
                    hasPending = true;
                } else if (c.getStatus() == ConsentStatus.ACTIVE && !c.hasScope(request.getRequiredScope())) {
                    hasDifferentScope = true;
                }
            }
        }

        if (hasDifferentScope) {
            return ConsentAuthorizationResult.denied(
                    request.getClientId(),
                    request.getDelegateUserId(),
                    request.getRequiredScope(),
                    "SCOPE_NOT_GRANTED",
                    "Active delegation exists but does not grant required scope: " + request.getRequiredScope());
        }
        if (hasExpired) {
            return ConsentAuthorizationResult.denied(
                    request.getClientId(),
                    request.getDelegateUserId(),
                    request.getRequiredScope(),
                    "EXPIRED",
                    "Taxpayer consent delegation for this client has expired");
        }
        if (hasRevoked) {
            return ConsentAuthorizationResult.denied(
                    request.getClientId(),
                    request.getDelegateUserId(),
                    request.getRequiredScope(),
                    "REVOKED",
                    "Taxpayer consent delegation has been explicitly revoked");
        }
        if (hasPending) {
            return ConsentAuthorizationResult.denied(
                    request.getClientId(),
                    request.getDelegateUserId(),
                    request.getRequiredScope(),
                    "PENDING_APPROVAL",
                    "Taxpayer consent delegation is awaiting approval");
        }

        return ConsentAuthorizationResult.denied(
                request.getClientId(),
                request.getDelegateUserId(),
                request.getRequiredScope(),
                "NO_ACTIVE_DELEGATION",
                "No active taxpayer consent delegation found for this client and delegate");
    }

    @Override
    @Transactional(readOnly = true)
    public ConsentAuthorizationResult requireConsent(UUID clientId, UUID delegateUserId, ConsentScope requiredScope) {
        ConsentAuthorizationResult result = evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientId)
                .delegateUserId(delegateUserId)
                .requiredScope(requiredScope)
                .build());

        if (!result.isAuthorized()) {
            throw new AppException(ErrorCode.FORBIDDEN, result.getReasonMessage());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConsent(UUID clientId, UUID delegateUserId, ConsentScope requiredScope) {
        return evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientId)
                .delegateUserId(delegateUserId)
                .requiredScope(requiredScope)
                .build()).isAuthorized();
    }
}
