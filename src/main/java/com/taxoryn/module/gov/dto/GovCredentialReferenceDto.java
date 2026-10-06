package com.taxoryn.module.gov.dto;

import com.taxoryn.module.gov.model.GovCredentialStatus;
import com.taxoryn.module.gov.model.GovCredentialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Non-sensitive metadata DTO for government credential references.
 * Never exposes raw secret payloads or encryption keys.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovCredentialReferenceDto {

    private UUID id;
    private UUID organizationId;
    private UUID connectionId;
    private GovCredentialType credentialType;
    private GovCredentialStatus credentialStatus;
    private String maskedIdentifier;
    private String secretStorageProvider;
    private Instant lastValidatedAt;
    private Instant expiresAt;
    private String metadata;
    private Instant createdAt;
    private Instant updatedAt;
}
