package com.taxoryn.module.notification.whatsapp.event;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class PaymentReceivedEvent {
    UUID organizationId;
    UUID invoiceId;
    UUID paymentId;
    UUID clientId;
    String clientName;
    String clientPhone;
    String clientEmail;
    String organizationName;
    String invoiceNumber;
    String paymentReference;
    BigDecimal amountPaid;
    BigDecimal remainingBalance;
    String currency;
    LocalDate paymentDate;
    String paymentMethod;
}
