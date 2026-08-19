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
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Invoice Line Item Details")
public class InvoiceItemDto {

    private UUID id;
    private BillingServiceType service;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;
    private BigDecimal tax;
    private BigDecimal amount;
}
