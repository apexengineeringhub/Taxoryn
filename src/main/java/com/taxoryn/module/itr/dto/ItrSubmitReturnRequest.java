package com.taxoryn.module.itr.dto;

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
@Schema(description = "ITR Return Submission Request Payload")
public class ItrSubmitReturnRequest {

    @Schema(description = "Existing scheduled/prepared ITR return record ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID returnId;

    @Schema(description = "Target Taxpayer PAN", example = "ABCDE1234F")
    private String pan;

    @Schema(description = "Assessment Year (e.g. 2026-27)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Financial Year (e.g. 2025-26)", example = "2025-26")
    private String financialYear;

    @Schema(description = "Return Type (e.g. ITR1, ITR2, ITR3, ITR4, ITR5, ITR6, ITR7)", example = "ITR1")
    private String returnType;

    @Schema(description = "Optional Government Connection ID. If omitted, auto-resolves active tenant Income Tax connection.")
    private UUID connectionId;

    @Schema(description = "Pre-computed payload fingerprint (if verified from preparation step)")
    private String payloadFingerprint;

    @Schema(description = "Optional pre-normalized ITR payload")
    private ItrReturnPayloadDto payload;

    @Schema(description = "Optional provider directives (e.g. mockOutcome for testing)")
    private Map<String, Object> options;
}
