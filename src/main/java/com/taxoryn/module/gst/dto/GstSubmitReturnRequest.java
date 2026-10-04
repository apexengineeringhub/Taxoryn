package com.taxoryn.module.gst.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "GST Return Submission Request Payload")
public class GstSubmitReturnRequest {

    @Schema(description = "Existing scheduled/prepared filing record ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID filingId;

    @Schema(description = "Target GSTIN", example = "27AAACA1234A1ZV")
    private String gstin;

    @Schema(description = "Return Type (e.g. GSTR1, GSTR3B, GSTR9)", example = "GSTR1")
    private String returnType;

    @Schema(description = "Return Period (e.g. 042026)", example = "042026")
    private String returnPeriod;

    @Schema(description = "Financial Year (e.g. 2026-27)", example = "2026-27")
    private String financialYear;

    @Schema(description = "Optional Government Connection ID. If omitted, auto-resolves active tenant GST connection.")
    private UUID connectionId;

    @Schema(description = "Pre-computed payload fingerprint (if verified from preparation step)")
    private String payloadFingerprint;

    @Schema(description = "Optional provider directives (e.g. mockOutcome for testing)")
    private Map<String, Object> options;
}
