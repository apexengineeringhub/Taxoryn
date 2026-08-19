package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gst.entity.GstMonthlySummaryEntity.ChallanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Save / Update GST Monthly Computation & Summary Request")
public class SaveGstMonthlySummaryRequest {

    @NotNull(message = "GST Profile ID is required")
    @Schema(description = "GST Profile ID", example = "d1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID gstProfileId;

    @NotBlank(message = "Period is required")
    @Schema(description = "Period (e.g. 2026-08)", example = "2026-08")
    private String period;

    @NotBlank(message = "Financial year is required")
    @Schema(description = "Financial Year (e.g. 2026-27)", example = "2026-27")
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
    @Schema(description = "Eligible ITC", example = "125000.00")
    private BigDecimal itcEligible;

    @Schema(description = "Ineligible ITC", example = "0.00")
    private BigDecimal itcIneligible;

    @Schema(description = "ITC reversed", example = "0.00")
    private BigDecimal itcReversed;

    @Schema(description = "Net ITC claimed", example = "125000.00")
    private BigDecimal itcNetClaimed;

    // Net Tax Liability
    @Schema(description = "Net tax liability payable", example = "82000.00")
    private BigDecimal netTaxLiability;

    @Schema(description = "Challan status")
    private ChallanStatus challanStatus;

    @Schema(description = "CPRN Number")
    private String challanCprn;

    @Schema(description = "Notes")
    private String notes;
}
