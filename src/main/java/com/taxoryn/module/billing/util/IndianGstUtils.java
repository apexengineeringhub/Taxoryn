package com.taxoryn.module.billing.util;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class IndianGstUtils {

    private IndianGstUtils() {}

    private static final Map<String, String> CODE_TO_STATE;
    private static final Map<String, String> STATE_TO_CODE;
    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    static {
        Map<String, String> c2s = new HashMap<>();
        c2s.put("01", "Jammu and Kashmir");
        c2s.put("02", "Himachal Pradesh");
        c2s.put("03", "Punjab");
        c2s.put("04", "Chandigarh");
        c2s.put("05", "Uttarakhand");
        c2s.put("06", "Haryana");
        c2s.put("07", "Delhi");
        c2s.put("08", "Rajasthan");
        c2s.put("09", "Uttar Pradesh");
        c2s.put("10", "Bihar");
        c2s.put("11", "Sikkim");
        c2s.put("12", "Arunachal Pradesh");
        c2s.put("13", "Nagaland");
        c2s.put("14", "Manipur");
        c2s.put("15", "Mizoram");
        c2s.put("16", "Tripura");
        c2s.put("17", "Meghalaya");
        c2s.put("18", "Assam");
        c2s.put("19", "West Bengal");
        c2s.put("20", "Jharkhand");
        c2s.put("21", "Odisha");
        c2s.put("22", "Chhattisgarh");
        c2s.put("23", "Madhya Pradesh");
        c2s.put("24", "Gujarat");
        c2s.put("26", "Dadra and Nagar Haveli and Daman and Diu");
        c2s.put("27", "Maharashtra");
        c2s.put("29", "Karnataka");
        c2s.put("30", "Goa");
        c2s.put("31", "Lakshadweep");
        c2s.put("32", "Kerala");
        c2s.put("33", "Tamil Nadu");
        c2s.put("34", "Puducherry");
        c2s.put("35", "Andaman and Nicobar Islands");
        c2s.put("36", "Telangana");
        c2s.put("37", "Andhra Pradesh");
        c2s.put("38", "Ladakh");
        c2s.put("97", "Other Territory");

        Map<String, String> s2c = new HashMap<>();
        for (Map.Entry<String, String> entry : c2s.entrySet()) {
            s2c.put(entry.getValue().toUpperCase(), entry.getKey());
        }
        s2c.put("ORISSA", "21");
        s2c.put("PONDICHERRY", "34");
        s2c.put("NEW DELHI", "07");
        s2c.put("JAMMU & KASHMIR", "01");
        s2c.put("ANDAMAN & NICOBAR", "35");
        s2c.put("DAMAN AND DIU", "26");
        s2c.put("DADRA AND NAGAR HAVELI", "26");

        CODE_TO_STATE = Collections.unmodifiableMap(c2s);
        STATE_TO_CODE = Collections.unmodifiableMap(s2c);
    }

    public static String extractStateCodeFromGstin(String gstin) {
        if (gstin != null && gstin.trim().length() >= 2) {
            String code = gstin.trim().substring(0, 2);
            if (CODE_TO_STATE.containsKey(code)) {
                return code;
            }
        }
        return null;
    }

    public static String getStateCode(String stateNameOrCode) {
        if (stateNameOrCode == null || stateNameOrCode.isBlank()) {
            return null;
        }
        String trimmed = stateNameOrCode.trim();
        if (trimmed.length() == 2 && CODE_TO_STATE.containsKey(trimmed)) {
            return trimmed;
        }
        return STATE_TO_CODE.get(trimmed.toUpperCase());
    }

    public static String getStateName(String stateCodeOrName) {
        if (stateCodeOrName == null || stateCodeOrName.isBlank()) {
            return null;
        }
        String trimmed = stateCodeOrName.trim();
        if (CODE_TO_STATE.containsKey(trimmed)) {
            return CODE_TO_STATE.get(trimmed);
        }
        String code = STATE_TO_CODE.get(trimmed.toUpperCase());
        if (code != null) {
            return CODE_TO_STATE.get(code);
        }
        return trimmed;
    }

    public static String computeFinancialYear(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        int year = date.getYear();
        int month = date.getMonthValue();
        if (month >= 4) {
            return String.format("%d-%02d", year, (year + 1) % 100);
        } else {
            return String.format("%d-%02d", year - 1, year % 100);
        }
    }

    public static String computeFinancialYearShort(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        int year = date.getYear();
        int month = date.getMonthValue();
        if (month >= 4) {
            return String.format("%02d-%02d", year % 100, (year + 1) % 100);
        } else {
            return String.format("%02d-%02d", (year - 1) % 100, year % 100);
        }
    }

    public static String defaultSacCode(com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType serviceType) {
        if (serviceType == null) {
            return "998231";
        }
        return switch (serviceType) {
            case GST_FILING -> "998231"; // Tax preparation services for statutory GST returns
            case ITR_FILING -> "998231"; // Tax preparation services for Income Tax Returns
            case TDS -> "998231";        // Tax preparation services for TDS compliance
            case AUDIT -> "998221";      // Financial auditing & statutory tax audit services
            case ACCOUNTING -> "998222"; // Bookkeeping and financial accounting services
            case ROC_COMPLIANCE -> "998213"; // Legal documentation and MCA filing services
            case CONSULTING, OTHER -> "998311"; // Management and tax consultancy services
        };
    }

    public record GstBreakdown(
            java.math.BigDecimal cgstRate,
            java.math.BigDecimal cgstAmount,
            java.math.BigDecimal sgstRate,
            java.math.BigDecimal sgstAmount,
            java.math.BigDecimal igstRate,
            java.math.BigDecimal igstAmount,
            java.math.BigDecimal totalTax
    ) {}

    public static GstBreakdown calculateGstBreakdown(java.math.BigDecimal taxableAmount, java.math.BigDecimal taxRate, boolean isIntraState) {
        if (taxableAmount == null) {
            taxableAmount = java.math.BigDecimal.ZERO;
        }
        if (taxRate == null) {
            taxRate = java.math.BigDecimal.ZERO;
        }

        java.math.BigDecimal totalTax = taxableAmount.multiply(taxRate)
                .divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

        if (taxRate.compareTo(java.math.BigDecimal.ZERO) <= 0 || taxableAmount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return new GstBreakdown(
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                    totalTax
            );
        }

        if (isIntraState) {
            java.math.BigDecimal halfRate = taxRate.divide(new java.math.BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal cgst = taxableAmount.multiply(halfRate)
                    .divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            // Settle SGST as totalTax - cgst to strictly guarantee: cgst + sgst == totalTax without 1-paisa rounding drifts
            java.math.BigDecimal sgst = totalTax.subtract(cgst);
            return new GstBreakdown(
                    halfRate, cgst,
                    halfRate, sgst,
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                    totalTax
            );
        } else {
            return new GstBreakdown(
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                    taxRate, totalTax,
                    totalTax
            );
        }
    }
}
