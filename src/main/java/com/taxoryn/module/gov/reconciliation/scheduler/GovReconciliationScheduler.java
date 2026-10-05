package com.taxoryn.module.gov.reconciliation.scheduler;

import com.taxoryn.module.gov.reconciliation.service.GovernmentOperationReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled background worker that initiates bounded batch reconciliation of in-flight operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GovReconciliationScheduler {

    private final GovernmentOperationReconciliationService reconciliationService;

    @Value("${taxoryn.gov.reconciliation.enabled:true}")
    private boolean reconciliationEnabled;

    @Value("${taxoryn.gov.reconciliation.batch-size:20}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${taxoryn.gov.reconciliation.interval-ms:30000}")
    public void scheduledReconciliation() {
        if (!reconciliationEnabled) {
            return;
        }

        try {
            log.debug("[GOV_RECONCILIATION_CRON_START] Triggering scheduled batch reconciliation (batchSize={})", batchSize);
            reconciliationService.reconcileBatch(batchSize);
        } catch (Exception ex) {
            log.error("[GOV_RECONCILIATION_CRON_ERROR] Error in scheduled government reconciliation: {}", ex.getMessage(), ex);
        }
    }
}
