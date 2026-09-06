package com.taxoryn.module.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.dto.CreatePlatformUserRequest;
import com.taxoryn.module.user.dto.UpdatePlatformUserRoleRequest;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaxorynPlatformUsersSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity platformOrg;
    private UserEntity superAdminUser;
    private UserEntity opsAdminUser;
    private UserEntity supportAdminUser;
    private UserEntity financeAdminUser;
    private UserEntity marketplaceAdminUser;
    private UserEntity contentAdminUser;
    private UserEntity securityAdminUser;
    private UserEntity engineeringAdminUser;

    private String superAdminToken;
    private String opsAdminToken;
    private String supportAdminToken;
    private String financeAdminToken;
    private String marketplaceAdminToken;
    private String contentAdminToken;
    private String securityAdminToken;
    private String engineeringAdminToken;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create Root Platform Organization
        platformOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Taxoryn Platform Global")
                .legalName("Taxoryn Global Inc")
                .email("admin@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Setup All 8 Platform Roles
        RoleEntity superAdminRole = getOrCreateRole("TAXORYN_SUPERADMIN", "Taxoryn SuperAdmin");
        RoleEntity opsAdminRole = getOrCreateRole("TAXORYN_OPERATIONS_ADMIN", "Taxoryn Operations Admin");
        RoleEntity supportAdminRole = getOrCreateRole("TAXORYN_SUPPORT_ADMIN", "Taxoryn Support Admin");
        RoleEntity financeAdminRole = getOrCreateRole("TAXORYN_FINANCE_ADMIN", "Taxoryn Finance Admin");
        RoleEntity marketplaceAdminRole = getOrCreateRole("TAXORYN_MARKETPLACE_ADMIN", "Taxoryn Marketplace Admin");
        RoleEntity contentAdminRole = getOrCreateRole("TAXORYN_CONTENT_ADMIN", "Taxoryn Content Admin");
        RoleEntity securityAdminRole = getOrCreateRole("TAXORYN_SECURITY_ADMIN", "Taxoryn Security Admin");
        RoleEntity engineeringAdminRole = getOrCreateRole("TAXORYN_ENGINEERING_ADMIN", "Taxoryn Engineering Admin");

        // 3. Create Users for Each Role
        superAdminUser = createUser("superadmin@taxoryn.com", "Super", "Admin", superAdminRole);
        opsAdminUser = createUser("ops@taxoryn.com", "Operations", "Admin", opsAdminRole);
        supportAdminUser = createUser("support@taxoryn.com", "Support", "Admin", supportAdminRole);
        financeAdminUser = createUser("finance@taxoryn.com", "Finance", "Admin", financeAdminRole);
        marketplaceAdminUser = createUser("marketplace@taxoryn.com", "Marketplace", "Admin", marketplaceAdminRole);
        contentAdminUser = createUser("content@taxoryn.com", "Content", "Admin", contentAdminRole);
        securityAdminUser = createUser("security@taxoryn.com", "Security", "Admin", securityAdminRole);
        engineeringAdminUser = createUser("engineering@taxoryn.com", "Engineering", "Admin", engineeringAdminRole);

        // 4. Generate JWT Tokens
        superAdminToken = jwtTokenProvider.generateAccessToken(
                superAdminUser.getId(), platformOrg.getId(), null, superAdminUser.getEmail(),
                Set.of("TAXORYN_SUPERADMIN", "SUPER_ADMIN"),
                Set.of("PLATFORM_VIEW", "PRACTICE_VIEW", "USER_VIEW", "USER_CREATE", "USER_UPDATE", "USER_DISABLE", "ROLE_READ", "ROLE_WRITE", "AUDIT_VIEW", "SECURITY_VIEW")
        );

        opsAdminToken = jwtTokenProvider.generateAccessToken(
                opsAdminUser.getId(), platformOrg.getId(), null, opsAdminUser.getEmail(),
                Set.of("TAXORYN_OPERATIONS_ADMIN"),
                Set.of("PLATFORM_VIEW", "PRACTICE_VIEW", "USER_VIEW", "USER_CREATE", "USER_UPDATE", "USER_DISABLE", "ONBOARDING_VIEW", "FEEDBACK_VIEW", "AUDIT_VIEW")
        );

        supportAdminToken = jwtTokenProvider.generateAccessToken(
                supportAdminUser.getId(), platformOrg.getId(), null, supportAdminUser.getEmail(),
                Set.of("TAXORYN_SUPPORT_ADMIN"),
                Set.of("PLATFORM_VIEW", "SUPPORT_VIEW", "FEEDBACK_VIEW", "FEEDBACK_RESPOND", "PRACTICE_VIEW")
        );

        financeAdminToken = jwtTokenProvider.generateAccessToken(
                financeAdminUser.getId(), platformOrg.getId(), null, financeAdminUser.getEmail(),
                Set.of("TAXORYN_FINANCE_ADMIN"),
                Set.of("PLATFORM_VIEW", "SUBSCRIPTION_VIEW", "MRR_VIEW", "PAYMENT_VIEW", "FINANCE_REPORT_VIEW")
        );

        marketplaceAdminToken = jwtTokenProvider.generateAccessToken(
                marketplaceAdminUser.getId(), platformOrg.getId(), null, marketplaceAdminUser.getEmail(),
                Set.of("TAXORYN_MARKETPLACE_ADMIN"),
                Set.of("PLATFORM_VIEW", "MARKETPLACE_VIEW", "MARKETPLACE_REQUIREMENT_VIEW", "CONSULTATION_VIEW")
        );

        contentAdminToken = jwtTokenProvider.generateAccessToken(
                contentAdminUser.getId(), platformOrg.getId(), null, contentAdminUser.getEmail(),
                Set.of("TAXORYN_CONTENT_ADMIN"),
                Set.of("PLATFORM_VIEW", "CONTENT_VIEW", "ARTICLE_CREATE", "ARTICLE_PUBLISH", "VIDEO_CREATE")
        );

        securityAdminToken = jwtTokenProvider.generateAccessToken(
                securityAdminUser.getId(), platformOrg.getId(), null, securityAdminUser.getEmail(),
                Set.of("TAXORYN_SECURITY_ADMIN"),
                Set.of("PLATFORM_VIEW", "AUDIT_VIEW", "AUDIT_SEARCH", "AUDIT_EXPORT", "SECURITY_VIEW", "ACCESS_REVIEW")
        );

        engineeringAdminToken = jwtTokenProvider.generateAccessToken(
                engineeringAdminUser.getId(), platformOrg.getId(), null, engineeringAdminUser.getEmail(),
                Set.of("TAXORYN_ENGINEERING_ADMIN"),
                Set.of("PLATFORM_VIEW", "PLATFORM_HEALTH_VIEW", "SYSTEM_STATUS_VIEW", "INTEGRATION_VIEW", "FEEDBACK_VIEW")
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private RoleEntity getOrCreateRole(String code, String name) {
        return roleRepository.findByCodeAndIsSystemRoleTrue(code).orElseGet(() ->
                roleRepository.save(RoleEntity.builder()
                        .code(code)
                        .name(name)
                        .isSystemRole(true)
                        .build()));
    }

    private UserEntity createUser(String email, String firstName, String lastName, RoleEntity role) {
        UserEntity user = UserEntity.builder()
                .organizationId(platformOrg.getId())
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(role)))
                .build();
        return userRepository.save(user);
    }

    @Test
    @DisplayName("SuperAdmin can provision any platform user and audit log is recorded")
    void testSuperAdminCanProvisionPlatformUser() throws Exception {
        CreatePlatformUserRequest request = CreatePlatformUserRequest.builder()
                .firstName("Karan")
                .lastName("Johar")
                .email("karan.security@taxoryn.com")
                .roleCode("TAXORYN_SECURITY_ADMIN")
                .status(UserStatus.ACTIVE)
                .temporaryPassword("SecretPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("karan.security@taxoryn.com"))
                .andExpect(jsonPath("$.data.roles[0].code").value("TAXORYN_SECURITY_ADMIN"));

        // Verify audit event
        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(log -> "TAXORYN_USER_CREATED".equals(log.getAction())));
    }

    @Test
    @DisplayName("Operations Admin can provision Support Admin but CANNOT escalate to SuperAdmin or Security Admin")
    void testOperationsAdminPrivilegeEscalationPrevented() throws Exception {
        // 1. Allowed: Operations Admin provisions Support Admin
        CreatePlatformUserRequest allowedReq = CreatePlatformUserRequest.builder()
                .firstName("Ritu")
                .lastName("Singh")
                .email("ritu.support@taxoryn.com")
                .roleCode("TAXORYN_SUPPORT_ADMIN")
                .status(UserStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + opsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allowedReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Denied (Escalation to SuperAdmin): Returns 403 Forbidden
        CreatePlatformUserRequest deniedSuperReq = CreatePlatformUserRequest.builder()
                .firstName("Hacker")
                .lastName("User")
                .email("hacker.super@taxoryn.com")
                .roleCode("TAXORYN_SUPERADMIN")
                .status(UserStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + opsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deniedSuperReq)))
                .andExpect(status().isForbidden());

        // 3. Denied (Escalation to Security Admin): Returns 403 Forbidden
        CreatePlatformUserRequest deniedSecReq = CreatePlatformUserRequest.builder()
                .firstName("Hacker")
                .lastName("User")
                .email("hacker.sec@taxoryn.com")
                .roleCode("TAXORYN_SECURITY_ADMIN")
                .status(UserStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + opsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deniedSecReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Operations Admin cannot change a SuperAdmin's role or status")
    void testOperationsAdminCannotModifySuperAdmin() throws Exception {
        UpdatePlatformUserRoleRequest roleReq = new UpdatePlatformUserRoleRequest("TAXORYN_SUPPORT_ADMIN");

        mockMvc.perform(put("/api/v1/admin/users/" + superAdminUser.getId() + "/role")
                        .header("Authorization", "Bearer " + opsAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/admin/users/" + superAdminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + opsAdminToken)
                        .param("status", "SUSPENDED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("All 8 Platform Roles can access the Platform Overview Cockpit")
    void testAllPlatformRolesCanViewPlatformOverview() throws Exception {
        List<String> tokens = List.of(
                superAdminToken, opsAdminToken, supportAdminToken, financeAdminToken,
                marketplaceAdminToken, contentAdminToken, securityAdminToken, engineeringAdminToken
        );

        for (String token : tokens) {
            mockMvc.perform(get("/api/v1/admin/platform/dashboard")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.summary").exists())
                    .andExpect(jsonPath("$.data.marketplace").exists());
        }
    }

    @Test
    @DisplayName("Security Admin can query audit logs while Content Admin is denied")
    void testSecurityAdminAuditAccessAndContentAdminDenied() throws Exception {
        // Security Admin queries /api/v1/audit-logs -> 200 OK
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + securityAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Content Admin attempts to query /api/v1/audit-logs -> 403 Forbidden
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + contentAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User lifecycle status and role changes generate authoritative audit events")
    void testUserLifecycleAuditGeneration() throws Exception {
        // 1. SuperAdmin updates user role
        UpdatePlatformUserRoleRequest roleReq = new UpdatePlatformUserRoleRequest("TAXORYN_SUPPORT_ADMIN");
        mockMvc.perform(put("/api/v1/admin/users/" + opsAdminUser.getId() + "/role")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isOk());

        // 2. SuperAdmin suspends user
        mockMvc.perform(patch("/api/v1/admin/users/" + opsAdminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .param("status", "SUSPENDED"))
                .andExpect(status().isOk());

        // 3. Verify audit events recorded
        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(log -> "TAXORYN_USER_ROLE_CHANGED".equals(log.getAction())));
        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(log -> "TAXORYN_USER_DISABLED".equals(log.getAction())));
    }

    @Test
    @DisplayName("Tenant ORG_ADMIN cannot access any platform-level administration endpoints (HTTP 403)")
    void testTenantAdminCannotAccessPlatformAdministrationEndpoints() throws Exception {
        OrganizationEntity practiceOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Sharma & Associates CA")
                .legalName("Sharma & Associates LLP")
                .email("info@sharmaca.in")
                .status(OrganizationStatus.ACTIVE)
                .build());

        RoleEntity orgAdminRole = getOrCreateRole("ORG_ADMIN", "Organization Administrator");
        UserEntity orgAdminUser = createUser("admin@sharmaca.in", "Ramesh", "Sharma", orgAdminRole);
        orgAdminUser.setOrganizationId(practiceOrg.getId());
        userRepository.save(orgAdminUser);

        String tenantAdminToken = jwtTokenProvider.generateAccessToken(
                orgAdminUser.getId(), practiceOrg.getId(), null, orgAdminUser.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("USER_VIEW", "USER_CREATE", "USER_UPDATE", "ROLE_READ", "ROLE_WRITE", "CLIENT_VIEW", "ORGANIZATION_VIEW")
        );

        // 1. Cannot list platform users
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + tenantAdminToken))
                .andExpect(status().isForbidden());

        // 2. Cannot create platform users
        CreatePlatformUserRequest createReq = CreatePlatformUserRequest.builder()
                .firstName("Rogue")
                .lastName("Admin")
                .email("rogue@taxoryn.com")
                .roleCode("TAXORYN_SUPPORT_ADMIN")
                .status(UserStatus.ACTIVE)
                .build();
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // 3. Cannot mutate platform user role
        UpdatePlatformUserRoleRequest roleReq = new UpdatePlatformUserRoleRequest("TAXORYN_SUPPORT_ADMIN");
        mockMvc.perform(put("/api/v1/admin/users/" + opsAdminUser.getId() + "/role")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isForbidden());

        // 4. Cannot update platform user status
        mockMvc.perform(patch("/api/v1/admin/users/" + opsAdminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .param("status", "SUSPENDED"))
                .andExpect(status().isForbidden());

        // 5. Cannot access platform overview dashboard
        mockMvc.perform(get("/api/v1/admin/platform/dashboard")
                        .header("Authorization", "Bearer " + tenantAdminToken))
                .andExpect(status().isForbidden());

        // 6. Cannot access platform support dashboard
        mockMvc.perform(get("/api/v1/admin/support/overview")
                        .header("Authorization", "Bearer " + tenantAdminToken))
                .andExpect(status().isForbidden());

        // 7. Cannot access marketplace KYC verification queue
        mockMvc.perform(get("/api/v1/admin/marketplace/verifications/pending")
                        .header("Authorization", "Bearer " + tenantAdminToken))
                .andExpect(status().isForbidden());

        // 8. Cannot access or create master tax services
        mockMvc.perform(get("/api/v1/admin/tax-services/categories")
                        .header("Authorization", "Bearer " + tenantAdminToken))
                .andExpect(status().isForbidden());
    }
}
