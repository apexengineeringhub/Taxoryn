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
    void sendOrganizationActivationEmail(String recipientEmail, String recipientName, String practiceName, String activationUrl, long expiryHours);
    void sendEmployeeInvitationEmail(String recipientEmail, String recipientName, String practiceName, String designation, String activationUrl, long expiryHours);
    void sendClientPortalInvitationEmail(String recipientEmail, String recipientName, String clientName, String practiceName, String activationUrl, long expiryHours);
    void sendClientPortalSuspendedEmail(String recipientEmail, String recipientName, String clientName, String practiceName);
    void sendClientPortalRestoredEmail(String recipientEmail, String recipientName, String clientName, String practiceName, String loginUrl);
    void sendClientPortalDeactivatedEmail(String recipientEmail, String recipientName, String clientName, String practiceName);
    void sendDocumentRequestEmail(String recipientEmail, String clientName, String purpose, String practiceName, java.time.LocalDate dueDate, String message, java.util.List<String> itemTitles);
    void sendDocumentReminderEmail(String recipientEmail, String clientName, String purpose, String practiceName, java.time.LocalDate dueDate, java.util.List<String> pendingItemTitles);
    void sendDocumentRejectedEmail(String recipientEmail, String clientName, String purpose, String documentTitle, String reason, String practiceName);
    void sendCustomerEmailVerification(String recipientEmail, String recipientName, String activationUrl, long expiryHours);
}
