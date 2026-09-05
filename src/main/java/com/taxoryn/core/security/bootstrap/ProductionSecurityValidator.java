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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Production environment security validator.
 * <p>
 * Enforces strict fail-closed validation during application startup in production mode:
 * 1. Prohibits mixing 'prod' with 'dev' or 'demo' profiles.
 * 2. Prohibits demo flags in production.
 * 3. Prohibits known repository default JWT secret.
 * 4. Prohibits active users with known default passwords in production databases.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionSecurityValidator implements SmartInitializingSingleton {

    public static final String DEFAULT_REPOSITORY_JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    public static final String KNOWN_DEMO_PASSWORD = "Password123!";

    private final Environment environment;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${taxoryn.jwt.secret:}")
    private String jwtSecret;

    @Value("${taxoryn.demo.enabled:false}")
    private boolean demoEnabled;

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

        log.info("Executing Phase 9 production security verification...");

        // 1. Profile Isolation Check
        Set<String> unsafeProfiles = Set.of("dev", "demo");
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

        // 3. JWT Default Secret Check
        if (DEFAULT_REPOSITORY_JWT_SECRET.equalsIgnoreCase(jwtSecret) || jwtSecret == null || jwtSecret.trim().length() < 32) {
            String error = "CRITICAL SECURITY VIOLATION: Production JWT secret is missing or using known repository default";
            log.error(error);
            throw new IllegalStateException(error);
        }

        // 4. Insecure Known Default Credential Check in Production DB
        Optional<UserEntity> legacySuperAdmin = userRepository.findByEmailIgnoreCase("superadmin@taxoryn.com");
        if (legacySuperAdmin.isPresent()) {
            UserEntity user = legacySuperAdmin.get();
            if (user.getStatus() == UserStatus.ACTIVE && passwordEncoder.matches(KNOWN_DEMO_PASSWORD, user.getPasswordHash())) {
                String error = "CRITICAL SECURITY VIOLATION: Active Super Admin user 'superadmin@taxoryn.com' with known default password detected in production database";
                log.error(error);
                throw new IllegalStateException(error);
            }
        }

        log.info("Phase 9 production environment security verification PASSED.");
    }
}
