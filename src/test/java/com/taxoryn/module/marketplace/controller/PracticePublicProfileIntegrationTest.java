package com.taxoryn.module.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.content.service.ContentService;
import com.taxoryn.module.marketplace.dto.CreateMarketplaceLeadRequest;
import com.taxoryn.module.marketplace.dto.UpdateMarketplaceProfileRequest;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PracticePublicProfileIntegrationTest {

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
    private MarketplaceProfileSlugRedirectRepository redirectRepository;

    @Autowired
    private PracticeLocationRepository locationRepository;

    @Autowired
    private PracticeServiceRepository practiceServiceRepository;

    @Autowired
    private TaxServiceCategoryRepository categoryRepository;

    @Autowired
    private TaxServiceRepository taxServiceRepository;

    @Autowired
    private MarketplaceReviewRepository reviewRepository;

    @Autowired
    private MarketplaceLeadRepository leadRepository;

    @Autowired
    private ContentService contentService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity practiceOrg;
    private UserEntity practiceAdmin;
    private String practiceAdminToken;
    private MarketplaceProfileEntity verifiedProfile;
    private TaxServiceEntity gstService;
    private TaxServiceCategoryEntity taxCategory;

    @BeforeEach
    void setUp() {
        leadRepository.deleteAll();
        reviewRepository.deleteAll();
        practiceServiceRepository.deleteAll();
        locationRepository.deleteAll();
        redirectRepository.deleteAll();
        profileRepository.deleteAll();

        // 1. Create Organization
        practiceOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA Associates " + UUID.randomUUID().toString().substring(0, 6))
                .email("admin@apexca" + UUID.randomUUID().toString().substring(0, 4) + ".com")
                .status(OrganizationStatus.ACTIVE)
                .city("Bangalore")
                .state("Karnataka")
                .country("India")
                .pincode("560001")
                .build());

        // 2. Setup Permissions & Roles
        PermissionEntity viewPerm = permissionRepository.findByCode("MARKETPLACE_VIEW")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().code("MARKETPLACE_VIEW").name("View MP").module("MARKETPLACE").build()));
        PermissionEntity writePerm = permissionRepository.findByCode("MARKETPLACE_WRITE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().code("MARKETPLACE_WRITE").name("Write MP").module("MARKETPLACE").build()));

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>(Set.of(viewPerm, writePerm)))
                        .build()));

        // 3. Setup User & JWT Token
        practiceAdmin = UserEntity.builder()
                .email("ca.admin" + UUID.randomUUID().toString().substring(0, 6) + "@apexca.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Rajesh")
                .lastName("Sharma")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserStatus.ACTIVE)
                .build();
        practiceAdmin.setOrganizationId(practiceOrg.getId());
        practiceAdmin = userRepository.save(practiceAdmin);

        practiceAdminToken = jwtTokenProvider.generateAccessToken(
                practiceAdmin.getId(),
                practiceOrg.getId(),
                practiceAdmin.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE")
        );

        // 4. Create Master Tax Category and Master Tax Service
        taxCategory = categoryRepository.findByCodeIgnoreCase("GST").orElseGet(() ->
                categoryRepository.save(TaxServiceCategoryEntity.builder()
                        .code("GST")
                        .name("Goods & Services Tax")
                        .sortOrder(1)
                        .isActive(true)
                        .build())
        );

        gstService = taxServiceRepository.findByCodeIgnoreCase("GST_FILING").orElseGet(() ->
                taxServiceRepository.save(TaxServiceEntity.builder()
                        .categoryId(taxCategory.getId())
                        .code("GST_FILING")
                        .name("GST Return Filing (GSTR-1 & 3B)")
                        .description("Monthly and quarterly GST return filing for regular taxpayers.")
                        .isActive(true)
                        .build())
        );

        // 5. Create Verified Marketplace Practice Profile
        String profileSlug = "apex-ca-associates-" + UUID.randomUUID().toString().substring(0, 6);
        verifiedProfile = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(practiceOrg.getId())
                .slug(profileSlug)
                .displayName("Apex CA Associates")
                .headline("Premier Chartered Accountants in Bangalore")
                .bio("Specialized in corporate tax optimization, GST representation, and income tax compliance.")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .experienceYears(12)
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560001")
                .languagesSpoken("English, Hindi, Kannada")
                .workingHours("Mon - Fri: 9:30 AM - 6:30 PM, Sat: 10:00 AM - 2:00 PM")
                .seoTitle("Apex CA Associates - Top Chartered Accountants Bangalore")
                .metaDescription("Verified CA firm providing GST, ITR, and corporate tax advisory services in Bangalore.")
                .canonicalUrl("https://taxoryn.com/practice/" + profileSlug)
                .startingFee(new BigDecimal("1499.00"))
                .hourlyRate(new BigDecimal("2500.00"))
                .averageRating(new BigDecimal("4.95"))
                .totalReviews(8)
                .verificationStatus(VerificationStatus.VERIFIED)
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .isPublished(true)
                .isFeatured(true)
                .consultationEnabled(true)
                .consultationFee(new BigDecimal("499.00"))
                .consultationDurationMinutes(30)
                .build());

        // 6. Attach Master Service to Practice
        practiceServiceRepository.save(PracticeServiceEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(verifiedProfile.getId())
                .taxServiceId(gstService.getId())
                .isActive(true)
                .build());

        // 7. Attach Primary Practice Location
        locationRepository.save(PracticeLocationEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(verifiedProfile.getId())
                .locationName("Headquarters - MG Road")
                .addressLine1("101, Brigade Towers, MG Road")
                .landmark("Near Trinity Metro Station")
                .city("Bangalore")
                .district("Bangalore Urban")
                .state("Karnataka")
                .pincode("560001")
                .latitude(new BigDecimal("12.971598"))
                .longitude(new BigDecimal("77.594566"))
                .isPrimary(true)
                .isActive(true)
                .build());

        // 8. Attach Approved Review
        reviewRepository.save(MarketplaceReviewEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(verifiedProfile.getId())
                .reviewerName("Vikram Singhania")
                .reviewerDesignation("Founder & CEO")
                .reviewerCompany("TechLogix India Pvt Ltd")
                .rating(5)
                .reviewTitle("Outstanding GST & Corporate Compliance Advisory")
                .reviewComment("Apex CA helped us streamline multi-state GST filing and resolve tax department notices swiftly.")
                .serviceTaken("GST Return Filing")
                .isVerifiedClient(true)
                .status(MarketplaceReviewEntity.ReviewStatus.APPROVED)
                .build());
    }

    @Test
    @DisplayName("7.1 - Public verified practice profile is accessible without authentication by vanity slug")
    void testPublicVerifiedPracticeIsAccessibleBySlug() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/profiles/slug/" + verifiedProfile.getSlug())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Apex CA Associates"))
                .andExpect(jsonPath("$.data.publicSlug").value(verifiedProfile.getSlug()))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.visibilityStatus").value("PUBLIC"))
                .andExpect(jsonPath("$.data.workingHours").value("Mon - Fri: 9:30 AM - 6:30 PM, Sat: 10:00 AM - 2:00 PM"))
                .andExpect(jsonPath("$.data.seoTitle").value("Apex CA Associates - Top Chartered Accountants Bangalore"))
                .andExpect(jsonPath("$.data.metaDescription").value("Verified CA firm providing GST, ITR, and corporate tax advisory services in Bangalore."))
                .andExpect(jsonPath("$.data.offeredServices", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.offeredServices[0].code").value("GST_FILING"))
                .andExpect(jsonPath("$.data.locations", hasSize(1)))
                .andExpect(jsonPath("$.data.locations[0].locationName").value("Headquarters - MG Road"))
                .andExpect(jsonPath("$.data.recentReviews", hasSize(1)))
                .andExpect(jsonPath("$.data.recentReviews[0].reviewerName").value("Vikram Singhania"));
    }

    @Test
    @DisplayName("7.2 - Private practice profile returns 404 for unauthenticated callers")
    void testPrivatePracticeReturns404ForUnauthenticated() throws Exception {
        verifiedProfile.setVisibilityStatus(VisibilityStatus.PRIVATE);
        verifiedProfile.setIsPublished(false);
        profileRepository.save(verifiedProfile);

        mockMvc.perform(get("/api/v1/marketplace/profiles/slug/" + verifiedProfile.getSlug())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("7.3 - Updating practice display name does not alter established public URL slug (Slug Stability)")
    void testSlugStabilityOnDisplayNameUpdate() throws Exception {
        String originalSlug = verifiedProfile.getSlug();

        UpdateMarketplaceProfileRequest request = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Global Tax Advisors (Updated)")
                .phone("+91 98888 77777")
                .email("contact@apexglobal.com")
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560001")
                .build();

        mockMvc.perform(put("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + practiceAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Apex Global Tax Advisors (Updated)"))
                .andExpect(jsonPath("$.data.publicSlug").value(originalSlug));

        MarketplaceProfileEntity refreshed = profileRepository.findById(verifiedProfile.getId()).orElseThrow();
        assertEquals(originalSlug, refreshed.getSlug(), "Public slug must remain stable when display name changes");
    }

    @Test
    @DisplayName("7.4 - Explicit slug change records redirect and flattens redirect chains without hops")
    void testExplicitSlugChangeCreatesRedirectWithoutChains() throws Exception {
        String slugA = verifiedProfile.getSlug();
        String slugB = "apex-tax-consulting-" + UUID.randomUUID().toString().substring(0, 6);
        String slugC = "apex-premier-tax-" + UUID.randomUUID().toString().substring(0, 6);

        // Step 1: Update slug A -> B
        UpdateMarketplaceProfileRequest update1 = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex CA Associates")
                .slug(slugB)
                .phone("+91 98888 77777")
                .email("contact@apexca.com")
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560001")
                .build();

        mockMvc.perform(put("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + practiceAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicSlug").value(slugB));

        // Step 2: Update slug B -> C
        UpdateMarketplaceProfileRequest update2 = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex CA Associates")
                .slug(slugC)
                .phone("+91 98888 77777")
                .email("contact@apexca.com")
                .city("Bangalore")
                .state("Karnataka")
                .pincode("560001")
                .build();

        mockMvc.perform(put("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + practiceAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicSlug").value(slugC));

        // Verify that old slug A now points directly to C (flattened chain)
        MarketplaceProfileSlugRedirectEntity redirectA = redirectRepository.findByOldSlug(slugA).orElseThrow();
        assertEquals(slugC, redirectA.getNewSlug(), "Chain must be flattened: slugA -> slugC");

        // Verify accessing old slug A resolves target profile with redirectSlug = slugC
        mockMvc.perform(get("/api/v1/marketplace/profiles/slug/" + slugA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicSlug").value(slugC))
                .andExpect(jsonPath("$.data.redirectSlug").value(slugC));
    }

    @Test
    @DisplayName("7.5 - Public profile DTO strictly redacts sensitive financial and internal identifiers")
    void testPublicProfileExposesOnlyApprovedFields() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/profiles/slug/" + verifiedProfile.getSlug())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pan").doesNotExist())
                .andExpect(jsonPath("$.data.gstin").doesNotExist())
                .andExpect(jsonPath("$.data.internalNotes").doesNotExist())
                .andExpect(jsonPath("$.data.verificationDocuments").doesNotExist());
    }

    @Test
    @DisplayName("7.6 - Customer enquiry preserves TAXORYN_PRACTICE_PROFILE source and selected tax service")
    void testCustomerEnquiryPreservesPracticeAndServiceContext() throws Exception {
        CreateMarketplaceLeadRequest request = CreateMarketplaceLeadRequest.builder()
                .marketplaceProfileId(verifiedProfile.getId())
                .taxServiceId(gstService.getId())
                .sourceType("TAXORYN_PRACTICE_PROFILE")
                .clientName("Suresh Raina")
                .clientEmail("suresh.raina@techcorp.in")
                .clientPhone("+91 99887 76655")
                .city("Bangalore")
                .requirementDescription("Need monthly GSTR-1 and GSTR-3B compliance for private limited company")
                .budgetRange("₹5,000 - ₹10,000")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientName").value("Suresh Raina"))
                .andExpect(jsonPath("$.data.sourceType").value("TAXORYN_PRACTICE_PROFILE"))
                .andExpect(jsonPath("$.data.taxServiceName").value("GST Return Filing (GSTR-1 & 3B)"));

        var leads = leadRepository.findAll();
        assertFalse(leads.isEmpty());
        assertEquals("TAXORYN_PRACTICE_PROFILE", leads.get(0).getSourceType());
        assertEquals(gstService.getId(), leads.get(0).getTaxServiceId());
    }

    @Test
    @DisplayName("7.7 - Dynamic sitemap contains eligible verified practice profile URLs and robots.txt allows /practice")
    void testDynamicSitemapAndRobotsTxtIntegration() throws Exception {
        // 1. Check sitemap
        String sitemapXml = contentService.generateSitemapXml();
        assertTrue(sitemapXml.contains("https://taxoryn.com/practice/" + verifiedProfile.getSlug()),
                "Sitemap must contain eligible verified practice profile URL");

        // 2. Check robots.txt
        String robotsTxt = contentService.getRobotsTxtContent();
        assertTrue(robotsTxt.contains("Allow: /practice\nAllow: /practice/*"),
                "robots.txt must allow /practice and /practice/*");
        assertTrue(robotsTxt.contains("Allow: /professional\nAllow: /professional/*"),
                "robots.txt must allow /professional");
    }

    @Test
    @DisplayName("7.8 - Authenticated Practice Admin can preview practice profile via /preview endpoint")
    void testPreviewPracticeProfileForAuthenticatedAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/practice-profile/preview")
                        .header("Authorization", "Bearer " + practiceAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Apex CA Associates"))
                .andExpect(jsonPath("$.data.publicSlug").value(verifiedProfile.getSlug()));
    }
}
