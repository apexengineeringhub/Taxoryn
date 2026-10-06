package com.taxoryn.module.gov.reliability.metrics;

import com.taxoryn.module.gov.model.GovErrorCode;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.reliability.dto.GovReliabilityMetricsSnapshot;
import com.taxoryn.module.gov.reliability.model.GovFailureClassification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe provider-neutral metrics collector for Government Integration reliability events,
 * outbox processing, and operation reconciliation cycles.
 */
@Slf4j
@Component
public class GovReliabilityMetrics {

    // Operation metrics
    private final AtomicLong operationsTotal = new AtomicLong(0);
    private final AtomicLong operationsSuccess = new AtomicLong(0);
    private final AtomicLong operationsFailed = new AtomicLong(0);
    private final AtomicLong retriesTotal = new AtomicLong(0);
    private final AtomicLong timeoutsTotal = new AtomicLong(0);
    private final AtomicLong rateLimitsTotal = new AtomicLong(0);
    private final AtomicLong providerUnavailableTotal = new AtomicLong(0);
    private final AtomicLong authFailuresTotal = new AtomicLong(0);

    // Outbox metrics
    private final AtomicLong outboxEnqueuedTotal = new AtomicLong(0);
    private final AtomicLong outboxCompletedTotal = new AtomicLong(0);
    private final AtomicLong outboxFailedTotal = new AtomicLong(0);
    private final AtomicLong outboxStaleRecoveredTotal = new AtomicLong(0);

    // Reconciliation metrics
    private final AtomicLong reconciliationsAttemptedTotal = new AtomicLong(0);
    private final AtomicLong reconciliationsSucceededTotal = new AtomicLong(0);
    private final AtomicLong reconciliationsStatusChangedTotal = new AtomicLong(0);
    private final AtomicLong reconciliationsFailedTotal = new AtomicLong(0);

    public void recordSuccess(GovProviderType providerType, String operationType, long durationMs) {
        operationsTotal.incrementAndGet();
        operationsSuccess.incrementAndGet();
        log.debug("[GOV_METRICS] Recorded SUCCESS: provider={}, op={}, duration={}ms", providerType, operationType, durationMs);
    }

    public void recordFailure(GovProviderType providerType, String operationType, GovErrorCode errorCode, GovFailureClassification classification) {
        operationsTotal.incrementAndGet();
        operationsFailed.incrementAndGet();

        if (errorCode != null) {
            switch (errorCode) {
                case TIMEOUT -> timeoutsTotal.incrementAndGet();
                case RATE_LIMITED -> rateLimitsTotal.incrementAndGet();
                case PROVIDER_UNAVAILABLE -> providerUnavailableTotal.incrementAndGet();
                case AUTH_REQUIRED, FORBIDDEN -> authFailuresTotal.incrementAndGet();
                default -> {}
            }
        }
        log.debug("[GOV_METRICS] Recorded FAILURE: provider={}, op={}, error={}, class={}",
                providerType, operationType, errorCode, classification);
    }

    public void recordRetry(GovProviderType providerType, String operationType, int attempt) {
        retriesTotal.incrementAndGet();
        log.debug("[GOV_METRICS] Recorded RETRY: provider={}, op={}, attempt={}", providerType, operationType, attempt);
    }

    // Outbox recording methods
    public void recordOutboxEnqueued() {
        outboxEnqueuedTotal.incrementAndGet();
        log.debug("[GOV_METRICS] Recorded OUTBOX ENQUEUED");
    }

    public void recordOutboxCompleted() {
        outboxCompletedTotal.incrementAndGet();
        log.debug("[GOV_METRICS] Recorded OUTBOX COMPLETED");
    }

    public void recordOutboxFailed() {
        outboxFailedTotal.incrementAndGet();
        log.debug("[GOV_METRICS] Recorded OUTBOX FAILED");
    }

    public void recordOutboxStaleRecovered() {
        outboxStaleRecoveredTotal.incrementAndGet();
        log.debug("[GOV_METRICS] Recorded OUTBOX STALE RECOVERED");
    }

    // Reconciliation recording methods
    public void recordReconciliationAttempt() {
        reconciliationsAttemptedTotal.incrementAndGet();
        log.debug("[GOV_METRICS] Recorded RECONCILIATION ATTEMPT");
    }

    public void recordReconciliationSuccess() {
        reconciliationsSucceededTotal.incrementAndGet();
        log.debug("[GOV_METRICS] Recorded RECONCILIATION SUCCESS");
    }

    public void recordReconciliationStatusChanged() {
        reconciliationsStatusChangedTotal.incrementAndGet();
        log.debug("[GOV_METRICS] Recorded RECONCILIATION STATUS CHANGED");
    }

    public void recordReconciliationFailure() {
        reconciliationsFailedTotal.incrementAndGet();
        log.debug("[GOV_METRICS] Recorded RECONCILIATION FAILURE");
    }

    public GovReliabilityMetricsSnapshot getSnapshot() {
        return GovReliabilityMetricsSnapshot.builder()
                .operationsTotal(operationsTotal.get())
                .operationsSuccess(operationsSuccess.get())
                .operationsFailed(operationsFailed.get())
                .retriesTotal(retriesTotal.get())
                .timeoutsTotal(timeoutsTotal.get())
                .rateLimitsTotal(rateLimitsTotal.get())
                .providerUnavailableTotal(providerUnavailableTotal.get())
                .authFailuresTotal(authFailuresTotal.get())
                .outboxEnqueuedTotal(outboxEnqueuedTotal.get())
                .outboxCompletedTotal(outboxCompletedTotal.get())
                .outboxFailedTotal(outboxFailedTotal.get())
                .outboxStaleRecoveredTotal(outboxStaleRecoveredTotal.get())
                .reconciliationsAttemptedTotal(reconciliationsAttemptedTotal.get())
                .reconciliationsSucceededTotal(reconciliationsSucceededTotal.get())
                .reconciliationsStatusChangedTotal(reconciliationsStatusChangedTotal.get())
                .reconciliationsFailedTotal(reconciliationsFailedTotal.get())
                .snapshotTimestamp(Instant.now())
                .build();
    }

    public void reset() {
        operationsTotal.set(0);
        operationsSuccess.set(0);
        operationsFailed.set(0);
        retriesTotal.set(0);
        timeoutsTotal.set(0);
        rateLimitsTotal.set(0);
        providerUnavailableTotal.set(0);
        authFailuresTotal.set(0);
        outboxEnqueuedTotal.set(0);
        outboxCompletedTotal.set(0);
        outboxFailedTotal.set(0);
        outboxStaleRecoveredTotal.set(0);
        reconciliationsAttemptedTotal.set(0);
        reconciliationsSucceededTotal.set(0);
        reconciliationsStatusChangedTotal.set(0);
        reconciliationsFailedTotal.set(0);
    }
}
