package com.taxoryn.module.gov.reliability.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility for sanitizing logging and diagnostic messages to prevent secret and sensitive credential leakage.
 */
public final class GovReliabilitySanitizer {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "secret", "token", "accesstoken", "refreshtoken",
            "otp", "evc", "evccode", "pin", "privatekey", "clientsecret",
            "authorization", "auth", "rawsecret", "credentials"
    );

    private static final Pattern AUTH_HEADER_PATTERN = Pattern.compile(
            "(?i)authorization\\s*[:=]\\s*['\"]?([^'\",\\n\\r]+)['\"]?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(password|secret|token|accesstoken|refreshtoken|otp|evc|pin|privatekey|client_secret)(?:\\s+code)?\\s*(?:[:=]|\\s+is\\s+|\\s*:\\s*|\\s*=\\s*)\\s*['\"]?([^'\",\\s&]+)['\"]?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)bearer\\s+([a-zA-Z0-9._\\-]+)",
            Pattern.CASE_INSENSITIVE
    );

    private GovReliabilitySanitizer() {
    }

    /**
     * Sanitizes a key-value map by masking values whose keys match sensitive names.
     */
    public static Map<String, Object> sanitizeMap(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            String normalizedKey = key.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (SENSITIVE_KEYS.contains(normalizedKey)) {
                sanitized.put(key, "[REDACTED]");
            } else if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) entry.getValue();
                sanitized.put(key, sanitizeMap(subMap));
            } else if (entry.getValue() instanceof String strVal) {
                sanitized.put(key, sanitizeString(strVal));
            } else {
                sanitized.put(key, entry.getValue());
            }
        }
        return sanitized;
    }

    /**
     * Sanitizes a string by masking known secret patterns, authorization headers, and Bearer tokens.
     */
    public static String sanitizeString(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String sanitized = AUTH_HEADER_PATTERN.matcher(input).replaceAll("Authorization: [REDACTED]");
        sanitized = SENSITIVE_PATTERN.matcher(sanitized).replaceAll("$1=[REDACTED]");
        sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("Bearer [REDACTED]");
        return sanitized;
    }
}
