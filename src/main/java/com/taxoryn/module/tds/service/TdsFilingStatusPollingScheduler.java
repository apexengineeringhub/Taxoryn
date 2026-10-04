package com.taxoryn.module.tds.service;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.tds.entity.TdsReturnEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.integration.TdsGovernmentIntegrationService;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Enterprise polling scheduler for submitted TDS Returns.
 * Periodically queries the Government Integration Framework for status updates on non-terminal submitted returns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tds.status.polling.enabled", havingValue = "true", matchIfMissing = true)
public class TdsFilingStatusPollingScheduler {

    public static final Set<TdsFilingStatus> POLLABLE_STATUSES = Set.of(
            TdsFilingStatus.SUBMITTED,
            TdsFilingStatus.SUBMISSION_IN_PROGRESS
    );

    private final TdsReturnRepository returnRepository;
    private final TdsGovernmentIntegrationService tdsGovIntegrationService;

    /**
     * Bounded, tenant-safe background polling of pending submitted TDS return statuses.
     */
    @Scheduled(
            fixedDelayString = "${tds.status.polling.fixed-delay:300000}",
            initialDelayString = "${tds.status.polling.initial-delay:60000}"
    )
    public void pollSubmittedReturnStatuses() {
        log.debug("[TDS_STATUS_POLLING] Checking for active pending TDS filings...");

        List<TdsReturnEntity> pendingReturns = returnRepository.findAllByFilingStatusIn(POLLABLE_STATUSES);
        if (pendingReturns == null || pendingReturns.isEmpty()) {
            return;
        }

        log.info("[TDS_STATUS_POLLING] Found {} TDS returns eligible for status polling", pendingReturns.size());

        for (TdsReturnEntity returnEntity : pendingReturns) {
            try {
                TenantContext.setTenantId(returnEntity.getOrganizationId());
                tdsGovIntegrationService.checkReturnStatus(returnEntity.getId());
            } catch (Exception e) {
                log.warn("[TDS_STATUS_POLLING_ERROR] Failed to poll status for return id={}: {}",
                        returnEntity.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
