package com.taxoryn.module.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.marketplace.dto.CreatePracticeProfileRequest;
import com.taxoryn.module.marketplace.dto.UpdateMarketplaceProfileRequest;
import com.taxoryn.module.marketplace.dto.UpdateProfileVisibilityRequest;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VisibilityStatus;
import com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarketplaceSecurityIntegrationTest {

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
    private MarketplaceProfileRepository marketplaceProfileRepository;

    @Autowired
    private com.taxoryn.module.marketplace.repository.MarketplaceLeadRepository marketplaceLeadRepository;

    @Autowired
    private com.taxoryn.module.marketplace.repository.MarketplaceConsultationRepository marketplaceConsultationRepository;

    @Autowired
    private com.taxoryn.module.marketplace.repository.MarketplaceProposalRepository marketplaceProposalRepository;

    @Autowired
    private com.taxoryn.module.marketplace.repository.MarketplaceOnboardingRepository marketplaceOnboardingRepository;

    @Autowired
    private com.taxoryn.module.marketplace.repository.MarketplaceVerificationRepository marketplaceVerificationRepository;

    @Autowired
    private com.taxoryn.module.marketplace.repository.MarketplaceServiceRepository marketplaceServiceRepository;

    @Autowired
    private com.taxoryn.module.marketplace.repository.MarketplaceReviewRepository marketplaceReviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private com.taxoryn.core.security.RateLimitingService rateLimitingService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private String orgAdminTokenA;
    private String viewerTokenA;
    private String clientUserToken;

    @BeforeEach
    void setUp() {
        if (rateLimitingService != null) {
            rateLimitingService.reset();
        }
        marketplaceProposalRepository.deleteAll();
        marketplaceOnboardingRepository.deleteAll();
        marketplaceLeadRepository.deleteAll();
        marketplaceConsultationRepository.deleteAll();
        marketplaceReviewRepository.deleteAll();
        marketplaceServiceRepository.deleteAll();
        marketplaceVerificationRepository.deleteAll();
        marketplaceProfileRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // 1. Create Organization A & B
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Practice Alpha Advisors")
                .email("admin@practicealpha.com")
                .city("Mumbai")
                .state("Maharashtra")
                .phone("+919811122233")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Practice Beta Consultants")
                .email("admin@practicebeta.com")
                .city("Delhi")
                .state("Delhi")
                .phone("+919844455566")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Create Permissions
        PermissionEntity mpView = permissionRepository.save(PermissionEntity.builder().code("MARKETPLACE_VIEW").name("View Marketplace").module("MARKETPLACE").build());
        PermissionEntity mpWrite = permissionRepository.save(PermissionEntity.builder().code("MARKETPLACE_WRITE").name("Write Marketplace").module("MARKETPLACE").build());

        // 3. Create Roles
        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Administrator")
                .isSystemRole(true)
                .permissions(new HashSet<>(Set.of(mpView, mpWrite)))
                .build());

        RoleEntity viewerRole = roleRepository.save(RoleEntity.builder()
                .code("VIEWER")
                .name("Read-Only Viewer")
                .isSystemRole(true)
                .permissions(new HashSet<>(Set.of(mpView)))
                .build());

        RoleEntity clientUserRole = roleRepository.save(RoleEntity.builder()
                .code("CLIENT_USER")
                .name("Customer Portal User")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 4. Create Users & Tokens
        UserEntity adminUserA = UserEntity.builder()
                .email("admin@practicealpha.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Alpha")
                .lastName("Admin")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserStatus.ACTIVE)
                .build();
        adminUserA.setOrganizationId(orgA.getId());
        adminUserA = userRepository.save(adminUserA);
        orgAdminTokenA = jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                orgA.getId(),
                adminUserA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE")
        );

        UserEntity viewerUserA = UserEntity.builder()
                .email("viewer@practicealpha.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Alpha")
                .lastName("Viewer")
                .roles(new HashSet<>(Set.of(viewerRole)))
                .status(UserStatus.ACTIVE)
                .build();
        viewerUserA.setOrganizationId(orgA.getId());
        viewerUserA = userRepository.save(viewerUserA);
        viewerTokenA = jwtTokenProvider.generateAccessToken(
                viewerUserA.getId(),
                orgA.getId(),
                viewerUserA.getEmail(),
                Set.of("VIEWER"),
                Set.of("MARKETPLACE_VIEW")
        );

        UserEntity clientCustomer = UserEntity.builder()
                .email("customer@clientportal.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Customer")
                .lastName("User")
                .roles(new HashSet<>(Set.of(clientUserRole)))
                .status(UserStatus.ACTIVE)
                .build();
        clientCustomer.setOrganizationId(orgA.getId());
        clientCustomer = userRepository.save(clientCustomer);
        clientUserToken = jwtTokenProvider.generateAccessToken(
                clientCustomer.getId(),
                orgA.getId(),
                clientCustomer.getEmail(),
                Set.of("CLIENT_USER"),
                Set.of()
        );
    }

    @Test
    @DisplayName("Security: Unauthenticated request to practice profile is rejected with 401 Unauthorized")
    void testUnauthenticatedAccess_Rejected() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/practice-profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/marketplace/practice-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreatePracticeProfileRequest.builder().displayName("Firm").build())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/marketplace/practice-profile/visibility")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateProfileVisibilityRequest.builder().visibility(VisibilityStatus.PUBLIC).build())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: Customer/Client user is forbidden (403) from modifying practice marketplace profile")
    void testCustomerUser_CannotModifyProfile() throws Exception {
        CreatePracticeProfileRequest createRequest = CreatePracticeProfileRequest.builder()
                .displayName("Malicious Customer Injection")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + clientUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        UpdateProfileVisibilityRequest visibilityRequest = UpdateProfileVisibilityRequest.builder()
                .visibility(VisibilityStatus.PUBLIC)
                .build();

        mockMvc.perform(patch("/api/v1/marketplace/practice-profile/visibility")
                        .header("Authorization", "Bearer " + clientUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(visibilityRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Authorized read-only practice user can read profile but cannot modify it")
    void testAuthorizedViewer_CanRead_CannotModify() throws Exception {
        // Read -> 200 OK
        mockMvc.perform(get("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + viewerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Modify -> 403 Forbidden
        UpdateMarketplaceProfileRequest updateRequest = UpdateMarketplaceProfileRequest.builder()
                .displayName("Unauthorized Edit Attempt")
                .build();

        mockMvc.perform(put("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + viewerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Practice Admin can create, update, and change visibility of their marketplace profile")
    void testPracticeAdmin_FullLifecycleAuthorized() throws Exception {
        // 1. Create Profile
        CreatePracticeProfileRequest createRequest = CreatePracticeProfileRequest.builder()
                .displayName("Alpha Tax Advisors")
                .description("Expert Corporate & International Tax Compliance")
                .city("Mumbai")
                .state("Maharashtra")
                .phone("+919811122233")
                .email("admin@practicealpha.com")
                .experienceYears(12)
                .build();

        mockMvc.perform(post("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + orgAdminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Alpha Tax Advisors"));

        // 2. Update Profile
        UpdateMarketplaceProfileRequest updateRequest = UpdateMarketplaceProfileRequest.builder()
                .displayName("Alpha Tax Advisors LLP")
                .phone("+919811122233")
                .email("admin@practicealpha.com")
                .build();

        mockMvc.perform(put("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + orgAdminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Alpha Tax Advisors LLP"));

        // 3. Change Visibility
        UpdateProfileVisibilityRequest visibilityRequest = UpdateProfileVisibilityRequest.builder()
                .visibility(VisibilityStatus.PUBLIC)
                .build();

        mockMvc.perform(patch("/api/v1/marketplace/practice-profile/visibility")
                        .header("Authorization", "Bearer " + orgAdminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(visibilityRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visibilityStatus").value("PUBLIC"));
    }

    @Test
    @DisplayName("Security: Cross-Tenant Isolation - Practice A user strictly operates on Practice A data")
    void testTenantIsolation_PracticeAOperatesStrictlyOnOrgA() throws Exception {
        // Practice A creates profile
        CreatePracticeProfileRequest createRequestA = CreatePracticeProfileRequest.builder()
                .displayName("Alpha Tax Group")
                .city("Mumbai")
                .state("Maharashtra")
                .phone("+919811122233")
                .email("admin@practicealpha.com")
                .build();

        mockMvc.perform(post("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + orgAdminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequestA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.displayName").value("Alpha Tax Group"));

        // When Practice A user gets profile, they receive Practice A's profile
        mockMvc.perform(get("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + orgAdminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Alpha Tax Group"));
    }

    @Test
    @DisplayName("Integration: Validation failures return 400 Bad Request with field errors")
    void testValidationFailures_Return400BadRequest() throws Exception {
        // Blank display name, negative experience, invalid email format
        CreatePracticeProfileRequest invalidRequest = CreatePracticeProfileRequest.builder()
                .displayName("")
                .email("invalid-email-string")
                .phone("bad-phone-###")
                .experienceYears(-5)
                .build();

        mockMvc.perform(post("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + orgAdminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Security: Practice A cannot access or mutate Practice B profile")
    void testPracticeA_CannotAccessOrMutatePracticeB() throws Exception {
        // Create Practice B Profile directly in database
        com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity profileB = marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgB.getId())
                        .displayName("Beta Secret Practice Profile")
                        .slug("beta-secret-practice")
                        .city("Delhi")
                        .state("Delhi")
                        .phone("+919844455566")
                        .email("admin@practicebeta.com")
                        .visibilityStatus(VisibilityStatus.PRIVATE)
                        .isPublished(false)
                        .build()
        );

        // Practice A Admin calls GET /api/v1/marketplace/practice-profile
        // Must return Practice A's profile (or initialized default for Org A), NEVER Practice B's
        mockMvc.perform(get("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + orgAdminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value(orgA.getName()));

        // Practice A Admin calls PUT /api/v1/marketplace/practice-profile
        // Modifying profile only updates Org A, profile B in DB remains completely unchanged
        UpdateMarketplaceProfileRequest updateRequestA = UpdateMarketplaceProfileRequest.builder()
                .displayName("Alpha Overwrite Attempt")
                .build();

        mockMvc.perform(put("/api/v1/marketplace/practice-profile")
                        .header("Authorization", "Bearer " + orgAdminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Alpha Overwrite Attempt"));

        // Verify Practice B profile was NOT touched
        com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity untouchedB = marketplaceProfileRepository.findById(profileB.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Beta Secret Practice Profile", untouchedB.getDisplayName());
        org.junit.jupiter.api.Assertions.assertEquals(orgB.getId(), untouchedB.getOrganizationId());
    }

    // =========================================================================
    // MANDATORY 18 SECURITY SCENARIOS
    // =========================================================================

    @Test
    @DisplayName("Scenario 1: Public user can access PUBLIC profile by ID and Slug")
    void testScenario1_PublicUser_CanAccessPublicProfile() throws Exception {
        com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity publicProfile = marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgA.getId())
                        .displayName("Public Alpha Firm")
                        .slug("public-alpha-firm")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .phone("+919811122233")
                        .email("contact@publicalpha.com")
                        .visibilityStatus(VisibilityStatus.PUBLIC)
                        .isPublished(true)
                        .build()
        );

        // Access by ID -> 200 OK
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + publicProfile.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Public Alpha Firm"))
                .andExpect(jsonPath("$.data.publicSlug").value("public-alpha-firm"));

        // Access by Slug -> 200 OK
        mockMvc.perform(get("/api/v1/marketplace/profiles/slug/public-alpha-firm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Public Alpha Firm"));
    }

    @Test
    @DisplayName("Scenario 2: Public user cannot access PRIVATE profile (returns 404 to prevent enumeration)")
    void testScenario2_PublicUser_CannotAccessPrivateProfile_Returns404() throws Exception {
        com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity privateProfile = marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgA.getId())
                        .displayName("Private Secret Practice")
                        .slug("private-secret-practice")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .phone("+919811122233")
                        .email("contact@secretpractice.com")
                        .visibilityStatus(VisibilityStatus.PRIVATE)
                        .isPublished(false)
                        .build()
        );

        // Access by ID -> 404 NOT FOUND
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + privateProfile.getId()))
                .andExpect(status().isNotFound());

        // Access by Slug -> 404 NOT FOUND
        mockMvc.perform(get("/api/v1/marketplace/profiles/slug/private-secret-practice"))
                .andExpect(status().isNotFound());

        // Access services -> 404 NOT FOUND
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + privateProfile.getId() + "/services"))
                .andExpect(status().isNotFound());

        // Access reviews -> 404 NOT FOUND
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + privateProfile.getId() + "/reviews"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 3: Public user cannot access SUSPENDED profile (returns 404)")
    void testScenario3_PublicUser_CannotAccessSuspendedProfile_Returns404() throws Exception {
        com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity suspendedProfile = marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgB.getId())
                        .displayName("Suspended Practice Firm")
                        .slug("suspended-practice-firm")
                        .city("Delhi")
                        .state("Delhi")
                        .visibilityStatus(VisibilityStatus.SUSPENDED)
                        .isPublished(false)
                        .build()
        );

        // Access by ID -> 404
        mockMvc.perform(get("/api/v1/marketplace/profiles/" + suspendedProfile.getId()))
                .andExpect(status().isNotFound());

        // Access by Slug -> 404
        mockMvc.perform(get("/api/v1/marketplace/profiles/slug/suspended-practice-firm"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 4: Private profile does not appear in search results")
    void testScenario4_PrivateProfile_DoesNotAppearInSearch() throws Exception {
        marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgA.getId())
                        .displayName("Stealth Mode Advisors")
                        .slug("stealth-mode-advisors")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .visibilityStatus(VisibilityStatus.PRIVATE)
                        .isPublished(false)
                        .build()
        );

        mockMvc.perform(get("/api/v1/marketplace/search")
                        .param("search", "Stealth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("Scenario 5: Suspended profile does not appear in search results")
    void testScenario5_SuspendedProfile_DoesNotAppearInSearch() throws Exception {
        marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgB.getId())
                        .displayName("Banned Practice Firm")
                        .slug("banned-practice-firm")
                        .city("Delhi")
                        .state("Delhi")
                        .visibilityStatus(VisibilityStatus.SUSPENDED)
                        .isPublished(false)
                        .build()
        );

        mockMvc.perform(get("/api/v1/marketplace/search")
                        .param("search", "Banned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("Scenario 8: Practice A cannot modify Practice B visibility")
    void testScenario8_PracticeACannotModifyPracticeBVisibility() throws Exception {
        // Create Practice B Profile
        com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity profileB = marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgB.getId())
                        .displayName("Beta Firm")
                        .slug("beta-firm")
                        .city("Delhi")
                        .state("Delhi")
                        .visibilityStatus(VisibilityStatus.PRIVATE)
                        .isPublished(false)
                        .build()
        );

        // Practice A changes visibility -> only updates Practice A
        UpdateProfileVisibilityRequest req = UpdateProfileVisibilityRequest.builder()
                .visibility(VisibilityStatus.PUBLIC)
                .build();

        // Seed valid profile for Org A so validatePublishingEligibility passes
        marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgA.getId())
                        .displayName("Alpha Tax Practice")
                        .slug("alpha-tax-practice")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .phone("+919811122233")
                        .email("admin@practicealpha.com")
                        .visibilityStatus(VisibilityStatus.PRIVATE)
                        .isPublished(false)
                        .build()
        );

        mockMvc.perform(patch("/api/v1/marketplace/practice-profile/visibility")
                        .header("Authorization", "Bearer " + orgAdminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Verify Practice B remains PRIVATE in database
        com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity dbB = marketplaceProfileRepository.findById(profileB.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(VisibilityStatus.PRIVATE, dbB.getVisibilityStatus());
    }

    @Test
    @DisplayName("Scenario 9: Practice A cannot access or mutate Practice B leads")
    void testScenario9_PracticeACannotAccessPracticeBLeads() throws Exception {
        com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity profileB = marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgB.getId())
                        .displayName("Beta Firm")
                        .slug("beta-firm-leads")
                        .city("Delhi")
                        .state("Delhi")
                        .visibilityStatus(VisibilityStatus.PUBLIC)
                        .isPublished(true)
                        .build()
        );

        com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity leadB = marketplaceLeadRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.builder()
                        .organizationId(orgB.getId())
                        .marketplaceProfileId(profileB.getId())
                        .clientName("Confidential Client of Beta")
                        .clientEmail("confidential@betaclient.com")
                        .clientPhone("+919988776655")
                        .leadStatus(com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.LeadStatus.NEW)
                        .build()
        );

        // Practice A lists leads -> Lead B is NOT in response
        mockMvc.perform(get("/api/v1/practice/marketplace/leads")
                        .header("Authorization", "Bearer " + orgAdminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());

        // Practice A attempts to mutate Lead B status -> 404 NOT FOUND
        mockMvc.perform(patch("/api/v1/practice/marketplace/leads/" + leadB.getId() + "/status")
                        .header("Authorization", "Bearer " + orgAdminTokenA)
                        .param("status", "CONTACTED"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 10: Customer cannot access another customer's lead or convert lead")
    void testScenario10_CustomerCannotAccessOrMutateAnotherCustomerLead() throws Exception {
        com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity profileA = marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgA.getId())
                        .displayName("Alpha Practice")
                        .slug("alpha-practice-leads")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .visibilityStatus(VisibilityStatus.PUBLIC)
                        .isPublished(true)
                        .build()
        );

        com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity lead = marketplaceLeadRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.builder()
                        .organizationId(orgA.getId())
                        .marketplaceProfileId(profileA.getId())
                        .clientName("Target Customer")
                        .clientEmail("target@customer.com")
                        .clientPhone("+919876543210")
                        .leadStatus(com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.LeadStatus.NEW)
                        .build()
        );

        // Customer token forbidden from practice CRM lead conversion
        mockMvc.perform(post("/api/v1/practice/marketplace/leads/" + lead.getId() + "/convert-to-client")
                        .header("Authorization", "Bearer " + clientUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 11: Customer / Public user cannot modify professional verification status")
    void testScenario11_CustomerCannotModifyProfessionalVerificationStatus() throws Exception {
        UUID randomVerificationId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/marketplace/verifications/" + randomVerificationId + "/process")
                        .header("Authorization", "Bearer " + clientUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verificationStatus\":\"VERIFIED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 12: Practice cannot self-verify (verification request defaults strictly to PENDING)")
    void testScenario12_PracticeCannotSelfVerify() throws Exception {
        com.taxoryn.module.marketplace.dto.SubmitVerificationRequest req = com.taxoryn.module.marketplace.dto.SubmitVerificationRequest.builder()
                .professionalBody("ICAI")
                .membershipNumber("123456")
                .copNumber("COP-789")
                .build();

        mockMvc.perform(post("/api/v1/practice/marketplace/verification")
                        .header("Authorization", "Bearer " + orgAdminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"));
    }

    @Test
    @DisplayName("Scenario 13 & 14: Public API does not expose sensitive customer or tenant-private information")
    void testScenario13And14_PublicApiDoesNotExposeSensitiveData() throws Exception {
        com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity profile = marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgA.getId())
                        .displayName("Secure Tax Practice")
                        .slug("secure-tax-practice")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .phone("+919811122233")
                        .email("contact@securetax.com")
                        .visibilityStatus(VisibilityStatus.PUBLIC)
                        .isPublished(true)
                        .build()
        );

        mockMvc.perform(get("/api/v1/marketplace/profiles/" + profile.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Secure Tax Practice"))
                // Ensure internal sensitive fields are NOT exposed
                .andExpect(jsonPath("$.data.organizationId").doesNotExist())
                .andExpect(jsonPath("$.data.pan").doesNotExist())
                .andExpect(jsonPath("$.data.gstin").doesNotExist())
                .andExpect(jsonPath("$.data.internalNotes").doesNotExist())
                .andExpect(jsonPath("$.data.subscriptionPlan").doesNotExist());
    }

    @Test
    @DisplayName("Scenario 15: Invalid proposal token is rejected with 404")
    void testScenario15_InvalidProposalTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/onboarding/proposal/invalid_non_existent_token"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 16: Expired proposal token is rejected with 400 Bad Request")
    void testScenario16_ExpiredProposalTokenIsRejected() throws Exception {
        com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity profileA = marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgA.getId())
                        .displayName("Alpha Practice")
                        .slug("alpha-practice-prop")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .visibilityStatus(VisibilityStatus.PUBLIC)
                        .isPublished(true)
                        .build()
        );

        com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity lead = marketplaceLeadRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.builder()
                        .organizationId(orgA.getId())
                        .marketplaceProfileId(profileA.getId())
                        .clientName("Expired Lead")
                        .clientEmail("expired@lead.com")
                        .clientPhone("+919811122233")
                        .leadStatus(com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.LeadStatus.PROPOSAL_SENT)
                        .build()
        );

        com.taxoryn.module.marketplace.entity.MarketplaceProposalEntity expiredProp = marketplaceProposalRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProposalEntity.builder()
                        .organizationId(orgA.getId())
                        .marketplaceProfileId(profileA.getId())
                        .leadId(lead.getId())
                        .proposalTitle("Outdated Proposal")
                        .scopeOfWork("Comprehensive Corporate Tax Filing")
                        .deliverables("Form 3CD, ITR-6 Filing")
                        .accessToken("prop_expired_123")
                        .proposalStatus(com.taxoryn.module.marketplace.entity.MarketplaceProposalEntity.ProposalStatus.SENT)
                        .validUntil(java.time.LocalDate.now().minusDays(5))
                        .build()
        );

        mockMvc.perform(get("/api/v1/marketplace/onboarding/proposal/prop_expired_123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expired")));
    }

    @Test
    @DisplayName("Scenario 17: Customer / Unauthorized user cannot mutate practice consultation status")
    void testScenario17_CustomerCannotMutatePracticeConsultationStatus() throws Exception {
        UUID randConsultationId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/practice/marketplace/consultations/" + randConsultationId + "/status")
                        .header("Authorization", "Bearer " + clientUserToken)
                        .param("status", "COMPLETED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 18: Practice A cannot access or mutate Practice B's consultations")
    void testScenario18_PracticeACannotAccessOrMutatePracticeBConsultation() throws Exception {
        com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity profileB = marketplaceProfileRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.builder()
                        .organizationId(orgB.getId())
                        .displayName("Beta Firm")
                        .slug("beta-firm-consult")
                        .city("Delhi")
                        .state("Delhi")
                        .visibilityStatus(VisibilityStatus.PUBLIC)
                        .isPublished(true)
                        .build()
        );

        com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity consultationB = marketplaceConsultationRepository.save(
                com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity.builder()
                        .organizationId(orgB.getId())
                        .marketplaceProfileId(profileB.getId())
                        .clientName("Beta Consultation Client")
                        .clientEmail("client@beta.com")
                        .clientPhone("+919844455566")
                        .topic("Corporate Restructuring")
                        .bookingDate(java.time.LocalDate.now())
                        .startTime("10:00 AM")
                        .endTime("10:30 AM")
                        .feeAmount(new java.math.BigDecimal("999.00"))
                        .consultationStatus(com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity.ConsultationStatus.SCHEDULED)
                        .build()
        );

        // Practice A Admin calls PATCH on Practice B's consultation -> 404 NOT FOUND
        mockMvc.perform(patch("/api/v1/practice/marketplace/consultations/" + consultationB.getId() + "/status")
                        .header("Authorization", "Bearer " + orgAdminTokenA)
                        .param("status", "COMPLETED"))
                .andExpect(status().isNotFound());
    }
}
