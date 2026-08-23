package com.taxoryn.module.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VerificationStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VisibilityStatus;
import com.taxoryn.module.marketplace.repository.*;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.PermissionRepository;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
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

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaxServiceSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private MarketplaceProfileRepository profileRepository;

    @Autowired
    private PracticeLocationRepository locationRepository;

    @Autowired
    private TaxServiceCategoryRepository categoryRepository;

    @Autowired
    private TaxServiceRepository taxServiceRepository;

    @Autowired
    private TaxServiceAliasRepository aliasRepository;

    @Autowired
    private PracticeServiceRepository practiceServiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity practiceOrg;
    private MarketplaceProfileEntity practiceProfile;
    private String practiceToken;
    private String superAdminToken;

    @BeforeEach
    void setUp() {
        practiceServiceRepository.deleteAll();
        aliasRepository.deleteAll();
        taxServiceRepository.deleteAll();
        categoryRepository.deleteAll();
        locationRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // 1. Create Organization
        practiceOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Indian Tax Advocates")
                .legalName("Apex Tax Advocates LLP")
                .email("admin@apextax.com")
                .city("Bengaluru")
                .state("Karnataka")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Create Permissions
        PermissionEntity mpView = permissionRepository.save(PermissionEntity.builder()
                .code("MARKETPLACE_VIEW")
                .name("View Marketplace")
                .module("MARKETPLACE")
                .build());

        PermissionEntity mpWrite = permissionRepository.save(PermissionEntity.builder()
                .code("MARKETPLACE_WRITE")
                .name("Write Marketplace")
                .module("MARKETPLACE")
                .build());

        // 3. Create Roles
        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Practice Administrator")
                .isSystemRole(true)
                .permissions(new HashSet<>(Set.of(mpView, mpWrite)))
                .build());

        RoleEntity superAdminRole = roleRepository.save(RoleEntity.builder()
                .code("SUPER_ADMIN")
                .name("Platform Super Administrator")
                .isSystemRole(true)
                .permissions(new HashSet<>(Set.of(mpView, mpWrite)))
                .build());

        // 4. Create Users & Tokens
        UserEntity practiceUser = UserEntity.builder()
                .email("admin@apextax.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Apex")
                .lastName("Admin")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserStatus.ACTIVE)
                .build();
        practiceUser.setOrganizationId(practiceOrg.getId());
        practiceUser = userRepository.save(practiceUser);

        practiceToken = jwtTokenProvider.generateAccessToken(
                practiceUser.getId(),
                practiceOrg.getId(),
                practiceUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN", "ORG_ADMIN"),
                Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE")
        );

        UserEntity superAdminUser = UserEntity.builder()
                .email("root@taxoryn.internal")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Super")
                .lastName("Admin")
                .roles(new HashSet<>(Set.of(superAdminRole)))
                .status(UserStatus.ACTIVE)
                .build();
        superAdminUser.setOrganizationId(practiceOrg.getId());
        superAdminUser = userRepository.save(superAdminUser);

        superAdminToken = jwtTokenProvider.generateAccessToken(
                superAdminUser.getId(),
                practiceOrg.getId(),
                superAdminUser.getEmail(),
                Set.of("ROLE_SUPER_ADMIN", "SUPER_ADMIN"),
                Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE", "ADMIN_READ", "ADMIN_WRITE")
        );

        // 5. Practice Profile & Location
        practiceProfile = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(practiceOrg.getId())
                .displayName("Apex Indian Tax Advocates")
                .slug("apex-tax-advocates")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .isPublished(true)
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .verificationStatus(VerificationStatus.VERIFIED)
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .averageRating(BigDecimal.valueOf(4.9))
                .totalReviews(28)
                .build());

        locationRepository.save(PracticeLocationEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .locationName("Head Office")
                .addressLine1("MG Road")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .latitude(BigDecimal.valueOf(12.9716))
                .longitude(BigDecimal.valueOf(77.5946))
                .isPrimary(true)
                .isActive(true)
                .build());
    }

    @Test
    @DisplayName("Admin Endpoints: SUPER_ADMIN can create category; Practice Admin gets 403 Forbidden")
    void testCategoryCreation_AccessControl() throws Exception {
        CreateTaxServiceCategoryRequest req = CreateTaxServiceCategoryRequest.builder()
                .code("INCOME_TAX")
                .name("Income Tax")
                .description("Direct tax returns and notices")
                .icon("FileText")
                .sortOrder(1)
                .isActive(true)
                .build();

        // 1. Practice Admin -> 403 Forbidden
        mockMvc.perform(post("/api/v1/admin/tax-services/categories")
                        .header("Authorization", "Bearer " + practiceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // 2. Super Admin -> 201 Created
        mockMvc.perform(post("/api/v1/admin/tax-services/categories")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("INCOME_TAX"))
                .andExpect(jsonPath("$.data.name").value("Income Tax"));
    }

    @Test
    @DisplayName("Admin Endpoints: SUPER_ADMIN can create Master Service and manage aliases")
    void testServiceAndAliasManagement_SuperAdmin() throws Exception {
        // 1. Create Category
        TaxServiceCategoryEntity cat = categoryRepository.save(TaxServiceCategoryEntity.builder()
                .code("GST")
                .name("Goods & Services Tax")
                .sortOrder(1)
                .isActive(true)
                .build());

        // 2. Create Master Service with Aliases
        CreateTaxServiceRequest svcReq = CreateTaxServiceRequest.builder()
                .categoryId(cat.getId())
                .code("GST_RETURN_FILING")
                .name("GST Return Filing")
                .description("Monthly GSTR-1 and 3B returns")
                .sortOrder(1)
                .isActive(true)
                .aliases(List.of("GSTR-3B", "GSTR-1", "GST Return"))
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/admin/tax-services")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(svcReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("GST_RETURN_FILING"))
                .andExpect(jsonPath("$.data.name").value("GST Return Filing"))
                .andExpect(jsonPath("$.data.aliases", hasSize(3)))
                .andReturn().getResponse().getContentAsString();

        UUID createdSvcId = UUID.fromString(objectMapper.readTree(responseJson).path("data").path("id").asText());

        // 3. Add Another Alias
        CreateTaxServiceAliasRequest aliasReq = CreateTaxServiceAliasRequest.builder()
                .alias("GST 3B")
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/tax-services/" + createdSvcId + "/aliases")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aliasReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.alias").value("GST 3B"))
                .andExpect(jsonPath("$.data.normalizedAlias").value("gst 3b"));
    }

    @Test
    @DisplayName("Public Catalog: Anonymous users can fetch active categories and resolve aliases")
    void testPublicCatalogAndResolve() throws Exception {
        TaxServiceCategoryEntity cat = categoryRepository.save(TaxServiceCategoryEntity.builder()
                .code("INCOME_TAX")
                .name("Income Tax")
                .sortOrder(1)
                .isActive(true)
                .build());

        TaxServiceEntity svc = taxServiceRepository.save(TaxServiceEntity.builder()
                .categoryId(cat.getId())
                .code("INCOME_TAX_RETURN")
                .name("Income Tax Return Filing")
                .description("Annual ITR preparation")
                .sortOrder(1)
                .isActive(true)
                .build());

        aliasRepository.save(TaxServiceAliasEntity.builder()
                .taxServiceId(svc.getId())
                .alias("ITR")
                .normalizedAlias("itr")
                .isActive(true)
                .build());

        // 1. Get Categories
        mockMvc.perform(get("/api/v1/marketplace/tax-services/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value("INCOME_TAX"))
                .andExpect(jsonPath("$.data[0].services", hasSize(1)))
                .andExpect(jsonPath("$.data[0].services[0].code").value("INCOME_TAX_RETURN"));

        // 2. Resolve Alias "ITR"
        mockMvc.perform(get("/api/v1/marketplace/tax-services/resolve")
                        .param("query", "ITR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("INCOME_TAX_RETURN"))
                .andExpect(jsonPath("$.data.name").value("Income Tax Return Filing"));
    }

    @Test
    @DisplayName("Practice Services Selection & Marketplace Geo Search Integration")
    void testPracticeServiceSelectionAndSearch() throws Exception {
        // 1. Seed Master Service
        TaxServiceCategoryEntity cat = categoryRepository.save(TaxServiceCategoryEntity.builder()
                .code("INCOME_TAX")
                .name("Income Tax")
                .sortOrder(1)
                .isActive(true)
                .build());

        TaxServiceEntity itrSvc = taxServiceRepository.save(TaxServiceEntity.builder()
                .categoryId(cat.getId())
                .code("INCOME_TAX_RETURN")
                .name("Income Tax Return Filing")
                .sortOrder(1)
                .isActive(true)
                .build());

        aliasRepository.save(TaxServiceAliasEntity.builder()
                .taxServiceId(itrSvc.getId())
                .alias("ITR")
                .normalizedAlias("itr")
                .isActive(true)
                .build());

        // 2. Practice selects INCOME_TAX_RETURN
        UpdatePracticeServicesRequest updateReq = UpdatePracticeServicesRequest.builder()
                .taxServiceIds(List.of(itrSvc.getId()))
                .build();

        mockMvc.perform(put("/api/v1/marketplace/practice-profile/tax-services")
                        .header("Authorization", "Bearer " + practiceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].taxServiceCode").value("INCOME_TAX_RETURN"));

        // 3. Customer searches with alias "ITR" and Geo coordinates (Bangalore 12.9716, 77.5946)
        mockMvc.perform(get("/api/v1/marketplace/search")
                        .param("service", "ITR")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946")
                        .param("radiusKm", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].displayName").value("Apex Indian Tax Advocates"))
                .andExpect(jsonPath("$.data.content[0].distanceKm").value(0.0))
                .andExpect(jsonPath("$.data.content[0].offeredServices[0].code").value("INCOME_TAX_RETURN"));
    }
}
