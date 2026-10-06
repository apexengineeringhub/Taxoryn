package com.taxoryn.module.compliance.obligation.util;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;

import java.util.regex.Pattern;

/**
 * Validates period key formatting and ensures semantic compatibility with compliance rule frequencies.
 */
public final class PeriodValidator {

    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final Pattern QUARTER_PATTERN = Pattern.compile("^\\d{4}-Q[1-4]$");
    private static final Pattern HALF_YEAR_PATTERN = Pattern.compile("^\\d{4}-H[1-2]$");
    private static final Pattern FY_AY_SHORT_PATTERN = Pattern.compile("^\\d{4}-\\d{2}$");
    private static final Pattern FY_AY_FULL_PATTERN = Pattern.compile("^\\d{4}-\\d{4}$");

    private PeriodValidator() {
        // utility class
    }

    /**
     * Validates that the period key conforms to standard format for the specified period type.
     */
    public static void validatePeriodKey(CompliancePeriodType periodType, String periodKey) {
        if (periodType == null) {
            throw new BadRequestException("Period type is required");
        }
        if (periodKey == null || periodKey.trim().isEmpty()) {
            throw new BadRequestException("Period key is required");
        }

        String trimmed = periodKey.trim();

        switch (periodType) {
            case MONTH -> {
                if (!MONTH_PATTERN.matcher(trimmed).matches()) {
                    throw new BadRequestException("Invalid MONTH period key: '" + periodKey + "'. Expected format 'YYYY-MM' (e.g. '2026-09', month between 01 and 12).");
                }
            }
            case QUARTER -> {
                if (!QUARTER_PATTERN.matcher(trimmed).matches()) {
                    throw new BadRequestException("Invalid QUARTER period key: '" + periodKey + "'. Expected format 'YYYY-QX' (e.g. '2026-Q1' to '2026-Q4').");
                }
            }
            case HALF_YEAR -> {
                if (!HALF_YEAR_PATTERN.matcher(trimmed).matches()) {
                    throw new BadRequestException("Invalid HALF_YEAR period key: '" + periodKey + "'. Expected format 'YYYY-HX' (e.g. '2026-H1' or '2026-H2').");
                }
            }
            case FINANCIAL_YEAR, ASSESSMENT_YEAR -> {
                if (!FY_AY_SHORT_PATTERN.matcher(trimmed).matches() && !FY_AY_FULL_PATTERN.matcher(trimmed).matches()) {
                    throw new BadRequestException("Invalid " + periodType.name() + " period key: '" + periodKey + "'. Expected format 'YYYY-YY' or 'YYYY-YYYY' (e.g. '2026-27').");
                }
            }
            case EVENT -> {
                if (trimmed.length() > 100) {
                    throw new BadRequestException("Invalid EVENT period key: Length exceeds 100 characters.");
                }
            }
        }
    }

    /**
     * Checks if a rule's frequency is compatible with the requested period type.
     */
    public static boolean isCompatible(ComplianceRuleFrequency frequency, CompliancePeriodType periodType) {
        if (frequency == null || periodType == null) {
            return false;
        }

        return switch (frequency) {
            case MONTHLY -> periodType == CompliancePeriodType.MONTH;
            case QUARTERLY -> periodType == CompliancePeriodType.QUARTER;
            case HALF_YEARLY -> periodType == CompliancePeriodType.HALF_YEAR;
            case ANNUAL -> periodType == CompliancePeriodType.FINANCIAL_YEAR
                    || periodType == CompliancePeriodType.ASSESSMENT_YEAR
                    || periodType == CompliancePeriodType.MONTH
                    || periodType == CompliancePeriodType.QUARTER
                    || periodType == CompliancePeriodType.EVENT;
            case ONE_TIME, EVENT_BASED -> true;
        };
    }

    /**
     * Asserts that a rule's frequency is compatible with the period type, or throws BadRequestException.
     */
    public static void validateCompatibility(String ruleCode, ComplianceRuleFrequency frequency, CompliancePeriodType periodType) {
        if (!isCompatible(frequency, periodType)) {
            throw new BadRequestException("Rule '" + ruleCode + "' with frequency '" + frequency + "' is incompatible with requested period type '" + periodType + "'.");
        }
    }
}
