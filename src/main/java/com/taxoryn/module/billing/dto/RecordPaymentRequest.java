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
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Record Client Payment Receipt Payload")
public class RecordPaymentRequest {

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    @Schema(description = "Amount received in INR", example = "5000.00")
    private BigDecimal amount;

    @NotNull(message = "Payment date is required")
    @Schema(description = "Date payment was received", example = "2026-08-20")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    @Schema(description = "Payment mode (BANK_TRANSFER, UPI, NEFT_RTGS, CHEQUE, CASH, CARD, OTHER)", example = "BANK_TRANSFER")
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    @Schema(description = "Payment transaction reference number / UTR / Cheque number", example = "UTR123456789")
    private String referenceNumber;

    @Schema(description = "Receipt notes or remarks")
    private String notes;
}
