package com.taxoryn.module.gst.service;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.integration.GstGovernmentIntegrationService;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Enterprise polling scheduler for submitted GST returns.
 * Periodically queries the Government Integration Framework for status updates on non-terminal filings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "gst.status.polling.enabled", havingValue = "true", matchIfMissing = true)
public class GstFilingStatusPollingScheduler {

    public static final Set<GstFilingStatus> POLLABLE_STATUSES = Set.of(
            GstFilingStatus.SUBMITTED,
            GstFilingStatus.PROCESSING,
            GstFilingStatus.SUBMISSION_IN_PROGRESS
    );

    private final GstReturnFilingRepository filingRepository;
    private final GstGovernmentIntegrationService gstGovIntegrationService;

    /**
     * Bounded, tenant-safe background polling of pending submitted return statuses.
     */
    @Scheduled(
            fixedDelayString = "${gst.status.polling.fixed-delay:300000}",
            initialDelayString = "${gst.status.polling.initial-delay:60000}"
    )
    public void pollSubmittedFilingStatuses() {
        log.debug("[GST_STATUS_POLLING] Checking for active submitted GST filings...");

        List<GstReturnFilingEntity> pendingFilings = filingRepository.findAllByFilingStatusIn(POLLABLE_STATUSES);
        if (pendingFilings == null || pendingFilings.isEmpty()) {
            return;
        }

        log.info("[GST_STATUS_POLLING] Found {} filings eligible for status polling", pendingFilings.size());

        for (GstReturnFilingEntity filing : pendingFilings) {
            try {
                TenantContext.setTenantId(filing.getOrganizationId());
                gstGovIntegrationService.checkReturnStatus(filing.getId());
            } catch (Exception e) {
                log.warn("[GST_STATUS_POLLING_ERROR] Failed to poll status for filing id={}: {}",
                        filing.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
