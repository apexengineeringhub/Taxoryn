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

    @Schema(description = "Service category", example = "GST_FILING")
    @Builder.Default
    private BillingServiceType service = BillingServiceType.CONSULTING;

    @Schema(description = "Linked Work Item ID")
    private java.util.UUID workItemId;

    @Schema(description = "Linked Time Entry ID")
    private java.util.UUID timeEntryId;

    @Schema(description = "Linked Service ID")
    private java.util.UUID serviceId;

    @Schema(description = "Taxoryn service catalog code used to resolve this line's default price")
    private String serviceCode;

    @Schema(description = "Description of professional services rendered", example = "GSTR-1 & GSTR-3B preparation and filing for August 2026")
    private String description;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    @Schema(description = "Quantity / hours", example = "1")
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
    @Schema(description = "Unit price in INR", example = "2500.00")
    private BigDecimal unitPrice;

    @Schema(description = "Unit rate in INR (alias for unitPrice)", example = "2500.00")
    private BigDecimal unitRate;

    public BigDecimal getEffectiveUnitPrice() {
        if (unitPrice != null) return unitPrice;
        if (unitRate != null) return unitRate;
        return BigDecimal.ZERO;
    }

    @Schema(description = "GST tax rate percentage (e.g. 18.00)", example = "18.00")
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("18.00");
}
