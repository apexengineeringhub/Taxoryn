package com.taxoryn.module.notification.whatsapp.event;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class InvoiceReminderEvent {
    UUID organizationId;
    UUID invoiceId;
    UUID clientId;
    String clientName;
    String clientEmail;
    String clientPhone;
    String organizationName;
    String invoiceNumber;
    BigDecimal totalAmount;
    BigDecimal balanceAmount;
    String currency;
    LocalDate dueDate;
    int overdueDays;
}
