package com.taxoryn.module.reminder.event;

import com.taxoryn.module.reminder.entity.AutomationRuleEntity;
import com.taxoryn.module.reminder.repository.AutomationRuleRepository;
import com.taxoryn.module.reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Listens for {@link TaxorynBusinessEvent} published after a transactional commit
 * and evaluates applicable automation rules to create reminders.
 *
 * <p>Mirrors the pattern of {@code TaxorynNotificationEventListener}:
 * <ul>
 *   <li>{@code @Async("notificationExecutor")} — runs on the shared async pool, not the caller thread</li>
 *   <li>{@code @TransactionalEventListener(AFTER_COMMIT)} — only fires if the originating transaction committed</li>
 *   <li>{@code fallbackExecution = true} — also fires if published outside a transaction context</li>
 * </ul>
 *
 * <p>For each matching enabled rule, the listener calls
 * {@link ReminderService#createAutomatedReminder} which handles idempotency internally.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderEventListener {

    private final AutomationRuleRepository automationRuleRepository;
    private final ReminderService reminderService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBusinessEvent(TaxorynBusinessEvent event) {
        log.debug("Received TaxorynBusinessEvent: type={}, org={}, taskId={}, assignedUser={}",
                event.getEventType(), event.getOrganizationId(), event.getTaskId(), event.getAssignedUserId());

        if (event.getOrganizationId() == null) {
            log.warn("Ignoring business event with null organizationId: {}", event.getEventType());
            return;
        }

        try {
            // Find all enabled rules matching this event type + org (including system defaults)
            List<AutomationRuleEntity> matchingRules = automationRuleRepository
                    .findEnabledByEventTypeForOrganization(event.getEventType(), event.getOrganizationId());

            if (matchingRules.isEmpty()) {
                log.debug("No automation rules match event {} for org {}", event.getEventType(), event.getOrganizationId());
                return;
            }

            log.info("Processing {} automation rule(s) for event {} in org {}",
                    matchingRules.size(), event.getEventType(), event.getOrganizationId());

            for (AutomationRuleEntity rule : matchingRules) {
                try {
                    reminderService.createAutomatedReminder(event, rule);
                } catch (Exception ex) {
                    // Per-rule isolation: one rule's failure does not block others
                    log.error("Automation rule '{}' ({}) failed for event {}: {}",
                            rule.getName(), rule.getId(), event.getEventType(), ex.getMessage(), ex);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to process business event {} for org {}: {}",
                    event.getEventType(), event.getOrganizationId(), ex.getMessage(), ex);
        }
    }
}
