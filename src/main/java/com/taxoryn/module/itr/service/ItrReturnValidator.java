package com.taxoryn.module.itr.service;

import com.taxoryn.module.itr.dto.ItrPrepareReturnRequest;
import com.taxoryn.module.itr.dto.ItrReturnValidationResultDto;
import com.taxoryn.module.itr.dto.ItrTaxSummaryDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator for statutory Income Tax Return (ITR) structures and financial consistency rules.
 */
@Component
public class ItrReturnValidator {

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    private static final Pattern AY_PATTERN = Pattern.compile("^([0-9]{4})-([0-9]{2})$");

    private static final Set<String> SUPPORTED_RETURN_TYPES = Set.of(
            "ITR-1", "ITR-2", "ITR-3", "ITR-4", "ITR-5", "ITR-6", "ITR-7",
            "ITR1", "ITR2", "ITR3", "ITR4", "ITR5", "ITR6", "ITR7"
    );

    public ItrReturnValidationResultDto validate(ItrPrepareReturnRequest request, ItrTaxSummaryDto taxSummary, String taxpayerType) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (request == null) {
            errors.add("Return preparation request must not be null");
            return ItrReturnValidationResultDto.failure(errors, warnings);
        }

        // 1. PAN validation
        if (!StringUtils.hasText(request.getPan())) {
            errors.add("PAN is mandatory for ITR return preparation");
        } else if (!PAN_PATTERN.matcher(request.getPan().trim().toUpperCase()).matches()) {
            errors.add("Invalid PAN format: '" + request.getPan() + "'. Expected standard 10-character alphanumeric format (e.g. ABCDE1234F)");
        }

        // 2. Assessment Year validation
        if (!StringUtils.hasText(request.getAssessmentYear())) {
            errors.add("Assessment Year is mandatory");
        } else {
            String ay = request.getAssessmentYear().trim();
            Matcher ayMatcher = AY_PATTERN.matcher(ay);
            if (!ayMatcher.matches()) {
                errors.add("Invalid Assessment Year format: '" + ay + "'. Expected format YYYY-YY (e.g. 2026-27)");
            } else {
                int startYear = Integer.parseInt(ayMatcher.group(1));
                int endYear = Integer.parseInt(ayMatcher.group(2));
                int expectedEndYear = (startYear + 1) % 100;
                if (endYear != expectedEndYear) {
                    errors.add("Invalid Assessment Year span: '" + ay + "'. Expected consecutive years (e.g. " + startYear + "-" + String.format("%02d", expectedEndYear) + ")");
                }
            }
        }

        // 3. Return Type validation
        String normalizedReturnType = null;
        if (!StringUtils.hasText(request.getReturnType())) {
            errors.add("Return type is mandatory");
        } else {
            String cleanType = request.getReturnType().trim().toUpperCase();
            if (!SUPPORTED_RETURN_TYPES.contains(cleanType)) {
                errors.add("Unsupported ITR return type: '" + request.getReturnType() + "'. Supported types: [ITR-1, ITR-2, ITR-3, ITR-4, ITR-5, ITR-6, ITR-7]");
            } else {
                normalizedReturnType = cleanType.contains("-") ? cleanType : cleanType.substring(0, 3) + "-" + cleanType.substring(3);
            }
        }

        // 4. Taxpayer Category & Form Compatibility
        String effectiveTaxpayerType = taxpayerType;
        if (!StringUtils.hasText(effectiveTaxpayerType) && StringUtils.hasText(request.getTaxpayerType())) {
            effectiveTaxpayerType = request.getTaxpayerType().trim().toUpperCase();
        }

        if (StringUtils.hasText(effectiveTaxpayerType) && normalizedReturnType != null) {
            validateFormCompatibility(effectiveTaxpayerType, normalizedReturnType, errors);
        }

        // 5. Financial Totals & Tax Summary validation
        ItrTaxSummaryDto summary = taxSummary != null ? taxSummary : request.getTaxSummary();
        if (summary != null) {
            validateTaxSummary(summary, errors, warnings);
        }

        // 6. Bank Details Check
        if (request.getBankDetails() == null || request.getBankDetails().isEmpty()) {
            warnings.add("Bank account details for refund credit and statutory compliance have not been specified.");
        }

