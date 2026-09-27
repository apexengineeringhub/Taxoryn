package com.taxoryn.module.billing.dto;

import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Invoice Details and Breakdown")
public class InvoiceDto {

    private UUID id;
    private UUID organizationId;
    private UUID locationId;
    private String locationName;
    private UUID clientId;
    private String clientName;
    private String clientGstin;
    private String clientPan;
    private UUID engagementId;
    private String engagementName;

    private String invoiceNumber;
    private String currency;
    private LocalDate invoiceDate;
    private LocalDate dueDate;

    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal total;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;

    public BigDecimal getTaxAmount() {
        return tax;
    }

    public BigDecimal getTotalAmount() {
        return total;
    }

    private InvoiceStatus status;
    private String notes;
    private String terms;

    private List<InvoiceItemDto> items;
    private List<InvoicePaymentDto> payments;

    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
