package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.entity.GstProfileEntity.GstType;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GST Practice & Employee Workload Dashboard")
public class GstWorkloadDashboardDto {

    @Schema(description = "Target period (e.g. 2026-08)", example = "2026-08")
    private String period;

    @Schema(description = "Period display label", example = "August 2026")
    private String periodLabel;

    @Schema(description = "Total registered GST clients in practice", example = "45")
    private long totalGstClients;

    @Schema(description = "Count of pending GSTR-1 returns", example = "12")
    private long gstr1PendingCount;

    @Schema(description = "Count of filed GSTR-1 returns", example = "33")
    private long gstr1FiledCount;

    @Schema(description = "Count of pending GSTR-3B returns", example = "18")
    private long gstr3bPendingCount;

    @Schema(description = "Count of filed GSTR-3B returns", example = "27")
    private long gstr3bFiledCount;

    @Schema(description = "Total Input Tax Credit (ITC) tracked across clients (₹)", example = "4500000.00")
    private BigDecimal totalItcTracked;

    @Schema(description = "Total output tax liability payable across clients (₹)", example = "2800000.00")
    private BigDecimal totalTaxLiability;

    @Schema(description = "Client workload items breakdown")
    private List<GstClientWorkloadItem> clients;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual GST Client Period Workload Card")
    public static class GstClientWorkloadItem {

        @Schema(description = "Client ID")
        private UUID clientId;

        @Schema(description = "Client display name", example = "ABC Traders")
        private String clientName;

        @Schema(description = "GST Profile ID")
        private UUID gstProfileId;

        @Schema(description = "GSTIN", example = "27AAACZ1234D1Z8")
        private String gstin;

        @Schema(description = "GST registration type", example = "REGULAR")
        private GstType gstType;

        @Schema(description = "Target period", example = "August 2026")
        private String period;

        @Schema(description = "GSTR-1 filing status", example = "PENDING")
        private GstFilingStatus gstr1Status;

        @Schema(description = "GSTR-3B filing status", example = "PENDING")
        private GstFilingStatus gstr3bStatus;

        @Schema(description = "CMP-08 filing status (for composition dealers)")
        private GstFilingStatus cmp08Status;

        @Schema(description = "Input Tax Credit (ITC) amount (₹)", example = "125000.00")
        private BigDecimal itc;

        @Schema(description = "Output Tax liability amount (₹)", example = "82000.00")
        private BigDecimal taxLiability;

        @Schema(description = "Statutory Due Date", example = "2026-09-20")
        private LocalDate dueDate;

        @Schema(description = "Assigned practitioner employee ID")
        private UUID assignedEmployeeId;

        @Schema(description = "Assigned practitioner name", example = "Rahul Sharma")
        private String assignedTo;

        @Schema(description = "Overall compliance status indicator", example = "PENDING")
        private String overallStatus;
    }
}
