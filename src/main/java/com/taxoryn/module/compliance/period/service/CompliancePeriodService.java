package com.taxoryn.module.compliance.period.service;

import com.taxoryn.module.compliance.period.model.CompliancePeriod;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;

public interface CompliancePeriodService {

    /**
     * Deterministically resolves a canonical compliance period given its period type and key.
     *
     * @param periodType The period classification (e.g. MONTH, QUARTER, FINANCIAL_YEAR, ASSESSMENT_YEAR).
     * @param periodKey  The formatted period key (e.g. '2026-09', '2026-27-Q2', '2026-27').
     * @return Fully normalized CompliancePeriod instance.
     */
    CompliancePeriod resolvePeriod(CompliancePeriodType periodType, String periodKey);
}
