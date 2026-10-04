package com.taxoryn.module.consent.dto;

import com.taxoryn.module.consent.model.ConsentScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request to evaluate taxpayer consent authorization for an operation")
public class ConsentAuthorizationRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Target Client / Taxpayer ID")
    private UUID clientId;

    @NotNull(message = "Delegate User ID is required")
    @Schema(description = "Acting User / Delegate ID")
    private UUID delegateUserId;

    @NotNull(message = "Required scope is required")
    @Schema(description = "Scope required for the intended business operation")
    private ConsentScope requiredScope;

    @Schema(description = "Timestamp at which to evaluate validity (defaults to now)")
    private Instant checkTime;
}
