package com.taxoryn.module.notification.whatsapp.event;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class InvoiceIssuedEvent {
    UUID organizationId;
    UUID invoiceId;
    UUID clientId;
    String clientName;
    String clientPhone;
    String clientEmail;
    String organizationName;
    String invoiceNumber;
    BigDecimal totalAmount;
    BigDecimal balanceAmount;
    String currency;
    LocalDate issueDate;
    LocalDate dueDate;
    String pdfUrl;
}
