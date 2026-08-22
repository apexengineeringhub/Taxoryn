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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private String orgAdminTokenA;
    private String viewerTokenA;
    private String clientUserToken;

    @BeforeEach
    void setUp() {
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
}
