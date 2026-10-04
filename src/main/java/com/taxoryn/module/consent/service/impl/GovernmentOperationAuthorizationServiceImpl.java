package com.taxoryn.module.consent.service.impl;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.consent.dto.ConsentAuthorizationRequest;
import com.taxoryn.module.consent.dto.ConsentAuthorizationResult;
import com.taxoryn.module.consent.model.ConsentScope;
import com.taxoryn.module.consent.service.ConsentAuthorizationService;
import com.taxoryn.module.consent.service.GovernmentOperationAuthorizationService;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Evaluates the full operational gate for a government filing/inquiry action:
 * Tenant + Client + Delegate + Active Taxpayer Consent + Government Gateway Authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GovernmentOperationAuthorizationServiceImpl implements GovernmentOperationAuthorizationService {

    private final ConsentAuthorizationService consentAuthService;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ConsentAuthorizationResult authorizeGovernmentOperation(
            UUID clientId,
            UUID delegateUserId,
            ConsentScope requiredScope,
            boolean isGovSessionAuthenticated) {

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ConsentAuthorizationResult.denied(
                    clientId, delegateUserId, requiredScope, "UNAUTHORIZED", "Active tenant context is required");
        }

        if (clientId == null || delegateUserId == null || requiredScope == null) {
            return ConsentAuthorizationResult.denied(
                    clientId, delegateUserId, requiredScope, "INVALID_PARAMETERS", "Client ID, Delegate User ID, and Required Scope are mandatory");
        }

        // 1. Verify Client belongs to Tenant
        if (clientRepository.findByIdAndOrganizationId(clientId, tenantId).isEmpty()) {
            return ConsentAuthorizationResult.denied(
                    clientId, delegateUserId, requiredScope, "CLIENT_NOT_FOUND", "Taxpayer client not found or does not belong to active organization");
        }

        // 2. Verify Delegate User belongs to Tenant
        if (userRepository.findByIdAndOrganizationId(delegateUserId, tenantId).isEmpty()) {
            return ConsentAuthorizationResult.denied(
                    clientId, delegateUserId, requiredScope, "DELEGATE_NOT_FOUND", "Delegate user not found or does not belong to active organization");
        }

        // 3. Verify Active Taxpayer Consent Delegation
        ConsentAuthorizationResult consentResult = consentAuthService.evaluateConsent(ConsentAuthorizationRequest.builder()
                .clientId(clientId)
                .delegateUserId(delegateUserId)
                .requiredScope(requiredScope)
                .build());

        if (!consentResult.isAuthorized()) {
            return consentResult;
        }

        // 4. Verify Active Government Authentication Session
        if (!isGovSessionAuthenticated) {
            return ConsentAuthorizationResult.denied(
                    clientId,
                    delegateUserId,
                    requiredScope,
                    "AUTHENTICATION_REQUIRED",
                    "Active taxpayer consent exists, but no authenticated government gateway session is established");
        }

        return consentResult;
    }
}
