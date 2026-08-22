package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice-wide TDS Compliance & Workload Executive Dashboard")
public class TdsWorkloadDashboardDto {

    @Schema(description = "Selected Quarter", example = "Q1")
    private String quarter;

    @Schema(description = "Selected Financial Year", example = "2026-27")
    private String financialYear;

    @Schema(description = "Total Deductor Clients / TANs")
    private int totalTanClients;

    @Schema(description = "Total Active TANs")
    private int activeTanProfiles;

    @Schema(description = "Total Scheduled Returns for Quarter")
    private int totalScheduledReturns;

    @Schema(description = "Returns Filed")
    private int filedReturns;

    @Schema(description = "Returns in Draft / Pending")
    private int pendingReturns;

    @Schema(description = "Returns Under Review / Ready to File")
    private int underReviewReturns;

    @Schema(description = "Overdue Returns")
    private int overdueReturns;

    @Schema(description = "Total TDS Deducted across Practice (INR)")
    private BigDecimal totalPracticeTdsDeducted;

    @Schema(description = "Total Challans Deposited across Practice (INR)")
    private BigDecimal totalPracticeChallansPaid;

    @Schema(description = "Unutilized Challans Balance (INR)")
    private BigDecimal unutilizedChallanBalance;

    @Schema(description = "Form 16 / 16A Pending Dispatch Count")
    private int pendingCertificatesCount;

    @Builder.Default
    @Schema(description = "Workload items table for the selected period")
    private List<TdsReturnDto> returnCards = new ArrayList<>();
}
