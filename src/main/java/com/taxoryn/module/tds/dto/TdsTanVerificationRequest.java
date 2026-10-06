package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request payload for verifying a TAN deductor profile against the Government TRACES / TDS gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to verify a TAN deductor profile against Government TDS/TRACES gateway")
public class TdsTanVerificationRequest {

    @NotBlank(message = "TAN must not be blank")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{5}[A-Z]{1}$", message = "TAN must be a valid 10-character alphanumeric code (e.g., MUMB12345A)")
    @Schema(description = "10-character Tax Deduction and Collection Account Number (TAN)", example = "MUMB12345A", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tan;

    @Schema(description = "Optional Government Connection ID. If omitted, the active tenant TDS connection is automatically resolved.")
    private UUID connectionId;

    @Schema(description = "Optional provider execution options or simulation directives")
    private Map<String, Object> options;
}
