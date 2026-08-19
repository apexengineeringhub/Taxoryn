package com.taxoryn.module.billing.dto;

import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create Invoice Line Item Payload")
public class CreateInvoiceItemRequest {

    @NotNull(message = "Billing service is required")
    @Schema(description = "Service category", example = "GST_FILING")
    private BillingServiceType service;

    @Schema(description = "Description of professional services rendered", example = "GSTR-1 & GSTR-3B preparation and filing for August 2026")
    private String description;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    @Schema(description = "Quantity / hours", example = "1")
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
    @Schema(description = "Unit price in INR", example = "2500.00")
    private BigDecimal unitPrice;

    @Schema(description = "GST tax rate percentage (e.g. 18.00)", example = "18.00")
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("18.00");
}
