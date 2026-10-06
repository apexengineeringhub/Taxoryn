package com.taxoryn.module.itr.dto;

import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
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
@Schema(description = "Normalized ITR Return Filing Status Tracking DTO")
public class ItrReturnStatusDto {

    @Schema(description = "ITR Return record ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID returnId;

    @Schema(description = "Taxpayer PAN", example = "ABCDE1234F")
    private String pan;

    @Schema(description = "Masked Taxpayer PAN", example = "ABCDE****F")
    private String maskedPan;

    @Schema(description = "Assessment Year", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Financial Year", example = "2025-26")
    private String financialYear;

    @Schema(description = "Return Type (e.g. ITR1, ITR2)", example = "ITR1")
    private String returnType;

    @Schema(description = "Current Taxoryn Return Lifecycle Status", example = "FILED")
    private ItrStatus filingStatus;

    @Schema(description = "Raw normalized provider status (e.g. PENDING, PROCESSING, FILED, REJECTED, FAILED)", example = "FILED")
    private String providerStatus;

    @Schema(description = "Is this return in a terminal lifecycle state (FILED, COMPLETED, CANCELLED)")
    private boolean terminal;

    @Schema(description = "Did the status query execute successfully")
    private boolean success;

    @Schema(description = "Acknowledgement reference number (ARN / ITR-V Ack)", example = "ITD-ACK-7890123456")
    private String acknowledgementNumber;

    @Schema(description = "Provider transaction / verification reference ID", example = "ITD-STATUS-REF-12345678")
    private String providerReference;

    @Schema(description = "Authoritative filing date confirmed by Income Tax portal (only populated on FILED)")
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
