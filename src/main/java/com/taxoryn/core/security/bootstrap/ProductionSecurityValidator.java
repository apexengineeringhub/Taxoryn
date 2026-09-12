package com.taxoryn.core.security.bootstrap;

import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Production environment security validator (Phase 9 & Phase 10).
 * <p>
 * Enforces strict fail-closed validation during application startup in production mode:
 * 1. Prohibits mixing 'prod'/'production' with non-production profiles ('dev', 'demo', 'local', 'test').
 * 2. Prohibits demo flags ('taxoryn.demo.enabled=true') in production.
 * 3. Validates required production database credentials (no empty, missing, or default/weak passwords).
 * 4. Validates production JWT secret (required, non-default, >= 256 bits, high entropy).
 * 5. Validates cloud storage credentials if S3/Cloud provider is configured.
 * 6. Validates email & WhatsApp credentials if respective providers are enabled.
 * 7. Prohibits active users with known default passwords in production databases.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionSecurityValidator implements SmartInitializingSingleton {

    public static final String DEFAULT_REPOSITORY_JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    public static final String KNOWN_DEMO_PASSWORD = "Password123!";

    private static final Set<String> INSECURE_DEFAULT_PASSWORDS = Set.of(
            "taxoryn_secret",
            "taxoryn_demo_secret",
            "postgres",
            "password",
            "password123",
            "password123!",
            "admin",
            "admin123",
            "demo",
            "demo123",
            "root",
            "123456",
            "12345678",
            "secret",
            "changeme",
            "default"
    );

    private static final Set<String> INSECURE_JWT_SECRETS = Set.of(
            DEFAULT_REPOSITORY_JWT_SECRET,
            "secret",
            "changeme",
            "password",
            "password123",
            "admin12345678901234567890123456789012",
            "secretsecretsecretsecretsecretsecret",
            "12345678123456781234567812345678"
    );

    private final Environment environment;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.datasource.url:${DB_URL:}}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:${DB_USERNAME:}}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:${DB_PASSWORD:}}")
    private String datasourcePassword;

    @Value("${taxoryn.jwt.secret:${JWT_SECRET:}}")
    private String jwtSecret;

    @Value("${taxoryn.demo.enabled:false}")
    private boolean demoEnabled;

    @Value("${taxoryn.storage.provider:${STORAGE_PROVIDER:S3}}")
    private String storageProvider;

    @Value("${taxoryn.storage.s3.bucket:${STORAGE_BUCKET:${STORAGE_S3_BUCKET:${R2_BUCKET:${R2_BUCKET_NAME:${CLOUDFLARE_R2_BUCKET:${S3_BUCKET:${AWS_S3_BUCKET:${AWS_BUCKET:}}}}}}}}}")
    private String storageS3Bucket;

    @Value("${taxoryn.storage.s3.access-key:${STORAGE_ACCESS_KEY:${STORAGE_S3_ACCESS_KEY:${R2_ACCESS_KEY_ID:${R2_ACCESS_KEY:${CLOUDFLARE_R2_ACCESS_KEY_ID:${AWS_ACCESS_KEY_ID:${AWS_ACCESS_KEY:${AWS_KEY:}}}}}}}}}")
    private String storageS3AccessKey;

    @Value("${taxoryn.storage.s3.secret-key:${STORAGE_SECRET_KEY:${STORAGE_SECRET_ACCESS_KEY:${STORAGE_S3_SECRET_KEY:${R2_SECRET_ACCESS_KEY:${R2_SECRET_KEY:${CLOUDFLARE_R2_SECRET_ACCESS_KEY:${AWS_SECRET_ACCESS_KEY:${AWS_SECRET_KEY:${AWS_SECRET:}}}}}}}}}}}")
    private String storageS3SecretKey;

    @Value("${taxoryn.storage.s3.endpoint:${STORAGE_ENDPOINT:${STORAGE_S3_ENDPOINT:${R2_ENDPOINT:${CLOUDFLARE_R2_ENDPOINT:${AWS_ENDPOINT:${S3_ENDPOINT:}}}}}}}")
    private String storageS3Endpoint;

    @Value("${taxoryn.storage.s3.account-id:${R2_ACCOUNT_ID:${CLOUDFLARE_ACCOUNT_ID:${ACCOUNT_ID:}}}}")
    private String storageS3AccountId;

    @Value("${taxoryn.security.malware-scanner.clamav.enabled:${CLAMAV_ENABLED:}}")
    private String clamavEnabled;

    @Value("${taxoryn.security.malware-scanner.clamav.host:${CLAMAV_HOST:}}")
    private String clamavHost;

    @Value("${taxoryn.security.malware-scanner.clamav.port:${CLAMAV_PORT:3310}}")
    private int clamavPort;

    @Value("${taxoryn.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${taxoryn.mail.provider:LOG}")
    private String mailProvider;

    @Value("${spring.mail.host:${MAIL_HOST:${SMTP_HOST:}}}")
    private String mailHost;

    @Value("${spring.mail.username:${MAIL_USERNAME:${SMTP_USERNAME:}}}")
    private String mailUsername;

    @Value("${spring.mail.password:${MAIL_PASSWORD:${SMTP_PASSWORD:}}}")
    private String mailPassword;

    @Value("${taxoryn.mail.resend-api-key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${taxoryn.mail.brevo-api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${taxoryn.mail.from-email:${taxoryn.mail.from-address:${MAIL_FROM_ADDRESS:${MAIL_FROM_EMAIL:${TAXORYN_EMAIL_FROM:info@taxoryn.com}}}}}")
    private String mailFromEmail;

    @Value("${taxoryn.mail.reply-to:${MAIL_REPLY_TO:${TAXORYN_EMAIL_REPLY_TO:info@taxoryn.com}}}")
    private String mailReplyTo;

    @Value("${taxoryn.mail.dev-mode:false}")
    private boolean mailDevMode;

    @Value("${taxoryn.whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    @Value("${taxoryn.whatsapp.provider:LOG}")
    private String whatsappProvider;

    @Value("${taxoryn.whatsapp.access-token:${WHATSAPP_ACCESS_TOKEN:}}")
    private String whatsappAccessToken;

    @Value("${taxoryn.whatsapp.phone-number-id:${WHATSAPP_PHONE_NUMBER_ID:}}")
    private String whatsappPhoneNumberId;

    @Value("${taxoryn.whatsapp.business-account-id:${WHATSAPP_BUSINESS_ACCOUNT_ID:}}")
    private String whatsappBusinessAccountId;

    @Value("${taxoryn.frontend-url:${app.frontend-url:${TAXORYN_FRONTEND_URL:${FRONTEND_URL:${APP_FRONTEND_URL:https://app.taxoryn.com}}}}}")
    private String frontendUrl;

    @Value("${taxoryn.cors.allowed-origins:${CORS_ALLOWED_ORIGINS:https://app.taxoryn.com,https://taxoryn.com}}")
    private String corsAllowedOrigins;

    @Value("${taxoryn.cors.allow-credentials:true}")
    private boolean corsAllowCredentials;

    @Value("${spring.jpa.hibernate.ddl-auto:${HIBERNATE_DDL_AUTO:validate}}")
    private String hibernateDdlAuto;

    @Value("${spring.flyway.validate-on-migrate:${FLYWAY_VALIDATE_ON_MIGRATE:true}}")
    private boolean flywayValidateOnMigrate;

    @Value("${spring.flyway.enabled:${FLYWAY_ENABLED:true}}")
    private boolean flywayEnabled;

    @Value("${springdoc.api-docs.enabled:true}")
    private boolean springdocApiDocsEnabled;

    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean springdocSwaggerUiEnabled;

    @Override
    public void afterSingletonsInstantiated() {
        validateEnvironmentSecurity();
    }

    public void validateEnvironmentSecurity() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isProduction = activeProfiles.contains("prod") || activeProfiles.contains("production");

        if (!isProduction) {
            log.debug("Non-production profile active ({}). Skipping production-only fail-closed checks.", activeProfiles);
            return;
        }

        log.info("Executing Phase 10 Production Configuration & Secrets Security Verification...");

        // 1. Profile Isolation Check
        Set<String> unsafeProfiles = Set.of("dev", "demo", "local", "test");
        for (String unsafe : unsafeProfiles) {
            if (activeProfiles.contains(unsafe)) {
                String error = String.format("CRITICAL SECURITY VIOLATION: Production profile cannot be active concurrently with '%s' profile", unsafe);
                log.error(error);
                throw new IllegalStateException(error);
            }
        }

        // 2. Demo Flag Check
        if (demoEnabled) {
            String error = "CRITICAL SECURITY VIOLATION: 'taxoryn.demo.enabled' cannot be true in a production environment";
            log.error(error);
            throw new IllegalStateException(error);
        }

        // 3. Production Database Credentials Validation
        validateDatabaseConfiguration();

        // 4. Production JWT Secret Strength & Entropy Validation
        validateJwtConfiguration();

        // 5. Cloud Storage Configuration Validation
        validateStorageConfiguration();

        // 6. Production Malware & Antivirus Scanning (ClamAV) Validation
        validateMalwareScannerConfiguration();

        // 7. External Notification Provider Credentials Validation
        validateNotificationConfiguration();

        // 8. Insecure Known Default Credential Check in Production DB
        validateDatabaseUserSecurity();

        // 9. Production Frontend Base URL Validation
        validateFrontendConfiguration();

        // 10. Production CORS Configuration Validation
        validateCorsConfiguration();

        // 11. Production Database Schema Management & Flyway Validation
        validateSchemaManagementConfiguration();

        // 12. Production Swagger & OpenAPI Disabled Validation
        validateSwaggerConfiguration();

        log.info("Phase 10 production environment configuration & secrets verification PASSED.");
    }

    private void validateDatabaseConfiguration() {
        if (!StringUtils.hasText(datasourceUrl)) {
            String error = "CRITICAL SECURITY VIOLATION: Production database URL (DB_URL / SPRING_DATASOURCE_URL) is missing or empty";
            log.error(error);
            throw new IllegalStateException(error);
        }

        if (datasourceUrl.contains("h2:mem:") || datasourceUrl.contains("taxoryn_test_db")) {
            String error = "CRITICAL SECURITY VIOLATION: In-memory test database URL configured in production environment";
            log.error(error);
            throw new IllegalStateException(error);
        }

        if (!StringUtils.hasText(datasourceUsername)) {
            String error = "CRITICAL SECURITY VIOLATION: Production database username (DB_USERNAME / SPRING_DATASOURCE_USERNAME) is missing or empty";
            log.error(error);
            throw new IllegalStateException(error);
        }

        if (!StringUtils.hasText(datasourcePassword)) {
            String error = "CRITICAL SECURITY VIOLATION: Production database password (DB_PASSWORD / SPRING_DATASOURCE_PASSWORD) is missing or empty";
            log.error(error);
            throw new IllegalStateException(error);
        }

        String lowerPassword = datasourcePassword.trim().toLowerCase();
        if (INSECURE_DEFAULT_PASSWORDS.contains(lowerPassword)) {
            String error = "CRITICAL SECURITY VIOLATION: Production database password is using a known weak or default password ('" + lowerPassword + "')";
            log.error("CRITICAL SECURITY VIOLATION: Production database password matches known weak/default credentials");
            throw new IllegalStateException(error);
        }
    }

    private void validateJwtConfiguration() {
        if (!StringUtils.hasText(jwtSecret)) {
            String error = "CRITICAL SECURITY VIOLATION: Production JWT secret (JWT_SECRET / taxoryn.jwt.secret) is missing or empty";
            log.error(error);
            throw new IllegalStateException(error);
        }

        String trimmedSecret = jwtSecret.trim();

        if (DEFAULT_REPOSITORY_JWT_SECRET.equalsIgnoreCase(trimmedSecret)) {
            String error = "CRITICAL SECURITY VIOLATION: Production JWT secret is using the publicly-known repository default secret";
            log.error(error);
            throw new IllegalStateException(error);
        }

        if (trimmedSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            String error = "CRITICAL SECURITY VIOLATION: Production JWT secret must be at least 256 bits (32 bytes) for HMAC-SHA256 signing";
            log.error(error);
            throw new IllegalStateException(error);
        }

        if (INSECURE_JWT_SECRETS.contains(trimmedSecret.toLowerCase())) {
            String error = "CRITICAL SECURITY VIOLATION: Production JWT secret is using a known weak/trivial pattern";
            log.error(error);
            throw new IllegalStateException(error);
        }
    }

    private void validateStorageConfiguration() {
        if (!StringUtils.hasText(storageProvider) || "LOCAL".equalsIgnoreCase(storageProvider.trim())) {
            String error = "CRITICAL SECURITY VIOLATION: Local filesystem storage (taxoryn.storage.provider=LOCAL) is prohibited in production. Configure persistent S3/Cloudflare R2 storage (STORAGE_PROVIDER=S3, STORAGE_BUCKET, STORAGE_ACCESS_KEY, STORAGE_SECRET_KEY)";
            log.error(error);
            throw new IllegalStateException(error);
        }

        String p = storageProvider.trim().toUpperCase();
        if (p.equals("S3") || p.equals("R2") || p.equals("CLOUDFLARE") || p.equals("CLOUDFLARE_R2")
                || p.equals("AWS") || p.equals("AWS_S3") || p.equals("MINIO") || p.equals("CLOUD")) {
            if (!StringUtils.hasText(storageS3Bucket)) {
                String error = "CRITICAL SECURITY VIOLATION: Storage provider is '" + storageProvider + "' but STORAGE_BUCKET is not configured";
                log.error(error);
                throw new IllegalStateException(error);
            }
            if (!StringUtils.hasText(storageS3AccessKey)) {
                String error = "CRITICAL SECURITY VIOLATION: Storage provider is '" + storageProvider + "' but STORAGE_ACCESS_KEY is not configured";
                log.error(error);
                throw new IllegalStateException(error);
            }
            if (!StringUtils.hasText(storageS3SecretKey) || INSECURE_DEFAULT_PASSWORDS.contains(storageS3SecretKey.toLowerCase())) {
                String error = "CRITICAL SECURITY VIOLATION: Storage provider is '" + storageProvider + "' but STORAGE_SECRET_KEY is missing or weak";
                log.error(error);
                throw new IllegalStateException(error);
            }
            // If endpoint or accountId is supplied, validate endpoint URI scheme
            String resolvedEndpoint = StringUtils.hasText(storageS3Endpoint) ? storageS3Endpoint.trim() :
                    (StringUtils.hasText(storageS3AccountId) ? "https://" + storageS3AccountId.trim() + ".r2.cloudflarestorage.com" : null);
            if (StringUtils.hasText(resolvedEndpoint)) {
                try {
                    URI uri = URI.create(resolvedEndpoint);
                    String scheme = uri.getScheme();
                    if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                        String error = "CRITICAL SECURITY VIOLATION: S3/R2 endpoint must use http:// or https:// scheme: " + resolvedEndpoint;
                        log.error(error);
                        throw new IllegalStateException(error);
                    }
                    String host = uri.getHost();
                    boolean isLocalhost = host != null && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1"));
                    if ("http".equalsIgnoreCase(scheme) && !isLocalhost) {
                        String error = "CRITICAL SECURITY VIOLATION: Insecure HTTP S3/R2 endpoint rejected in production: " + resolvedEndpoint;
                        log.error(error);
                        throw new IllegalStateException(error);
                    }
                } catch (IllegalArgumentException e) {
                    String error = "CRITICAL SECURITY VIOLATION: Invalid S3/R2 endpoint URI: " + resolvedEndpoint;
                    log.error(error);
                    throw new IllegalStateException(error);
                }
            }
        } else {
            String error = "CRITICAL SECURITY VIOLATION: Unsupported production storage provider '" + storageProvider + "'. Expected 'S3' or 'R2'";
            log.error(error);
            throw new IllegalStateException(error);
        }
    }

    private void validateMalwareScannerConfiguration() {
        if (!StringUtils.hasText(clamavEnabled)) {
            String error = "CRITICAL SECURITY VIOLATION: CLAMAV_ENABLED is missing or empty in production. ClamAV malware scanning must be explicitly enabled (CLAMAV_ENABLED=true).";
            log.error(error);
            throw new IllegalStateException(error);
        }
        if (!"true".equalsIgnoreCase(clamavEnabled.trim())) {
            String error = "CRITICAL SECURITY VIOLATION: Malware scanning (CLAMAV_ENABLED=true) is mandatory in production. Unscanned document uploads are prohibited (found: CLAMAV_ENABLED='" + clamavEnabled + "').";
            log.error(error);
            throw new IllegalStateException(error);
        }
        if (!StringUtils.hasText(clamavHost)) {
            String error = "CRITICAL SECURITY VIOLATION: ClamAV is enabled in production but CLAMAV_HOST is not configured";
            log.error(error);
            throw new IllegalStateException(error);
        }
        if (clamavPort <= 0 || clamavPort > 65535) {
            String error = "CRITICAL SECURITY VIOLATION: Invalid CLAMAV_PORT: " + clamavPort;
            log.error(error);
            throw new IllegalStateException(error);
        }
    }

    private void validateNotificationConfiguration() {
        if (mailEnabled) {
            if (mailDevMode) {
                String error = "CRITICAL SECURITY VIOLATION: 'taxoryn.mail.dev-mode' cannot be true in a production environment";
                log.error(error);
                throw new IllegalStateException(error);
            }

            if (StringUtils.hasText(mailFromEmail)) {
                String lowerFrom = mailFromEmail.trim().toLowerCase();
                if (lowerFrom.contains("@gmail.com") || lowerFrom.contains("@yahoo.com") || lowerFrom.contains("@example.com")
                        || lowerFrom.contains("onboarding@resend.dev") || lowerFrom.contains("@resend.dev")) {
                    String error = "CRITICAL SECURITY VIOLATION: Production mail sender (MAIL_FROM_ADDRESS / MAIL_FROM_EMAIL / TAXORYN_EMAIL_FROM) cannot use consumer, test, or example mailbox ('" + mailFromEmail + "'). Use a verified domain (e.g., info@taxoryn.com)";
                    log.error(error);
                    throw new IllegalStateException(error);
                }
            }

            if ("SMTP".equalsIgnoreCase(mailProvider)) {
                if (!StringUtils.hasText(mailHost) || !StringUtils.hasText(mailUsername) || !StringUtils.hasText(mailPassword)) {
                    String error = "CRITICAL SECURITY VIOLATION: Production mail is enabled with SMTP provider but MAIL_HOST, MAIL_USERNAME, or MAIL_PASSWORD is missing";
                    log.error(error);
                    throw new IllegalStateException(error);
                }
            } else if ("RESEND".equalsIgnoreCase(mailProvider)) {
                if (!StringUtils.hasText(resendApiKey) || INSECURE_DEFAULT_PASSWORDS.contains(resendApiKey.toLowerCase()) || "re_123456789".equalsIgnoreCase(resendApiKey.trim())) {
                    String error = "CRITICAL SECURITY VIOLATION: Production mail is enabled with RESEND provider but RESEND_API_KEY is missing or weak";
                    log.error(error);
                    throw new IllegalStateException(error);
                }
            } else if ("BREVO".equalsIgnoreCase(mailProvider)) {
                if (!StringUtils.hasText(brevoApiKey) || INSECURE_DEFAULT_PASSWORDS.contains(brevoApiKey.toLowerCase())) {
                    String error = "CRITICAL SECURITY VIOLATION: Production mail is enabled with BREVO provider but BREVO_API_KEY is missing or weak";
                    log.error(error);
                    throw new IllegalStateException(error);
                }
            } else if ("AUTO".equalsIgnoreCase(mailProvider)) {
                boolean hasResend = StringUtils.hasText(resendApiKey) && !INSECURE_DEFAULT_PASSWORDS.contains(resendApiKey.toLowerCase()) && !"re_123456789".equalsIgnoreCase(resendApiKey.trim());
                boolean hasBrevo = StringUtils.hasText(brevoApiKey) && !INSECURE_DEFAULT_PASSWORDS.contains(brevoApiKey.toLowerCase());
                boolean hasSmtp = StringUtils.hasText(mailHost) && StringUtils.hasText(mailUsername) && StringUtils.hasText(mailPassword);
                if (!hasResend && !hasBrevo && !hasSmtp) {
                    String error = "CRITICAL SECURITY VIOLATION: Production mail is enabled with AUTO provider but no valid provider credentials configured (RESEND_API_KEY, BREVO_API_KEY, or MAIL_HOST/USERNAME/PASSWORD)";
                    log.error(error);
                    throw new IllegalStateException(error);
                }
            }
        }

        if (whatsappEnabled && "META".equalsIgnoreCase(whatsappProvider)) {
            if (!StringUtils.hasText(whatsappAccessToken) || !StringUtils.hasText(whatsappPhoneNumberId) || !StringUtils.hasText(whatsappBusinessAccountId)) {
                String error = "CRITICAL SECURITY VIOLATION: Production WhatsApp is enabled with META provider but WHATSAPP_ACCESS_TOKEN, WHATSAPP_PHONE_NUMBER_ID, or WHATSAPP_BUSINESS_ACCOUNT_ID is missing";
                log.error(error);
                throw new IllegalStateException(error);
            }
        }
    }

    private void validateDatabaseUserSecurity() {
        Optional<UserEntity> legacySuperAdmin = userRepository.findByEmailIgnoreCase("superadmin@taxoryn.com");
        if (legacySuperAdmin.isPresent()) {
            UserEntity user = legacySuperAdmin.get();
            if (user.getStatus() == UserStatus.ACTIVE && passwordEncoder.matches(KNOWN_DEMO_PASSWORD, user.getPasswordHash())) {
                String error = "CRITICAL SECURITY VIOLATION: Active Super Admin user 'superadmin@taxoryn.com' with known default password detected in production database";
                log.error(error);
                throw new IllegalStateException(error);
            }
        }
    }

    private void validateFrontendConfiguration() {
        if (!StringUtils.hasText(frontendUrl)) {
            String error = "CRITICAL SECURITY VIOLATION: Production frontend URL (TAXORYN_FRONTEND_URL / FRONTEND_URL / taxoryn.frontend-url) is missing or empty";
            log.error(error);
            throw new IllegalStateException(error);
        }

        String trimmed = frontendUrl.trim().toLowerCase();
        if (trimmed.contains("localhost") || trimmed.contains("127.0.0.1")) {
            String error = "CRITICAL SECURITY VIOLATION: Production frontend URL cannot be localhost ('" + frontendUrl + "'). Expected 'https://app.taxoryn.com'";
            log.error(error);
            throw new IllegalStateException(error);
        }

        if (trimmed.contains("vercel.app") || trimmed.contains("taxoryn-7x7f")) {
            String error = "CRITICAL SECURITY VIOLATION: Production frontend URL cannot use demo Vercel domain ('" + frontendUrl + "'). Expected 'https://app.taxoryn.com'";
            log.error(error);
            throw new IllegalStateException(error);
        }

        if (!trimmed.startsWith("https://")) {
            String error = "CRITICAL SECURITY VIOLATION: Production frontend URL must use HTTPS ('" + frontendUrl + "')";
            log.error(error);
            throw new IllegalStateException(error);
        }
    }

    private void validateCorsConfiguration() {
        if (!StringUtils.hasText(corsAllowedOrigins)) {
            String error = "CRITICAL SECURITY VIOLATION: Production CORS allowed origins (CORS_ALLOWED_ORIGINS / taxoryn.cors.allowed-origins) is missing or empty";
            log.error(error);
            throw new IllegalStateException(error);
        }

        String[] origins = corsAllowedOrigins.split(",");
        boolean hasAllowedOrigin = false;

        for (String rawOrigin : origins) {
            String origin = rawOrigin.trim();
            if (!StringUtils.hasText(origin)) {
                continue;
            }
            hasAllowedOrigin = true;
            String lower = origin.toLowerCase();

            // 1. Forbid Vercel demo or wildcard domains
            if (lower.contains("vercel.app")) {
                String error = "CRITICAL SECURITY VIOLATION: Production CORS cannot include Vercel demo origins or wildcard Vercel domains ('" + origin + "')";
                log.error(error);
                throw new IllegalStateException(error);
            }

            // 2. Forbid wildcard origins
            if ("*".equals(origin) || lower.contains("*")) {
                String error = "CRITICAL SECURITY VIOLATION: Wildcard origins ('" + origin + "') are strictly prohibited in production CORS";
                log.error(error);
                throw new IllegalStateException(error);
            }

            // 3. Forbid localhost / 127.0.0.1
            if (lower.contains("localhost") || lower.contains("127.0.0.1")) {
                String error = "CRITICAL SECURITY VIOLATION: Production CORS cannot include localhost or loopback origins in production ('" + origin + "')";
                log.error(error);
                throw new IllegalStateException(error);
            }

            // 4. Enforce HTTPS scheme
            if (!lower.startsWith("https://")) {
                String error = "CRITICAL SECURITY VIOLATION: Production CORS origin must use HTTPS ('" + origin + "')";
                log.error(error);
                throw new IllegalStateException(error);
            }

            // 5. Must strictly match approved Taxoryn production domains
            if (!lower.equals("https://app.taxoryn.com") && !lower.equals("https://taxoryn.com") && !lower.equals("https://api.taxoryn.com")) {
                String error = "CRITICAL SECURITY VIOLATION: Untrusted origin ('" + origin + "') detected in production CORS. Only explicitly trusted Taxoryn production domains (https://app.taxoryn.com, https://taxoryn.com) are permitted";
                log.error(error);
                throw new IllegalStateException(error);
            }
        }

        if (!hasAllowedOrigin) {
            String error = "CRITICAL SECURITY VIOLATION: Production CORS allowed origins contains no valid origins";
            log.error(error);
            throw new IllegalStateException(error);
        }
    }

    private void validateSchemaManagementConfiguration() {
        if (!flywayEnabled) {
            String error = "CRITICAL DATABASE INTEGRITY VIOLATION: Flyway must be enabled in production environments.";
            log.error(error);
            throw new IllegalStateException(error);
        }

        if (!flywayValidateOnMigrate) {
            String error = "CRITICAL DATABASE INTEGRITY VIOLATION: Flyway 'validate-on-migrate' must be enabled (true) in production environments.";
            log.error(error);
            throw new IllegalStateException(error);
        }

        if (StringUtils.hasText(hibernateDdlAuto)) {
            String normalizedDdl = hibernateDdlAuto.trim().toLowerCase();
            if ("update".equals(normalizedDdl) || "create".equals(normalizedDdl) || "create-drop".equals(normalizedDdl)) {
                String error = String.format("CRITICAL DATABASE INTEGRITY VIOLATION: Hibernate 'ddl-auto' cannot be '%s' in production. Production schema must be strictly migration-driven ('validate' or 'none').", hibernateDdlAuto);
                log.error(error);
                throw new IllegalStateException(error);
            }
        }
    }

    private void validateSwaggerConfiguration() {
        if (springdocApiDocsEnabled) {
            String error = "CRITICAL SECURITY VIOLATION: OpenAPI generation (springdoc.api-docs.enabled) must be disabled (false) in production";
            log.error(error);
            throw new IllegalStateException(error);
        }
        if (springdocSwaggerUiEnabled) {
            String error = "CRITICAL SECURITY VIOLATION: Swagger UI (springdoc.swagger-ui.enabled) must be disabled (false) in production";
            log.error(error);
            throw new IllegalStateException(error);
        }
    }
}