        if (errors.isEmpty()) {
            return ItrReturnValidationResultDto.success(warnings);
        } else {
            return ItrReturnValidationResultDto.failure(errors, warnings);
        }
    }

    private void validateFormCompatibility(String taxpayerType, String returnType, List<String> errors) {
        String type = taxpayerType.toUpperCase();
        switch (type) {
            case "COMPANY" -> {
                if (!returnType.equals("ITR-6") && !returnType.equals("ITR-7")) {
                    errors.add("Taxpayer category COMPANY is not eligible to file " + returnType + ". Expected ITR-6 or ITR-7.");
                }
            }
            case "FIRM", "LLP" -> {
                if (!returnType.equals("ITR-5") && !returnType.equals("ITR-7")) {
                    errors.add("Taxpayer category " + type + " is not eligible to file " + returnType + ". Expected ITR-5.");
                }
            }
            case "TRUST" -> {
                if (!returnType.equals("ITR-7")) {
                    errors.add("Taxpayer category TRUST is not eligible to file " + returnType + ". Expected ITR-7.");
                }
            }
            case "INDIVIDUAL", "HUF" -> {
                if (returnType.equals("ITR-5") || returnType.equals("ITR-6")) {
                    errors.add("Taxpayer category " + type + " is not eligible to file " + returnType + ". Expected ITR-1, ITR-2, ITR-3, or ITR-4.");
                }
            }
            default -> {
                // Other entity types allowed according to specific guidelines
            }
        }
    }

    private void validateTaxSummary(ItrTaxSummaryDto summary, List<String> errors, List<String> warnings) {
        BigDecimal grossIncome = summary.getGrossTotalIncome() != null ? summary.getGrossTotalIncome() : BigDecimal.ZERO;
        BigDecimal deductions = summary.getTotalDeductions() != null ? summary.getTotalDeductions() : BigDecimal.ZERO;
        BigDecimal taxableIncome = summary.getTaxableIncome() != null ? summary.getTaxableIncome() : BigDecimal.ZERO;
        BigDecimal taxPayable = summary.getTaxPayable() != null ? summary.getTaxPayable() : BigDecimal.ZERO;
        BigDecimal surcharge = summary.getSurcharge() != null ? summary.getSurcharge() : BigDecimal.ZERO;
        BigDecimal cess = summary.getCess() != null ? summary.getCess() : BigDecimal.ZERO;
        BigDecimal totalLiability = summary.getTotalTaxLiability() != null ? summary.getTotalTaxLiability() : BigDecimal.ZERO;
        BigDecimal tdsTcs = summary.getTdsTcsCredit() != null ? summary.getTdsTcsCredit() : BigDecimal.ZERO;
        BigDecimal advanceTax = summary.getAdvanceTaxPaid() != null ? summary.getAdvanceTaxPaid() : BigDecimal.ZERO;
        BigDecimal selfAssessmentTax = summary.getSelfAssessmentTaxPaid() != null ? summary.getSelfAssessmentTaxPaid() : BigDecimal.ZERO;
        BigDecimal totalPaid = summary.getTotalTaxesPaid() != null ? summary.getTotalTaxesPaid() : BigDecimal.ZERO;

        if (grossIncome.compareTo(BigDecimal.ZERO) < 0) {
            warnings.add("Negative gross total income detected; verify business loss or capital loss schedules.");
        }

        if (deductions.compareTo(grossIncome) > 0 && grossIncome.compareTo(BigDecimal.ZERO) > 0) {
            warnings.add("Total Chapter VI-A deductions (" + deductions + ") exceed gross total income (" + grossIncome + "); deductions will be capped at gross income.");
        }

        // Taxable income arithmetic check
        BigDecimal expectedTaxable = grossIncome.subtract(deductions).max(BigDecimal.ZERO);
        if (summary.getTaxableIncome() != null && taxableIncome.compareTo(BigDecimal.ZERO) > 0 && taxableIncome.compareTo(expectedTaxable) != 0) {
            errors.add("Taxable income arithmetic mismatch: declared taxable income (" + taxableIncome
                    + ") does not equal gross income minus deductions (" + expectedTaxable + ")");
        }

        // Total liability arithmetic check
        BigDecimal expectedLiability = taxPayable.add(surcharge).add(cess);
        if (summary.getTotalTaxLiability() != null && totalLiability.compareTo(BigDecimal.ZERO) > 0 && totalLiability.compareTo(expectedLiability) != 0) {
            errors.add("Total tax liability arithmetic mismatch: declared liability (" + totalLiability
                    + ") does not equal sum of tax, surcharge, and cess (" + expectedLiability + ")");
        }

        // Total taxes paid arithmetic check
        BigDecimal expectedPaid = tdsTcs.add(advanceTax).add(selfAssessmentTax);
        if (summary.getTotalTaxesPaid() != null && totalPaid.compareTo(BigDecimal.ZERO) > 0 && totalPaid.compareTo(expectedPaid) != 0) {
            errors.add("Total taxes paid mismatch: declared total taxes paid (" + totalPaid
                    + ") does not equal sum of TDS/TCS, advance tax, and self-assessment tax (" + expectedPaid + ")");
        }

        // Refund vs balance payable checks
        if (totalPaid.compareTo(totalLiability) > 0) {
            warnings.add("Total taxes paid exceed total tax liability; net refund of " + totalPaid.subtract(totalLiability) + " will be claimed.");
        }
    }
}
