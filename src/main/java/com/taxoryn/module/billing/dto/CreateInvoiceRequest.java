package com.taxoryn.module.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Create Professional Tax Invoice Payload")
public class CreateInvoiceRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Target Client ID")
    private UUID clientId;

    @Schema(description = "Operating Location ID")
    private UUID locationId;

    @Schema(description = "Engagement ID")
    private UUID engagementId;

    @Schema(description = "Currency code", example = "INR")
    @Builder.Default
    private String currency = "INR";

    @Schema(description = "Custom invoice number (leave blank for automatic sequence generation)", example = "INV-2026-0001")
    private String invoiceNumber;

    @NotNull(message = "Invoice date is required")
    @Schema(description = "Invoice issue date", example = "2026-08-20")
    private LocalDate invoiceDate;

    @NotNull(message = "Due date is required")
    @Schema(description = "Payment due date", example = "2026-09-05")
    private LocalDate dueDate;

    @Schema(description = "Discount amount", example = "0.00")
    private BigDecimal discount;

    @Schema(description = "Invoice status (DRAFT or ISSUED)", example = "DRAFT")
    private com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus status;

    @NotEmpty(message = "Invoice must contain at least one line item")
    @Valid
    @Schema(description = "List of professional service line items")
    private List<CreateInvoiceItemRequest> items;

    @Schema(description = "Additional client notes or memo")
    private String notes;

    @Schema(description = "Payment terms and bank details", example = "Payment due within 15 days of invoice. Bank transfer details: Apex CA, HDFC Bank, A/C: 50200012345678, IFSC: HDFC0001234")
    private String terms;
}
