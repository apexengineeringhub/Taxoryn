package com.taxoryn.module.reminder.scheduler;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled job that processes due reminders — finds PENDING reminders
 * whose {@code scheduledAt ≤ now}, creates notifications with retry handling,
 * and spawns subsequent recurring occurrences.
 *
 * <p>Configurable via {@code taxoryn.reminder.scheduler.*} properties in application.yml.
 * Uses bounded per-tenant batch processing to prevent memory or connection exhaustion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "taxoryn.reminder.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private final OrganizationRepository organizationRepository;
    private final ReminderService reminderService;

    @Value("${taxoryn.reminder.scheduler.batch-size:50}")
    private int batchSize;

    /**
     * Bounded reminder processing loop.
     * Cron expression is externalized via application configuration.
     */
    @Scheduled(cron = "${taxoryn.reminder.scheduler.cron:0 5 7 * * ?}")
    public void processDueReminders() {
        log.info("Starting reminder processing run (batchSize={})", batchSize);

        List<OrganizationEntity> activeOrgs = organizationRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrganizationStatus.ACTIVE)
                .toList();

        int totalTriggered = 0;

        for (OrganizationEntity org : activeOrgs) {
            try {
                TenantContext.setTenantId(org.getId());
                int triggered = reminderService.processAllDueReminders(org.getId(), batchSize);
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

        log.info("Reminder processing run completed: {} reminder(s) triggered across {} org(s)",
                totalTriggered, activeOrgs.size());
    }
}
