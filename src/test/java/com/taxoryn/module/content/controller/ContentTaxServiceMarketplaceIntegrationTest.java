package com.taxoryn.module.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.module.content.dto.CreateContentRequest;
import com.taxoryn.module.content.entity.ContentOwnershipScope;
import com.taxoryn.module.content.entity.ContentType;
import com.taxoryn.module.content.repository.ContentRepository;
import com.taxoryn.module.marketplace.dto.CreateMarketplaceLeadRequest;
import com.taxoryn.module.marketplace.dto.CreateTaxRequirementRequest;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.repository.*;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentTaxServiceMarketplaceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private TaxServiceRepository taxServiceRepository;

    @Autowired
    private TaxServiceCategoryRepository categoryRepository;

    @Autowired
    private MarketplaceProfileRepository profileRepository;

    @Autowired
    private MarketplaceCustomerProfileRepository customerProfileRepository;

    @Autowired
    private CustomerTaxRequirementRepository requirementRepository;

    @Autowired
    private MarketplaceLeadRepository leadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private TaxServiceCategoryEntity testCategory;
    private TaxServiceEntity gstFilingService;
    private TaxServiceEntity gstRegistrationService;
    private TaxServiceEntity inactiveService;
    private OrganizationEntity practiceOrg;
    private MarketplaceProfileEntity practiceProfile;
    private MarketplaceCustomerProfileEntity customerProfile;
    private UserEntity customerUser;

    @BeforeEach
    void setUp() {
        OrganizationEntity taxorynOrg = organizationRepository.save(
                OrganizationEntity.builder()
                        .name("Taxoryn Platform " + UUID.randomUUID())
                        .email("platform." + UUID.randomUUID() + "@taxoryn.com")
                        .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                        .subscriptionPlan(OrganizationEntity.SubscriptionPlan.ENTERPRISE)
                        .build()
        );

        // Practice Org & Marketplace Profile
        practiceOrg = organizationRepository.save(
                OrganizationEntity.builder()
                        .name("Apex CA Practice " + UUID.randomUUID())
                        .email("practice." + UUID.randomUUID() + "@apex.com")
                        .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                        .subscriptionPlan(OrganizationEntity.SubscriptionPlan.BUSINESS)
                        .build()
        );

        practiceProfile = profileRepository.save(
                MarketplaceProfileEntity.builder()
                        .organizationId(practiceOrg.getId())
                        .slug("apex-tax-consultants-" + UUID.randomUUID())
                        .displayName("Apex Tax Consultants")
                        .professionalType(MarketplaceProfileEntity.ProfessionalType.CHARTERED_ACCOUNTANT)
                        .verificationStatus(MarketplaceProfileEntity.VerificationStatus.VERIFIED)
                        .isPublished(true)
                        .city("Bengaluru")
                        .state("Karnataka")
                        .visibilityStatus(MarketplaceProfileEntity.VisibilityStatus.PUBLIC)
                        .build()
        );

        // Roles & Customer User & Profile
        RoleEntity customerRole = roleRepository.save(
                RoleEntity.builder()
                        .code("ROLE_MARKETPLACE_CUSTOMER")
                        .name("Marketplace Customer")
                        .permissions(new HashSet<>())
                        .build()
        );

        customerUser = userRepository.save(
                UserEntity.builder()
                        .organizationId(taxorynOrg.getId())
                        .email("taxpayer." + UUID.randomUUID() + "@gmail.com")
                        .passwordHash("Secret123!")
                        .firstName("Rahul")
                        .lastName("Sharma")
                        .status(UserStatus.ACTIVE)
                        .roles(Set.of(customerRole))
                        .build()
        );

        customerProfile = customerProfileRepository.save(
                MarketplaceCustomerProfileEntity.builder()
                        .userId(customerUser.getId())
                        .firstName("Rahul")
                        .lastName("Sharma")
                        .displayName("Rahul Sharma")
                        .email(customerUser.getEmail())
                        .phone("+919876543210")
                        .customerType(MarketplaceCustomerProfileEntity.CustomerType.INDIVIDUAL)
                        .city("Bengaluru")
                        .state("Karnataka")
                        .status(MarketplaceCustomerProfileEntity.CustomerProfileStatus.ACTIVE)
                        .build()
        );

        // Tax Service Category & Master Services
        testCategory = categoryRepository.save(
                TaxServiceCategoryEntity.builder()
                        .code("GST_" + UUID.randomUUID().toString().substring(0, 8))
                        .name("Goods and Services Tax")
                        .description("GST compliance and filing services")
                        .sortOrder(1)
                        .isActive(true)
                        .build()
        );

        gstFilingService = taxServiceRepository.save(
                TaxServiceEntity.builder()
                        .categoryId(testCategory.getId())
                        .code("GST_FILING_" + UUID.randomUUID().toString().substring(0, 8))
                        .name("GST Return Filing")
                        .description("Monthly & Quarterly GSTR-1, GSTR-3B filings")
                        .sortOrder(1)
                        .isActive(true)
                        .build()
        );

        gstRegistrationService = taxServiceRepository.save(
                TaxServiceEntity.builder()
                        .categoryId(testCategory.getId())
                        .code("GST_REG_" + UUID.randomUUID().toString().substring(0, 8))
                        .name("GST Registration")
                        .description("New GSTIN application and certificate")
                        .sortOrder(2)
                        .isActive(true)
                        .build()
        );

        inactiveService = taxServiceRepository.save(
                TaxServiceEntity.builder()
                        .categoryId(testCategory.getId())
                        .code("DEPRECATED_SVC_" + UUID.randomUUID().toString().substring(0, 8))
                        .name("Deprecated Old Service")
                        .description("Discontinued tax service")
                        .sortOrder(99)
                        .isActive(false)
                        .build()
        );
    }

    @AfterEach
    void tearDown() {
        leadRepository.deleteAll();
        requirementRepository.deleteAll();
        contentRepository.deleteAll();
        taxServiceRepository.deleteAll();
        categoryRepository.deleteAll();
        profileRepository.deleteAll();
        customerProfileRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "content.admin@taxoryn.com", roles = {"TAXORYN_CONTENT_ADMIN"})
    @DisplayName("Content can reference multiple active Tax Services and returns them to customers")
    void testContentWithMultipleActiveTaxServices() throws Exception {
        String slug = "gst-comprehensive-guide-" + UUID.randomUUID();
        CreateContentRequest request = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("GST Comprehensive Guide for Small Businesses")
                .slug(slug)
                .summary("Complete guide on GST registration and returns.")
                .body("Step 1: Apply for GSTIN. Step 2: File monthly GSTR-3B.")
                .categoryId(testCategory.getId())
                .taxServiceId(gstFilingService.getId())
                .taxServiceIds(Set.of(gstFilingService.getId(), gstRegistrationService.getId()))
                .scope(ContentOwnershipScope.PLATFORM)
                .build();

        // 1. Create content via Admin API
        String createResponse = mockMvc.perform(post("/api/v1/admin/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("GST Comprehensive Guide for Small Businesses"))
                .andExpect(jsonPath("$.data.taxServices", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        String contentId = objectMapper.readTree(createResponse).path("data").path("id").asText();

        // 2. Publish Content
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/submit-review"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/approve"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/publish"))
                .andExpect(status().isOk());

        // 3. Customer retrieves public content by slug
        mockMvc.perform(get("/api/v1/public/content/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value(slug))
                .andExpect(jsonPath("$.data.marketplaceCtaEnabled").value(true))
                .andExpect(jsonPath("$.data.taxServices", hasSize(2)))
                .andExpect(jsonPath("$.data.taxServices[*].name", hasItems("GST Return Filing", "GST Registration")));
    }

    @Test
    @WithMockUser(username = "content.admin@taxoryn.com", roles = {"TAXORYN_CONTENT_ADMIN"})
    @DisplayName("Attempting to attach an inactive Tax Service throws BusinessValidationException")
    void testCannotAttachInactiveTaxService() throws Exception {
        CreateContentRequest request = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("Guide with Deprecated Service")
                .slug("deprecated-service-guide-" + UUID.randomUUID())
                .body("Educational content with deprecated service.")
                .taxServiceId(inactiveService.getId())
                .build();

        mockMvc.perform(post("/api/v1/admin/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot attach inactive Tax Service")));
    }

    @Test
    @WithMockUser(username = "content.admin@taxoryn.com", roles = {"TAXORYN_CONTENT_ADMIN"})
    @DisplayName("Deactivating a Tax Service dynamically omits it from public content response")
    void testDeactivatedServiceIsDynamicallyOmitted() throws Exception {
        String slug = "dynamic-tax-service-test-" + UUID.randomUUID();
        CreateContentRequest request = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("Dynamic Service Test")
                .slug(slug)
                .body("Content attached to GST filing.")
                .taxServiceId(gstFilingService.getId())
                .build();

        String createResponse = mockMvc.perform(post("/api/v1/admin/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String contentId = objectMapper.readTree(createResponse).path("data").path("id").asText();

        // Publish
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/submit-review"));
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/approve"));
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/publish"));

        // Public check when active
        mockMvc.perform(get("/api/v1/public/content/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketplaceCtaEnabled").value(true))
                .andExpect(jsonPath("$.data.taxServices", hasSize(1)));

        // Deactivate the tax service in master
        gstFilingService.setIsActive(false);
        taxServiceRepository.save(gstFilingService);

        // Public check after deactivation: service is omitted and CTA is disabled
        mockMvc.perform(get("/api/v1/public/content/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketplaceCtaEnabled").value(false))
                .andExpect(jsonPath("$.data.taxServices", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "content.admin@taxoryn.com", roles = {"TAXORYN_CONTENT_ADMIN"})
    @DisplayName("End-to-End Flow: Customer discovers content -> clicks CTA -> creates Requirement and Enquiry retaining source attribution")
    void testEndToEndContentToMarketplaceAttribution() throws Exception {
        // 1. Create & Publish Article
        String slug = "itr-filing-masterclass-" + UUID.randomUUID();
        CreateContentRequest request = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("ITR Filing Masterclass")
                .slug(slug)
                .body("Detailed instructions on filing income tax returns.")
                .taxServiceId(gstFilingService.getId())
                .build();

        String createResponse = mockMvc.perform(post("/api/v1/admin/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String contentIdStr = objectMapper.readTree(createResponse).path("data").path("id").asText();
        UUID contentId = UUID.fromString(contentIdStr);

        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/submit-review"));
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/approve"));
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/publish"));

        // 2. Customer submits direct marketplace Enquiry / Lead originating from Content
        CreateMarketplaceLeadRequest leadPayload = CreateMarketplaceLeadRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .taxServiceId(gstFilingService.getId())
                .clientName("Rahul Sharma")
                .clientEmail("rahul.sharma@gmail.com")
                .clientPhone("+919876543210")
                .city("Bengaluru")
                .serviceCategory("GST")
                .sourceType("TAXORYN_LEARN")
                .sourceContentId(contentId)
                .requirementDescription("Inquiry generated from ITR Masterclass guide.")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sourceType").value("TAXORYN_LEARN"))
                .andExpect(jsonPath("$.data.sourceContentId").value(contentIdStr))
                .andExpect(jsonPath("$.data.taxServiceName").value(gstFilingService.getName()))
                .andExpect(jsonPath("$.data.pan").doesNotExist())
                .andExpect(jsonPath("$.data.gstin").doesNotExist());
    }

    @Test
    @WithMockUser(username = "content.admin@taxoryn.com", roles = {"TAXORYN_CONTENT_ADMIN"})
    @DisplayName("Content without attached Tax Service does not enable marketplace CTA")
    void testContentWithoutTaxServiceHasCtaDisabled() throws Exception {
        String slug = "general-record-keeping-" + UUID.randomUUID();
        CreateContentRequest request = CreateContentRequest.builder()
                .contentType(ContentType.ARTICLE)
                .title("Organizing Financial Records")
                .slug(slug)
                .body("Keep all receipts in cloud storage.")
                .build();

        String createResponse = mockMvc.perform(post("/api/v1/admin/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String contentIdStr = objectMapper.readTree(createResponse).path("data").path("id").asText();
        UUID contentId = UUID.fromString(contentIdStr);

        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/submit-review"));
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/approve"));
        mockMvc.perform(post("/api/v1/admin/content/" + contentId + "/publish"));

        mockMvc.perform(get("/api/v1/public/content/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketplaceCtaEnabled").value(false))
                .andExpect(jsonPath("$.data.taxServices", hasSize(0)))
                .andExpect(jsonPath("$.data.taxServiceId").doesNotExist());
    }
}
