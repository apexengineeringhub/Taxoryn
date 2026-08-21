package com.taxoryn.module.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk Invoice Generation Batch Result")
public class BulkInvoiceResultDto {

    @Schema(description = "Total clients processed")
    private int totalProcessed;

    @Schema(description = "Total invoices created")
    private int totalCreated;

    @Schema(description = "Total invoices skipped")
    private int totalSkipped;

    @Schema(description = "Total invoices failed")
    private int totalFailed;

    @Schema(description = "Total cumulative billed amount in INR")
    @Builder.Default
    private BigDecimal totalBilledAmount = BigDecimal.ZERO;

    @Schema(description = "List of created invoices")
    @Builder.Default
    private List<InvoiceDto> createdInvoices = new ArrayList<>();

    @Schema(description = "Errors encountered during batch processing")
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
