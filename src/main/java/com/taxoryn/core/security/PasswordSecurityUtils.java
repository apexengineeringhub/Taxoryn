package com.taxoryn.core.security;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Security utility for cryptographic password generation and weak password defense.
 */
public final class PasswordSecurityUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?";
    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;

    private static final Set<String> KNOWN_WEAK_PASSWORDS = Set.of(
            "password123!",
            "password123",
            "password",
            "admin123!",
            "admin123",
            "admin",
            "taxoryn123!",
            "taxoryn123",
            "taxoryn",
            "12345678",
            "123456789",
            "1234567890",
            "qwertyuiop",
            "superadmin123!",
            "changeme"
    );

    private PasswordSecurityUtils() {
    }

    /**
     * Generates a 16-character high-entropy temporary password containing uppercase,
     * lowercase, digits, and special characters.
     */
    public static String generateSecureTemporaryPassword() {
        StringBuilder password = new StringBuilder();
        password.append(UPPERCASE.charAt(RANDOM.nextInt(UPPERCASE.length())));
        password.append(LOWERCASE.charAt(RANDOM.nextInt(LOWERCASE.length())));
        password.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        password.append(SPECIAL.charAt(RANDOM.nextInt(SPECIAL.length())));

        for (int i = 4; i < 16; i++) {
            password.append(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }

        List<Character> chars = password.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        Collections.shuffle(chars, RANDOM);

        StringBuilder shuffled = new StringBuilder();
        chars.forEach(shuffled::append);
        return shuffled.toString();
    }

    /**
     * Checks whether a password matches known weak/demo passwords.
     */
    public static boolean isKnownDefaultOrWeakPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return true;
        }
        String normalized = password.trim().toLowerCase();
        return KNOWN_WEAK_PASSWORDS.contains(normalized);
    }

    /**
     * Validates that a password satisfies production security requirements (>= 12 chars, upper, lower, digit, special, not weak).
     */
    public static boolean isStrongProductionPassword(String password) {
        if (password == null || password.length() < 12) {
            return false;
        }
        if (isKnownDefaultOrWeakPassword(password)) {
            return false;
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> SPECIAL.indexOf(ch) >= 0);

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
