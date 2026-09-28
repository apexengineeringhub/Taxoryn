package com.taxoryn.module.moduleconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.moduleconfig.entity.ProductModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCategory;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.ProductModuleRepository;
import com.taxoryn.module.moduleconfig.service.ModuleConfigurationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GlobalModuleEntitlementIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private ProductModuleRepository productModuleRepository;
    @Autowired private ModuleConfigurationService moduleConfigurationService;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private OrganizationEntity organization;
    private UserEntity adminUser;
    private ClientEntity client;
    private String adminToken;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        seedProductModulesIfMissing();

        organization = organizationRepository.save(OrganizationEntity.builder()
                .name("Global Module Enforcement Firm - " + UUID.randomUUID())
                .email("firm." + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(organization.getId());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(organization.getId())
                .plan(SubscriptionPlan.ENTERPRISE)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(12))
                .maxUsers(100)
                .maxClients(1000)
                .build());

        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        adminUser = userRepository.save(UserEntity.builder()
                .organizationId(organization.getId())
                .email("admin-" + UUID.randomUUID() + "@firm.com")
                .passwordHash(passwordEncoder.encode("Secure123!"))
                .firstName("Global")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        client = clientRepository.save(ClientEntity.builder()
                .displayName("Acme Global Corp")
                .legalName("Acme Global Corporation")
                .pan("ACMEG1234K")
                .clientType(ClientType.COMPANY)
                .status(ClientStatus.ACTIVE)
                .build());

        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser.getId(),
                organization.getId(),
                adminUser.getEmail(),
                Set.of("ORG_ADMIN", "ROLE_ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_WRITE", "DOCUMENT_VIEW", "DOCUMENT_WRITE", "DOC_REQUEST_VIEW", "DOC_REQUEST_CREATE", "AUDIT_VIEW", "NOTIFICATION_VIEW"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        cleanDatabase();
    }

    private void setSecurityContext() {
        SecurityUser securityUser = SecurityUser.builder()
                .userId(adminUser.getId())
                .organizationId(organization.getId())
                .email(adminUser.getEmail())
                .password("Secure123!")
                .roles(Set.of("ORG_ADMIN", "ROLE_ORG_ADMIN"))
                .permissions(Set.of("ORGANIZATION_UPDATE", "ORG_WRITE", "CLIENT_VIEW", "CLIENT_WRITE", "DOCUMENT_VIEW", "DOCUMENT_WRITE", "DOC_REQUEST_VIEW", "DOC_REQUEST_CREATE", "AUDIT_VIEW", "NOTIFICATION_VIEW", "MODULE_CONFIGURE"))
                .enabled(true)
                .build();
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(organization.getId());
    }

    private void cleanDatabase() {
        if (organization != null) {
            jdbcTemplate.execute("DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE organization_id = '" + organization.getId() + "')");
            jdbcTemplate.execute("DELETE FROM organization_modules WHERE organization_id = '" + organization.getId() + "'");
            jdbcTemplate.execute("DELETE FROM subscriptions WHERE organization_id = '" + organization.getId() + "'");
            jdbcTemplate.execute("DELETE FROM clients WHERE id = '" + (client != null ? client.getId() : UUID.randomUUID()) + "'");
            jdbcTemplate.execute("DELETE FROM users WHERE organization_id = '" + organization.getId() + "'");
            jdbcTemplate.execute("DELETE FROM organizations WHERE id = '" + organization.getId() + "'");
        }
    }

    private void seedProductModulesIfMissing() {
        for (ProductModuleCode code : ProductModuleCode.values()) {
            if (productModuleRepository.findByCode(code).isEmpty()) {
                productModuleRepository.save(ProductModuleEntity.builder()
                        .code(code)
                        .name(code.name())
                        .description(code.name() + " module")
                        .category(ProductModuleCategory.CORE)
                        .status("ACTIVE")
                        .enabledByDefault(true)
                        .displayOrder(1)
                        .build());
            }
        }
    }

    @Test
    @DisplayName("Gating: Disabling CLIENTS returns 403 on /api/v1/clients and cascades to CLIENT_PORTAL")
    void shouldDenyClientAccessAndCascadeToClientPortalWhenClientsModuleDisabled() throws Exception {
        // 1. Initial State: CLIENTS is enabled -> 200 OK
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 2. Disable CLIENTS
        setSecurityContext();
        moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.CLIENTS, false);

        // 3. /api/v1/clients must return 403 Forbidden
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("CLIENTS is disabled")));

        // 4. /api/v1/portal must also return 403 Forbidden because CLIENT_PORTAL depends on CLIENTS
        mockMvc.perform(get("/api/v1/portal/dashboard")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 5. Effective configuration should omit CLIENTS and CLIENT_PORTAL from navigation
        setSecurityContext();
        var effectiveConfig = moduleConfigurationService.getEffectiveConfiguration(organization.getId());
        org.junit.jupiter.api.Assertions.assertFalse(effectiveConfig.getModules().get("CLIENTS"));
        org.junit.jupiter.api.Assertions.assertFalse(effectiveConfig.getModules().get("CLIENT_PORTAL"));
        org.junit.jupiter.api.Assertions.assertFalse(effectiveConfig.getNavigationItems().contains("CLIENTS"));
        org.junit.jupiter.api.Assertions.assertFalse(effectiveConfig.getNavigationItems().contains("CLIENT_PORTAL"));

        // 6. Re-enable CLIENTS -> 200 OK restored
        setSecurityContext();
        moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.CLIENTS, true);
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gating: Disabling DOCUMENTS returns 403 on /api/v1/documents and cascades to DOCUMENT_REQUESTS")
    void shouldDenyDocumentAccessAndCascadeToDocumentRequestsWhenDocumentsDisabled() throws Exception {
        // 1. Initial State -> 200 OK
        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 2. Disable DOCUMENTS
        setSecurityContext();
        moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.DOCUMENTS, false);

        // 3. /api/v1/documents must return 403 Forbidden
        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("DOCUMENTS is disabled")));

        // 4. /api/v1/document-requests must return 403 Forbidden due to dependency
        mockMvc.perform(get("/api/v1/document-requests")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 5. Re-enable DOCUMENTS -> 200 OK restored
        setSecurityContext();
        moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.DOCUMENTS, true);
        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gating: Disabling AUDIT returns 403 on /api/v1/audit-logs")
    void shouldDenyAuditAccessWhenAuditModuleDisabled() throws Exception {
        // 1. Initial State -> 200 OK
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 2. Disable AUDIT
        setSecurityContext();
        moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.AUDIT, false);

        // 3. /api/v1/audit-logs must return 403 Forbidden
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("AUDIT is disabled")));

        // 4. Re-enable AUDIT -> 200 OK restored
        setSecurityContext();
        moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.AUDIT, true);
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Gating: Disabling NOTIFICATIONS returns 403 on /api/v1/notifications and Gmail controllers")
    void shouldDenyNotificationAndGmailAccessWhenNotificationsModuleDisabled() throws Exception {
        // 1. Initial State -> 200 OK
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 2. Disable NOTIFICATIONS
        setSecurityContext();
        moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.NOTIFICATIONS, false);

        // 3. /api/v1/notifications must return 403 Forbidden
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("NOTIFICATIONS is disabled")));

        // 4. /api/v1/gmail/conversations must also return 403 Forbidden
        mockMvc.perform(get("/api/v1/gmail/conversations")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 5. Re-enable NOTIFICATIONS -> 200 OK restored
        setSecurityContext();
        moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.NOTIFICATIONS, true);
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
