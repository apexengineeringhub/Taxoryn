package com.taxoryn.module.marketplace.util;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility for normalizing tax service search terms, alias matching strings, and stable service codes.
 */
public final class TaxServiceNormalizationUtils {

    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s]");
    private static final Pattern MULTIPLE_WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern CODE_SANITIZER_PATTERN = Pattern.compile("[^A-Z0-9_]");

    private TaxServiceNormalizationUtils() {
        // utility class
    }

    /**
     * Normalizes an alias or search query string for uniform matching.
     * Example: "  I.T.R. - Filing  " -> "itr filing"
     * Example: "GSTR-3B" -> "gstr 3b"
     */
    public static String normalize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String withoutDots = text.replace(".", "");
        String clean = NON_ALPHANUMERIC_PATTERN.matcher(withoutDots).replaceAll(" ");
        String collapsed = MULTIPLE_WHITESPACE_PATTERN.matcher(clean).replaceAll(" ");
        return collapsed.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Generates a stable machine-readable code from a display title if not explicitly provided.
     * Example: "Income Tax Return Filing" -> "INCOME_TAX_RETURN_FILING"
     */
    public static String toCode(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        String upper = name.trim().toUpperCase(Locale.ROOT);
        String replaced = upper.replaceAll("[\\s\\-\\./]+", "_");
        String sanitized = CODE_SANITIZER_PATTERN.matcher(replaced).replaceAll("");
        return sanitized.replaceAll("_+", "_").replaceAll("^_|_$", "");
    }
}
