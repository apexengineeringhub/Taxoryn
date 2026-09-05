package com.taxoryn.core.security.bootstrap;

import com.taxoryn.core.security.PasswordSecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.PermissionRepository;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.core.bootstrap.DemoDataSeeder;
import com.taxoryn.core.bootstrap.MarketplaceDemoDataSeeder;
import com.taxoryn.core.bootstrap.LearnContentDemoDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultCredentialsAndDemoSecurityTest {

    @Mock
    private Environment environment;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private AuditService auditService;

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    // =========================================================================
    // 1. PasswordSecurityUtils Tests
    // =========================================================================

    @Test
    @DisplayName("PasswordSecurityUtils generates high-entropy, compliant temporary passwords")
    void testPasswordSecurityUtils_GenerateSecureTemporaryPassword() {
        for (int i = 0; i < 20; i++) {
            String password = PasswordSecurityUtils.generateSecureTemporaryPassword();
            assertNotNull(password);
            assertTrue(password.length() >= 16, "Generated password must have length >= 16");
            assertTrue(PasswordSecurityUtils.isStrongProductionPassword(password), "Generated password must satisfy production strength requirements");
            assertFalse(PasswordSecurityUtils.isKnownDefaultOrWeakPassword(password), "Generated password must not be in known weak set");
        }
    }

    @Test
    @DisplayName("PasswordSecurityUtils correctly identifies known demo and weak passwords")
    void testPasswordSecurityUtils_IsKnownDefaultOrWeakPassword() {
        assertTrue(PasswordSecurityUtils.isKnownDefaultOrWeakPassword("Password123!"));
        assertTrue(PasswordSecurityUtils.isKnownDefaultOrWeakPassword("password123!"));
        assertTrue(PasswordSecurityUtils.isKnownDefaultOrWeakPassword("admin123"));
        assertTrue(PasswordSecurityUtils.isKnownDefaultOrWeakPassword("taxoryn123!"));
        assertTrue(PasswordSecurityUtils.isKnownDefaultOrWeakPassword("12345678"));
        assertTrue(PasswordSecurityUtils.isKnownDefaultOrWeakPassword(""));
        assertTrue(PasswordSecurityUtils.isKnownDefaultOrWeakPassword(null));

        assertFalse(PasswordSecurityUtils.isKnownDefaultOrWeakPassword("V#8mK9$qL2!pXz7@"));
    }

    @Test
    @DisplayName("PasswordSecurityUtils correctly validates production password strength")
    void testPasswordSecurityUtils_IsStrongProductionPassword() {
        assertFalse(PasswordSecurityUtils.isStrongProductionPassword("short1!"));
        assertFalse(PasswordSecurityUtils.isStrongProductionPassword("Password123!")); // In known weak list
        assertFalse(PasswordSecurityUtils.isStrongProductionPassword("alllowercase12345!"));
        assertFalse(PasswordSecurityUtils.isStrongProductionPassword("ALLUPPERCASE12345!"));
        assertFalse(PasswordSecurityUtils.isStrongProductionPassword("NoSpecialChars12345"));

        assertTrue(PasswordSecurityUtils.isStrongProductionPassword("Tx9#SecureP@ss2026!"));
    }

    // =========================================================================
    // 2. ProductionSecurityValidator Fail-Closed Tests
    // =========================================================================

    @Test
    @DisplayName("ProductionSecurityValidator passes when running with non-production profiles")
    void testProductionSecurityValidator_SkipsWhenNonProduction() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev", "local"});
        ProductionSecurityValidator validator = new ProductionSecurityValidator(environment, userRepository, passwordEncoder);

        assertDoesNotThrow(validator::validateEnvironmentSecurity);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("ProductionSecurityValidator fails closed if 'prod' and 'dev' are active concurrently")
    void testProductionSecurityValidator_FailsWhenProdAndDevCombined() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod", "dev"});
        ProductionSecurityValidator validator = new ProductionSecurityValidator(environment, userRepository, passwordEncoder);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("CRITICAL SECURITY VIOLATION"));
        assertTrue(ex.getMessage().contains("dev"));
    }

    @Test
    @DisplayName("ProductionSecurityValidator fails closed if 'prod' and 'demo' are active concurrently")
    void testProductionSecurityValidator_FailsWhenProdAndDemoCombined() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod", "demo"});
        ProductionSecurityValidator validator = new ProductionSecurityValidator(environment, userRepository, passwordEncoder);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("CRITICAL SECURITY VIOLATION"));
        assertTrue(ex.getMessage().contains("demo"));
    }

    @Test
    @DisplayName("ProductionSecurityValidator fails closed if 'taxoryn.demo.enabled' is true in production")
    void testProductionSecurityValidator_FailsWhenDemoFlagTrueInProduction() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        ProductionSecurityValidator validator = new ProductionSecurityValidator(environment, userRepository, passwordEncoder);
        ReflectionTestUtils.setField(validator, "demoEnabled", true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("demo.enabled"));
    }

    @Test
    @DisplayName("ProductionSecurityValidator fails closed if JWT secret is repository default in production")
    void testProductionSecurityValidator_FailsWhenDefaultJwtSecretInProduction() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        ProductionSecurityValidator validator = new ProductionSecurityValidator(environment, userRepository, passwordEncoder);
        ReflectionTestUtils.setField(validator, "demoEnabled", false);
        ReflectionTestUtils.setField(validator, "jwtSecret", ProductionSecurityValidator.DEFAULT_REPOSITORY_JWT_SECRET);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("JWT secret"));
    }

    @Test
    @DisplayName("ProductionSecurityValidator fails closed if active SuperAdmin with default password exists in production DB")
    void testProductionSecurityValidator_FailsWhenActiveSuperAdminHasDefaultPasswordInProduction() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        ProductionSecurityValidator validator = new ProductionSecurityValidator(environment, userRepository, passwordEncoder);
        ReflectionTestUtils.setField(validator, "demoEnabled", false);
        ReflectionTestUtils.setField(validator, "jwtSecret", "c3VwZXJzZWNyZXRwcm9kdWN0aW9ua2V5MTIzNDU2Nzg5MDEyMzQ1Njc4OTA=");

        UserEntity insecureUser = UserEntity.builder()
                .email("superadmin@taxoryn.com")
                .status(UserStatus.ACTIVE)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .build();
        when(userRepository.findByEmailIgnoreCase("superadmin@taxoryn.com")).thenReturn(Optional.of(insecureUser));

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateEnvironmentSecurity);
        assertTrue(ex.getMessage().contains("Active Super Admin user 'superadmin@taxoryn.com' with known default password"));
    }

    @Test
    @DisplayName("ProductionSecurityValidator passes when running with valid production configuration and disabled legacy admin")
    void testProductionSecurityValidator_PassesWithSafeConfiguration() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        ProductionSecurityValidator validator = new ProductionSecurityValidator(environment, userRepository, passwordEncoder);
        ReflectionTestUtils.setField(validator, "demoEnabled", false);
        ReflectionTestUtils.setField(validator, "jwtSecret", "c3VwZXJzZWNyZXRwcm9kdWN0aW9ua2V5MTIzNDU2Nzg5MDEyMzQ1Njc4OTA=");

        UserEntity disabledLegacyUser = UserEntity.builder()
                .email("superadmin@taxoryn.com")
                .status(UserStatus.INACTIVE)
                .passwordHash("$2a$12$INACTIVE.LEGACY.SEED.ACCOUNT.INVALID.HASH.DO.NOT.USE.XXXX")
                .build();
        when(userRepository.findByEmailIgnoreCase("superadmin@taxoryn.com")).thenReturn(Optional.of(disabledLegacyUser));

        assertDoesNotThrow(validator::validateEnvironmentSecurity);
    }

    // =========================================================================
    // 3. ProductionAdminBootstrapService Tests
    // =========================================================================

    @Test
    @DisplayName("ProductionAdminBootstrapService creates SuperAdmin with strong password when none exists")
    void testProductionAdminBootstrapService_CreatesSuperAdminSuccess() {
        ProductionAdminBootstrapService bootstrapService = new ProductionAdminBootstrapService(
                userRepository, organizationRepository, roleRepository, permissionRepository, passwordEncoder, auditService
        );

        ReflectionTestUtils.setField(bootstrapService, "bootstrapEmail", "owner@taxoryn.in");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "K#9xL2$vM8!pQz7@");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPhone", "+919876543210");
        ReflectionTestUtils.setField(bootstrapService, "firstName", "Chief");
        ReflectionTestUtils.setField(bootstrapService, "lastName", "Admin");

        OrganizationEntity platformOrg = OrganizationEntity.builder().name("Taxoryn Platform Operations").build();
        platformOrg.setId(UUID.randomUUID());
        when(organizationRepository.findByEmailIgnoreCase("admin@taxoryn.com")).thenReturn(Optional.of(platformOrg));
        when(permissionRepository.findAll()).thenReturn(Collections.emptyList());

        when(roleRepository.save(any(RoleEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmailIgnoreCase("owner@taxoryn.in")).thenReturn(Optional.empty());

        UserEntity savedUser = UserEntity.builder()
                .email("owner@taxoryn.in")
                .status(UserStatus.ACTIVE)
                .build();
        savedUser.setId(UUID.randomUUID());
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        boolean result = bootstrapService.bootstrapProductionAdmin();
        assertTrue(result);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity created = userCaptor.getValue();
        assertEquals("owner@taxoryn.in", created.getEmail());
        assertEquals(UserStatus.ACTIVE, created.getStatus());
        assertTrue(passwordEncoder.matches("K#9xL2$vM8!pQz7@", created.getPasswordHash()));

        verify(auditService).logEvent(eq("BOOTSTRAP_SUPERADMIN_CREATED"), eq("USER"), anyString(), isNull(), any());
    }

    @Test
    @DisplayName("ProductionAdminBootstrapService rejects weak or known demo passwords")
    void testProductionAdminBootstrapService_RejectsWeakPassword() {
        ProductionAdminBootstrapService bootstrapService = new ProductionAdminBootstrapService(
                userRepository, organizationRepository, roleRepository, permissionRepository, passwordEncoder, auditService
        );

        ReflectionTestUtils.setField(bootstrapService, "bootstrapEmail", "owner@taxoryn.in");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "Password123!");

        IllegalStateException ex = assertThrows(IllegalStateException.class, bootstrapService::bootstrapProductionAdmin);
        assertTrue(ex.getMessage().contains("Bootstrap password does not meet production complexity standards"));
    }

    @Test
    @DisplayName("ProductionAdminBootstrapService blocks replay when an active SuperAdmin already exists")
    void testProductionAdminBootstrapService_ReplayBlockedWhenActiveSuperAdminExists() {
        ProductionAdminBootstrapService bootstrapService = new ProductionAdminBootstrapService(
                userRepository, organizationRepository, roleRepository, permissionRepository, passwordEncoder, auditService
        );

        ReflectionTestUtils.setField(bootstrapService, "bootstrapEmail", "owner@taxoryn.in");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "K#9xL2$vM8!pQz7@");

        RoleEntity superAdminRole = RoleEntity.builder().code("TAXORYN_SUPERADMIN").build();
        when(roleRepository.findByCodeAndIsSystemRoleTrue("TAXORYN_SUPERADMIN")).thenReturn(Optional.of(superAdminRole));

        UserEntity activeAdmin = UserEntity.builder()
                .email("existing.admin@taxoryn.com")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(superAdminRole)))
                .build();
        activeAdmin.setId(UUID.randomUUID());
        when(userRepository.findAll()).thenReturn(List.of(activeAdmin));

        boolean result = bootstrapService.bootstrapProductionAdmin();
        assertFalse(result, "Bootstrap must return false when active SuperAdmin exists");
        verify(userRepository, never()).save(any());
    }

    // =========================================================================
    // 4. Demo Data Seeders Guard Tests
    // =========================================================================

    @Test
    @DisplayName("DemoDataSeeder refuses to execute when production profile is active")
    void testDemoDataSeeder_RefusesToRunInProduction() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        DemoDataSeeder seeder = new DemoDataSeeder(
                organizationRepository, userRepository, roleRepository, permissionRepository,
                mock(com.taxoryn.module.client.repository.ClientRepository.class),
                mock(com.taxoryn.module.employee.repository.EmployeeRepository.class),
                mock(com.taxoryn.module.task.repository.TaskRepository.class),
                mock(com.taxoryn.module.itr.repository.ItrProfileRepository.class),
                mock(com.taxoryn.module.itr.repository.ItrReturnRepository.class),
                mock(com.taxoryn.module.gst.repository.GstProfileRepository.class),
                mock(com.taxoryn.module.gst.repository.GstReturnFilingRepository.class),
                mock(com.taxoryn.module.tds.repository.TdsProfileRepository.class),
                mock(com.taxoryn.module.tds.repository.TdsReturnRepository.class),
                mock(com.taxoryn.module.tds.repository.TdsChallanRepository.class),
                mock(com.taxoryn.module.billing.repository.InvoiceRepository.class),
                mock(com.taxoryn.module.portal.repository.ClientDocumentRequestRepository.class),
                passwordEncoder,
                mock(com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository.class),
                mock(com.taxoryn.module.marketplace.repository.MarketplaceServiceRepository.class),
                mock(com.taxoryn.module.marketplace.repository.MarketplaceLeadRepository.class),
                environment
        );

        seeder.run();

        verifyNoInteractions(organizationRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("MarketplaceDemoDataSeeder refuses to execute when production profile is active")
    void testMarketplaceDemoDataSeeder_RefusesToRunInProduction() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"production"});

        MarketplaceDemoDataSeeder seeder = new MarketplaceDemoDataSeeder(
                organizationRepository,
                mock(com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository.class),
                mock(com.taxoryn.module.marketplace.repository.PracticeLocationRepository.class),
                mock(com.taxoryn.module.marketplace.repository.PracticeServiceRepository.class),
                mock(com.taxoryn.module.marketplace.repository.MarketplaceVerificationRepository.class),
                mock(com.taxoryn.module.marketplace.repository.TaxServiceCategoryRepository.class),
                mock(com.taxoryn.module.marketplace.repository.TaxServiceRepository.class),
                mock(com.taxoryn.module.marketplace.repository.MarketplaceCustomerProfileRepository.class),
                mock(com.taxoryn.module.marketplace.repository.CustomerTaxRequirementRepository.class),
                mock(com.taxoryn.module.marketplace.repository.MarketplaceLeadRepository.class),
                mock(com.taxoryn.module.marketplace.repository.MarketplaceProposalRepository.class),
                mock(com.taxoryn.module.marketplace.repository.MarketplaceConsultationRepository.class),
                mock(com.taxoryn.module.marketplace.repository.MarketplaceReviewRepository.class),
                userRepository,
                roleRepository,
                passwordEncoder,
                environment
        );

        seeder.run();

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("LearnContentDemoDataSeeder refuses to execute when production profile is active")
    void testLearnContentDemoDataSeeder_RefusesToRunInProduction() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        LearnContentDemoDataSeeder seeder = new LearnContentDemoDataSeeder(
                mock(com.taxoryn.module.content.repository.ContentRepository.class),
                mock(com.taxoryn.module.content.repository.ContentTagRepository.class),
                mock(com.taxoryn.module.marketplace.repository.TaxServiceCategoryRepository.class),
                mock(com.taxoryn.module.marketplace.repository.TaxServiceRepository.class),
                userRepository,
                environment
        );

        seeder.run();

        verifyNoInteractions(userRepository);
    }
}
