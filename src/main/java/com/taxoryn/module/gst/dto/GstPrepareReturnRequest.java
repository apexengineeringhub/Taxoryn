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
@Schema(description = "Request payload to prepare and normalize a statutory GST return")
public class GstPrepareReturnRequest {

    @NotBlank(message = "GSTIN is required")
    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GSTIN format. Expected standard 15-character alphanumeric format (e.g. 27AAAAA0000A1Z5)"
    )
    @Schema(description = "15-character statutory GSTIN", example = "27AAAPL1234C1ZV")
    private String gstin;

    @NotBlank(message = "Return type is required")
    @Schema(description = "Statutory GST return type (GSTR1, GSTR3B, GSTR9, GSTR9C, CMP08, GSTR4)", example = "GSTR1")
    private String returnType;

    @NotBlank(message = "Return period is required")
    @Schema(description = "Return period identifier (e.g., 042026, 2026-04, Q1-2026, 2026-27)", example = "042026")
    private String returnPeriod;

    @Schema(description = "Financial year (e.g. 2026-27)", example = "2026-27")
    private String financialYear;

    @Schema(description = "Optional existing GST Return Filing ID to bind and transition")
    private UUID filingId;

    @Schema(description = "Optional Government Connection ID. If omitted, active connection for organization is auto-resolved.")
    private UUID connectionId;

    @Schema(description = "Optional section data or table overrides (b2b, b2cl, b2cs, cdnr, supplies, itc, etc.)")
    private Map<String, Object> sections;

    @Schema(description = "Optional simulation directives or runtime options (e.g. mockOutcome)")
    private Map<String, Object> options;
}
