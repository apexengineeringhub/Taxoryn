package com.taxoryn.module.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository;
import com.taxoryn.module.moduleconfig.entity.ProductModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCategory;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.ProductModuleRepository;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionStatus;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ModuleAndSubscriptionEntitlementSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private ProductModuleRepository productModuleRepository;
    @Autowired private ModuleConfigurationService moduleConfigurationService;
    @Autowired private MarketplaceProfileRepository marketplaceProfileRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private OrganizationEntity org;
    private UserEntity adminUser;
    private UserEntity staffUser;
    private UserEntity clientPortalUser;
    private ClientEntity client;
    private EmployeeEntity staffEmployee;
    private SubscriptionEntity subscription;
    private MarketplaceProfileEntity marketplaceProfile;

    private String adminToken;
    private String staffToken;
    private String clientPortalToken;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        seedProductModulesIfMissing();

        // 1. Create Organization
        org = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Advisory LLP")
                .status(OrganizationStatus.ACTIVE)
                .organizationType(OrganizationType.GROWING_PRACTICE)
                .subscriptionPlan(OrganizationEntity.SubscriptionPlan.PROFESSIONAL)
                .email("info@apexadvisory.com")
                .country("India")
                .build());

        TenantContext.setTenantId(org.getId());

        // 2. Create Active Subscription
        subscription = subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(org.getId())
                .plan(SubscriptionPlan.PROFESSIONAL)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .maxUsers(15)
                .maxClients(100)
                .build());

        // 3. Setup Roles
        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Administrator")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .code("STAFF")
                .name("Staff Member")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity clientAdminRole = roleRepository.save(RoleEntity.builder()
                .code("CLIENT_ADMIN")
                .name("Client Portal Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 4. Create Client
        client = clientRepository.save(ClientEntity.builder()
                .displayName("Tata Consultancy Client")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("ABCDE1234F")
                .email("tax@tata.com")
                .status(ClientStatus.ACTIVE)
                .build());

        // 5. Create Staff Employee & User
        staffUser = userRepository.save(UserEntity.builder()
                .email("staff@apexadvisory.com")
                .passwordHash(passwordEncoder.encode("Staff@123456"))
                .firstName("Rajesh")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        staffEmployee = employeeRepository.save(EmployeeEntity.builder()
                .userId(staffUser.getId())
                .employeeCode("EMP-SEC-01")
                .firstName("Rajesh")
                .lastName("Sharma")
                .email("staff@apexadvisory.com")
                .status(EmployeeStatus.ACTIVE)
                .designation("Senior Tax Associate")
                .department("Direct Tax")
                .build());

        // 6. Create Admin User
        adminUser = userRepository.save(UserEntity.builder()
                .email("admin@apexadvisory.com")
                .passwordHash(passwordEncoder.encode("Admin@123456"))
                .firstName("Sunil")
                .lastName("Mehta")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        // 7. Create Client Portal User
        clientPortalUser = userRepository.save(UserEntity.builder()
                .email("portal@tata.com")
                .passwordHash(passwordEncoder.encode("Client@123456"))
                .firstName("Tata")
                .lastName("Manager")
                .status(UserStatus.ACTIVE)
                .clientId(client.getId())
                .roles(new HashSet<>(Set.of(clientAdminRole)))
                .build());

        // 8. Create Marketplace Profile
        marketplaceProfile = marketplaceProfileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(org.getId())
                .slug("apex-advisory")
                .displayName("Apex Advisory LLP")
                .headline("Leading Tax Advisory Firm")
                .bio("Expert GST & ITR consultants")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .experienceYears(10)
                .build());

        // 9. Generate JWT Access Tokens
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser.getId(), org.getId(), adminUser.getEmail(), Set.of("ORG_ADMIN", "ROLE_ORG_ADMIN"),
                Set.of("GST_CREATE", "GST_VIEW", "GST_READ", "ITR_CREATE", "ITR_VIEW", "ITR_READ",
                        "TDS_CREATE", "TDS_VIEW", "TDS_READ", "BILLING_CREATE", "BILLING_VIEW",
                        "REPORT_VIEW", "MARKETPLACE_VIEW", "MARKETPLACE_READ", "NOTICE_VIEW",
                        "NOTICE_CREATE", "NOTICE_UPDATE"));

        staffToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                staffUser.getId(), org.getId(), staffUser.getEmail(), Set.of("STAFF", "ROLE_STAFF"),
                Set.of("GST_VIEW", "ITR_VIEW", "TDS_VIEW", "BILLING_VIEW", "REPORT_VIEW", "NOTICE_VIEW"));

        clientPortalToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientPortalUser.getId(), org.getId(), client.getId(), clientPortalUser.getEmail(),
                Set.of("CLIENT_ADMIN", "ROLE_CLIENT_ADMIN"),
                Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_PROFILE_VIEW"));

        TenantContext.clear();
    }

    private void seedProductModulesIfMissing() {
        int order = 1;
        for (ProductModuleCode code : ProductModuleCode.values()) {
            if (productModuleRepository.findByCode(code).isEmpty()) {
                productModuleRepository.save(ProductModuleEntity.builder()
                        .code(code)
                        .name(code.name())
                        .category(ProductModuleCategory.CORE)
                        .status("ACTIVE")
                        .enabledByDefault(true)
                        .displayOrder(order++)
                        .build());
            }
        }
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
        TenantContext.clear();
    }

    private void cleanDatabase() {
        TenantContext.clear();
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        }
        try {
            marketplaceProfileRepository.deleteAll();
            subscriptionRepository.deleteAll();
            clientRepository.deleteAll();
            employeeRepository.deleteAll();
            userRepository.deleteAll();
            roleRepository.deleteAll();
            organizationRepository.deleteAll();
        } finally {
            if (jdbcTemplate != null) {
                jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
        }
    }

    // =========================================================================
    // Scenario A: Enabled Module + Active Subscription -> Allowed (200 / 201)
    // =========================================================================

    @Test
    @DisplayName("Scenario A1: GST endpoints allow access when GST module enabled and subscription active")
    void testA1_gstAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/gst/profiles")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Scenario A2: ITR endpoints allow access when ITR module enabled and subscription active")
    void testA2_itrAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/itr/returns")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Scenario A3: TDS endpoints allow access when TDS module enabled and subscription active")
    void testA3_tdsAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/tds/profiles")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Scenario A4: BILLING endpoints allow access when BILLING module enabled and subscription active")
    void testA4_billingAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/invoices")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Scenario A5: REPORTS endpoints allow access when REPORTS module enabled and subscription active")
    void testA5_reportsAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/reports/overview")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Scenario A6: CLIENT_PORTAL endpoints allow access when CLIENT_PORTAL module enabled and subscription active")
    void testA6_clientPortalAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/portal/preview/" + client.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Scenario A7: MARKETPLACE endpoints allow access when MARKETPLACE module enabled and subscription active")
    void testA7_marketplaceAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/practice-profile/completeness")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Scenario A8: TAX_NOTICES endpoints allow access when TAX_NOTICES module enabled and subscription active")
    void testA8_taxNoticesAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/tax-notices")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // Scenario B: Disabled Module -> 403 Forbidden with exact diagnostic message
    // =========================================================================

    @Test
    @DisplayName("Scenario B1: Disabled GST module blocks GST endpoints with 403 Forbidden")
    void testB1_disabledGstModuleForbidden() throws Exception {
        TenantContext.setTenantId(org.getId());
        moduleConfigurationService.updateModuleStatus(org.getId(), ProductModuleCode.GST, false);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/gst/profiles")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Product module GST is disabled for this organization"));
    }

    @Test
    @DisplayName("Scenario B2: Disabled ITR module blocks ITR endpoints with 403 Forbidden")
    void testB2_disabledItrModuleForbidden() throws Exception {
        TenantContext.setTenantId(org.getId());
        moduleConfigurationService.updateModuleStatus(org.getId(), ProductModuleCode.ITR, false);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/itr/returns")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Product module ITR is disabled for this organization"));
    }

    @Test
    @DisplayName("Scenario B3: Disabled TDS module blocks TDS endpoints with 403 Forbidden")
    void testB3_disabledTdsModuleForbidden() throws Exception {
        TenantContext.setTenantId(org.getId());
        moduleConfigurationService.updateModuleStatus(org.getId(), ProductModuleCode.TDS, false);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/tds/profiles")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Product module TDS is disabled for this organization"));
    }

    @Test
    @DisplayName("Scenario B4: Disabled BILLING module blocks billing endpoints with 403 Forbidden")
    void testB4_disabledBillingModuleForbidden() throws Exception {
        TenantContext.setTenantId(org.getId());
        moduleConfigurationService.updateModuleStatus(org.getId(), ProductModuleCode.BILLING, false);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/invoices")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Product module BILLING is disabled for this organization"));
    }

    @Test
    @DisplayName("Scenario B5: Disabled REPORTS module blocks reporting endpoints with 403 Forbidden")
    void testB5_disabledReportsModuleForbidden() throws Exception {
        TenantContext.setTenantId(org.getId());
        moduleConfigurationService.updateModuleStatus(org.getId(), ProductModuleCode.REPORTS, false);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/reports/overview")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Product module REPORTS is disabled for this organization"));
    }

    @Test
    @DisplayName("Scenario B6: Disabled CLIENT_PORTAL module blocks portal endpoints with 403 Forbidden")
    void testB6_disabledClientPortalModuleForbidden() throws Exception {
        TenantContext.setTenantId(org.getId());
        moduleConfigurationService.updateModuleStatus(org.getId(), ProductModuleCode.CLIENT_PORTAL, false);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/portal/preview/" + client.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Product module CLIENT_PORTAL is disabled for this organization"));
    }

    @Test
    @DisplayName("Scenario B7: Disabled MARKETPLACE module blocks practice marketplace endpoints with 403 Forbidden")
    void testB7_disabledMarketplaceModuleForbidden() throws Exception {
        TenantContext.setTenantId(org.getId());
        moduleConfigurationService.updateModuleStatus(org.getId(), ProductModuleCode.MARKETPLACE, false);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/marketplace/practice-profile/completeness")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Product module MARKETPLACE is disabled for this organization"));
    }

    @Test
    @DisplayName("Scenario B8: Disabled TAX_NOTICES module blocks notice endpoints with 403 Forbidden")
    void testB8_disabledTaxNoticesModuleForbidden() throws Exception {
        TenantContext.setTenantId(org.getId());
        moduleConfigurationService.updateModuleStatus(org.getId(), ProductModuleCode.TAX_NOTICES, false);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/tax-notices")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Product module TAX_NOTICES is disabled for this organization"));
    }

    // =========================================================================
    // Scenario C: Inactive / Expired / Canceled Subscription -> 403 Forbidden
    // =========================================================================

    @Test
    @DisplayName("Scenario C1: Expired subscription blocks GST endpoints with active subscription required message")
    void testC1_expiredSubscriptionBlocksGst() throws Exception {
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);

        mockMvc.perform(get("/api/v1/gst/profiles")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Active subscription required to access GST. Current subscription status: EXPIRED"));
    }

    @Test
    @DisplayName("Scenario C2: Canceled subscription blocks ITR endpoints with active subscription required message")
    void testC2_canceledSubscriptionBlocksItr() throws Exception {
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscriptionRepository.save(subscription);

        mockMvc.perform(get("/api/v1/itr/returns")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Active subscription required to access ITR. Current subscription status: CANCELED"));
    }

    @Test
    @DisplayName("Scenario C3: Expired subscription blocks Billing endpoints with active subscription required message")
    void testC3_expiredSubscriptionBlocksBilling() throws Exception {
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);

        mockMvc.perform(get("/api/v1/invoices")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Active subscription required to access BILLING. Current subscription status: EXPIRED"));
    }

    // =========================================================================
    // Scenario D: Re-enabling Disabled Module Restores Access
    // =========================================================================

    @Test
    @DisplayName("Scenario D1: Disabling then re-enabling GST module toggles access dynamically")
    void testD1_moduleToggleDynamicLifecycle() throws Exception {
        // 1. Disable
        TenantContext.setTenantId(org.getId());
        moduleConfigurationService.updateModuleStatus(org.getId(), ProductModuleCode.GST, false);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/gst/profiles")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden());

        // 2. Re-enable
        TenantContext.setTenantId(org.getId());
        moduleConfigurationService.updateModuleStatus(org.getId(), ProductModuleCode.GST, true);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/gst/profiles")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // Scenario E: OrganizationType Independence
    // =========================================================================

    @Test
    @DisplayName("Scenario E1: Changing OrganizationType does not alter module or subscription gating")
    void testE1_organizationTypeIndependence() throws Exception {
        // Change Org to SOLO_PRACTITIONER
        org.setOrganizationType(OrganizationType.SOLO_PRACTITIONER);
        organizationRepository.save(org);

        // GST is enabled -> access allowed
        mockMvc.perform(get("/api/v1/gst/profiles")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        // Disable GST
        TenantContext.setTenantId(org.getId());
        moduleConfigurationService.updateModuleStatus(org.getId(), ProductModuleCode.GST, false);
        TenantContext.clear();

        // GST is disabled -> access forbidden regardless of SOLO_PRACTITIONER persona
        mockMvc.perform(get("/api/v1/gst/profiles")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Product module GST is disabled for this organization"));
    }
}
