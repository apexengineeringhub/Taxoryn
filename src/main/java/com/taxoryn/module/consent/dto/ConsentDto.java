package com.taxoryn.module.consent.dto;

import com.taxoryn.module.consent.model.ConsentMethod;
import com.taxoryn.module.consent.model.ConsentScope;
import com.taxoryn.module.consent.model.ConsentStatus;
import com.taxoryn.module.consent.model.DelegationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Taxpayer Consent and Practitioner Delegation Details")
public class ConsentDto {

    @Schema(description = "Consent unique identifier")
    private UUID id;

    @Schema(description = "Organization / Tenant ID")
    private UUID organizationId;

    @Schema(description = "Client / Taxpayer ID")
    private UUID clientId;

    @Schema(description = "Consenting taxpayer User ID")
    private UUID consentingUserId;

    @Schema(description = "Delegate User ID (Practitioner / CA)")
    private UUID delegateUserId;

    @Schema(description = "Type of representative delegation")
    private DelegationType delegationType;

    @Schema(description = "Consent lifecycle status")
    private ConsentStatus status;

    @Schema(description = "Method of consent acquisition")
    private ConsentMethod consentMethod;

    @Schema(description = "Authorized operational scopes")
    private Set<ConsentScope> scopes;

    @Schema(description = "Consent validity start timestamp")
    private Instant validFrom;

    @Schema(description = "Consent validity expiration timestamp")
    private Instant validUntil;

    @Schema(description = "Revocation timestamp if revoked")
    private Instant revokedAt;

    @Schema(description = "User ID who revoked the consent")
    private UUID revokedBy;

    @Schema(description = "Reason provided for revocation")
    private String revocationReason;

    @Schema(description = "Reason provided for rejection if rejected")
    private String rejectionReason;

    @Schema(description = "Safe audit reference identifier")
    private String consentReference;

    @Schema(description = "Correlation ID")
    private String correlationId;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
