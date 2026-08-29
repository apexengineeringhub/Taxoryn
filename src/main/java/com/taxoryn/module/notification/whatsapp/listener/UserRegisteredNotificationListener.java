package com.taxoryn.module.notification.whatsapp.listener;

import com.taxoryn.module.notification.email.service.EmailNotificationService;
import com.taxoryn.module.notification.whatsapp.event.InvoiceIssuedEvent;
import com.taxoryn.module.notification.whatsapp.event.InvoiceReminderEvent;
import com.taxoryn.module.notification.whatsapp.event.PaymentReceivedEvent;
import com.taxoryn.module.notification.whatsapp.event.UserRegisteredEvent;
import com.taxoryn.module.notification.whatsapp.service.WhatsAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredNotificationListener {

    private final WhatsAppNotificationService whatsAppNotificationService;
    private final EmailNotificationService emailNotificationService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent: type={}, email={}, orgName={}, phone={}",
                event.getRegistrationType(), event.getEmail(), event.getOrganizationName(),
                event.getPhone() != null ? "***" : "none");

        // 1. WhatsApp Welcome Notification
        try {
            whatsAppNotificationService.sendWelcomeMessage(event);
        } catch (Exception ex) {
            log.error("Unhandled error processing WhatsApp welcome notification for event {}: {}", event, ex.getMessage(), ex);
        }

        // 2. Email Welcome Notification
        try {
            emailNotificationService.sendWelcomeEmail(event);
        } catch (Exception ex) {
            log.error("Unhandled error processing welcome email notification for event {}: {}", event, ex.getMessage(), ex);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInvoiceIssued(InvoiceIssuedEvent event) {
        log.info("Received InvoiceIssuedEvent: invoiceNumber={}, clientName={}, amount={}",
                event.getInvoiceNumber(), event.getClientName(), event.getTotalAmount());
        try {
            whatsAppNotificationService.sendInvoiceIssuedMessage(event);
        } catch (Exception ex) {
            log.error("Unhandled error processing WhatsApp invoice notification for invoice {}: {}", event.getInvoiceNumber(), ex.getMessage(), ex);
        }

        try {
            emailNotificationService.sendInvoiceIssuedEmail(event);
        } catch (Exception ex) {
            log.error("Unhandled error processing email invoice notification for invoice {}: {}", event.getInvoiceNumber(), ex.getMessage(), ex);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPaymentReceived(PaymentReceivedEvent event) {
        log.info("Received PaymentReceivedEvent: invoiceNumber={}, clientName={}, amountPaid={}",
                event.getInvoiceNumber(), event.getClientName(), event.getAmountPaid());
        try {
            whatsAppNotificationService.sendPaymentReceivedMessage(event);
        } catch (Exception ex) {
            log.error("Unhandled error processing WhatsApp payment receipt notification for invoice {}: {}", event.getInvoiceNumber(), ex.getMessage(), ex);
        }

        try {
            emailNotificationService.sendPaymentReceivedEmail(event);
        } catch (Exception ex) {
            log.error("Unhandled error processing email payment receipt notification for invoice {}: {}", event.getInvoiceNumber(), ex.getMessage(), ex);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInvoiceReminder(InvoiceReminderEvent event) {
        log.info("Received InvoiceReminderEvent: invoiceNumber={}, clientName={}, balanceAmount={}",
                event.getInvoiceNumber(), event.getClientName(), event.getBalanceAmount());
        try {
            whatsAppNotificationService.sendInvoiceReminderMessage(event);
        } catch (Exception ex) {
            log.error("Unhandled error processing WhatsApp invoice reminder for invoice {}: {}", event.getInvoiceNumber(), ex.getMessage(), ex);
        }

        try {
            emailNotificationService.sendInvoiceReminderEmail(event);
        } catch (Exception ex) {
            log.error("Unhandled error processing email invoice reminder for invoice {}: {}", event.getInvoiceNumber(), ex.getMessage(), ex);
        }
    }
}

