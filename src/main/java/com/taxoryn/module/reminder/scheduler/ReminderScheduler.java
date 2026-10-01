package com.taxoryn.module.reminder.scheduler;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled job that processes due reminders — finds all PENDING reminders
 * whose {@code scheduledAt ≤ now}, creates notifications, and marks them TRIGGERED.
 *
 * <p>Runs daily at 07:00 AM, matching the existing {@code NotificationScheduler} cadence.
 * Uses the same per-tenant iteration pattern:
 * <ol>
 *   <li>Load all active organizations</li>
 *   <li>For each org: set {@code TenantContext} → process → clear context</li>
 *   <li>Per-org try-catch for failure isolation</li>
 * </ol>
 *
 * <p><strong>Idempotency:</strong> Each reminder is processed at most once because
 * {@code processAllDueReminders()} only selects PENDING reminders and atomically
 * transitions them to TRIGGERED. Re-running the scheduler does not produce duplicate
 * notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final OrganizationRepository organizationRepository;
    private final ReminderService reminderService;

    /**
     * Daily reminder processing — runs at 07:05 AM (5 minutes after NotificationScheduler
     * to stagger load and avoid lock contention on shared tables).
     */
    @Scheduled(cron = "0 5 7 * * ?")
    public void processDueReminders() {
        log.info("Starting daily reminder processing");

        List<OrganizationEntity> activeOrgs = organizationRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrganizationStatus.ACTIVE)
                .toList();

        int totalTriggered = 0;

        for (OrganizationEntity org : activeOrgs) {
            try {
                TenantContext.setTenantId(org.getId());
                int triggered = reminderService.processAllDueReminders(org.getId());
                totalTriggered += triggered;
                if (triggered > 0) {
                    log.info("Triggered {} reminder(s) for org {} ({})", triggered, org.getName(), org.getId());
                }
            } catch (Exception ex) {
                log.error("Reminder processing failed for org {} ({}): {}",
                        org.getName(), org.getId(), ex.getMessage(), ex);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("Daily reminder processing completed: {} reminder(s) triggered across {} org(s)",
                totalTriggered, activeOrgs.size());
    }
}
