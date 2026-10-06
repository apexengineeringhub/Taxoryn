package com.taxoryn.module.consent.dto;

import com.taxoryn.module.consent.model.ConsentScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of taxpayer consent and delegation authorization check")
public class ConsentAuthorizationResult {

    @Schema(description = "Whether the operation is authorized by active taxpayer consent")
    private boolean authorized;

    @Schema(description = "Matching Consent ID if authorized")
    private UUID consentId;

    @Schema(description = "Client / Taxpayer ID")
    private UUID clientId;

    @Schema(description = "Delegate User ID")
    private UUID delegateUserId;

    @Schema(description = "Evaluated operational scope")
    private ConsentScope scope;

    @Schema(description = "Consent validity start timestamp")
    private Instant validFrom;

    @Schema(description = "Consent validity expiration timestamp")
    private Instant validUntil;

    @Schema(description = "Safe diagnostic reason code (e.g. AUTHORIZED, NO_ACTIVE_DELEGATION, SCOPE_NOT_GRANTED, EXPIRED)")
    private String reasonCode;

    @Schema(description = "Human-readable non-sensitive explanation")
    private String reasonMessage;

    public static ConsentAuthorizationResult authorized(
            UUID consentId, UUID clientId, UUID delegateUserId, ConsentScope scope, Instant from, Instant until) {
        return ConsentAuthorizationResult.builder()
                .authorized(true)
                .consentId(consentId)
                .clientId(clientId)
                .delegateUserId(delegateUserId)
                .scope(scope)
                .validFrom(from)
                .validUntil(until)
                .reasonCode("AUTHORIZED")
                .reasonMessage("Operation is authorized under active taxpayer consent delegation")
                .build();
    }

    public static ConsentAuthorizationResult denied(
            UUID clientId, UUID delegateUserId, ConsentScope scope, String reasonCode, String message) {
        return ConsentAuthorizationResult.builder()
                .authorized(false)
                .clientId(clientId)
                .delegateUserId(delegateUserId)
                .scope(scope)
                .reasonCode(reasonCode)
                .reasonMessage(message)
                .build();
    }
}
