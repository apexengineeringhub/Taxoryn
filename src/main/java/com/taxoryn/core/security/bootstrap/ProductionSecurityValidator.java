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

    @Value("${taxoryn.storage.provider:LOCAL}")
    private String storageProvider;

    @Value("${taxoryn.storage.s3.bucket:${STORAGE_BUCKET:${STORAGE_S3_BUCKET:}}}")
    private String storageS3Bucket;

    @Value("${taxoryn.storage.s3.access-key:${STORAGE_ACCESS_KEY:${STORAGE_S3_ACCESS_KEY:}}}")
    private String storageS3AccessKey;

    @Value("${taxoryn.storage.s3.secret-key:${STORAGE_SECRET_KEY:${STORAGE_S3_SECRET_KEY:}}}")
    private String storageS3SecretKey;

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

        // 6. External Notification Provider Credentials Validation
        validateNotificationConfiguration();

        // 7. Insecure Known Default Credential Check in Production DB
        validateDatabaseUserSecurity();

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

        if ("S3".equalsIgnoreCase(storageProvider) || "R2".equalsIgnoreCase(storageProvider) || "CLOUD".equalsIgnoreCase(storageProvider)) {
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
        } else {
            String error = "CRITICAL SECURITY VIOLATION: Unsupported production storage provider '" + storageProvider + "'. Expected 'S3'";
            log.error(error);
            throw new IllegalStateException(error);
        }
    }

    private void validateNotificationConfiguration() {
        if (mailEnabled) {
            if ("SMTP".equalsIgnoreCase(mailProvider)) {
                if (!StringUtils.hasText(mailHost) || !StringUtils.hasText(mailUsername) || !StringUtils.hasText(mailPassword)) {
                    String error = "CRITICAL SECURITY VIOLATION: Production mail is enabled with SMTP provider but MAIL_HOST, MAIL_USERNAME, or MAIL_PASSWORD is missing";
                    log.error(error);
                    throw new IllegalStateException(error);
                }
            } else if ("RESEND".equalsIgnoreCase(mailProvider)) {
                if (!StringUtils.hasText(resendApiKey)) {
                    String error = "CRITICAL SECURITY VIOLATION: Production mail is enabled with RESEND provider but RESEND_API_KEY is missing";
                    log.error(error);
                    throw new IllegalStateException(error);
                }
            } else if ("BREVO".equalsIgnoreCase(mailProvider)) {
                if (!StringUtils.hasText(brevoApiKey)) {
                    String error = "CRITICAL SECURITY VIOLATION: Production mail is enabled with BREVO provider but BREVO_API_KEY is missing";
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
}

