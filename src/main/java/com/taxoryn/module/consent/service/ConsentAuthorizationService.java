package com.taxoryn.module.consent.service;

import com.taxoryn.module.consent.dto.ConsentAuthorizationRequest;
import com.taxoryn.module.consent.dto.ConsentAuthorizationResult;
import com.taxoryn.module.consent.model.ConsentScope;

import java.util.UUID;

public interface ConsentAuthorizationService {

    ConsentAuthorizationResult evaluateConsent(ConsentAuthorizationRequest request);

    ConsentAuthorizationResult requireConsent(UUID clientId, UUID delegateUserId, ConsentScope requiredScope);

    boolean hasConsent(UUID clientId, UUID delegateUserId, ConsentScope requiredScope);
}
