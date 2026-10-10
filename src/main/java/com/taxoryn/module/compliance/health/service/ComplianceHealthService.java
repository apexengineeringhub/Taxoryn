package com.taxoryn.module.compliance.health.service;

import com.taxoryn.module.compliance.health.dto.ClientComplianceHealthDto;
import com.taxoryn.module.compliance.health.dto.PracticeComplianceHealthSummaryDto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for computing deterministic, explainable Compliance Health & Intelligence.
 */
public interface ComplianceHealthService {

    /**
     * Computes the compliance health and intelligence profile for a specific client.
     *
     * @param clientId      target client UUID
     * @param referenceDate business reference date (defaults to today if null)
     * @return comprehensive deterministic health report
     */
    ClientComplianceHealthDto getClientComplianceHealth(UUID clientId, LocalDate referenceDate);

    /**
     * Computes practice-wide compliance health summary for the authenticated tenant.
     *
     * @param referenceDate business reference date (defaults to today if null)
     * @return practice-wide overview
     */
    PracticeComplianceHealthSummaryDto getPracticeComplianceHealth(LocalDate referenceDate);
}
