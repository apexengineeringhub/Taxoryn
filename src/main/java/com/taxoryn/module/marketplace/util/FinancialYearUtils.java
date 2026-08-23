package com.taxoryn.module.marketplace.util;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for Indian Financial Year parsing, validation, normalization, and display formatting.
 * Standard Indian Financial Year format runs from April 1 to March 31 (e.g. 2025-26).
 */
public final class FinancialYearUtils {

    // Matches patterns like "2025-26", "2025-2026", "FY_2025_26", "FY 2025-26", "FY2025-26"
    private static final Pattern FY_PATTERN = Pattern.compile("^(?:FY[\\s_]*)?(\\d{4})[\\s_\\-\\/]+(\\d{2}|\\d{4})$", Pattern.CASE_INSENSITIVE);

    private FinancialYearUtils() {
        // utility class
    }

    /**
     * Validates whether a string represents a valid Indian Financial Year.
     */
    public static boolean isValid(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        Matcher matcher = FY_PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            return false;
        }
        try {
            int startYear = Integer.parseInt(matcher.group(1));
            String endYearStr = matcher.group(2);
            int endYear = Integer.parseInt(endYearStr);

            if (endYearStr.length() == 4) {
                return endYear == startYear + 1 && startYear >= 2015 && startYear <= 2040;
            } else {
                int expectedEnd2Digits = (startYear + 1) % 100;
                return endYear == expectedEnd2Digits && startYear >= 2015 && startYear <= 2040;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Normalizes any supported input format to the canonical "YYYY-YY" representation.
     * Example: "FY_2025_26" -> "2025-26"
     * Example: "FY 2025-26" -> "2025-26"
     * Example: "2025-2026" -> "2025-26"
     */
    public static String normalize(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        Matcher matcher = FY_PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Financial Year format: '" + input + "'. Expected format e.g. 'FY 2025-26' or '2025-26'");
        }

        int startYear = Integer.parseInt(matcher.group(1));
        int endYear2Digits = (startYear + 1) % 100;
        return String.format("%04d-%02d", startYear, endYear2Digits);
    }

    /**
     * Formats canonical FY "YYYY-YY" to user-friendly display string "FY YYYY-YY".
     * Example: "2025-26" -> "FY 2025-26"
     */
    public static String toDisplayString(String normalizedFy) {
        if (!StringUtils.hasText(normalizedFy)) {
            return "";
        }
        if (normalizedFy.startsWith("FY ")) {
            return normalizedFy;
        }
        return "FY " + normalizedFy;
    }

    /**
     * Computes the current Indian Financial Year based on system date.
     * (Before April 1 is previous year, on or after April 1 is current year).
     */
    public static String getCurrentFinancialYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        if (now.getMonthValue() < 4) {
            year -= 1;
        }
        int endYear2Digits = (year + 1) % 100;
        return String.format("%04d-%02d", year, endYear2Digits);
    }

    /**
     * Returns a list of standard selectable financial years (e.g. 4 prior years, current FY, and 1 forward FY).
     */
    public static List<String> getStandardFinancialYears() {
        LocalDate now = LocalDate.now();
        int baseYear = now.getYear();
        if (now.getMonthValue() < 4) {
            baseYear -= 1;
        }

        List<String> years = new ArrayList<>();
        // Range: from 3 years prior to 1 year forward
        for (int y = baseYear + 1; y >= baseYear - 3; y--) {
            int endYear2Digits = (y + 1) % 100;
            years.add(String.format("%04d-%02d", y, endYear2Digits));
        }
        return years;
    }
}
