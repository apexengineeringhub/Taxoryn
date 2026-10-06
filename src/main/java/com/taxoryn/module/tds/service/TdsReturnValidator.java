package com.taxoryn.module.tds.service;

import com.taxoryn.module.tds.dto.TdsPrepareReturnRequest;
import com.taxoryn.module.tds.dto.TdsReturnTotalsDto;
import com.taxoryn.module.tds.dto.TdsReturnValidationResultDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator for statutory TDS/TCS quarterly statement structures and arithmetic consistency rules.
 */
@Component
public class TdsReturnValidator {

    private static final Pattern TAN_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{5}[A-Z]{1}$");
    private static final Pattern FY_PATTERN = Pattern.compile("^([0-9]{4})-([0-9]{2})$");
    private static final Pattern FY_FOUR_DIGIT_PATTERN = Pattern.compile("^([0-9]{4})-([0-9]{4})$");

    private static final Set<String> SUPPORTED_FORM_TYPES = Set.of(
            "FORM_24Q", "FORM_26Q", "FORM_27Q", "FORM_27EQ",
            "24Q", "26Q", "27Q", "27EQ",
            "FORM_26QB", "FORM_26QC", "FORM_26QD", "FORM_26QE"
    );

    private static final Set<String> VALID_QUARTERS = Set.of("Q1", "Q2", "Q3", "Q4");

    public TdsReturnValidationResultDto validate(TdsPrepareReturnRequest request, TdsReturnTotalsDto totals, String deductorType) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (request == null) {
            errors.add("Return preparation request must not be null");
            return TdsReturnValidationResultDto.failure(errors, warnings);
        }

        // 1. TAN validation
        if (!StringUtils.hasText(request.getTan())) {
            errors.add("TAN is mandatory for TDS return preparation");
        } else if (!TAN_PATTERN.matcher(request.getTan().trim().toUpperCase()).matches()) {
            errors.add("Invalid TAN format: '" + request.getTan() + "'. Expected standard 10-character alphanumeric format (e.g. ABCD12345E)");
        }

        // 2. Financial Year validation
        if (!StringUtils.hasText(request.getFinancialYear())) {
            errors.add("Financial Year is mandatory");
        } else {
            String fy = request.getFinancialYear().trim();
            Matcher fyMatcher = FY_PATTERN.matcher(fy);
            Matcher fyFourMatcher = FY_FOUR_DIGIT_PATTERN.matcher(fy);

            if (fyMatcher.matches()) {
                int startYear = Integer.parseInt(fyMatcher.group(1));
                int endYear = Integer.parseInt(fyMatcher.group(2));
                int expectedEndYear = (startYear + 1) % 100;
                if (endYear != expectedEndYear) {
                    errors.add("Invalid Financial Year span: '" + fy + "'. Expected consecutive years (e.g. " + startYear + "-" + String.format("%02d", expectedEndYear) + ")");
                }
            } else if (fyFourMatcher.matches()) {
                int startYear = Integer.parseInt(fyFourMatcher.group(1));
                int endYear = Integer.parseInt(fyFourMatcher.group(2));
                if (endYear != startYear + 1) {
                    errors.add("Invalid Financial Year span: '" + fy + "'. Expected consecutive years (e.g. " + startYear + "-" + (startYear + 1) + ")");
                }
            } else {
                errors.add("Invalid Financial Year format: '" + fy + "'. Expected format YYYY-YY (e.g. 2025-26)");
            }
        }

        // 3. Quarter validation
        if (!StringUtils.hasText(request.getQuarter())) {
            errors.add("Quarter is mandatory");
        } else {
            String quarter = request.getQuarter().trim().toUpperCase();
            if (!VALID_QUARTERS.contains(quarter)) {
                errors.add("Invalid quarter: '" + request.getQuarter() + "'. Supported quarters: [Q1, Q2, Q3, Q4]");
            }
        }

