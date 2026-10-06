package com.taxoryn.module.consent.dto;

import com.taxoryn.module.consent.model.ConsentMethod;
import com.taxoryn.module.consent.model.ConsentScope;
import com.taxoryn.module.consent.model.DelegationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request to establish a new taxpayer consent or practitioner delegation")
public class CreateConsentRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Taxpayer / Client ID")
    private UUID clientId;

    @NotNull(message = "Delegate User ID is required")
    @Schema(description = "Delegate User ID (Practitioner / CA / Employee)")
    private UUID delegateUserId;

    @NotNull(message = "Delegation Type is required")
    @Schema(description = "Type of representative delegation")
    private DelegationType delegationType;

    @NotEmpty(message = "At least one consent scope is required")
    @Schema(description = "Set of authorized operational scopes")
    private Set<ConsentScope> scopes;

    @Schema(description = "Consent validity start timestamp (defaults to current time)")
    private Instant validFrom;

    @NotNull(message = "Valid until timestamp is required")
    @Schema(description = "Consent validity expiration timestamp")
    private Instant validUntil;

    @Schema(description = "Method of consent acquisition (defaults to IN_APP)")
    @Builder.Default
    private ConsentMethod consentMethod = ConsentMethod.IN_APP;

    @Schema(description = "Correlation ID for audit tracing")
    private String correlationId;

    @Schema(description = "Whether to immediately activate consent if creator has sufficient privilege")
    @Builder.Default
    private boolean autoApprove = false;
}
