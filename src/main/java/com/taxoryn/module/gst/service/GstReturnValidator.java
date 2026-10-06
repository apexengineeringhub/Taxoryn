package com.taxoryn.module.gst.service;

import com.taxoryn.module.gst.dto.GstPrepareReturnRequest;
import com.taxoryn.module.gst.dto.GstReturnTotalsDto;
import com.taxoryn.module.gst.dto.GstReturnValidationResultDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for statutory GST return structures and financial consistency rules.
 */
@Component
public class GstReturnValidator {

    private static final Pattern GSTIN_PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    private static final Set<String> SUPPORTED_RETURN_TYPES = Set.of(
            "GSTR1", "GSTR3B", "GSTR9", "GSTR9C", "CMP08", "GSTR4", "GSTR7", "GSTR8"
    );

    public GstReturnValidationResultDto validate(GstPrepareReturnRequest request, GstReturnTotalsDto totals) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (request == null) {
            errors.add("Return preparation request must not be null");
            return GstReturnValidationResultDto.failure(errors, warnings);
        }

        // 1. GSTIN format
        if (!StringUtils.hasText(request.getGstin())) {
            errors.add("GSTIN is mandatory for return preparation");
        } else if (!GSTIN_PATTERN.matcher(request.getGstin().trim().toUpperCase()).matches()) {
            errors.add("Invalid GSTIN format: '" + request.getGstin() + "'. Expected 15-character statutory alphanumeric format");
        }

        // 2. Return Type validation
        if (!StringUtils.hasText(request.getReturnType())) {
            errors.add("Return type is mandatory");
        } else {
            String cleanType = request.getReturnType().trim().toUpperCase().replace("-", "");
            if (!SUPPORTED_RETURN_TYPES.contains(cleanType)) {
                errors.add("Unsupported GST return type: '" + request.getReturnType() + "'. Supported types: " + SUPPORTED_RETURN_TYPES);
            }
        }

        // 3. Return Period validation
        if (!StringUtils.hasText(request.getReturnPeriod())) {
            errors.add("Return period is mandatory");
        } else if (request.getReturnPeriod().trim().length() < 4) {
            errors.add("Invalid return period format: '" + request.getReturnPeriod() + "'. Expected MMYYYY, YYYY-MM, or QX-YYYY");
        }

        // 4. Financial Totals validation
        if (totals != null) {
            if (totals.getTaxableValue() != null && totals.getTaxableValue().compareTo(BigDecimal.ZERO) < 0) {
                warnings.add("Negative total taxable value detected. Verify credit note or reverse charge adjustments");
            }

            BigDecimal igst = totals.getIgst() != null ? totals.getIgst() : BigDecimal.ZERO;
            BigDecimal cgst = totals.getCgst() != null ? totals.getCgst() : BigDecimal.ZERO;
            BigDecimal sgst = totals.getSgst() != null ? totals.getSgst() : BigDecimal.ZERO;
            BigDecimal cess = totals.getCess() != null ? totals.getCess() : BigDecimal.ZERO;
            BigDecimal expectedTotalTax = igst.add(cgst).add(sgst).add(cess);

            if (totals.getTotalTax() != null && totals.getTotalTax().compareTo(expectedTotalTax) != 0) {
                errors.add("Tax liability arithmetic mismatch: declared total tax (" + totals.getTotalTax()
                        + ") does not equal sum of tax components (" + expectedTotalTax + ")");
            }

            // Intra-state symmetry check: CGST and SGST must match for standard supplies
            if (cgst.compareTo(BigDecimal.ZERO) > 0 || sgst.compareTo(BigDecimal.ZERO) > 0) {
                if (cgst.compareTo(sgst) != 0) {
                    warnings.add("CGST (" + cgst + ") and SGST (" + sgst + ") amounts differ; verify differential rate or UTGST classification");
                }
            }
        }

        // 5. Sections validation
        if (request.getSections() != null && !request.getSections().isEmpty()) {
            for (Map.Entry<String, Object> entry : request.getSections().entrySet()) {
                if (!StringUtils.hasText(entry.getKey())) {
                    errors.add("Section name must not be blank in return payload");
                }
            }
        }

        if (errors.isEmpty()) {
            return GstReturnValidationResultDto.success(warnings);
        } else {
            return GstReturnValidationResultDto.failure(errors, warnings);
        }
    }
}
