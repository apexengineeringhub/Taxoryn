package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.entity.GstMonthlySummaryEntity.ChallanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GST Monthly Summary (Sales, Purchase, ITC, Tax Liability)")
public class GstMonthlySummaryDto {

    @Schema(description = "Summary ID")
    private UUID id;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "GST Profile ID")
    private UUID gstProfileId;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client display name", example = "ABC Traders")
    private String clientName;

    @Schema(description = "GSTIN", example = "27AAACZ1234D1Z8")
    private String gstin;

    @Schema(description = "Period (e.g. 2026-08)", example = "2026-08")
    private String period;

    @Schema(description = "Financial Year", example = "2026-27")
    private String financialYear;

    // Sales Breakdown
    @Schema(description = "Total taxable sales value", example = "1000000.00")
    private BigDecimal totalSalesTaxable;

    @Schema(description = "IGST on sales", example = "80000.00")
    private BigDecimal igstSales;

    @Schema(description = "CGST on sales", example = "50000.00")
    private BigDecimal cgstSales;

    @Schema(description = "SGST on sales", example = "50000.00")
    private BigDecimal sgstSales;

    @Schema(description = "Cess on sales", example = "0.00")
    private BigDecimal cessSales;

    // Purchase Breakdown
    @Schema(description = "Total taxable purchase value", example = "600000.00")
    private BigDecimal totalPurchaseTaxable;

    @Schema(description = "IGST on purchase", example = "40000.00")
    private BigDecimal igstPurchase;

    @Schema(description = "CGST on purchase", example = "34000.00")
    private BigDecimal cgstPurchase;

    @Schema(description = "SGST on purchase", example = "34000.00")
    private BigDecimal sgstPurchase;

    @Schema(description = "Cess on purchase", example = "0.00")
    private BigDecimal cessPurchase;

    // ITC Tracking
    @Schema(description = "Total eligible ITC available", example = "125000.00")
    private BigDecimal itcEligible;

    @Schema(description = "Ineligible ITC (e.g. Section 17(5))", example = "0.00")
    private BigDecimal itcIneligible;

    @Schema(description = "ITC reversed (e.g. Rule 42/43)", example = "0.00")
    private BigDecimal itcReversed;

    @Schema(description = "Net ITC claimed", example = "125000.00")
    private BigDecimal itcNetClaimed;

    // Net Tax Liability
    @Schema(description = "Net tax liability payable in cash", example = "82000.00")
    private BigDecimal netTaxLiability;

    @Schema(description = "Payment challan status", example = "NOT_GENERATED")
    private ChallanStatus challanStatus;

    @Schema(description = "GST PMT-06 Challan CPRN Number")
    private String challanCprn;

    @Schema(description = "Internal computation notes")
    private String notes;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
