package com.taxoryn.module.compliance.applicability.evaluator;

import com.taxoryn.module.compliance.applicability.dto.EvaluatedRuleApplicabilityDto;
import com.taxoryn.module.compliance.applicability.model.ComplianceFactContext;
import com.taxoryn.module.compliance.rule.entity.ComplianceRuleEntity;

import java.time.LocalDate;

/**
 * Deterministic engine component responsible for evaluating a single compliance rule
 * against a client's compliance fact context.
 */
public interface RuleApplicabilityEvaluator {

    /**
     * Evaluates whether a compliance rule applies to a client.
     *
     * @param rule           compliance rule to evaluate
     * @param facts          resolved client facts
     * @param evaluationDate date of evaluation (for validity window / statutory checks)
     * @return evaluated result DTO with state and explainable reason
     */
    EvaluatedRuleApplicabilityDto evaluate(ComplianceRuleEntity rule, ComplianceFactContext facts, LocalDate evaluationDate);
}
