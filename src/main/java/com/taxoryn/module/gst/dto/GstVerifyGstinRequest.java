package com.taxoryn.module.gst.dto;

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
@Schema(description = "Request payload to verify GSTIN registration with Government GST Portal")
public class GstVerifyGstinRequest {

    @NotBlank(message = "GSTIN is required")
    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GSTIN format. Expected standard 15-character alphanumeric format (e.g. 27AAAAA0000A1Z5)"
    )
    @Schema(description = "15-character statutory Goods and Services Tax Identification Number", example = "27AAAPL1234C1ZV")
    private String gstin;

    @Schema(description = "Optional Government Connection ID. If omitted, the default active GST connection for the organization is used.")
    private UUID connectionId;

    @Schema(description = "Optional execution parameters or simulation directives (e.g., mockOutcome)")
    private Map<String, Object> options;
}
