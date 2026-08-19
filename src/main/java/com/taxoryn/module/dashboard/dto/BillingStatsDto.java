package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization Billing Statistics")
public class BillingStatsDto {

    @Schema(description = "Total billed amount across active invoices", example = "500000.00")
    private BigDecimal totalInvoiceAmount;

    @Schema(description = "Total collected / paid amount", example = "420000.00")
    private BigDecimal paidAmount;

    @Schema(description = "Total outstanding balance due", example = "80000.00")
    private BigDecimal outstandingAmount;
}
