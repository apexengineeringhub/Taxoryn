package com.taxoryn.module.gst.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update GST Filing Lifecycle Status & Record ARN Payload")
public class UpdateGstFilingStatusRequest {

    @NotNull(message = "Filing status is required")
    @JsonAlias({"status", "newStatus", "state"})
    @Schema(description = "Target filing status", example = "FILED")
    private GstFilingStatus filingStatus;

    @JsonAlias({"date", "filedDate", "filing_date"})
    @Schema(description = "Date of return filing", example = "2026-09-15")
    private LocalDate filingDate;

    @JsonAlias({"arnNumber", "arn", "acknowledgementNo", "ackNumber", "arn_number"})
    @Schema(description = "GST Portal Acknowledgement Reference Number (ARN)", example = "AA2708260012345")
    private String acknowledgementNumber;

    @Schema(description = "Total taxable turnover value")
    private BigDecimal totalTaxableValue;

    @Schema(description = "Total tax liability")
    private BigDecimal totalTaxLiability;

    @Schema(description = "Total ITC claimed")
    private BigDecimal totalItcClaimed;

    @Schema(description = "Tax paid in cash")
    private BigDecimal taxPaidCash;

    @Schema(description = "Tax paid via ITC")
    private BigDecimal taxPaidItc;

    @Schema(description = "Practitioner notes")
    private String notes;
}
