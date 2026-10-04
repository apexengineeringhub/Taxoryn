package com.taxoryn.module.itr.dto;

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
@Schema(description = "Request to initiate an Income Tax Return (ITR) EVC challenge")
public class ItrEvcStartRequest {

    @NotNull(message = "Connection ID is required")
    @Schema(description = "Income Tax Government Connection ID")
    private UUID connectionId;

    @Schema(description = "Taxpayer Permanent Account Number (PAN)")
    private String pan;

    @Schema(description = "Optional ITR Profile ID")
    private UUID profileId;

    @Schema(description = "Optional ITR Return ID to be verified")
    private UUID returnId;

    @Schema(description = "Correlation ID")
    private String correlationId;

    @Schema(description = "Mock directives or provider options")
    private Map<String, Object> options;
}
