package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Normalized GST Return Filing Status Tracking DTO")
public class GstReturnStatusDto {

    @Schema(description = "Filing record ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID filingId;

    @Schema(description = "Taxpayer GSTIN", example = "27AAACA1234A1ZV")
    private String gstin;

    @Schema(description = "Return Type", example = "GSTR1")
    private String returnType;

    @Schema(description = "Return Period", example = "042026")
    private String returnPeriod;

    @Schema(description = "Financial Year", example = "2026-27")
    private String financialYear;

    @Schema(description = "Current Taxoryn Filing Lifecycle Status", example = "FILED")
    private GstFilingStatus filingStatus;

    @Schema(description = "Raw normalized provider status (e.g. PENDING, PROCESSING, FILED, REJECTED, FAILED)", example = "FILED")
    private String providerStatus;

    @Schema(description = "Is this filing in a terminal lifecycle state")
    private boolean terminal;

    @Schema(description = "Did the status query execute successfully")
    private boolean success;

    @Schema(description = "Final or intermediate acknowledgement reference number (ARN)", example = "MOCK-GST-ACK-E3B0C442")
    private String acknowledgementNumber;

    @Schema(description = "Provider transaction/submission reference ID", example = "REF-GST-SUB-98765432")
    private String providerReference;

    @Schema(description = "Authoritative filing date confirmed by GST portal (only populated on FILED)")
    private LocalDate filingDate;

    @Schema(description = "Timestamp when the status was verified with the provider")
    private Instant lastCheckedAt;

    @Schema(description = "Government Framework Operation ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID operationId;

    @Schema(description = "Error code if status query failed", example = "AUTH_REQUIRED")
    private String errorCode;

    @Schema(description = "Error message if status query failed", example = "Session expired")
    private String errorMessage;

    @Schema(description = "Provider response metadata")
    private Map<String, Object> metadata;
}
