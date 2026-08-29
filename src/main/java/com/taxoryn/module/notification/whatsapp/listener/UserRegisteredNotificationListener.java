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
}
