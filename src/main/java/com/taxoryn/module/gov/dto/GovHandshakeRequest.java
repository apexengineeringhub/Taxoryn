package com.taxoryn.module.gov.dto;

import com.taxoryn.module.gov.model.GovCredentialType;
import com.taxoryn.module.gov.model.GovProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provider-neutral handshake request model.
 * Contains only non-sensitive metadata for checking gateway availability and connection handshake.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovHandshakeRequest {

    private UUID organizationId;
    private UUID connectionId;
    private GovProviderType providerType;
    private String adapterCode;
    private String environment;
    private GovCredentialType credentialType;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
