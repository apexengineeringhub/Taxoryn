package com.taxoryn.module.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.marketplace.dto.CreatePracticeLocationRequest;
import com.taxoryn.module.marketplace.dto.UpdatePracticeLocationRequest;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VisibilityStatus;
import com.taxoryn.module.marketplace.entity.PracticeLocationEntity;
import com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository;
import com.taxoryn.module.marketplace.repository.PracticeLocationRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PracticeLocationSecurityIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private MarketplaceProfileEntity profileA;
    private MarketplaceProfileEntity profileB;
    private String tokenOrgA;
    private String tokenOrgB;

    @BeforeEach
    void setUp() {
        locationRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // 1. Create Organization A & B
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Loc Practice Alpha")
                .legalName("Loc Practice Alpha LLP")
                .email("admin@localpha.com")
                .city("Bengaluru")
                .state("Karnataka")
                .phone("+919811122233")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Loc Practice Beta")
                .legalName("Loc Practice Beta LLP")
                .email("admin@locbeta.com")
                .city("Mumbai")
                .state("Maharashtra")
                .phone("+919844455566")
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
                .name("Organization Administrator")
                .isSystemRole(true)
                .permissions(new HashSet<>(Set.of(mpView, mpWrite)))
                .build());

        // 4. Create Users & Tokens
        UserEntity adminUserA = UserEntity.builder()
                .email("admin@localpha.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Alpha")
                .lastName("Admin")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserStatus.ACTIVE)
                .build();
        adminUserA.setOrganizationId(orgA.getId());
        adminUserA = userRepository.save(adminUserA);

        tokenOrgA = jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                orgA.getId(),
                adminUserA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE")
        );

        UserEntity adminUserB = UserEntity.builder()
                .email("admin@locbeta.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Beta")
                .lastName("Admin")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserStatus.ACTIVE)
                .build();
        adminUserB.setOrganizationId(orgB.getId());
        adminUserB = userRepository.save(adminUserB);

        tokenOrgB = jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                orgB.getId(),
                adminUserB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE")
        );

        // 5. Practice Profiles
        profileA = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(orgA.getId())
                .displayName("Loc Alpha Advisors")
                .slug("loc-alpha-advisors-" + UUID.randomUUID().toString().substring(0, 6))
                .bio("Practice Alpha multi-location tax advisory firm")
                .email("contact@localpha.com")
                .phone("+919876543210")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .isPublished(true)
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .build());

        profileB = profileRepository.save(MarketplaceProfileEntity.builder()
                .organizationId(orgB.getId())
                .displayName("Loc Beta Advisors")
                .slug("loc-beta-advisors-" + UUID.randomUUID().toString().substring(0, 6))
                .bio("Practice Beta multi-location tax advisory firm")
                .email("contact@locbeta.com")
                .phone("+919876543211")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .isPublished(true)
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .build());
    }

    @Test
    @DisplayName("Practice Admin can create a new branch location, query it, and update it")
    void testCreateAndManagePracticeLocation_Success() throws Exception {
        CreatePracticeLocationRequest createReq = CreatePracticeLocationRequest.builder()
                .locationName("Indiranagar Branch")
                .addressLine1("100 Feet Road, Indiranagar")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560038")
                .latitude(new BigDecimal("12.978400"))
                .longitude(new BigDecimal("77.640800"))
                .isPrimary(false)
                .build();

        // 1. Create location
        String respJson = mockMvc.perform(post("/api/v1/marketplace/practice-profile/locations")
                        .header("Authorization", "Bearer " + tokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.locationName").value("Indiranagar Branch"))
                .andExpect(jsonPath("$.data.city").value("Bengaluru"))
                .andExpect(jsonPath("$.data.pincode").value("560038"))
                .andReturn().getResponse().getContentAsString();

        String locId = objectMapper.readTree(respJson).path("data").path("id").asText();

        // 2. List locations
        mockMvc.perform(get("/api/v1/marketplace/practice-profile/locations")
                        .header("Authorization", "Bearer " + tokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[*].locationName", hasItem("Indiranagar Branch")));

        // 3. Update location
        UpdatePracticeLocationRequest updateReq = UpdatePracticeLocationRequest.builder()
                .locationName("Indiranagar Prime Branch")
                .addressLine1("100 Feet Road, Indiranagar")
                .addressLine2("2nd Floor")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560038")
                .build();

        mockMvc.perform(put("/api/v1/marketplace/practice-profile/locations/" + locId)
                        .header("Authorization", "Bearer " + tokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationName").value("Indiranagar Prime Branch"));
    }

    @Test
    @DisplayName("Primary location switching: marking new location as primary clears other primary locations")
    void testPrimaryLocationSwitching_Success() throws Exception {
        // Create Location 1 (Primary)
        CreatePracticeLocationRequest req1 = CreatePracticeLocationRequest.builder()
                .locationName("Head Office A")
                .addressLine1("MG Road Suite 101")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .isPrimary(true)
                .build();

        String res1 = mockMvc.perform(post("/api/v1/marketplace/practice-profile/locations")
                        .header("Authorization", "Bearer " + tokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id1 = objectMapper.readTree(res1).path("data").path("id").asText();

        // Create Location 2 (Non-Primary)
        CreatePracticeLocationRequest req2 = CreatePracticeLocationRequest.builder()
                .locationName("Koramangala Branch")
                .addressLine1("80 Feet Road")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560034")
                .isPrimary(false)
                .build();

        String res2 = mockMvc.perform(post("/api/v1/marketplace/practice-profile/locations")
                        .header("Authorization", "Bearer " + tokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id2 = objectMapper.readTree(res2).path("data").path("id").asText();

        // Set Location 2 as Primary
        mockMvc.perform(patch("/api/v1/marketplace/practice-profile/locations/" + id2 + "/primary")
                        .header("Authorization", "Bearer " + tokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPrimary").value(true));

        // Verify Location 1 is now non-primary
        mockMvc.perform(get("/api/v1/marketplace/practice-profile/locations/" + id1)
                        .header("Authorization", "Bearer " + tokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPrimary").value(false));
    }

    @Test
    @DisplayName("Security & Isolation: Practice B cannot read, update, or delete Practice A locations")
    void testCrossTenantLocationIsolation_ForbiddenOrNotFound() throws Exception {
        // Create location under Practice A
        PracticeLocationEntity locA = locationRepository.save(PracticeLocationEntity.builder()
                .organizationId(orgA.getId())
                .marketplaceProfileId(profileA.getId())
                .locationName("Secret Org A Branch")
                .addressLine1("Private Lane 9")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .isActive(true)
                .isPrimary(false)
                .build());

        // Practice B attempts to GET Practice A's location -> 404 NOT_FOUND
        mockMvc.perform(get("/api/v1/marketplace/practice-profile/locations/" + locA.getId())
                        .header("Authorization", "Bearer " + tokenOrgB))
                .andExpect(status().isNotFound());

        // Practice B attempts to PUT Practice A's location -> 404 NOT_FOUND
        UpdatePracticeLocationRequest updateReq = UpdatePracticeLocationRequest.builder()
                .locationName("Hijacked Branch")
                .addressLine1("Hacked Road")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .build();

        mockMvc.perform(put("/api/v1/marketplace/practice-profile/locations/" + locA.getId())
                        .header("Authorization", "Bearer " + tokenOrgB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Practice B attempts to DELETE Practice A's location -> 404 NOT_FOUND
        mockMvc.perform(delete("/api/v1/marketplace/practice-profile/locations/" + locA.getId())
                        .header("Authorization", "Bearer " + tokenOrgB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Public profile discovery exposes only active locations and omits inactive branches")
    void testPublicProfileLocationsExposure_OnlyActive() throws Exception {
        // Create Active Location for Profile A
        locationRepository.save(PracticeLocationEntity.builder()
                .organizationId(orgA.getId())
                .marketplaceProfileId(profileA.getId())
                .locationName("Public Active Branch")
                .addressLine1("MG Road 50")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .isActive(true)
                .isPrimary(true)
                .build());

        // Create Inactive Location for Profile A
        locationRepository.save(PracticeLocationEntity.builder()
                .organizationId(orgA.getId())
                .marketplaceProfileId(profileA.getId())
                .locationName("Hidden Inactive Branch")
                .addressLine1("Closed Lane 1")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .isActive(false)
                .isPrimary(false)
                .build());

        // Public GET Profile by slug
        mockMvc.perform(get("/api/v1/marketplace/profiles/slug/" + profileA.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locations[*].locationName", hasItem("Public Active Branch")))
                .andExpect(jsonPath("$.data.locations[*].locationName", not(hasItem("Hidden Inactive Branch"))))
                .andExpect(jsonPath("$.data.primaryLocation.locationName").value("Public Active Branch"));
    }
}
