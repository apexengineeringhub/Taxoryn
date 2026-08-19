package com.taxoryn.module.billing.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Invoice Search & Filter Parameters")
public class InvoiceFilterRequest extends PageRequestDto {

    @Schema(description = "Search term across invoice number and client name")
    private String search;

    @Schema(description = "Filter by Client ID")
    private UUID clientId;

    @Schema(description = "Filter by Invoice Status")
    private InvoiceStatus status;

    @Schema(description = "Filter invoices issued on or after date")
    private LocalDate startDate;

    @Schema(description = "Filter invoices issued on or before date")
    private LocalDate endDate;
}
