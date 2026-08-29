package com.taxoryn.module.notification.whatsapp.listener;

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

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent: type={}, email={}, orgName={}, phone={}",
                event.getRegistrationType(), event.getEmail(), event.getOrganizationName(),
                event.getPhone() != null ? "***" : "none");

        try {
            whatsAppNotificationService.sendWelcomeMessage(event);
        } catch (Exception ex) {
            log.error("Unhandled error processing WhatsApp welcome notification for event {}: {}", event, ex.getMessage(), ex);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInvoiceIssued(com.taxoryn.module.notification.whatsapp.event.InvoiceIssuedEvent event) {
        log.info("Received InvoiceIssuedEvent: invoiceNumber={}, clientName={}, amount={}",
                event.getInvoiceNumber(), event.getClientName(), event.getTotalAmount());
        try {
            whatsAppNotificationService.sendInvoiceIssuedMessage(event);
        } catch (Exception ex) {
            log.error("Unhandled error processing WhatsApp invoice notification for invoice {}: {}", event.getInvoiceNumber(), ex.getMessage(), ex);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPaymentReceived(com.taxoryn.module.notification.whatsapp.event.PaymentReceivedEvent event) {
        log.info("Received PaymentReceivedEvent: invoiceNumber={}, clientName={}, amountPaid={}",
                event.getInvoiceNumber(), event.getClientName(), event.getAmountPaid());
        try {
            whatsAppNotificationService.sendPaymentReceivedMessage(event);
        } catch (Exception ex) {
            log.error("Unhandled error processing WhatsApp payment receipt notification for invoice {}: {}", event.getInvoiceNumber(), ex.getMessage(), ex);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInvoiceReminder(com.taxoryn.module.notification.whatsapp.event.InvoiceReminderEvent event) {
        log.info("Received InvoiceReminderEvent: invoiceNumber={}, clientName={}, balanceAmount={}",
                event.getInvoiceNumber(), event.getClientName(), event.getBalanceAmount());
        try {
            whatsAppNotificationService.sendInvoiceReminderMessage(event);
        } catch (Exception ex) {
            log.error("Unhandled error processing WhatsApp invoice reminder for invoice {}: {}", event.getInvoiceNumber(), ex.getMessage(), ex);
        }
    }
}
