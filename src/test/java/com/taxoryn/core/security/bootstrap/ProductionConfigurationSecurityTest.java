package com.taxoryn.core.security.bootstrap;

import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionConfigurationSecurityTest {

    @Mock
    private Environment environment;

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;

    private static final String VALID_PROD_JWT_SECRET = "c3VwZXJzZWNyZXRwcm9kdWN0aW9ua2V5MTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
    private static final String VALID_PROD_DB_URL = "jdbc:postgresql://db.taxoryn.internal:5432/taxoryn_prod";
    private static final String VALID_PROD_DB_USER = "taxoryn_prod_app";
    private static final String VALID_PROD_DB_PASS = "Tx9#SecureP@ss2026!ProdDb";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    private ProductionSecurityValidator createValidator() {
        return new ProductionSecurityValidator(environment, userRepository, passwordEncoder);
    }

    private void configureValidProductionBasics(ProductionSecurityValidator validator) {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        ReflectionTestUtils.setField(validator, "datasourceUrl", VALID_PROD_DB_URL);
        ReflectionTestUtils.setField(validator, "datasourceUsername", VALID_PROD_DB_USER);
        ReflectionTestUtils.setField(validator, "datasourcePassword", VALID_PROD_DB_PASS);
        ReflectionTestUtils.setField(validator, "jwtSecret", VALID_PROD_JWT_SECRET);
        ReflectionTestUtils.setField(validator, "demoEnabled", false);
        ReflectionTestUtils.setField(validator, "storageProvider", "S3");
        ReflectionTestUtils.setField(validator, "storageS3Bucket", "taxoryn-production-docs");
        ReflectionTestUtils.setField(validator, "storageS3AccessKey", "AKIAIOSFODNN7EXAMPLE");
        ReflectionTestUtils.setField(validator, "storageS3SecretKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        ReflectionTestUtils.setField(validator, "mailEnabled", false);
        ReflectionTestUtils.setField(validator, "mailDevMode", false);
        ReflectionTestUtils.setField(validator, "whatsappEnabled", false);
        ReflectionTestUtils.setField(validator, "frontendUrl", "https://app.taxoryn.com");
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "https://app.taxoryn.com,https://taxoryn.com");
        ReflectionTestUtils.setField(validator, "hibernateDdlAuto", "validate");
        ReflectionTestUtils.setField(validator, "flywayValidateOnMigrate", true);
        ReflectionTestUtils.setField(validator, "flywayEnabled", true);
    }

    // =========================================================================
    // 1. Production JWT Fail-Fast Tests
    // =========================================================================

    @Test
    @DisplayName("Fail-Fast: Production fails when JWT_SECRET is missing or empty")
    void testProductionFailsWhenJwtSecretMissing() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "jwtSecret", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("JWT secret"));
        assertTrue(ex.getMessage().contains("missing or empty"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when JWT_SECRET is the repository default")
    void testProductionFailsWhenJwtSecretIsRepositoryDefault() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "jwtSecret", ProductionSecurityValidator.DEFAULT_REPOSITORY_JWT_SECRET);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("publicly-known repository default"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when JWT_SECRET is shorter than 256 bits")
    void testProductionFailsWhenJwtSecretIsTooShort() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "jwtSecret", "short_secret_under_32_bytes");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("must be at least 256 bits (32 bytes)"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when JWT_SECRET is a trivial repeating string")
    void testProductionFailsWhenJwtSecretIsTrivialPattern() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "jwtSecret", "secretsecretsecretsecretsecretsecret");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("known weak/trivial pattern"));
    }

    // =========================================================================
    // 2. Production Database Credentials Fail-Fast Tests
    // =========================================================================

    @Test
    @DisplayName("Fail-Fast: Production fails when DB_URL is missing")
    void testProductionFailsWhenDbUrlMissing() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "datasourceUrl", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("database URL"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when DB_URL points to in-memory H2 in production")
    void testProductionFailsWhenDbUrlIsH2InProd() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "datasourceUrl", "jdbc:h2:mem:taxoryn_test_db");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("In-memory test database URL"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when DB_USERNAME is missing")
    void testProductionFailsWhenDbUsernameMissing() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "datasourceUsername", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("database username"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when DB_PASSWORD is missing")
    void testProductionFailsWhenDbPasswordMissing() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "datasourcePassword", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("database password"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when DB_PASSWORD matches default password 'taxoryn_secret'")
    void testProductionFailsWhenDbPasswordIsDefaultTaxorynSecret() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "datasourcePassword", "taxoryn_secret");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("known weak or default password"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when DB_PASSWORD matches standard default 'postgres' or 'password123'")
    void testProductionFailsWhenDbPasswordIsCommonDefault() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "datasourcePassword", "postgres");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("known weak or default password"));
    }

    // =========================================================================
    // 3. Storage Provider Fail-Fast Tests
    // =========================================================================

    @Test
    @DisplayName("Fail-Fast: Production fails when local storage provider is used")
    void testProductionFailsWhenLocalStorageInProduction() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "storageProvider", "LOCAL");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("Local filesystem storage"));
        assertTrue(ex.getMessage().contains("prohibited in production"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when S3 storage provider is selected but STORAGE_BUCKET is missing")
    void testProductionFailsWhenS3BucketMissing() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "storageProvider", "S3");
        ReflectionTestUtils.setField(validator, "storageS3Bucket", "");
        ReflectionTestUtils.setField(validator, "storageS3AccessKey", "AKIAIOSFODNN7EXAMPLE");
        ReflectionTestUtils.setField(validator, "storageS3SecretKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("STORAGE_BUCKET is not configured"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when S3 storage provider is selected but STORAGE_ACCESS_KEY is missing")
    void testProductionFailsWhenS3AccessKeyMissing() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "storageProvider", "S3");
        ReflectionTestUtils.setField(validator, "storageS3Bucket", "taxoryn-production-docs");
        ReflectionTestUtils.setField(validator, "storageS3AccessKey", "");
        ReflectionTestUtils.setField(validator, "storageS3SecretKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("STORAGE_ACCESS_KEY is not configured"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when S3 storage provider is selected but STORAGE_SECRET_KEY is missing or default")
    void testProductionFailsWhenS3SecretKeyMissingOrWeak() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "storageProvider", "S3");
        ReflectionTestUtils.setField(validator, "storageS3Bucket", "taxoryn-production-docs");
        ReflectionTestUtils.setField(validator, "storageS3AccessKey", "AKIAIOSFODNN7EXAMPLE");
        ReflectionTestUtils.setField(validator, "storageS3SecretKey", "changeme");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("STORAGE_SECRET_KEY is missing or weak"));
    }

    // =========================================================================
    // 4. Notification Provider Fail-Fast Tests
    // =========================================================================

    @Test
    @DisplayName("Fail-Fast: Production fails when Mail is enabled with SMTP but SMTP credentials are missing")
    void testProductionFailsWhenSmtpCredentialsMissing() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "mailEnabled", true);
        ReflectionTestUtils.setField(validator, "mailProvider", "SMTP");
        ReflectionTestUtils.setField(validator, "mailHost", "smtp.sendgrid.net");
        ReflectionTestUtils.setField(validator, "mailUsername", "apikey");
        ReflectionTestUtils.setField(validator, "mailPassword", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("MAIL_HOST, MAIL_USERNAME, or MAIL_PASSWORD is missing"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when Mail is enabled with RESEND but API key is missing")
    void testProductionFailsWhenResendApiKeyMissing() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "mailEnabled", true);
        ReflectionTestUtils.setField(validator, "mailProvider", "RESEND");
        ReflectionTestUtils.setField(validator, "resendApiKey", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("RESEND_API_KEY is missing or weak"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when Mail is enabled with RESEND but API key is placeholder")
    void testProductionFailsWhenResendApiKeyIsPlaceholder() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "mailEnabled", true);
        ReflectionTestUtils.setField(validator, "mailProvider", "RESEND");
        ReflectionTestUtils.setField(validator, "resendApiKey", "re_123456789");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("RESEND_API_KEY is missing or weak"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when Mail sender is generic gmail in production")
    void testProductionFailsWhenSenderIsGmailInProd() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "mailEnabled", true);
        ReflectionTestUtils.setField(validator, "mailProvider", "RESEND");
        ReflectionTestUtils.setField(validator, "resendApiKey", "re_987654321_ValidResendApiKeyProduction123");
        ReflectionTestUtils.setField(validator, "mailFromEmail", "taxoryn@gmail.com");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("cannot use consumer, test, or example mailbox"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when Mail sender is onboarding@resend.dev test sender")
    void testProductionFailsWhenSenderIsOnboardingResendDevInProd() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "mailEnabled", true);
        ReflectionTestUtils.setField(validator, "mailProvider", "RESEND");
        ReflectionTestUtils.setField(validator, "resendApiKey", "re_987654321_ValidResendApiKeyProduction123");
        ReflectionTestUtils.setField(validator, "mailFromEmail", "onboarding@resend.dev");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("cannot use consumer, test, or example mailbox"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when Mail dev-mode is enabled in production")
    void testProductionFailsWhenMailDevModeEnabledInProd() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "mailEnabled", true);
        ReflectionTestUtils.setField(validator, "mailProvider", "RESEND");
        ReflectionTestUtils.setField(validator, "resendApiKey", "re_987654321_ValidResendApiKeyProduction123");
        ReflectionTestUtils.setField(validator, "mailFromEmail", "info@taxoryn.com");
        ReflectionTestUtils.setField(validator, "mailDevMode", true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("'taxoryn.mail.dev-mode' cannot be true in a production environment"));
    }

    @Test
    @DisplayName("Success: Production passes when Mail is enabled with valid RESEND configuration")
    void testProductionPassesWithValidResendConfiguration() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "mailEnabled", true);
        ReflectionTestUtils.setField(validator, "mailProvider", "RESEND");
        ReflectionTestUtils.setField(validator, "resendApiKey", "re_987654321_ValidResendApiKeyProduction123");
        ReflectionTestUtils.setField(validator, "mailFromEmail", "notifications@taxoryn.com");
        ReflectionTestUtils.setField(validator, "mailReplyTo", "support@taxoryn.com");

        UserEntity inactiveLegacyUser = UserEntity.builder()
                .email("superadmin@taxoryn.com")
                .status(UserStatus.INACTIVE)
                .passwordHash("$2a$12$DISABLED.INACTIVE.ACCOUNT.LOCKOUT.HASH.taxoryn.prod.safe.guard000")
                .build();
        when(userRepository.findByEmailIgnoreCase("superadmin@taxoryn.com")).thenReturn(Optional.of(inactiveLegacyUser));

        assertDoesNotThrow(validator::validateEnvironmentSecurity);
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when WhatsApp is enabled with META but token is missing")
    void testProductionFailsWhenWhatsAppTokenMissing() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "whatsappEnabled", true);
        ReflectionTestUtils.setField(validator, "whatsappProvider", "META");
        ReflectionTestUtils.setField(validator, "whatsappAccessToken", "");
        ReflectionTestUtils.setField(validator, "whatsappPhoneNumberId", "12345");
        ReflectionTestUtils.setField(validator, "whatsappBusinessAccountId", "67890");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("WHATSAPP_ACCESS_TOKEN"));
    }

    // =========================================================================
    // 5. Frontend URL & CORS Fail-Fast Tests
    // =========================================================================

    @Test
    @DisplayName("Fail-Fast: Production fails when frontend URL is missing")
    void testProductionFailsWhenFrontendUrlMissing() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "frontendUrl", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("Production frontend URL"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when frontend URL is localhost")
    void testProductionFailsWhenFrontendUrlIsLocalhost() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "frontendUrl", "http://localhost:5173");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("Production frontend URL cannot be localhost"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when frontend URL is a demo Vercel domain")
    void testProductionFailsWhenFrontendUrlIsVercelDemo() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "frontendUrl", "https://taxoryn-7x7f.vercel.app");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("Production frontend URL cannot use demo Vercel domain"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when CORS contains *.vercel.app wildcard")
    void testProductionFailsWhenCorsContainsVercelWildcard() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "https://app.taxoryn.com,https://*.vercel.app");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("Production CORS cannot include Vercel demo origins or wildcard Vercel domains"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when CORS contains localhost HTTP origin in production")
    void testProductionFailsWhenCorsContainsLocalhostInProd() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "https://app.taxoryn.com,http://localhost:5173");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("Production CORS cannot include localhost HTTP origins"));
    }

    // =========================================================================
    // 6. Dev, Demo & Valid Production Success Tests
    // =========================================================================

    @Test
    @DisplayName("Success: Dev and Demo profiles skip strict production validation")
    void testNonProductionProfilesPassValidation() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        ProductionSecurityValidator validator = createValidator();
        assertDoesNotThrow(validator::validateEnvironmentSecurity);

        when(environment.getActiveProfiles()).thenReturn(new String[]{"demo"});
        assertDoesNotThrow(validator::validateEnvironmentSecurity);
    }

    @Test
    @DisplayName("Success: Production passes with all valid high-entropy credentials injected")
    void testProductionPassesWithValidInjectedSecrets() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);

        UserEntity inactiveLegacyUser = UserEntity.builder()
                .email("superadmin@taxoryn.com")
                .status(UserStatus.INACTIVE)
                .passwordHash("$2a$12$DISABLED.INACTIVE.ACCOUNT.LOCKOUT.HASH.taxoryn.prod.safe.guard000")
                .build();
        when(userRepository.findByEmailIgnoreCase("superadmin@taxoryn.com")).thenReturn(Optional.of(inactiveLegacyUser));

        assertDoesNotThrow(validator::validateEnvironmentSecurity);
    }

    // =========================================================================
    // 7. Database Schema Management & Flyway Hardening Tests
    // =========================================================================

    @Test
    @DisplayName("Fail-Fast: Production fails when Hibernate ddl-auto is 'update'")
    void testProductionFailsWhenHibernateDdlAutoIsUpdate() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "hibernateDdlAuto", "update");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("Hibernate 'ddl-auto' cannot be 'update' in production"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when Hibernate ddl-auto is 'create'")
    void testProductionFailsWhenHibernateDdlAutoIsCreate() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "hibernateDdlAuto", "create");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("Hibernate 'ddl-auto' cannot be 'create' in production"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when Flyway validate-on-migrate is disabled")
    void testProductionFailsWhenFlywayValidateOnMigrateIsFalse() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "flywayValidateOnMigrate", false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("Flyway 'validate-on-migrate' must be enabled (true) in production"));
    }

    @Test
    @DisplayName("Fail-Fast: Production fails when Flyway is disabled in production")
    void testProductionFailsWhenFlywayIsDisabled() {
        ProductionSecurityValidator validator = createValidator();
        configureValidProductionBasics(validator);
        ReflectionTestUtils.setField(validator, "flywayEnabled", false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("Flyway must be enabled in production"));
    }
}
