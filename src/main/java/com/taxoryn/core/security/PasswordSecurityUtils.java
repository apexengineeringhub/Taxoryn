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

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 100;
    public static final String PASSWORD_REQUIREMENTS_MESSAGE =
            "Password must be at least 12 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character";

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
     * Validates that a password satisfies production security requirements (>= 12 chars, <= 100 chars, upper, lower, digit, special, not weak).
     */
    public static boolean isStrongProductionPassword(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }
        if (isKnownDefaultOrWeakPassword(password)) {
            return false;
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> SPECIAL.indexOf(ch) >= 0 || (!Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch)));

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Programmatic validator throwing BadRequestException with a clear security message.
     */
    public static void validatePassword(String password) {
        if (!isStrongProductionPassword(password)) {
            if (isKnownDefaultOrWeakPassword(password)) {
                throw new com.taxoryn.core.exception.BadRequestException("Password is too weak or commonly used. Please choose a stronger password.");
            }
            throw new com.taxoryn.core.exception.BadRequestException(PASSWORD_REQUIREMENTS_MESSAGE);
        }
    }

    /**
     * Generates a 32-byte cryptographically secure URL-safe token.
     */
    public static String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Hashes a raw token using SHA-256.
     */
    public static String hashSha256(String rawToken) {
        if (rawToken == null) return null;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
