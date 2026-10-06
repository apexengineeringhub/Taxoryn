package com.taxoryn.module.gov.outbox.processor;

import com.taxoryn.module.gov.outbox.entity.GovOutboxEventEntity;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;
import com.taxoryn.module.gov.outbox.repository.GovOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Scheduled background processor that claims pending outbox events using atomic status updates,
 * delegates execution to isolated transactions, and recovers stale locks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GovOutboxProcessor {

    private final GovOutboxEventRepository outboxRepository;
    private final GovOutboxEventExecutor eventExecutor;
    private final com.taxoryn.module.gov.reliability.metrics.GovReliabilityMetrics reliabilityMetrics;

    @Value("${taxoryn.gov.outbox.enabled:true}")
    private boolean outboxEnabled;

    @Value("${taxoryn.gov.outbox.batch-size:20}")
    private int defaultBatchSize;

    @Value("${taxoryn.gov.outbox.stale-threshold-seconds:300}")
    private long staleThresholdSeconds;

    @Scheduled(fixedDelayString = "${taxoryn.gov.outbox.fixed-delay-ms:5000}")
    public void scheduledOutboxPolling() {
        if (!outboxEnabled) {
            return;
        }
        try {
            processPendingBatch(defaultBatchSize);
        } catch (Exception ex) {
            log.error("[GOV_OUTBOX_POLLING_ERROR] Unexpected error during scheduled outbox polling: {}", ex.getMessage(), ex);
        }
    }

    @Scheduled(fixedDelayString = "${taxoryn.gov.outbox.stale-check-delay-ms:30000}")
    public void scheduledStaleLockRecovery() {
        if (!outboxEnabled) {
            return;
        }
        try {
            recoverStaleLocks(Duration.ofSeconds(staleThresholdSeconds), defaultBatchSize);
        } catch (Exception ex) {
            log.error("[GOV_OUTBOX_STALE_CHECK_ERROR] Unexpected error during stale lock recovery: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Polls and processes a bounded batch of pending outbox events.
     * Uses atomic claim to ensure no duplicate executions across concurrent workers.
     *
     * @param batchSize maximum number of events to process in this iteration
     * @return number of successfully claimed and dispatched events
     */
    public int processPendingBatch(int batchSize) {
        int limit = Math.max(1, Math.min(batchSize, 100));
        Instant now = Instant.now();

        List<GovOutboxEventEntity> pendingEvents = outboxRepository.findPendingEvents(
                GovOutboxStatus.PENDING,
                now,
                PageRequest.of(0, limit)
        );

        if (pendingEvents.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        for (GovOutboxEventEntity candidate : pendingEvents) {
            Instant lockTime = Instant.now();
            int claimed = outboxRepository.claimEvent(
                    candidate.getId(),
                    GovOutboxStatus.PENDING,
                    GovOutboxStatus.PROCESSING,
                    lockTime,
                    lockTime
            );

            if (claimed > 0) {
                try {
                    boolean success = eventExecutor.executeClaimedEvent(candidate.getId());
                    if (success) {
                        processedCount++;
                    }
                } catch (Exception e) {
                    log.error("[GOV_OUTBOX_EVENT_EXEC_ERROR] Failed to execute claimed event {}: {}",
                            candidate.getId(), e.getMessage(), e);
                }
            } else {
                log.debug("[GOV_OUTBOX_CLAIM_SKIPPED] Event {} already claimed by another worker", candidate.getId());
            }
        }

        return processedCount;
    }

    /**
     * Scans for outbox events stuck in PROCESSING status past the stale threshold and resets them.
     *
     * @param staleThreshold duration after which a locked event is deemed stale
     * @param batchSize maximum number of stale events to recover in one batch
     * @return number of stale events recovered
     */
    public int recoverStaleLocks(Duration staleThreshold, int batchSize) {
        int limit = Math.max(1, Math.min(batchSize, 100));
        Instant staleBefore = Instant.now().minus(staleThreshold);

        List<GovOutboxEventEntity> staleEvents = outboxRepository.findStaleLockedEvents(
                GovOutboxStatus.PROCESSING,
                staleBefore,
                PageRequest.of(0, limit)
        );

        if (staleEvents.isEmpty()) {
            return 0;
        }

        int recoveredCount = 0;
        for (GovOutboxEventEntity staleCandidate : staleEvents) {
            try {
                boolean recovered = eventExecutor.resetStaleEvent(staleCandidate.getId());
                if (recovered) {
                    recoveredCount++;
                    reliabilityMetrics.recordOutboxStaleRecovered();
                }
            } catch (Exception e) {
                log.error("[GOV_OUTBOX_STALE_RECOVERY_ERROR] Failed to recover stale event {}: {}",
                        staleCandidate.getId(), e.getMessage(), e);
            }
        }

        return recoveredCount;
    }
}
