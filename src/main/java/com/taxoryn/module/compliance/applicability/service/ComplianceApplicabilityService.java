package com.taxoryn.module.compliance.applicability.service;

import com.taxoryn.module.compliance.applicability.dto.ClientComplianceApplicabilityDto;
import com.taxoryn.module.compliance.applicability.dto.ComplianceApplicabilitySummaryDto;
import com.taxoryn.module.compliance.applicability.dto.EvaluatedRuleApplicabilityDto;
import com.taxoryn.module.compliance.applicability.model.ApplicabilityResultState;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Service orchestrating dynamic, deterministic compliance applicability evaluation
 * for clients against the statutory and practice compliance rule catalog.
 */
public interface ComplianceApplicabilityService {

    /**
     * Evaluates all catalog compliance rules for a client with optional filtering.
     *
     * @param clientId       client UUID
     * @param evaluationDate statutory evaluation date (defaults to today)
     * @param domainFilter   optional domain filter (e.g. GST, TDS, INCOME_TAX)
     * @param statusFilter   optional result state filter (e.g. APPLICABLE, INSUFFICIENT_DATA)
     * @return full client applicability report
     */
    ClientComplianceApplicabilityDto evaluateClientApplicability(
            UUID clientId,
            LocalDate evaluationDate,
            ComplianceRuleDomain domainFilter,
            ApplicabilityResultState statusFilter
    );

    /**
     * Evaluates a single specific compliance rule by rule code for a client.
     *
     * @param clientId       client UUID
     * @param ruleCode       unique standard or custom rule code
     * @param evaluationDate statutory evaluation date (defaults to today)
     * @return evaluated rule applicability result
     */
    EvaluatedRuleApplicabilityDto evaluateClientRule(
            UUID clientId,
            String ruleCode,
            LocalDate evaluationDate
    );

    /**
     * Computes a compact summary of a client's compliance applicability.
     *
     * @param clientId       client UUID
     * @param evaluationDate statutory evaluation date (defaults to today)
     * @return compact summary DTO
     */
    ComplianceApplicabilitySummaryDto getApplicabilitySummary(
            UUID clientId,
            LocalDate evaluationDate
    );

    /**
     * Computes a compact summary for a client in a specific organization context.
     *
     * @param organizationId organization/tenant UUID
     * @param clientId       client UUID
     * @param evaluationDate statutory evaluation date
     * @return optional compact summary
     */
    Optional<ComplianceApplicabilitySummaryDto> getApplicabilitySummary(
            UUID organizationId,
            UUID clientId,
            LocalDate evaluationDate
    );
}
