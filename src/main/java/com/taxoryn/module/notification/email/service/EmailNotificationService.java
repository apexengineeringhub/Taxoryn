package com.taxoryn.module.notification.email.service;

import com.taxoryn.module.notification.whatsapp.event.InvoiceIssuedEvent;
import com.taxoryn.module.notification.whatsapp.event.InvoiceReminderEvent;
import com.taxoryn.module.notification.whatsapp.event.PaymentReceivedEvent;
import com.taxoryn.module.notification.whatsapp.event.UserRegisteredEvent;

public interface EmailNotificationService {

    void sendWelcomeEmail(UserRegisteredEvent event);

    void sendInvoiceIssuedEmail(InvoiceIssuedEvent event);

    void sendPaymentReceivedEmail(PaymentReceivedEvent event);

    void sendInvoiceReminderEmail(InvoiceReminderEvent event);

    void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetUrl, long expiryMinutes);
}
