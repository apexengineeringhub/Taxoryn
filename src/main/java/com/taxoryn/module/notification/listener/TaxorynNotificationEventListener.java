package com.taxoryn.module.notification.listener;

import com.taxoryn.module.notification.event.TaxorynNotificationEvent;
import com.taxoryn.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaxorynNotificationEventListener {

    private final NotificationService notificationService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTaxorynNotification(TaxorynNotificationEvent event) {
        log.info("Received TaxorynNotificationEvent: type={}, severity={}, category={}, org={}, userId={}, clientId={}",
                event.getNotificationType(), event.getSeverity(), event.getCategory(),
                event.getOrganizationId(), event.getUserId(), event.getClientId());

        try {
            notificationService.notify(
                    event.getOrganizationId(),
                    event.getUserId(),
                    event.getClientId(),
                    event.getNotificationType(),
                    event.getSeverity(),
                    event.getCategory(),
                    event.getEntityType(),
                    event.getEntityId(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getChannels(),
                    event.getActionUrl(),
                    event.getMetadata(),
                    event.getExpiresAt()
            );
        } catch (Exception ex) {
            log.error("Failed to handle TaxorynNotificationEvent {}: {}", event, ex.getMessage(), ex);
        }
    }
}