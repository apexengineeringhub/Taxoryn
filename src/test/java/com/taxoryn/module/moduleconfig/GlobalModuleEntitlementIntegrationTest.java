package com.taxoryn.module.moduleconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.BusinessValidationException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Admin")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

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
                Set.of("CLIENT_VIEW", "CLIENT_WRITE", "DOCUMENT_VIEW", "DOCUMENT_WRITE", "DOC_REQUEST_VIEW", "DOC_REQUEST_CREATE", "AUDIT_VIEW", "NOTIFICATION_VIEW", "GST_VIEW", "TAX_NOTICE_VIEW"));
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
                .permissions(Set.of("ORGANIZATION_UPDATE", "ORG_WRITE", "CLIENT_VIEW", "CLIENT_WRITE", "DOCUMENT_VIEW", "DOCUMENT_WRITE", "DOC_REQUEST_VIEW", "DOC_REQUEST_CREATE", "AUDIT_VIEW", "NOTIFICATION_VIEW", "MODULE_CONFIGURE", "GST_VIEW", "TAX_NOTICE_VIEW"))
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
            ProductModuleCategory category;
            boolean mandatory;
            boolean configurable;
            boolean subscriptionControlled;

            if (code == ProductModuleCode.NOTIFICATIONS || code == ProductModuleCode.AUDIT
                    || code == ProductModuleCode.ORGANIZATION || code == ProductModuleCode.USERS) {
                category = ProductModuleCategory.CORE;
                mandatory = true;
                configurable = false;
                subscriptionControlled = false;
            } else if (code == ProductModuleCode.CLIENTS || code == ProductModuleCode.TASKS
                    || code == ProductModuleCode.DOCUMENTS || code == ProductModuleCode.BILLING
                    || code == ProductModuleCode.REPORTS || code == ProductModuleCode.DASHBOARD) {
                category = ProductModuleCategory.FOUNDATION;
                mandatory = true;
                configurable = false;
                subscriptionControlled = false;
            } else if (code == ProductModuleCode.GST || code == ProductModuleCode.GST_COMPLIANCE
                    || code == ProductModuleCode.ITR || code == ProductModuleCode.ITR_COMPLIANCE
                    || code == ProductModuleCode.TDS || code == ProductModuleCode.TDS_COMPLIANCE
                    || code == ProductModuleCode.TAX_NOTICES || code == ProductModuleCode.TAX_NOTICE_MANAGEMENT) {
                category = ProductModuleCategory.BUSINESS;
                mandatory = false;
                configurable = true;
                subscriptionControlled = true;
            } else {
                category = ProductModuleCategory.OPTIONAL;
                mandatory = false;
                configurable = true;
                subscriptionControlled = true;
            }

            ProductModuleCategory finalCategory = category;
            boolean finalMandatory = mandatory;
            boolean finalConfigurable = configurable;
            boolean finalSubscriptionControlled = subscriptionControlled;

            productModuleRepository.findByCode(code).ifPresentOrElse(
                    existing -> {
                        existing.setCategory(finalCategory);
                        existing.setMandatory(finalMandatory);
                        existing.setConfigurable(finalConfigurable);
                        existing.setSubscriptionControlled(finalSubscriptionControlled);
                        productModuleRepository.save(existing);
                    },
                    () -> {
                        productModuleRepository.save(ProductModuleEntity.builder()
                                .code(code)
                                .name(code.name())
                                .description(code.name() + " module")
                                .category(finalCategory)
                                .mandatory(finalMandatory)
                                .configurable(finalConfigurable)
                                .subscriptionControlled(finalSubscriptionControlled)
                                .status("ACTIVE")
                                .enabledByDefault(true)
                                .displayOrder(1)
                                .build());
                    }
            );
        }
    }

    @Test
    @DisplayName("1. Architecture: FOUNDATION module CLIENTS cannot be disabled by organization")
    void foundationModuleClientsCannotBeDisabled() throws Exception {
        // 1. Initial State: CLIENTS is enabled -> 200 OK
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 2. Attempting to disable CLIENTS must throw BusinessValidationException
        setSecurityContext();
        assertThatThrownBy(() -> moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.CLIENTS, false))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("mandatory FOUNDATION module and cannot be disabled");

        // 3. /api/v1/clients remains 200 OK
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. Gating: Disabling BUSINESS module GST returns 403 on /api/v1/gst and re-enabling restores access")
    void shouldDenyGstAccessWhenBusinessModuleDisabled() throws Exception {
        // 1. Initial State: GST is enabled -> 200 OK
        mockMvc.perform(get("/api/v1/gst/filings")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 2. Disable GST
        setSecurityContext();
        moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.GST, false);

        // 3. /api/v1/gst/filings must return 403 Forbidden
        mockMvc.perform(get("/api/v1/gst/filings")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("GST is disabled")));

        // 4. Effective configuration should omit GST from navigation
        setSecurityContext();
        var effectiveConfig = moduleConfigurationService.getEffectiveConfiguration(organization.getId());
        org.junit.jupiter.api.Assertions.assertFalse(effectiveConfig.getModules().get("GST"));
        org.junit.jupiter.api.Assertions.assertFalse(effectiveConfig.getNavigationItems().contains("GST"));

        // 5. Re-enable GST -> 200 OK restored
        setSecurityContext();
        moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.GST, true);
        mockMvc.perform(get("/api/v1/gst/filings")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("3. Architecture: CORE modules AUDIT and NOTIFICATIONS cannot be disabled")
    void coreModulesCannotBeDisabled() {
        setSecurityContext();

        assertThatThrownBy(() -> moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.AUDIT, false))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("mandatory CORE module and cannot be disabled");

        assertThatThrownBy(() -> moduleConfigurationService.updateModuleStatus(organization.getId(), ProductModuleCode.NOTIFICATIONS, false))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("mandatory CORE module and cannot be disabled");
    }
}
