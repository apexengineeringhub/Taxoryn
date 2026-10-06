package com.taxoryn.module.gov.dto;

import com.taxoryn.module.gov.model.GovCredentialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Request payload to register or rotate a government credential reference for a connection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterGovCredentialRequest {

    @NotNull(message = "Connection ID is required")
    private UUID connectionId;

    @NotNull(message = "Credential type is required")
    private GovCredentialType credentialType;

    private String maskedIdentifier;

    @NotBlank(message = "Raw secret payload is required")
    @ToString.Exclude
    private String rawSecret;

    private Instant expiresAt;

    private String metadata;
}
