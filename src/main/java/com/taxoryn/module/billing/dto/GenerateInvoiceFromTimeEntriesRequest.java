package com.taxoryn.module.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Generate Invoice From Unbilled Time Entries Request")
public class GenerateInvoiceFromTimeEntriesRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Client ID to invoice")
    private UUID clientId;

    @Schema(description = "Location ID")
    private UUID locationId;

    @Schema(description = "Engagement ID")
    private UUID engagementId;

    @NotEmpty(message = "At least one time entry ID is required")
    @Schema(description = "List of unbilled time entry IDs to convert into invoice line items")
    private List<UUID> timeEntryIds;

    @NotNull(message = "Invoice date is required")
    @Schema(description = "Invoice date", example = "2026-08-20")
    private LocalDate invoiceDate;

    @NotNull(message = "Due date is required")
    @Schema(description = "Due date", example = "2026-09-05")
    private LocalDate dueDate;

    @Schema(description = "Tax rate percentage (default 18.00)", example = "18.00")
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("18.00");

    @Schema(description = "Discount amount", example = "0.00")
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Schema(description = "Currency", example = "INR")
    @Builder.Default
    private String currency = "INR";

    @Schema(description = "Invoice notes or memo")
    private String notes;

    @Schema(description = "Terms and conditions")
    private String terms;
}
