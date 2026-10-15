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

    @Schema(description = "Operating Location ID")
    private java.util.UUID locationId;

    @Schema(description = "Engagement ID")
    private java.util.UUID engagementId;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Invoice issue date")
    private LocalDate invoiceDate;

    @Schema(description = "Payment due date")
    private LocalDate dueDate;

    @Valid
    @Schema(description = "Updated line items (only allowed in DRAFT status)")
    private List<CreateInvoiceItemRequest> items;

    @Schema(description = "Notes")
    private String notes;

    @Schema(description = "Payment terms")
    private String terms;

    @Schema(description = "Place of Supply State Name (e.g. Maharashtra, Delhi)")
    private String placeOfSupplyState;

    @Schema(description = "Place of Supply 2-digit GST State Code (e.g. 27, 07)")
    private String placeOfSupplyStateCode;

    @Schema(description = "Whether tax is payable under Reverse Charge Mechanism (RCM)")
    private Boolean isRcm;

    @Schema(description = "Configurable invoice numbering series prefix", example = "INV")
    private String seriesPrefix;
}
