package com.taxoryn.module.gov.service;

import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovCredentialReferenceDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.dto.UpdateGovConnectionRequest;
import com.taxoryn.module.gov.model.GovProviderType;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for managing tenant-scoped government connections and credential references.
 */
public interface GovernmentConnectionService {

    GovConnectionDto createConnection(CreateGovConnectionRequest request);

    GovConnectionDto getConnection(UUID connectionId);

    List<GovConnectionDto> listConnections();

    List<GovConnectionDto> listConnectionsByProvider(GovProviderType providerType);

    GovConnectionDto updateConnection(UUID connectionId, UpdateGovConnectionRequest request);

    GovConnectionDto activateConnection(UUID connectionId);

    GovConnectionDto deactivateConnection(UUID connectionId);

    GovConnectionDto markAuthRequired(UUID connectionId);

    GovConnectionDto markFailed(UUID connectionId, String reason);

    GovCredentialReferenceDto registerCredential(RegisterGovCredentialRequest request);

    GovCredentialReferenceDto invalidateCredential(UUID credentialRefId);

    GovCredentialReferenceDto getCredentialMetadata(UUID credentialRefId);

    String getDecryptedSecret(UUID connectionId);
}
