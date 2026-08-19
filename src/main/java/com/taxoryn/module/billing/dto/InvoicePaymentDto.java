package com.taxoryn.module.billing.dto;

import com.taxoryn.module.billing.entity.InvoicePaymentEntity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Invoice Payment Receipt Details")
public class InvoicePaymentDto {

    private UUID id;
    private UUID invoiceId;
    private UUID clientId;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private String notes;
    private String recordedBy;
    private Instant recordedAt;
}