        // 4. Form Type validation
        if (!StringUtils.hasText(request.getFormType())) {
            errors.add("Form type is mandatory");
        } else {
            String form = request.getFormType().trim().toUpperCase();
            if (!SUPPORTED_FORM_TYPES.contains(form)) {
                errors.add("Unsupported TDS return form type: '" + request.getFormType() + "'. Supported forms: [FORM_24Q, FORM_26Q, FORM_27Q, FORM_27EQ]");
            }
        }

        // 5. Totals & Arithmetic Validation
        TdsReturnTotalsDto effectiveTotals = totals != null ? totals : request.getTotals();
        if (effectiveTotals != null) {
            validateTotals(effectiveTotals, errors, warnings, request);
        }

        if (errors.isEmpty()) {
            return TdsReturnValidationResultDto.success(warnings);
        } else {
            return TdsReturnValidationResultDto.failure(errors, warnings);
        }
    }

    private void validateTotals(TdsReturnTotalsDto totals, List<String> errors, List<String> warnings, TdsPrepareReturnRequest request) {
        BigDecimal paid = totals.getTotalAmountPaid() != null ? totals.getTotalAmountPaid() : BigDecimal.ZERO;
        BigDecimal deducted = totals.getTotalTaxDeducted() != null ? totals.getTotalTaxDeducted() : BigDecimal.ZERO;
        BigDecimal deposited = totals.getTotalTaxDeposited() != null ? totals.getTotalTaxDeposited() : BigDecimal.ZERO;
        BigDecimal interest = totals.getTotalInterest() != null ? totals.getTotalInterest() : BigDecimal.ZERO;
        BigDecimal lateFee = totals.getTotalLateFee() != null ? totals.getTotalLateFee() : BigDecimal.ZERO;
        BigDecimal penalty = totals.getTotalPenalty() != null ? totals.getTotalPenalty() : BigDecimal.ZERO;
        BigDecimal challanAmount = totals.getTotalChallanAmount() != null ? totals.getTotalChallanAmount() : BigDecimal.ZERO;

        if (paid.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Total amount paid cannot be negative: " + paid);
        }
        if (deducted.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Total tax deducted cannot be negative: " + deducted);
        }
        if (deposited.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Total tax deposited cannot be negative: " + deposited);
        }
        if (interest.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Total interest cannot be negative: " + interest);
        }
        if (lateFee.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Total late fee cannot be negative: " + lateFee);
        }
        if (penalty.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Total penalty cannot be negative: " + penalty);
        }
        if (challanAmount.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Total challan amount cannot be negative: " + challanAmount);
        }

        // Short payment check: if tax deducted is strictly greater than tax deposited when both are specified
        if (deducted.compareTo(BigDecimal.ZERO) > 0 && deposited.compareTo(BigDecimal.ZERO) > 0 && deducted.compareTo(deposited) > 0) {
            errors.add("Total tax deducted (" + deducted + ") exceeds total tax deposited (" + deposited + "). Short payment detected.");
        }

        // Challan allocation check
        if (challanAmount.compareTo(BigDecimal.ZERO) > 0 && deposited.compareTo(BigDecimal.ZERO) > 0 && challanAmount.compareTo(deposited) < 0) {
            warnings.add("Total challan amount (" + challanAmount + ") is less than total tax deposited (" + deposited + "). Verify challan entries.");
        }

        // Check deductee & challan lists against counts
        long deducteeCount = totals.getTotalDeducteeCount() != null ? totals.getTotalDeducteeCount() : 0L;
        if (deducteeCount > 0 && (request.getDeductees() == null || request.getDeductees().isEmpty())) {
            warnings.add("Declared deductee count is " + deducteeCount + ", but deductee transaction records are not attached in this preparation request.");
        }

        long challanCount = totals.getTotalChallanCount() != null ? totals.getTotalChallanCount() : 0L;
        if (challanCount > 0 && (request.getChallans() == null || request.getChallans().isEmpty())) {
            warnings.add("Declared challan count is " + challanCount + ", but challan deposit records are not attached in this preparation request.");
        }
    }
}
