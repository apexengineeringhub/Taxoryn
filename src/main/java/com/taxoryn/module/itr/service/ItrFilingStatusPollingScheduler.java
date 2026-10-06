package com.taxoryn.module.itr.service;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.integration.ItrGovernmentIntegrationService;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Enterprise polling scheduler for submitted Income Tax Returns.
 * Periodically queries the Government Integration Framework for status updates on non-terminal returns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "itr.status.polling.enabled", havingValue = "true", matchIfMissing = true)
public class ItrFilingStatusPollingScheduler {

    public static final Set<ItrStatus> POLLABLE_STATUSES = Set.of(
            ItrStatus.VERIFICATION_PENDING
    );

    private final ItrReturnRepository returnRepository;
    private final ItrGovernmentIntegrationService itrGovIntegrationService;

    /**
     * Bounded, tenant-safe background polling of pending submitted return statuses.
     */
    @Scheduled(
            fixedDelayString = "${itr.status.polling.fixed-delay:300000}",
            initialDelayString = "${itr.status.polling.initial-delay:60000}"
    )
    public void pollSubmittedReturnStatuses() {
        log.debug("[ITR_STATUS_POLLING] Checking for active pending ITR filings...");

        List<ItrReturnEntity> pendingReturns = returnRepository.findAllByStatusIn(POLLABLE_STATUSES);
        if (pendingReturns == null || pendingReturns.isEmpty()) {
            return;
        }

        log.info("[ITR_STATUS_POLLING] Found {} ITR returns eligible for status polling", pendingReturns.size());

        for (ItrReturnEntity returnEntity : pendingReturns) {
            try {
                TenantContext.setTenantId(returnEntity.getOrganizationId());
                itrGovIntegrationService.checkReturnStatus(returnEntity.getId());
            } catch (Exception e) {
                log.warn("[ITR_STATUS_POLLING_ERROR] Failed to poll status for return id={}: {}",
                        returnEntity.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
