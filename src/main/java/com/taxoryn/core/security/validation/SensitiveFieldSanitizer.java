package com.taxoryn.core.security.validation;

import java.util.Locale;
import java.util.Set;

/**
 * Central security utility to detect sensitive fields (passwords, tokens, secrets, API keys, PINs)
 * and ensure submitted secret values are never reflected in validation or error response payloads.
 */
public final class SensitiveFieldSanitizer {

    private static final Set<String> SENSITIVE_EXACT_FIELDS = Set.of(
            "password",
            "confirmpassword",
            "currentpassword",
            "newpassword",
            "oldpassword",
            "adminpassword",
            "temporarypassword",
            "userpassword",
            "token",
            "refreshtoken",
            "resettoken",
            "activationtoken",
            "verificationtoken",
            "authtoken",
            "bearer",
            "jwt",
            "secret",
            "clientsecret",
            "apisecret",
            "sharedsecret",
            "apikey",
            "api_key",
            "accesstoken",
            "access_token",
            "pin",
            "otp",
            "cvv",
            "cvv2",
            "authorization"
    );

    private SensitiveFieldSanitizer() {
    }

    /**
     * Determines whether the given field name represents a sensitive credential or secret.
     */
    public static boolean isSensitiveField(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }

        // Extract leaf field name in nested property paths (e.g. "user.password", "items[0].apiKey")
        String leafName = fieldName;
        int lastDot = leafName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < leafName.length() - 1) {
            leafName = leafName.substring(lastDot + 1);
        }
        int lastBracket = leafName.lastIndexOf(']');
        if (lastBracket >= 0 && lastBracket < leafName.length() - 1) {
            leafName = leafName.substring(lastBracket + 1);
        }

        String normalized = leafName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (SENSITIVE_EXACT_FIELDS.contains(normalized)) {
            return true;
        }

        return normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("apikey")
                || normalized.contains("authcode")
                || normalized.contains("otp")
                || normalized.contains("pin");
    }

    /**
     * Sanitizes the rejected value for validation responses.
     * Returns {@code null} if the field is sensitive (which is omitted from JSON via @JsonInclude(NON_NULL)),
     * otherwise returns the original value.
     */
    public static Object sanitizeRejectedValue(String fieldName, Object rejectedValue) {
        if (isSensitiveField(fieldName)) {
            return null;
        }
        return rejectedValue;
    }
}
