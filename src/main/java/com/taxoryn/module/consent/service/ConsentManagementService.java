package com.taxoryn.module.consent.service;

import com.taxoryn.module.consent.dto.ApproveConsentRequest;
import com.taxoryn.module.consent.dto.ConsentDto;
import com.taxoryn.module.consent.dto.CreateConsentRequest;
import com.taxoryn.module.consent.dto.RevokeConsentRequest;

import java.util.List;
import java.util.UUID;

public interface ConsentManagementService {

    ConsentDto createConsent(CreateConsentRequest request);

    ConsentDto getConsentById(UUID consentId);

    List<ConsentDto> getConsentsForClient(UUID clientId);

    List<ConsentDto> getDelegationsForUser(UUID delegateUserId);

    ConsentDto approveConsent(UUID consentId, ApproveConsentRequest request);

    ConsentDto rejectConsent(UUID consentId, String reason);

    ConsentDto revokeConsent(UUID consentId, RevokeConsentRequest request);

    List<ConsentDto> getActiveConsentsForClient(UUID clientId);
}
