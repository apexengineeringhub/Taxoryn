package com.taxoryn.module.compliance.applicability.evaluator;

import com.taxoryn.module.compliance.applicability.dto.EvaluatedRuleApplicabilityDto;
import com.taxoryn.module.compliance.applicability.model.ApplicabilityResultState;
import com.taxoryn.module.compliance.applicability.model.ComplianceFactContext;
import com.taxoryn.module.compliance.rule.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RuleApplicabilityEvaluatorImpl implements RuleApplicabilityEvaluator {

    @Override
    public EvaluatedRuleApplicabilityDto evaluate(ComplianceRuleEntity rule, ComplianceFactContext facts, LocalDate evaluationDate) {
        if (rule == null) {
            throw new IllegalArgumentException("Compliance rule must not be null for evaluation.");
        }
        if (facts == null) {
            throw new IllegalArgumentException("Compliance facts context must not be null for evaluation.");
        }
        if (evaluationDate == null) {
            evaluationDate = LocalDate.now();
        }

        // 1. Check Rule Lifecycle Status
        if (rule.getStatus() != null && rule.getStatus() != ComplianceRuleStatus.ACTIVE) {
            return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                    "Rule is currently " + rule.getStatus() + " (inactive).", evaluationDate);
        }

        // 2. Check Statutory Validity Window (Effective From / To)
        if (rule.getEffectiveFrom() != null && evaluationDate.isBefore(rule.getEffectiveFrom())) {
            return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                    "Rule is not effective until " + rule.getEffectiveFrom() + " (evaluation date: " + evaluationDate + ").", evaluationDate);
        }
        if (rule.getEffectiveTo() != null && evaluationDate.isAfter(rule.getEffectiveTo())) {
            return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                    "Rule expired on " + rule.getEffectiveTo() + " (evaluation date: " + evaluationDate + ").", evaluationDate);
        }

        // 3. Check if Client Compliance Profile is Missing / Unconfigured
        if (!facts.isProfilePresent()) {
            return buildResult(rule, ApplicabilityResultState.INSUFFICIENT_DATA,
                    "Client compliance profile has not been configured. Setup the profile to evaluate statutory applicability.", evaluationDate);
        }

        // 4. Domain Level & Criteria Evaluation
        ComplianceRuleDomain domain = rule.getDomain() != null ? rule.getDomain() : ComplianceRuleDomain.OTHER;

        return switch (domain) {
            case GST -> evaluateGstRule(rule, facts, evaluationDate);
            case TDS -> evaluateTdsRule(rule, facts, evaluationDate);
            case INCOME_TAX -> evaluateIncomeTaxRule(rule, facts, evaluationDate);
            case MCA_ROC -> evaluateMcaRocRule(rule, facts, evaluationDate);
            case STATUTORY_AUDIT -> evaluateStatutoryAuditRule(rule, facts, evaluationDate);
            case PAYROLL_LABOUR -> evaluatePayrollLabourRule(rule, facts, evaluationDate);
            case OTHER -> evaluateOtherRule(rule, facts, evaluationDate);
        };
    }

    private EvaluatedRuleApplicabilityDto evaluateGstRule(ComplianceRuleEntity rule, ComplianceFactContext facts, LocalDate evaluationDate) {
        // Master GST applicability check
        if (facts.getGstApplicable() == null) {
            return buildResult(rule, ApplicabilityResultState.INSUFFICIENT_DATA,
                    "GST applicability is unconfigured in the client compliance profile.", evaluationDate);
        }
        if (Boolean.FALSE.equals(facts.getGstApplicable())) {
            return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                    "GST compliance is disabled for this client.", evaluationDate);
        }

        // Registration Type Criteria (e.g. REGULAR, COMPOSITION, TDS_TCS)
        if (rule.getApplicableGstRegistrationTypes() != null && !rule.getApplicableGstRegistrationTypes().isBlank()) {
            if (facts.getGstRegistrationType() == null) {
                return buildResult(rule, ApplicabilityResultState.INSUFFICIENT_DATA,
                        "GST registration type is not configured in the client compliance profile.", evaluationDate);
            }
            Set<String> allowedTypes = parseCommaSeparatedTokens(rule.getApplicableGstRegistrationTypes());
            if (!allowedTypes.contains(facts.getGstRegistrationType().toUpperCase())) {
                return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                        "Client GST registration type (" + facts.getGstRegistrationType() + ") does not match required type(s): " + rule.getApplicableGstRegistrationTypes() + ".", evaluationDate);
            }
        }

        // Return Filing Frequency Criteria (e.g. MONTHLY, QUARTERLY)
        if (rule.getApplicableFilingFrequencies() != null && !rule.getApplicableFilingFrequencies().isBlank()) {
            if (facts.getGstFilingFrequency() == null) {
                return buildResult(rule, ApplicabilityResultState.INSUFFICIENT_DATA,
                        "GST return filing frequency is not configured in the client compliance profile.", evaluationDate);
            }
            Set<String> allowedFreqs = parseCommaSeparatedTokens(rule.getApplicableFilingFrequencies());
            if (!allowedFreqs.contains(facts.getGstFilingFrequency().toUpperCase())) {
                return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                        "Client GST filing frequency (" + facts.getGstFilingFrequency() + ") does not match required frequency: " + rule.getApplicableFilingFrequencies() + ".", evaluationDate);
            }
        }

        // Entity type filter if specified on rule
        EvaluatedRuleApplicabilityDto entityTypeCheck = evaluateEntityTypeCriteria(rule, facts, evaluationDate);
        if (entityTypeCheck != null) {
            return entityTypeCheck;
        }

        return buildResult(rule, ApplicabilityResultState.APPLICABLE,
                "GST compliance is active with " + (facts.getGstRegistrationType() != null ? facts.getGstRegistrationType() : "standard") +
                        " registration and " + (facts.getGstFilingFrequency() != null ? facts.getGstFilingFrequency() : "standard") + " filing frequency.", evaluationDate);
    }

    private EvaluatedRuleApplicabilityDto evaluateTdsRule(ComplianceRuleEntity rule, ComplianceFactContext facts, LocalDate evaluationDate) {
        // Master TDS applicability check
        if (facts.getTdsApplicable() == null) {
            return buildResult(rule, ApplicabilityResultState.INSUFFICIENT_DATA,
                    "TDS / TCS compliance applicability is unconfigured in the client compliance profile.", evaluationDate);
        }
        if (Boolean.FALSE.equals(facts.getTdsApplicable())) {
            return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                    "TDS / TCS compliance is disabled for this client.", evaluationDate);
        }

        // Check if rule requires active TDS deductor status
        if (Boolean.TRUE.equals(rule.getRequiresTdsDeductor()) && Boolean.FALSE.equals(facts.getTdsApplicable())) {
            return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                    "Client is not configured as a TDS deductor.", evaluationDate);
        }

        // Entity type filter if specified on rule
        EvaluatedRuleApplicabilityDto entityTypeCheck = evaluateEntityTypeCriteria(rule, facts, evaluationDate);
        if (entityTypeCheck != null) {
            return entityTypeCheck;
        }

        return buildResult(rule, ApplicabilityResultState.APPLICABLE,
                "TDS / TCS compliance is enabled for the client" +
                        (facts.getTdsDeductorCategory() != null ? " as deductor category " + facts.getTdsDeductorCategory() : "") + ".", evaluationDate);
    }

    private EvaluatedRuleApplicabilityDto evaluateIncomeTaxRule(ComplianceRuleEntity rule, ComplianceFactContext facts, LocalDate evaluationDate) {
        // Master ITR applicability check
        if (facts.getItrApplicable() == null) {
            return buildResult(rule, ApplicabilityResultState.INSUFFICIENT_DATA,
                    "Income Tax (ITR) compliance applicability is unconfigured in the client compliance profile.", evaluationDate);
        }
        if (Boolean.FALSE.equals(facts.getItrApplicable())) {
            return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                    "Income Tax (ITR) compliance is disabled for this client.", evaluationDate);
        }

        // Tax Audit Requirement (Section 44AB)
        if (rule.getRequiresTaxAudit() != null) {
            if (Boolean.TRUE.equals(rule.getRequiresTaxAudit()) && Boolean.FALSE.equals(facts.getItrTaxAuditApplicable())) {
                return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                        "Tax Audit (Section 44AB) is not applicable for this client.", evaluationDate);
            }
            // For non-audit return rules, if client is explicitly under tax audit, non-audit rule does not apply
            if (Boolean.FALSE.equals(rule.getRequiresTaxAudit()) && Boolean.TRUE.equals(facts.getItrTaxAuditApplicable())) {
                return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                        "Client is liable for Tax Audit (Section 44AB), hence non-audit annual return does not apply.", evaluationDate);
            }
        }

        // Transfer Pricing Requirement (Section 92E)
        if (rule.getRequiresTransferPricing() != null) {
            if (Boolean.TRUE.equals(rule.getRequiresTransferPricing()) && Boolean.FALSE.equals(facts.getItrTransferPricingApplicable())) {
                return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                        "Transfer Pricing (Section 92E) reporting is not applicable for this client.", evaluationDate);
            }
        }

        // Advance Tax Rules
        if (rule.getRuleCode() != null && rule.getRuleCode().contains("ADVANCE_TAX")) {
            if (Boolean.FALSE.equals(facts.getAdvanceTaxApplicable())) {
                return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                        "Advance tax payment compliance is disabled for this client.", evaluationDate);
            }
        }

        // Entity type filter if specified on rule
        EvaluatedRuleApplicabilityDto entityTypeCheck = evaluateEntityTypeCriteria(rule, facts, evaluationDate);
        if (entityTypeCheck != null) {
            return entityTypeCheck;
        }

        return buildResult(rule, ApplicabilityResultState.APPLICABLE,
                "Income Tax compliance is enabled" +
                        (Boolean.TRUE.equals(facts.getItrTaxAuditApplicable()) ? " with Tax Audit (44AB)" : "") +
                        (Boolean.TRUE.equals(facts.getItrTransferPricingApplicable()) ? " and Transfer Pricing (92E)" : "") + ".", evaluationDate);
    }

    private EvaluatedRuleApplicabilityDto evaluateMcaRocRule(ComplianceRuleEntity rule, ComplianceFactContext facts, LocalDate evaluationDate) {
        // Master MCA applicability check
        if (facts.getMcaFilingApplicable() == null) {
            return buildResult(rule, ApplicabilityResultState.INSUFFICIENT_DATA,
                    "MCA / ROC filing applicability is unconfigured in the client compliance profile.", evaluationDate);
        }
        if (Boolean.FALSE.equals(facts.getMcaFilingApplicable())) {
            return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                    "MCA / ROC statutory compliance is disabled for this client.", evaluationDate);
        }

        // Non-corporate check: Individuals, HUFs, Partnerships without MCA filing
        if (facts.getClientType() != null) {
            String ct = facts.getClientType().toUpperCase();
            if (Set.of("INDIVIDUAL", "PROPRIETOR", "PROPRIETORSHIP", "HUF").contains(ct) && Boolean.FALSE.equals(facts.getMcaFilingApplicable())) {
                return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                        "Client constitution (" + facts.getClientType() + ") is not an MCA-registered corporate entity.", evaluationDate);
            }
        }

        // Entity type filter if specified on rule
        EvaluatedRuleApplicabilityDto entityTypeCheck = evaluateEntityTypeCriteria(rule, facts, evaluationDate);
        if (entityTypeCheck != null) {
            return entityTypeCheck;
        }

        return buildResult(rule, ApplicabilityResultState.APPLICABLE,
                "MCA / ROC statutory filing is enabled for this corporate client.", evaluationDate);
    }

    private EvaluatedRuleApplicabilityDto evaluateStatutoryAuditRule(ComplianceRuleEntity rule, ComplianceFactContext facts, LocalDate evaluationDate) {
        boolean mcaAudit = Boolean.TRUE.equals(facts.getMcaFilingApplicable());
        boolean taxAudit = Boolean.TRUE.equals(facts.getItrTaxAuditApplicable());

        if (!mcaAudit && !taxAudit) {
            return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                    "Neither Companies Act statutory audit nor Section 44AB tax audit is applicable for this client.", evaluationDate);
        }

        // Entity type filter if specified on rule
        EvaluatedRuleApplicabilityDto entityTypeCheck = evaluateEntityTypeCriteria(rule, facts, evaluationDate);
        if (entityTypeCheck != null) {
            return entityTypeCheck;
        }

        return buildResult(rule, ApplicabilityResultState.APPLICABLE,
                "Statutory audit requirement is active" + (mcaAudit ? " under Companies Act" : "") + (taxAudit ? " and Income Tax Section 44AB" : "") + ".", evaluationDate);
    }

    private EvaluatedRuleApplicabilityDto evaluatePayrollLabourRule(ComplianceRuleEntity rule, ComplianceFactContext facts, LocalDate evaluationDate) {
        if (facts.getPfEsiApplicable() == null) {
            return buildResult(rule, ApplicabilityResultState.INSUFFICIENT_DATA,
                    "Payroll & labour compliance (EPF/ESIC) is unconfigured in the client compliance profile.", evaluationDate);
        }
        if (Boolean.FALSE.equals(facts.getPfEsiApplicable())) {
            return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                    "Payroll & EPF/ESIC compliance is disabled for this client.", evaluationDate);
        }

        // Entity type filter if specified on rule
        EvaluatedRuleApplicabilityDto entityTypeCheck = evaluateEntityTypeCriteria(rule, facts, evaluationDate);
        if (entityTypeCheck != null) {
            return entityTypeCheck;
        }

        return buildResult(rule, ApplicabilityResultState.APPLICABLE,
                "Payroll & labour law (EPF/ESIC) compliance is enabled for this client.", evaluationDate);
    }

    private EvaluatedRuleApplicabilityDto evaluateOtherRule(ComplianceRuleEntity rule, ComplianceFactContext facts, LocalDate evaluationDate) {
        // Entity type filter if specified on rule
        EvaluatedRuleApplicabilityDto entityTypeCheck = evaluateEntityTypeCriteria(rule, facts, evaluationDate);
        if (entityTypeCheck != null) {
            return entityTypeCheck;
        }

        return buildResult(rule, ApplicabilityResultState.APPLICABLE,
                "Practice or statutory compliance rule is applicable.", evaluationDate);
    }

    private EvaluatedRuleApplicabilityDto evaluateEntityTypeCriteria(ComplianceRuleEntity rule, ComplianceFactContext facts, LocalDate evaluationDate) {
        if (rule.getApplicableEntityTypes() != null && !rule.getApplicableEntityTypes().isBlank()) {
            if (facts.getClientType() == null) {
                return buildResult(rule, ApplicabilityResultState.INSUFFICIENT_DATA,
                        "Client entity constitution type is unknown.", evaluationDate);
            }
            Set<String> allowedTypes = parseCommaSeparatedTokens(rule.getApplicableEntityTypes());
            if (!allowedTypes.contains(facts.getClientType().toUpperCase())) {
                return buildResult(rule, ApplicabilityResultState.NOT_APPLICABLE,
                        "Client entity type (" + facts.getClientType() + ") is not eligible for this rule (requires " + rule.getApplicableEntityTypes() + ").", evaluationDate);
            }
        }
        return null;
    }

    private Set<String> parseCommaSeparatedTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private EvaluatedRuleApplicabilityDto buildResult(ComplianceRuleEntity rule, ApplicabilityResultState result, String reason, LocalDate evaluationDate) {
        return EvaluatedRuleApplicabilityDto.builder()
                .ruleId(rule.getId())
                .ruleCode(rule.getRuleCode())
                .ruleName(rule.getRuleName() != null ? rule.getRuleName() : rule.getName())
                .domain(rule.getDomain())
                .frequency(rule.getFrequency())
                .periodType(rule.getPeriodType())
                .isSystemRule(rule.isSystemRule())
                .result(result)
                .reason(reason)
                .statutoryAct(rule.getStatutoryAct())
                .statutorySection(rule.getStatutorySection())
                .statutoryFormCode(rule.getStatutoryFormCode())
                .dueDateDescription(rule.getDueDateDescription())
                .ruleVersion(rule.getVersion())
                .evaluatedAt(Instant.now())
                .build();
    }
}
