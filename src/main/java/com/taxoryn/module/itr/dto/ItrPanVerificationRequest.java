package com.taxoryn.module.itr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Request payload to verify PAN taxpayer registration with Income Tax Department Portal")
public class ItrPanVerificationRequest {

    @NotBlank(message = "PAN is required")
    @Pattern(
            regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$",
            message = "Invalid PAN format. Expected standard 10-character alphanumeric format (e.g. ABCDE1234F)"
    )
    @Schema(description = "10-character statutory Permanent Account Number", example = "ABCDE1234F")
    private String pan;

    @Schema(description = "Optional Government Connection ID. If omitted, the default active Income Tax connection for the organization is used.")
    private UUID connectionId;

    @Schema(description = "Optional execution parameters or simulation directives (e.g., mockOutcome)")
    private Map<String, Object> options;
}
