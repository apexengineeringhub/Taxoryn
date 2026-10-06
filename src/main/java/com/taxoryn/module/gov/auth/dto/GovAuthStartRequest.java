package com.taxoryn.module.gov.auth.dto;

import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to initiate government authentication session")
public class GovAuthStartRequest {

    @NotNull(message = "Government Connection ID is required")
    @Schema(description = "Target Government Connection ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID connectionId;

    @NotNull(message = "Authentication Method is required")
    @Schema(description = "Authentication Method (OAUTH2, OTP, EVC, DSC)", requiredMode = Schema.RequiredMode.REQUIRED)
    private GovAuthMethod authMethod;

    @Schema(description = "Optional tracking correlation ID")
    private String correlationId;

    @Schema(description = "Optional provider simulation/execution directives")
    private Map<String, Object> options;
}
