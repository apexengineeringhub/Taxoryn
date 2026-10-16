package com.taxoryn.module.billing.service;

import com.taxoryn.module.billing.entity.PracticeInvoiceSeriesEntity;
import com.taxoryn.module.billing.repository.PracticeInvoiceSeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeInvoiceSeriesService {

    private final PracticeInvoiceSeriesRepository practiceInvoiceSeriesRepository;

    /**
     * Guarantees that a series row exists in the database.
     * Uses REQUIRES_NEW propagation so any concurrent insert race condition and resultant
     * unique constraint conflict does not mark the caller's transaction rollback-only.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureSeriesExists(UUID organizationId, String financialYear, String seriesPrefix) {
        if (practiceInvoiceSeriesRepository.findByOrganizationIdAndFinancialYearAndSeriesPrefix(organizationId, financialYear, seriesPrefix).isPresent()) {
            return;
        }

        try {
            PracticeInvoiceSeriesEntity newSeries = PracticeInvoiceSeriesEntity.builder()
                    .financialYear(financialYear)
                    .seriesPrefix(seriesPrefix)
                    .delimiter("-")
                    .currentSequence(0L)
                    .build();
            newSeries.setOrganizationId(organizationId);
            practiceInvoiceSeriesRepository.saveAndFlush(newSeries);
            log.info("Initialized invoice series: org={}, fy={}, prefix={}", organizationId, financialYear, seriesPrefix);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Invoice series was concurrently created: org={}, fy={}, prefix={}", organizationId, financialYear, seriesPrefix);
        } catch (Exception ex) {
            log.warn("Handled series initialization exception: org={}, fy={}, prefix={}, error={}", organizationId, financialYear, seriesPrefix, ex.getMessage());
        }
    }
}
