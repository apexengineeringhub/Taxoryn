package com.taxoryn.module.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk Invoice Generation Request Payload")
public class BulkCreateInvoicesRequest {

    @Schema(description = "Target Client IDs (leave empty or null to generate for all active practice clients)")
    private List<UUID> clientIds;

    @NotNull(message = "Invoice date is required")
    @Schema(description = "Invoice issue date", example = "2026-08-20")
    private LocalDate invoiceDate;

    @NotNull(message = "Due date is required")
    @Schema(description = "Payment due date", example = "2026-09-05")
    private LocalDate dueDate;

    @NotEmpty(message = "Invoice must contain at least one line item")
    @Valid
    @Schema(description = "List of professional service line items to apply to each client")
    private List<CreateInvoiceItemRequest> items;

    @Schema(description = "Automatically issue invoices upon creation", defaultValue = "true")
    @Builder.Default
    private boolean autoIssue = true;

    @Schema(description = "Additional client notes or memo")
    private String notes;

    @Schema(description = "Payment terms and bank details")
    private String terms;
}
