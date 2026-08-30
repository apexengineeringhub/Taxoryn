package com.taxoryn.module.notification.whatsapp.util;

import org.springframework.util.StringUtils;

public final class PhoneNumberNormalizer {

    private PhoneNumberNormalizer() {}

    /**
     * Normalizes phone number to E.164 format.
     * For Indian 10-digit mobile numbers (e.g. 9876543210), prepends +91.
     * Handles +91XXXXXXXXXX, 91XXXXXXXXXX, and international formats.
     */
    public static String normalize(String rawPhone) {
        if (!StringUtils.hasText(rawPhone)) {
            return null;
        }

        String cleaned = rawPhone.trim().replaceAll("[\\s\\-\\(\\)]", "");

        if (cleaned.startsWith("+")) {
            String digits = cleaned.substring(1);
            if (digits.matches("\\d{10,15}")) {
                return cleaned;
            }
        }

        if (cleaned.matches("\\d{10}")) {
            return "+91" + cleaned;
        }

        if (cleaned.matches("91\\d{10}")) {
            return "+" + cleaned;
        }

        if (cleaned.matches("0\\d{10}")) {
            return "+91" + cleaned.substring(1);
        }

        if (cleaned.matches("\\d{11,15}")) {
            return "+" + cleaned;
        }

        return cleaned;
    }
}
