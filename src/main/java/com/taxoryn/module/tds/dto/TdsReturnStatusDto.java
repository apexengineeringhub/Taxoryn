package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Normalized TDS Return Filing Status Tracking & Challan Reconciliation DTO")
public class TdsReturnStatusDto {

    @Schema(description = "TDS Return record ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID returnId;

    @Schema(description = "Deductor TAN", example = "MUMB12345A")
    private String tan;

    @Schema(description = "Masked Deductor TAN", example = "MUMB*****A")
    private String maskedTan;

    @Schema(description = "Form Type (e.g. FORM_24Q, FORM_26Q, FORM_27Q, FORM_27EQ)", example = "FORM_26Q")
    private String formType;

    @Schema(description = "Quarter (Q1, Q2, Q3, Q4)", example = "Q1")
    private String quarter;

    @Schema(description = "Financial Year", example = "2025-26")
    private String financialYear;

    @Schema(description = "Assessment Year", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Current Taxoryn Return Lifecycle Status", example = "FILED")
    private TdsFilingStatus filingStatus;

    @Schema(description = "Raw normalized provider status (e.g. PENDING, PROCESSING, FILED, REJECTED, FAILED)", example = "FILED")
    private String providerStatus;

    @Schema(description = "Is this return in a terminal lifecycle state (FILED, CANCELLED)")
    private boolean terminal;

    @Schema(description = "Did the status query execute successfully")
    private boolean success;

    @Schema(description = "Acknowledgement reference number (Receipt Number / Token)", example = "TRACES-ACK-MUMB12345A-Q1-202526-001")
    private String acknowledgementNumber;

    @Schema(description = "Token / PRN number", example = "010022300045678")
    private String tokenNumber;

    @Schema(description = "Receipt number", example = "TRACES-REC-998877")
    private String receiptNumber;

    @Schema(description = "Provider transaction / verification reference ID", example = "TRACES-STATUS-REF-12345678")
    private String providerReference;

    @Schema(description = "Authoritative filing date confirmed by TRACES portal (only populated on FILED)")
    private LocalDate filingDate;

    @Schema(description = "Timestamp when the status was verified with the provider")
    private Instant lastCheckedAt;

    @Schema(description = "Government Framework Operation ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID operationId;

    @Schema(description = "Error code if status query failed", example = "AUTH_REQUIRED")
    private String errorCode;

    @Schema(description = "Error message if status query failed", example = "Session expired")
    private String errorMessage;

    @Schema(description = "Reconciled challans status list")
    private List<TdsChallanReconciliationDto> challans;

    @Schema(description = "Provider response metadata")
    private Map<String, Object> metadata;
}
