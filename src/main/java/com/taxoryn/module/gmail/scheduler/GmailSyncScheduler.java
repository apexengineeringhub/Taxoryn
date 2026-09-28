package com.taxoryn.module.gmail.scheduler;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gmail.config.GmailProperties;
import com.taxoryn.module.gmail.entity.GmailAccountEntity;
import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.entity.GmailSyncType;
import com.taxoryn.module.gmail.repository.GmailAccountRepository;
import com.taxoryn.module.gmail.service.GmailSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled background job for periodic metadata synchronization across all connected Gmail mailboxes.
 * Ensures strict tenant context isolation and graceful error recovery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GmailSyncScheduler {

    private final GmailProperties properties;
    private final GmailSyncService syncService;
    private final GmailAccountRepository accountRepository;

    /**
     * Periodic background synchronization job to fetch email metadata across all connected mailboxes.
     * Defaults to every 15 minutes.
     */
    @Scheduled(cron = "${taxoryn.gmail.sync.cron:0 */15 * * * ?}")
    public void runPeriodicSync() {
        if (!properties.isEnabled() || properties.getSync() == null || !properties.getSync().isEnabled()) {
            log.debug("Gmail periodic background sync is disabled. Skipping.");
            return;
        }

        log.info("Starting scheduled background Gmail synchronization");
        List<GmailAccountEntity> connectedAccounts = accountRepository.findAll().stream()
                .filter(a -> a.getStatus() == GmailAccountStatus.CONNECTED)
                .toList();

        int totalSyncedThreads = 0;
        int successCount = 0;
        int failureCount = 0;

        for (GmailAccountEntity account : connectedAccounts) {
            try {
                TenantContext.setTenantId(account.getOrganizationId());
                var result = syncService.syncAccount(account.getOrganizationId(), account.getId(), GmailSyncType.INCREMENTAL);
                if (result.getStatus() == com.taxoryn.module.gmail.entity.GmailSyncStatus.SUCCESS) {
                    successCount++;
                    totalSyncedThreads += result.getThreadsSynced();
                } else {
                    failureCount++;
                }
            } catch (Exception ex) {
                failureCount++;
                log.error("Scheduled Gmail sync failed for account {} (orgId={}): {}",
                        account.getEmailAddress(), account.getOrganizationId(), ex.getMessage(), ex);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("Scheduled background Gmail synchronization finished. Mailboxes processed: {} ({} success, {} failed). Synced threads: {}",
                connectedAccounts.size(), successCount, failureCount, totalSyncedThreads);
    }
}
