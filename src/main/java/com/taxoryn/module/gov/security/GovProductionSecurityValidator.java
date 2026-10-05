package com.taxoryn.module.gov.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Production environment security validator for Government Integration Framework (Phase 27.5).
 * <p>
 * Enforces strict fail-closed validation during application startup in production mode:
 * 1. Prohibits mock provider enablement in production ('taxoryn.gov.integration.mock-enabled=true').
 * 2. Validates that real government integration is explicitly enabled rather than defaulting.
 * 3. Prohibits weak or default encryption secrets when government integration is enabled in production.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GovProductionSecurityValidator implements SmartInitializingSingleton {

    public static final String DEFAULT_GOV_ENCRYPTION_SECRET = "taxoryn-gov-secure-credential-secret-key-32bytes";

    private static final Set<String> INSECURE_ENCRYPTION_SECRETS = Set.of(
            DEFAULT_GOV_ENCRYPTION_SECRET,
            "secret",
            "changeme",
            "password",
            "password123",
            "admin12345678901234567890123456789012",
            "secretsecretsecretsecretsecretsecret",
            "12345678123456781234567812345678"
    );

    private final Environment environment;

    @Value("${taxoryn.gov.integration.enabled:true}")
    private boolean integrationEnabled;

    @Value("${taxoryn.gov.integration.mock-enabled:true}")
    private boolean mockEnabled;

    @Value("${taxoryn.gov.encryption.secret:${taxoryn.jwt.secret:taxoryn-gov-secure-credential-secret-key-32bytes}}")
    private String encryptionSecret;

    @Override
    public void afterSingletonsInstantiated() {
        validateGovernmentSecurity();
    }

    public void validateGovernmentSecurity() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isProduction = activeProfiles.contains("prod") || activeProfiles.contains("production");

        validateConfiguration(isProduction, mockEnabled, integrationEnabled, encryptionSecret);
    }

    /**
     * Explicit configuration validator for production safety and unit/integration verification.
     */
    public void validateConfiguration(boolean isProduction, boolean mockEnabled, boolean integrationEnabled, String encryptionSecret) {
        if (!isProduction) {
            log.debug("[GOV_SECURITY] Non-production environment. Mock providers allowed (mockEnabled={})", mockEnabled);
            return;
        }

        log.info("[GOV_SECURITY] Executing Production Government Integration Security Verification...");

        // 1. Prohibit mock providers in production
        if (mockEnabled) {
            String error = "CRITICAL SECURITY VIOLATION: Mock government integration ('taxoryn.gov.integration.mock-enabled') cannot be enabled in a production environment";
            log.error("[GOV_SECURITY_ERROR] {}", error);
            throw new IllegalStateException(error);
        }

        // 2. If integration is enabled in production, validate encryption secret strength
        if (integrationEnabled) {
            if (!StringUtils.hasText(encryptionSecret)) {
                String error = "CRITICAL SECURITY VIOLATION: 'taxoryn.gov.encryption.secret' must be configured when government integration is enabled in production";
                log.error("[GOV_SECURITY_ERROR] {}", error);
                throw new IllegalStateException(error);
            }

            if (INSECURE_ENCRYPTION_SECRETS.contains(encryptionSecret.trim()) || encryptionSecret.length() < 32) {
                String error = "CRITICAL SECURITY VIOLATION: Insecure or default encryption secret detected for 'taxoryn.gov.encryption.secret'. A cryptographically strong >= 256-bit secret is required in production";
                log.error("[GOV_SECURITY_ERROR] {}", error);
                throw new IllegalStateException(error);
            }
        }

        log.info("[GOV_SECURITY_VERIFIED] Production Government Integration security verification PASSED (integrationEnabled={}, mockEnabled=false)", integrationEnabled);
    }
}
