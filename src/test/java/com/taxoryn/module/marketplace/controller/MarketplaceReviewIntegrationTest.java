package com.taxoryn.module.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.marketplace.dto.ModerateReviewRequest;
import com.taxoryn.module.marketplace.dto.SubmitMarketplaceReviewRequest;
import com.taxoryn.module.marketplace.entity.EnquiryStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceReviewEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceReviewEntity.ReviewStatus;
import com.taxoryn.module.marketplace.repository.MarketplaceCustomerProfileRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceLeadRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceReviewRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.PermissionRepository;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
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
class MarketplaceReviewIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MarketplaceProfileRepository profileRepository;

    @Autowired
    private MarketplaceLeadRepository leadRepository;

    @Autowired
    private MarketplaceReviewRepository reviewRepository;

    @Autowired
    private MarketplaceCustomerProfileRepository customerProfileRepository;

    private OrganizationEntity practiceOrg;
    private UserEntity practiceAdminUser;
    private String practiceAdminToken;

    private UserEntity superAdminUser;
    private String superAdminToken;

    private UserEntity customerUser;
    private String customerToken;

    private UserEntity otherCustomerUser;
    private String otherCustomerToken;

    private MarketplaceProfileEntity practiceProfile;
    private MarketplaceLeadEntity completedLead;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        leadRepository.deleteAll();
        profileRepository.deleteAll();

        // 1. Roles & Permissions Setup
        PermissionEntity pView = permissionRepository.findByCode("MARKETPLACE_VIEW")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().code("MARKETPLACE_VIEW").name("View").module("MARKETPLACE").build()));
        PermissionEntity pWrite = permissionRepository.findByCode("MARKETPLACE_WRITE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().code("MARKETPLACE_WRITE").name("Write").module("MARKETPLACE").build()));

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build()));
        orgAdminRole.setPermissions(new HashSet<>(Set.of(pView, pWrite)));
        roleRepository.save(orgAdminRole);

        RoleEntity superAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().code("SUPER_ADMIN").name("Super Admin").isSystemRole(true).build()));

        RoleEntity customerRole = roleRepository.findByCodeAndIsSystemRoleTrue("MARKETPLACE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().code("MARKETPLACE_CUSTOMER").name("Customer").isSystemRole(true).build()));

        // 2. Practice Firm Setup
        practiceOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Veritas Tax Advisors " + UUID.randomUUID().toString().substring(0, 6))
                .legalName("Veritas Tax Advisors LLP")
                .email("admin@veritastax" + UUID.randomUUID().toString().substring(0, 4) + ".com")
                .phone("+91 98200 11223")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .pan("AABCV1234F")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        practiceAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(practiceOrg.getId())
                .email("admin@veritastax" + UUID.randomUUID().toString().substring(0, 6) + ".com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Aditya")
                .lastName("Verma")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        practiceAdminToken = jwtTokenProvider.generateAccessToken(
                practiceAdminUser.getId(),
                practiceOrg.getId(),
                practiceAdminUser.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE")
        );

        // 3. Super Admin Setup
        superAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(practiceOrg.getId())
                .email("superadmin" + UUID.randomUUID().toString().substring(0, 6) + "@taxoryn.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Platform")
                .lastName("SuperAdmin")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(superAdminRole)))
                .build());

        superAdminToken = jwtTokenProvider.generateAccessToken(
                superAdminUser.getId(),
                practiceOrg.getId(),
                superAdminUser.getEmail(),
                Set.of("SUPER_ADMIN"),
                Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE")
        );

        // 4. Eligible Customer Setup
        customerUser = userRepository.save(UserEntity.builder()
                .email("client.aarav" + UUID.randomUUID().toString().substring(0, 6) + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Aarav")
                .lastName("Mehta")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(customerRole)))
                .build());

        customerToken = jwtTokenProvider.generateAccessToken(
                customerUser.getId(),
                null,
                customerUser.getEmail(),
                Set.of("MARKETPLACE_CUSTOMER"),
                Set.of()
        );

        // 5. Ineligible Customer Setup (No completed service with this practice)
        otherCustomerUser = userRepository.save(UserEntity.builder()
                .email("unrelated.user" + UUID.randomUUID().toString().substring(0, 6) + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Kavita")
                .lastName("Nair")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(customerRole)))
                .build());

        otherCustomerToken = jwtTokenProvider.generateAccessToken(
                otherCustomerUser.getId(),
                null,
                otherCustomerUser.getEmail(),
                Set.of("MARKETPLACE_CUSTOMER"),
                Set.of()
        );

        // 6. Published Public Practice Profile
        practiceProfile = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(practiceOrg.getId())
                .displayName("Veritas Tax Advisors")
                .slug("veritas-tax-advisors-" + UUID.randomUUID().toString().substring(0, 6))
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .email("contact@veritastax.com")
                .phone("+91 98200 11223")
                .professionalType(MarketplaceProfileEntity.ProfessionalType.CHARTERED_ACCOUNTANT)
                .isPublished(true)
                .visibilityStatus(MarketplaceProfileEntity.VisibilityStatus.PUBLIC)
                .averageRating(new BigDecimal("0.00"))
                .totalReviews(0)
                .build());

        // 7. Authoritative Completed Engagement/Lead
        completedLead = leadRepository.save(MarketplaceLeadEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .customerId(customerUser.getId())
                .referenceNumber("TXN-2026-REV" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .clientName("Aarav Mehta")
                .clientEmail(customerUser.getEmail())
                .clientPhone("+91 98765 43210")
                .city("Bengaluru")
                .serviceCategory("GST Filing & Advisory")
                .enquiryStatus(EnquiryStatus.COMPLETED)
                .completedAt(java.time.Instant.now())
                .build());
    }

    @Test
    @DisplayName("3.4.1 - Eligible client successfully submits review; starts in PENDING moderation state")
    void testSubmitReview_EligibleClient_Success() throws Exception {
        SubmitMarketplaceReviewRequest request = SubmitMarketplaceReviewRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .leadId(completedLead.getId())
                .rating(5)
                .reviewTitle("Exceptional GST Advisory")
                .reviewComment("Veritas CA provided thorough support and cleared our queries promptly.")
                .serviceTaken("GST Filing")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.reviewerName").value("Aarav Mehta"))
                .andExpect(jsonPath("$.data.isVerifiedClient").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.reviewTitle").value("Exceptional GST Advisory"));

        // Verify review exists in DB with PENDING status
        assertEquals(1, reviewRepository.count());
        MarketplaceReviewEntity saved = reviewRepository.findAll().get(0);
        assertEquals(ReviewStatus.PENDING, saved.getStatus());
        assertEquals(customerUser.getId(), saved.getCustomerId());
        assertEquals(completedLead.getId(), saved.getLeadId());

        // Verify public rating is still 0 until approved by moderation
        MarketplaceProfileEntity currentProfile = profileRepository.findById(practiceProfile.getId()).orElseThrow();
        assertEquals(0, currentProfile.getTotalReviews());
    }

    @Test
    @DisplayName("3.4.2 - Unauthenticated submission is rejected with 401 Unauthorized")
    void testSubmitReview_Unauthenticated_Rejected() throws Exception {
        SubmitMarketplaceReviewRequest request = SubmitMarketplaceReviewRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .rating(5)
                .reviewComment("Anonymous unverified review attempting submission.")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3.4.3 - Ineligible user without completed engagement is rejected with 400 Bad Request")
    void testSubmitReview_IneligibleUser_Rejected() throws Exception {
        SubmitMarketplaceReviewRequest request = SubmitMarketplaceReviewRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .rating(4)
                .reviewComment("I have never engaged this firm but am writing a review.")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .header("Authorization", "Bearer " + otherCustomerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("after completing an engagement")));
    }

    @Test
    @DisplayName("3.4.4 - Duplicate review submission for same completed engagement is rejected")
    void testSubmitReview_DuplicateSubmission_Rejected() throws Exception {
        SubmitMarketplaceReviewRequest request = SubmitMarketplaceReviewRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .leadId(completedLead.getId())
                .rating(5)
                .reviewComment("First legitimate review for completed GST filing.")
                .build();

        // First submission succeeds
        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate submission fails
        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already been submitted")));
    }

    @Test
    @DisplayName("3.4.5 - Invalid ratings below 1 or above 5 are rejected")
    void testSubmitReview_InvalidRating_Rejected() throws Exception {
        SubmitMarketplaceReviewRequest lowRating = SubmitMarketplaceReviewRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .leadId(completedLead.getId())
                .rating(0)
                .reviewComment("Attempting 0 star review.")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowRating)))
                .andExpect(status().isBadRequest());

        SubmitMarketplaceReviewRequest highRating = SubmitMarketplaceReviewRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .leadId(completedLead.getId())
                .rating(6)
                .reviewComment("Attempting 6 star review.")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(highRating)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("3.4.6 - Review comment with whitespace or under 10 chars is rejected")
    void testSubmitReview_ShortComment_Rejected() throws Exception {
        SubmitMarketplaceReviewRequest shortComment = SubmitMarketplaceReviewRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .leadId(completedLead.getId())
                .rating(5)
                .reviewComment("  Good  ")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortComment)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("3.4.7 - Practitioner cannot review their own practice")
    void testSubmitReview_PractitionerCannotReviewOwnFirm_Rejected() throws Exception {
        SubmitMarketplaceReviewRequest request = SubmitMarketplaceReviewRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .rating(5)
                .reviewComment("We are the best tax firm in Bengaluru!")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .header("Authorization", "Bearer " + practiceAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot review their own practice")));
    }

    @Test
    @DisplayName("3.4.8 - Public review listing and rating summary only expose APPROVED reviews")
    void testPublicReviews_ExcludesNonApprovedReviews() throws Exception {
        // 1 Approved review
        reviewRepository.save(MarketplaceReviewEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .customerId(customerUser.getId())
                .reviewerName("Approved Client")
                .rating(5)
                .reviewComment("Approved positive review comment here.")
                .status(ReviewStatus.APPROVED)
                .isVerifiedClient(true)
                .build());

        // 1 Pending review
        reviewRepository.save(MarketplaceReviewEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .customerId(otherCustomerUser.getId())
                .reviewerName("Pending Reviewer")
                .rating(1)
                .reviewComment("Pending moderation review comment.")
                .status(ReviewStatus.PENDING)
                .isVerifiedClient(true)
                .build());

        // 1 Rejected review
        reviewRepository.save(MarketplaceReviewEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .reviewerName("Rejected Reviewer")
                .rating(1)
                .reviewComment("Spam comment rejected by platform.")
                .status(ReviewStatus.REJECTED)
                .isVerifiedClient(false)
                .build());

        // Fetch public reviews
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + practiceProfile.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].reviewerName").value("Approved Client"))
                .andExpect(jsonPath("$.data[0].rating").value(5))
                .andExpect(jsonPath("$.data[0].moderatedBy").doesNotExist())
                .andExpect(jsonPath("$.data[0].moderationNote").doesNotExist())
                .andExpect(jsonPath("$.data[0].leadId").doesNotExist());

        // Fetch rating summary
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + practiceProfile.getId() + "/rating-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReviews").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(5.0))
                .andExpect(jsonPath("$.data.fiveStarCount").value(1))
                .andExpect(jsonPath("$.data.oneStarCount").value(0));
    }

    @Test
    @DisplayName("3.4.9 - Super Admin moderates pending review to APPROVED; aggregate rating recalculates deterministically")
    void testModeration_SuperAdminApproves_RecalculatesRating() throws Exception {
        // Seed Pending Review
        MarketplaceReviewEntity review = reviewRepository.save(MarketplaceReviewEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .customerId(customerUser.getId())
                .reviewerName("Aarav Mehta")
                .rating(4)
                .reviewComment("Excellent timely compliance execution.")
                .status(ReviewStatus.PENDING)
                .isVerifiedClient(true)
                .build());

        ModerateReviewRequest moderateReq = ModerateReviewRequest.builder()
                .status(ReviewStatus.APPROVED)
                .moderationNote("Verified against GST engagement reference TXN-2026-REV.")
                .build();

        mockMvc.perform(patch("/api/v1/admin/marketplace/reviews/" + review.getId() + "/status")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moderateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.moderatedBy").value(superAdminUser.getEmail()));

        // Profile should now have totalReviews = 1 and averageRating = 4.00
        MarketplaceProfileEntity updatedProfile = profileRepository.findById(practiceProfile.getId()).orElseThrow();
        assertEquals(1, updatedProfile.getTotalReviews());
        assertEquals(new BigDecimal("4.00"), updatedProfile.getAverageRating());
    }

    @Test
    @DisplayName("3.4.10 - Unauthorized moderation attempt is rejected with 403 Forbidden")
    void testModeration_UnauthorizedUser_Forbidden() throws Exception {
        MarketplaceReviewEntity review = reviewRepository.save(MarketplaceReviewEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .customerId(customerUser.getId())
                .reviewerName("Aarav Mehta")
                .rating(5)
                .reviewComment("Client attempting to self-approve review.")
                .status(ReviewStatus.PENDING)
                .build());

        ModerateReviewRequest moderateReq = ModerateReviewRequest.builder()
                .status(ReviewStatus.APPROVED)
                .build();

        mockMvc.perform(patch("/api/v1/admin/marketplace/reviews/" + review.getId() + "/status")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moderateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3.4.11 - Eligibility endpoint returns authoritative eligible status for qualified client")
    void testEligibilityEndpoint_EvaluatesCorrectly() throws Exception {
        // Customer has completed lead -> eligible = true
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + practiceProfile.getId() + "/reviews/eligibility")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.alreadyReviewed").value(false))
                .andExpect(jsonPath("$.data.completedLeadId").value(completedLead.getId().toString()));

        // Unrelated customer -> eligible = false
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + practiceProfile.getId() + "/reviews/eligibility")
                        .header("Authorization", "Bearer " + otherCustomerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(false))
                .andExpect(jsonPath("$.data.alreadyReviewed").value(false));
    }

    @Test
    @DisplayName("3.4.12 - Practice Reviews Feed adheres to strict multi-tenant isolation")
    void testPracticeReviews_TenantIsolation() throws Exception {
        // Create second practice organization
        OrganizationEntity practiceOrgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Consultants " + UUID.randomUUID().toString().substring(0, 6))
                .legalName("Apex Tax Consultants LLP")
                .email("admin@apextax" + UUID.randomUUID().toString().substring(0, 4) + ".com")
                .phone("+91 98200 99887")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .pan("AABCA9876F")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseThrow();
        UserEntity practiceAdminUserB = userRepository.save(UserEntity.builder()
                .organizationId(practiceOrgB.getId())
                .email("admin@apextax" + UUID.randomUUID().toString().substring(0, 6) + ".com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Suresh")
                .lastName("Rao")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        String practiceAdminTokenB = jwtTokenProvider.generateAccessToken(
                practiceAdminUserB.getId(),
                practiceOrgB.getId(),
                practiceAdminUserB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE")
        );

        MarketplaceProfileEntity practiceProfileB = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(practiceOrgB.getId())
                .displayName("Apex Tax Consultants")
                .slug("apex-tax-" + UUID.randomUUID().toString().substring(0, 6))
                .isPublished(true)
                .visibilityStatus(MarketplaceProfileEntity.VisibilityStatus.PUBLIC)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build());

        // Review for Org A
        reviewRepository.save(MarketplaceReviewEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .reviewerName("Org A Client")
                .rating(5)
                .reviewComment("Review specifically for Practice A.")
                .status(ReviewStatus.APPROVED)
                .isVerifiedClient(true)
                .build());

        // Review for Org B
        reviewRepository.save(MarketplaceReviewEntity.builder()
                .organizationId(practiceOrgB.getId())
                .marketplaceProfileId(practiceProfileB.getId())
                .reviewerName("Org B Client")
                .rating(4)
                .reviewComment("Review specifically for Practice B.")
                .status(ReviewStatus.APPROVED)
                .isVerifiedClient(true)
                .build());

        // 1. Practice A requests /reviews -> should only see Org A's review
        mockMvc.perform(get("/api/v1/marketplace/practice-profile/reviews")
                        .header("Authorization", "Bearer " + practiceAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].reviewerName").value("Org A Client"))
                .andExpect(jsonPath("$.data[0].moderatedBy").doesNotExist())
                .andExpect(jsonPath("$.data[0].moderationNote").doesNotExist());

        // 2. Practice B requests /reviews -> should only see Org B's review
        mockMvc.perform(get("/api/v1/marketplace/practice-profile/reviews")
                        .header("Authorization", "Bearer " + practiceAdminTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].reviewerName").value("Org B Client"))
                .andExpect(jsonPath("$.data[0].moderatedBy").doesNotExist())
                .andExpect(jsonPath("$.data[0].moderationNote").doesNotExist());
    }

    @Test
    @DisplayName("3.4.13 - Public endpoints (list, page, profile) completely exclude admin identity and moderation notes")
    void testPublicReviews_DoesNotLeakSensitiveAdminMetadata() throws Exception {
        // Create an approved review that has moderation metadata
        MarketplaceReviewEntity review = reviewRepository.save(MarketplaceReviewEntity.builder()
                .organizationId(practiceOrg.getId())
                .marketplaceProfileId(practiceProfile.getId())
                .customerId(customerUser.getId())
                .leadId(completedLead.getId())
                .reviewerName("Aarav Mehta")
                .rating(5)
                .reviewTitle("Great GST Guidance")
                .reviewComment("Comprehensive guidance on GST compliance and invoicing.")
                .status(ReviewStatus.APPROVED)
                .moderatedBy("superadmin.secret@taxoryn.internal")
                .moderatedAt(java.time.Instant.now())
                .moderationNote("CONFIDENTIAL: Verified with bank statement and ledger match.")
                .isVerifiedClient(true)
                .build());

        // 1. Check GET /profiles/{id}/reviews
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + practiceProfile.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reviewerName").value("Aarav Mehta"))
                .andExpect(jsonPath("$.data[0].moderatedBy").doesNotExist())
                .andExpect(jsonPath("$.data[0].moderationNote").doesNotExist())
                .andExpect(jsonPath("$.data[0].leadId").doesNotExist());

        // 2. Check GET /profiles/{id}/reviews/page
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + practiceProfile.getId() + "/reviews/page?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].reviewerName").value("Aarav Mehta"))
                .andExpect(jsonPath("$.data.content[0].moderatedBy").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].moderationNote").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].leadId").doesNotExist());

        // 3. Check GET /profiles/{id} embedded recentReviews
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + practiceProfile.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentReviews[0].reviewerName").value("Aarav Mehta"))
                .andExpect(jsonPath("$.data.recentReviews[0].moderatedBy").doesNotExist())
                .andExpect(jsonPath("$.data.recentReviews[0].moderationNote").doesNotExist())
                .andExpect(jsonPath("$.data.recentReviews[0].leadId").doesNotExist());

        // 4. Verify admin endpoint STILL retains moderation metadata
        mockMvc.perform(get("/api/v1/admin/marketplace/reviews?status=APPROVED")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].moderatedBy").value("superadmin.secret@taxoryn.internal"))
                .andExpect(jsonPath("$.data.content[0].moderationNote").value("CONFIDENTIAL: Verified with bank statement and ledger match."));
    }

    @Test
    @DisplayName("3.4.14 - Non-lead direct review duplicate constraint rejects duplicate submission gracefully")
    void testDirectReview_NonLeadDuplicateConstraint_Rejected() throws Exception {
        // Authenticated client of firm (without lead)
        UUID firmClientId = UUID.randomUUID();
        String firmClientToken = jwtTokenProvider.generateAccessToken(
                customerUser.getId(),
                practiceOrg.getId(),
                firmClientId,
                customerUser.getEmail(),
                Set.of("MARKETPLACE_CUSTOMER"),
                Set.of()
        );

        SubmitMarketplaceReviewRequest request = SubmitMarketplaceReviewRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .rating(5)
                .reviewTitle("Direct Client Review")
                .reviewComment("Direct engagement review for corporate accounting.")
                .build();

        // First submission succeeds
        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .header("Authorization", "Bearer " + firmClientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // Duplicate submission fails gracefully with 400 Bad Request
        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .header("Authorization", "Bearer " + firmClientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already been submitted")));
    }

    @Test
    @DisplayName("3.4.15 - Direct review constraint allows distinct practitioner profiles for same customer")
    void testDirectReview_DistinctPractitionerProfiles_Allowed() throws Exception {
        // Create second practice organization and profile
        OrganizationEntity practiceOrg2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Zenith Tax Partners " + UUID.randomUUID().toString().substring(0, 6))
                .legalName("Zenith Tax Partners LLP")
                .email("admin@zenith" + UUID.randomUUID().toString().substring(0, 4) + ".com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        MarketplaceProfileEntity practiceProfile2 = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(practiceOrg2.getId())
                .displayName("Zenith Tax Partners")
                .slug("zenith-tax-" + UUID.randomUUID().toString().substring(0, 6))
                .isPublished(true)
                .visibilityStatus(MarketplaceProfileEntity.VisibilityStatus.PUBLIC)
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .build());

        // Client linked to practiceOrg
        UUID firmClientId = UUID.randomUUID();
        String firmClientToken1 = jwtTokenProvider.generateAccessToken(
                customerUser.getId(),
                practiceOrg.getId(),
                firmClientId,
                customerUser.getEmail(),
                Set.of("MARKETPLACE_CUSTOMER"),
                Set.of()
        );

        SubmitMarketplaceReviewRequest request1 = SubmitMarketplaceReviewRequest.builder()
                .marketplaceProfileId(practiceProfile.getId())
                .rating(5)
                .reviewComment("Review for practice 1 from firm client.")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .header("Authorization", "Bearer " + firmClientToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Client also has completed lead with practiceOrg2
        MarketplaceLeadEntity lead2 = leadRepository.save(MarketplaceLeadEntity.builder()
                .organizationId(practiceOrg2.getId())
                .marketplaceProfileId(practiceProfile2.getId())
                .customerId(customerUser.getId())
                .referenceNumber("TXN-ZENITH-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .clientName("Aarav Mehta")
                .clientEmail(customerUser.getEmail())
                .clientPhone("+91 98765 43210")
                .city("Bengaluru")
                .serviceCategory("ITR Filing")
                .enquiryStatus(EnquiryStatus.COMPLETED)
                .build());

        SubmitMarketplaceReviewRequest request2 = SubmitMarketplaceReviewRequest.builder()
                .marketplaceProfileId(practiceProfile2.getId())
                .leadId(lead2.getId())
                .rating(4)
                .reviewComment("Review for practice 2 with distinct lead is completely allowed.")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        assertEquals(2, reviewRepository.count());
    }
}
