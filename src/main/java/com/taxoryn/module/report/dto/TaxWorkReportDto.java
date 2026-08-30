package com.taxoryn.module.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Consolidated Tax Work Report across GST, ITR, and TDS")
public class TaxWorkReportDto {

    @Schema(description = "Consolidated summary rows by Tax Type (GST, ITR, TDS)")
    private List<TaxTypeSummaryDto> taxWorkSummary;

    // GST Breakdown
    @Schema(description = "Total GST clients registered")
    private long gstTotalClients;
    @Schema(description = "GST returns pending preparation")
    private long gstPending;
    @Schema(description = "GST returns under review")
    private long gstReview;
    @Schema(description = "GST returns filed")
    private long gstFiled;
    @Schema(description = "GST returns overdue")
    private long gstOverdue;
    @Schema(description = "GST filings count grouped by Return Type (e.g. GSTR-1, GSTR-3B)")
    private Map<String, Long> gstByReturnType;

    // ITR Breakdown
    @Schema(description = "Total ITR clients registered")
    private long itrTotalClients;
    @Schema(description = "ITR returns pending / docs pending")
    private long itrPending;
    @Schema(description = "ITR returns in data entry / preparation")
    private long itrPreparation;
    @Schema(description = "ITR returns under review")
    private long itrReview;
    @Schema(description = "ITR returns filed (pending verification)")
    private long itrFiled;
    @Schema(description = "ITR returns completed / verified")
    private long itrCompleted;
    @Schema(description = "ITR returns overdue")
    private long itrOverdue;
    @Schema(description = "ITR returns count grouped by ITR Form Type (ITR-1 to ITR-7)")
    private Map<String, Long> itrByFormType;

    // TDS Breakdown
    @Schema(description = "Total TDS client profiles registered")
    private long tdsTotalClients;
    @Schema(description = "TDS returns pending / draft")
    private long tdsPending;
    @Schema(description = "TDS returns with challans attached")
    private long tdsChallansAttached;
    @Schema(description = "TDS returns under review")
    private long tdsReview;
    @Schema(description = "TDS returns filed")
    private long tdsFiled;
    @Schema(description = "TDS returns overdue")
    private long tdsOverdue;
    @Schema(description = "TDS returns count grouped by Quarter (Q1, Q2, Q3, Q4)")
    private Map<String, Long> tdsByQuarter;
    @Schema(description = "TDS returns count grouped by Form Type (24Q, 26Q, 27Q, 27EQ)")
    private Map<String, Long> tdsByFormType;

    // Statutory Compliance Breakdown
    @Schema(description = "Total statutory compliance obligations")
    private long complianceTotal;
    @Schema(description = "Compliance obligations due today")
    private long complianceDueToday;
    @Schema(description = "Compliance obligations due this week")
    private long complianceDueThisWeek;
    @Schema(description = "Upcoming compliance obligations")
    private long complianceUpcoming;
    @Schema(description = "Overdue compliance obligations")
    private long complianceOverdue;
    @Schema(description = "Completed compliance obligations")
    private long complianceCompleted;
    @Schema(description = "Compliance obligations grouped by Tax Type (GST, ITR, TDS, ROC, ADVANCE_TAX)")
    private Map<String, Long> complianceByType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Summary row for a single tax type in the Tax Work Report matrix")
    public static class TaxTypeSummaryDto {
        @Schema(description = "Tax type name (GST, ITR, TDS)", example = "GST")
        private String taxType;
        @Schema(description = "Number of returns/filings in pending status")
        private long pending;
        @Schema(description = "Number of returns/filings in review status")
        private long review;
        @Schema(description = "Number of returns/filings filed/completed")
        private long filed;
        @Schema(description = "Number of returns/filings overdue")
        private long overdue;
        @Schema(description = "Total returns tracked for this tax type")
        private long total;
    }
}
