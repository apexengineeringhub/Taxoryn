package com.taxoryn.module.dashboard.controller;

import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity platformOrg;
    private OrganizationEntity practiceTenant;
    private UserEntity superAdminUser;
    private UserEntity opsAdminUser;
    private UserEntity financeAdminUser;
    private UserEntity practiceAdminUser;
    private UserEntity clientUser;

    private String superAdminToken;
    private String opsAdminToken;
    private String financeAdminToken;
    private String practiceAdminToken;
    private String clientToken;

    @BeforeEach
    void setUp() {
        RoleEntity taxorynSuperAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("TAXORYN_SUPERADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("TAXORYN_SUPERADMIN").name("Taxoryn Platform SuperAdmin").isSystemRole(true).build()));

        RoleEntity opsAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("TAXORYN_OPERATIONS_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("TAXORYN_OPERATIONS_ADMIN").name("Taxoryn Operations Admin").isSystemRole(true).build()));

        RoleEntity financeAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("TAXORYN_FINANCE_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("TAXORYN_FINANCE_ADMIN").name("Taxoryn Finance Admin").isSystemRole(true).build()));

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Organization Administrator").isSystemRole(true).build()));

        RoleEntity clientRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("CLIENT_USER").name("Client User").isSystemRole(true).build()));

        // 1. Platform Root Organization
        platformOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Taxoryn Platform Global")
                .email("platform." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        superAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(platformOrg.getId())
                .email("superadmin." + UUID.randomUUID() + "@taxoryn.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Platform")
                .lastName("SuperAdmin")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(taxorynSuperAdminRole)))
                .build());

        superAdminToken = jwtTokenProvider.generateAccessToken(
                superAdminUser.getId(), platformOrg.getId(), null, superAdminUser.getEmail(),
                Set.of("TAXORYN_SUPERADMIN", "SUPER_ADMIN"),
                Set.of("PLATFORM_VIEW", "PRACTICE_VIEW", "USER_VIEW", "USER_DISABLE", "ORGANIZATION_VIEW")
        );

        opsAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(platformOrg.getId())
                .email("ops." + UUID.randomUUID() + "@taxoryn.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Ops")
                .lastName("Admin")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(opsAdminRole)))
                .build());

        opsAdminToken = jwtTokenProvider.generateAccessToken(
                opsAdminUser.getId(), platformOrg.getId(), null, opsAdminUser.getEmail(),
                Set.of("TAXORYN_OPERATIONS_ADMIN"),
                Set.of("PLATFORM_VIEW", "PRACTICE_VIEW", "USER_VIEW", "USER_DISABLE")
        );

        financeAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(platformOrg.getId())
                .email("finance." + UUID.randomUUID() + "@taxoryn.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Finance")
                .lastName("Admin")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(financeAdminRole)))
                .build());

        financeAdminToken = jwtTokenProvider.generateAccessToken(
                financeAdminUser.getId(), platformOrg.getId(), null, financeAdminUser.getEmail(),
                Set.of("TAXORYN_FINANCE_ADMIN"),
                Set.of("PLATFORM_VIEW", "SUBSCRIPTION_VIEW", "PAYMENT_VIEW")
        );

        // 2. Practice Tenant
        practiceTenant = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax LLP " + UUID.randomUUID())
                .email("admin." + UUID.randomUUID() + "@apextax.com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .subscriptionPlan(OrganizationEntity.SubscriptionPlan.PROFESSIONAL)
                .build());

        practiceAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(practiceTenant.getId())
                .email(practiceTenant.getEmail())
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Apex")
                .lastName("Admin")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build());

        practiceAdminToken = jwtTokenProvider.generateAccessToken(
                practiceAdminUser.getId(), practiceTenant.getId(), null, practiceAdminUser.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("DASHBOARD_VIEW", "CLIENT_VIEW")
        );

        // 3. Customer User
        clientUser = userRepository.save(UserEntity.builder()
                .organizationId(practiceTenant.getId())
                .email("client." + UUID.randomUUID() + "@gmail.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Sneha")
                .lastName("Patel")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(clientRole)))
                .build());

        clientToken = jwtTokenProvider.generateAccessToken(
                clientUser.getId(), practiceTenant.getId(), null, clientUser.getEmail(),
                Set.of("CLIENT_USER"),
                Set.of("PORTAL_VIEW")
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET /api/v1/admin/platform/dashboard should succeed for TAXORYN_SUPERADMIN with aggregated metrics")
    void testGetPlatformDashboardSuccessForSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/platform/dashboard")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.activePractices").isNumber())
                .andExpect(jsonPath("$.data.summary.totalPractices").isNumber())
                .andExpect(jsonPath("$.data.summary.platformUsers").isNumber())
                .andExpect(jsonPath("$.data.summary.marketplaceCustomers").isNumber())
                .andExpect(jsonPath("$.data.summary.activeSubscriptions").isNumber())
                .andExpect(jsonPath("$.data.marketplace.newRequirements").isNumber())
                .andExpect(jsonPath("$.data.marketplace.activeEnquiries").isNumber())
                .andExpect(jsonPath("$.data.attention.pendingPracticeVerification").isNumber())
                .andExpect(jsonPath("$.data.attention.openFeedback").isNumber())
                .andExpect(jsonPath("$.data.health.api").value("HEALTHY"))
                .andExpect(jsonPath("$.data.health.database").exists())
                .andExpect(jsonPath("$.data.recentActivity").isArray())
                .andExpect(jsonPath("$.data.kpis.totalPractices").isNumber())
                .andExpect(jsonPath("$.data.kpis.activePractices").isNumber())
                .andExpect(jsonPath("$.data.kpis.activeUsers").isNumber())
                .andExpect(jsonPath("$.data.kpis.platformStatus").value("HEALTHY"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/platform/dashboard should succeed for TAXORYN_OPERATIONS_ADMIN")
    void testGetPlatformDashboardSuccessForOpsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/platform/dashboard")
                        .header("Authorization", "Bearer " + opsAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/admin/platform/dashboard should succeed for TAXORYN_FINANCE_ADMIN")
    void testGetPlatformDashboardSuccessForFinanceAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/platform/dashboard")
                        .header("Authorization", "Bearer " + financeAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/admin/platform/dashboard should return 403 Forbidden for Practice Admin")
    void testGetPlatformDashboardForbiddenForPracticeAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/platform/dashboard")
                        .header("Authorization", "Bearer " + practiceAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/platform/dashboard should return 403 Forbidden for Client User")
    void testGetPlatformDashboardForbiddenForClientUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/platform/dashboard")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/platform/dashboard should return 401 Unauthorized when unauthenticated")
    void testGetPlatformDashboardUnauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/admin/platform/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/admin/users should return paginated platform users for TAXORYN_SUPERADMIN")
    void testGetPlatformUsersForSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/status should update user status for TAXORYN_OPERATIONS_ADMIN")
    void testUpdateUserStatusForOpsAdmin() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + clientUser.getId() + "/status")
                        .param("status", "SUSPENDED")
                        .header("Authorization", "Bearer " + opsAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }
}
