package com.taxoryn.module.gst.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Deterministic, normalized, provider-neutral GST return payload")
public class GstReturnPayloadDto {

    @Schema(description = "15-character statutory GSTIN", example = "27AAAPL1234C1ZV")
    private String gstin;

    @Schema(description = "Statutory GST return type code (GSTR1, GSTR3B, GSTR9, CMP08)", example = "GSTR1")
    private String returnType;

    @Schema(description = "Return filing period (e.g. 042026, 2026-04, Q1-2026)", example = "042026")
    private String returnPeriod;

    @Schema(description = "Applicable financial year", example = "2026-27")
    private String financialYear;

    @Schema(description = "Return table sections containing categorized supply and ITC summaries")
    @Builder.Default
    private Map<String, Object> sections = new HashMap<>();

    @Schema(description = "Computed tax and liability totals")
    private GstReturnTotalsDto totals;

    @Schema(description = "SHA-256 fingerprint ensuring deterministic payload identity across submissions", example = "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e")
    private String payloadFingerprint;

    @Schema(description = "Extensible metadata headers (e.g., placeOfSupply, schemaVersion, sourceSystem)")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
