package com.taxoryn.module.organization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.moduleconfig.dto.UpdateOrganizationFeatureRequest;
import com.taxoryn.module.moduleconfig.dto.UpdateOrganizationModuleRequest;
import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.entity.ProductModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCategory;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationFeatureRepository;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
import com.taxoryn.module.moduleconfig.repository.ProductFeatureRepository;
import com.taxoryn.module.moduleconfig.repository.ProductModuleRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.entity.SubscriptionPlanEntity;
import com.taxoryn.module.subscription.entity.SubscriptionPlanFeatureEntity;
import com.taxoryn.module.subscription.entity.SubscriptionPlanModuleEntity;
import com.taxoryn.module.subscription.repository.SubscriptionPlanFeatureRepository;
import com.taxoryn.module.subscription.repository.SubscriptionPlanModuleRepository;
import com.taxoryn.module.subscription.repository.SubscriptionPlanRepository;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SubscriptionEntitlementAndModuleConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private SubscriptionPlanModuleRepository subscriptionPlanModuleRepository;

    @Autowired
    private SubscriptionPlanFeatureRepository subscriptionPlanFeatureRepository;

    @Autowired
    private ProductModuleRepository productModuleRepository;

    @Autowired
    private OrganizationModuleRepository organizationModuleRepository;

    @Autowired
    private ProductFeatureRepository productFeatureRepository;

    @Autowired
    private OrganizationFeatureRepository organizationFeatureRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity testOrg;
    private String orgAdminToken;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        organizationFeatureRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();

        // Ensure product modules exist in test DB
        ensureProductModulesExist();

        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax Practice")
                .legalName("Alpha Tax Practice LLP")
                .email("admin@alphatax.com")
                .organizationType(OrganizationType.FIRM)
                .build());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(testOrg.getId())
                .plan(SubscriptionPlan.STARTER)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .build());

        testUserId = UUID.randomUUID();
        orgAdminToken = jwtTokenProvider.generateAccessToken(testUserId, testOrg.getId(), "admin@alphatax.com",
                Set.of("ROLE_ORG_ADMIN"), Set.of("ORGANIZATION_VIEW", "ORGANIZATION_UPDATE", "ORG_READ", "ORG_WRITE", "MODULE_VIEW", "MODULE_MANAGE"));
    }

    private void ensureProductModulesExist() {
        for (ProductModuleCode code : ProductModuleCode.values()) {
            if (productModuleRepository.findByCode(code).isEmpty()) {
                productModuleRepository.save(ProductModuleEntity.builder()
                        .code(code)
                        .name(code.name())
                        .description(code.name() + " Module")
                        .category(ProductModuleCategory.CORE)
                        .enabledByDefault(true)
                        .displayOrder(1)
                        .build());
            }
        }

        if (productFeatureRepository.findByModuleCodeAndCode("TAX_NOTICES", "NOTICE_CAPTURE").isEmpty()) {
            productFeatureRepository.save(com.taxoryn.module.moduleconfig.entity.ProductFeatureEntity.builder()
                    .moduleCode("TAX_NOTICES")
                    .code("NOTICE_CAPTURE")
                    .name("Notice Capture")
                    .description("Notice Capture & Ingestion")
                    .enabledByDefault(true)
                    .displayOrder(1)
                    .build());
        }
    }

    @Test
    @DisplayName("Should retrieve effective configuration with boolean gates")
    void testGetEffectiveConfiguration() throws Exception {
        mockMvc.perform(get("/api/v1/modules/effective")
                        .header("Authorization", "Bearer " + orgAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.organizationId").value(testOrg.getId().toString()))
                .andExpect(jsonPath("$.data.subscriptionPlan").value("STARTER"))
                .andExpect(jsonPath("$.data.modules.GST").value(true))
                .andExpect(jsonPath("$.data.modules.ITR").value(true))
                .andExpect(jsonPath("$.data.multiLocationEnabled").value(false))
                .andExpect(jsonPath("$.data.maxLocations").value(1))
                .andExpect(jsonPath("$.data.navigationItems").isArray())
                .andExpect(jsonPath("$.data.navigationItems[0]").value("DASHBOARD"));
    }

    @Test
    @DisplayName("Should disable an entitled module and verify it is false in effective configuration")
    void testDisableEntitledModule() throws Exception {
        UpdateOrganizationModuleRequest request = new UpdateOrganizationModuleRequest();
        request.setEnabled(false);

        mockMvc.perform(put("/api/v1/modules/GST")
                        .header("Authorization", "Bearer " + orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        // Verify effective configuration now reflects false for GST
        mockMvc.perform(get("/api/v1/modules/effective")
                        .header("Authorization", "Bearer " + orgAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modules.GST").value(false))
                .andExpect(jsonPath("$.data.modules.ITR").value(true));
    }

    @Test
    @DisplayName("Should reject enabling a module not included in subscription plan entitlements")
    void testCannotEnableUnentitledModule() throws Exception {
        // Explicitly set subscription plan module entitlement for STARTER where BILLING is excluded
        subscriptionPlanModuleRepository.deleteAll();
        subscriptionPlanModuleRepository.save(SubscriptionPlanModuleEntity.builder()
                .planCode("STARTER")
                .moduleCode("BILLING")
                .isIncluded(false)
                .build());

        UpdateOrganizationModuleRequest request = new UpdateOrganizationModuleRequest();
        request.setEnabled(true);

        mockMvc.perform(put("/api/v1/modules/BILLING")
                        .header("Authorization", "Bearer " + orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should toggle feature configuration for an organization")
    void testToggleFeatureConfiguration() throws Exception {
        UpdateOrganizationFeatureRequest request = new UpdateOrganizationFeatureRequest(false);

        mockMvc.perform(put("/api/v1/organization-features/module/TAX_NOTICES/feature/NOTICE_CAPTURE")
                        .header("Authorization", "Bearer " + orgAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.featureCode").value("NOTICE_CAPTURE"));
    }
}
