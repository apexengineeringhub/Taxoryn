package com.taxoryn.module.gov.auth.service;

import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.dto.GovAuthStartRequest;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface orchestrating tenant-scoped government authentication sessions and provider contracts.
 */
public interface GovernmentAuthenticationService {

    GovAuthSessionDto startAuthentication(GovAuthStartRequest request);

    GovAuthSessionDto getAuthenticationStatus(UUID sessionId);

    GovAuthSessionDto getAuthenticationStatus(UUID sessionId, Map<String, Object> options);

    GovAuthSessionDto revokeAuthentication(UUID sessionId);

    GovAuthSessionDto getActiveSessionForConnection(UUID connectionId);
}
