package com.taxoryn.module.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Invoice Payload")
public class UpdateInvoiceRequest {

    @Schema(description = "Payment due date")
    private LocalDate dueDate;

    @Valid
    @Schema(description = "Updated line items (only allowed in DRAFT status)")
    private List<CreateInvoiceItemRequest> items;

    @Schema(description = "Notes")
    private String notes;

    @Schema(description = "Payment terms")
    private String terms;
}
