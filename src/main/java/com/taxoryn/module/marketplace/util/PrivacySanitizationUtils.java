package com.taxoryn.module.marketplace.util;

import com.taxoryn.module.marketplace.entity.CustomerTaxpayerType;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for enforcing Minimum Necessary Disclosure and redacting sensitive
 * identity & financial identifiers during early marketplace inquiries (Level 2).
 */
public final class PrivacySanitizationUtils {

    // Regex for Indian Permanent Account Number (PAN): 5 letters, 4 digits, 1 letter
    private static final Pattern PAN_PATTERN = Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b", Pattern.CASE_INSENSITIVE);

    // Regex for Indian Aadhaar Number: 12 digits (with optional spaces or dashes)
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\b[2-9]\\d{3}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}\\b");

    // Regex for Indian GSTIN: 15 alphanumeric
    private static final Pattern GSTIN_PATTERN = Pattern.compile("\\b[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}\\b", Pattern.CASE_INSENSITIVE);

    // Regex for Bank Account numbers or sensitive account details
    private static final Pattern BANK_ACCOUNT_PATTERN = Pattern.compile("(?i)\\b(?:a/c|acc(?:ount)?|bank account)[\\s:#]*(\\d{9,18})\\b");

    // Regex for explicit income/salary disclosures (e.g., "salary of 32 lakh", "earning 25 lpa", "40 lakh capital gains")
    private static final Pattern INCOME_DISCLOSURE_PATTERN = Pattern.compile(
            "(?i)\\b(?:salary|income|ctc|earning|capital gain[s]?|profit)[\\s:=]+(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?\\s*(?:lakh|lac|cr|crore|lpa|k)?)\\b"
    );

    private PrivacySanitizationUtils() {
        // utility class
    }

    /**
     * Sanitizes customer-generated inquiry text for Level 2 Early Enquiry disclosure.
     * Removes HTML tags, redacts PAN, Aadhaar, GSTIN, Bank accounts, and direct income figures.
     */
    public static String sanitizeForEarlyEnquiry(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }

        // 1. Strip HTML tags
        String sanitized = input.replaceAll("<[^>]*>", "").trim();

        // 2. Redact PAN
        sanitized = PAN_PATTERN.matcher(sanitized).replaceAll("[PROTECTED-PAN]");

        // 3. Redact Aadhaar
        sanitized = AADHAAR_PATTERN.matcher(sanitized).replaceAll("[PROTECTED-AADHAAR]");

        // 4. Redact GSTIN
        sanitized = GSTIN_PATTERN.matcher(sanitized).replaceAll("[PROTECTED-GSTIN]");

        // 5. Redact Bank Account numbers
        sanitized = BANK_ACCOUNT_PATTERN.matcher(sanitized).replaceAll("Account [PROTECTED-BANK-A/C]");

        // 6. Redact explicit salary/income figures
        sanitized = INCOME_DISCLOSURE_PATTERN.matcher(sanitized).replaceAll("[FINANCIAL-DISCLOSURE-PROTECTED]");

        // 7. Enforce max character limit for early inquiry summary
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 497) + "...";
        }

        return sanitized;
    }

    /**
     * Generates a standard privacy-safe Early Enquiry Summary when the customer
     * does not provide an explicit custom early enquiry message.
     */
    public static String generateSafeEarlyEnquirySummary(
            TaxServiceEntity service,
            CustomerTaxpayerType customerType,
            String financialYear
    ) {
        StringBuilder sb = new StringBuilder("Seeking professional assistance");

        if (service != null && StringUtils.hasText(service.getName())) {
            sb.append(" for ").append(service.getName());
        }

        if (StringUtils.hasText(financialYear)) {
            sb.append(" (").append(FinancialYearUtils.toDisplayString(financialYear)).append(")");
        }

        if (customerType != null) {
            sb.append(" as a ").append(customerType.getDisplayName());
        } else {
            sb.append(" as an Individual Taxpayer");
        }

        sb.append(".");
        return sb.toString();
    }

    /**
     * Masks an email address for early disclosure (e.g. rahul.sharma@gmail.com -> r***a@gmail.com).
     */
    public static String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "c***r@customer.taxoryn";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    /**
     * Masks a phone number for early disclosure (e.g. +919876543210 -> +91******3210).
     */
    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "******0000";
        }
        String clean = phone.trim();
        if (clean.length() <= 4) {
            return "******";
        }
        String last4 = clean.substring(clean.length() - 4);
        return "+91******" + last4;
    }
}
