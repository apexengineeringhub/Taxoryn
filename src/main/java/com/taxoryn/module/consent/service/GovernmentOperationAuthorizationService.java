package com.taxoryn.module.consent.service;

import com.taxoryn.module.consent.dto.ConsentAuthorizationResult;
import com.taxoryn.module.consent.model.ConsentScope;

import java.util.UUID;

/**
 * High-level authorization service combining:
 * Tenant Isolation + Client Ownership + RBAC + Active Taxpayer Consent + Government Authentication.
 */
public interface GovernmentOperationAuthorizationService {

    ConsentAuthorizationResult authorizeGovernmentOperation(
            UUID clientId,
            UUID delegateUserId,
            ConsentScope requiredScope,
            boolean isGovSessionAuthenticated);
}
