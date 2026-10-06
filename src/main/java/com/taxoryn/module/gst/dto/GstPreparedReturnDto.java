package com.taxoryn.module.gst.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Summary and normalized payload of a prepared GST return")
public class GstPreparedReturnDto {

    @Schema(description = "Associated GST Return Filing ID (if linked)")
    private UUID filingId;

    @Schema(description = "15-character statutory GSTIN", example = "27AAAPL1234C1ZV")
    private String gstin;

    @Schema(description = "Legal registered name of business entity", example = "Apex Enterprises Private Limited")
    private String legalName;

    @Schema(description = "Trade name of business entity", example = "Apex Solutions")
    private String tradeName;

    @Schema(description = "Statutory GST return type code (GSTR1, GSTR3B, GSTR9)", example = "GSTR1")
    private String returnType;

    @Schema(description = "Return period (e.g., 042026, 2026-04)", example = "042026")
    private String returnPeriod;

    @Schema(description = "Financial year", example = "2026-27")
    private String financialYear;

    @Schema(description = "Current return status (PREPARED, VALIDATION_FAILED, READY_FOR_SUBMISSION)", example = "PREPARED")
    private String status;

    @Schema(description = "Flag indicating return is verified, structurally compliant, and ready for government submission", example = "true")
    private boolean readyForSubmission;

    @Schema(description = "Detailed structural and arithmetic validation results")
    private GstReturnValidationResultDto validationResult;

    @Schema(description = "Normalized provider-neutral return payload with deterministic fingerprint")
    private GstReturnPayloadDto payload;

    @Schema(description = "Unique audit ID of the integration operation (if operation tracked)")
    private UUID operationId;

    @Schema(description = "Provider simulation acknowledgement reference", example = "PREP-GST-7B32F1")
    private String providerReferenceId;

    @Schema(description = "Timestamp when the return was prepared")
    private Instant preparedAt;

    @Schema(description = "Error code if preparation or validation failed")
    private String errorCode;

    @Schema(description = "Error message if preparation or validation failed")
    private String errorMessage;
}
