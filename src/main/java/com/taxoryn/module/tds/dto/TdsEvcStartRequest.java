package com.taxoryn.module.tds.dto;

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
@Schema(description = "Request to initiate a TDS / TRACES EVC challenge")
public class TdsEvcStartRequest {

    @NotNull(message = "Connection ID is required")
    @Schema(description = "TDS Government Connection ID")
    private UUID connectionId;

    @Schema(description = "Tax Deduction and Collection Account Number (TAN)")
    private String tan;

    @Schema(description = "Optional TDS Profile ID")
    private UUID profileId;

    @Schema(description = "Optional TDS Return ID to be verified")
    private UUID returnId;

    @Schema(description = "Correlation ID")
    private String correlationId;

    @Schema(description = "Mock directives or provider options")
    private Map<String, Object> options;
}
