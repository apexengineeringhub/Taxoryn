package com.taxoryn.module.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import com.taxoryn.module.user.dto.AssignUserLocationsRequest;
import com.taxoryn.module.user.dto.CreateUserRequest;
import com.taxoryn.module.user.dto.UpdateUserStatusRequest;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserLocationRepository;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserRoleAndLocationScopeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLocationRepository userLocationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private LocationEntity locMumbaiOrgA;
    private LocationEntity locBangaloreOrgA;
    private LocationEntity locDelhiOrgB;

    private UserEntity adminUserOrgA;
    private UserEntity staffUserOrgA;
    private UserEntity adminUserOrgB;

    private String adminTokenOrgA;
    private String staffTokenOrgA;
    private String adminTokenOrgB;

    private RoleEntity practiceAdminRole;
    private RoleEntity practitionerRole;
    private RoleEntity staffRole;

    @BeforeEach
    void setUp() {
        userLocationRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
        subscriptionRepository.deleteAll();
        organizationRepository.deleteAll();

        // Ensure roles exist
        practiceAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("PRACTICE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("PRACTICE_ADMIN")
                        .name("Practice Administrator")
                        .isSystemRole(true)
                        .build()));

        practitionerRole = roleRepository.findByCodeAndIsSystemRoleTrue("PRACTITIONER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("PRACTITIONER")
                        .name("Tax Practitioner")
                        .isSystemRole(true)
                        .build()));

        staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("STAFF")
                        .name("Junior Staff")
                        .isSystemRole(true)
                        .build()));

        // Create Org A (Business tier)
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA Practice")
                .legalName("Apex CA Practice LLP")
                .email("admin@apexca.com")
                .organizationType(OrganizationType.FIRM)
                .build());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(orgA.getId())
                .plan(SubscriptionPlan.BUSINESS)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .build());

        // Create Org B (Starter tier)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Tax Solutions")
                .legalName("Beta Tax Solutions")
                .email("admin@betatax.com")
                .organizationType(OrganizationType.SOLO)
                .build());

        subscriptionRepository.save(SubscriptionEntity.builder()
                .organizationId(orgB.getId())
                .plan(SubscriptionPlan.STARTER)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .build());

        // Locations for Org A
        LocationEntity locM = LocationEntity.builder()
                .name("Mumbai Head Office")
                .code("MUM-01")
                .city("Mumbai")
                .state("Maharashtra")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        locM.setOrganizationId(orgA.getId());
        locMumbaiOrgA = locationRepository.save(locM);

        LocationEntity locB = LocationEntity.builder()
                .name("Bangalore Branch")
                .code("BLR-01")
                .city("Bangalore")
                .state("Karnataka")
                .isHeadOffice(false)
                .isActive(true)
                .build();
        locB.setOrganizationId(orgA.getId());
        locBangaloreOrgA = locationRepository.save(locB);

        // Location for Org B
        LocationEntity locD = LocationEntity.builder()
                .name("Delhi Office")
                .code("DEL-01")
                .city("New Delhi")
                .state("Delhi")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        locD.setOrganizationId(orgB.getId());
        locDelhiOrgB = locationRepository.save(locD);

        // Admin User for Org A
        adminUserOrgA = UserEntity.builder()
                .email("admin@apexca.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Aditi")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practiceAdminRole)))
                .build();
        adminUserOrgA.setOrganizationId(orgA.getId());
        adminUserOrgA = userRepository.save(adminUserOrgA);

        // Staff User for Org A
        staffUserOrgA = UserEntity.builder()
                .email("staff@apexca.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Ravi")
                .lastName("Kumar")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build();
        staffUserOrgA.setOrganizationId(orgA.getId());
        staffUserOrgA = userRepository.save(staffUserOrgA);

        // Admin User for Org B
        adminUserOrgB = UserEntity.builder()
                .email("admin@betatax.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Vikram")
                .lastName("Singh")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practiceAdminRole)))
                .build();
        adminUserOrgB.setOrganizationId(orgB.getId());
        adminUserOrgB = userRepository.save(adminUserOrgB);

        // JWT Tokens
        adminTokenOrgA = "Bearer " + jwtTokenProvider.generateAccessToken(adminUserOrgA.getId(), orgA.getId(), "admin@apexca.com", Set.of("ROLE_PRACTICE_ADMIN", "PRACTICE_ADMIN"), Set.of("USER_VIEW", "USER_CREATE", "USER_UPDATE", "USER_DELETE", "ORGANIZATION_VIEW", "ORGANIZATION_UPDATE"));
        staffTokenOrgA = "Bearer " + jwtTokenProvider.generateAccessToken(staffUserOrgA.getId(), orgA.getId(), "staff@apexca.com", Set.of("ROLE_STAFF", "STAFF"), Set.of("ORGANIZATION_VIEW", "USER_VIEW"));
        adminTokenOrgB = "Bearer " + jwtTokenProvider.generateAccessToken(adminUserOrgB.getId(), orgB.getId(), "admin@betatax.com", Set.of("ROLE_PRACTICE_ADMIN", "PRACTICE_ADMIN"), Set.of("USER_VIEW", "USER_CREATE", "USER_UPDATE", "USER_DELETE", "ORGANIZATION_VIEW", "ORGANIZATION_UPDATE"));
    }

    @Test
    @DisplayName("Admin can create user with PRACTITIONER role and assign roles")
    void testUserRoleCreationAndAssignment() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("practitioner@apexca.com")
                .password("Password@123")
                .firstName("Pooja")
                .lastName("Verma")
                .roleCodes(Set.of("PRACTITIONER"))
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("practitioner@apexca.com"))
                .andExpect(jsonPath("$.data.roles[0].code").value("PRACTITIONER"));
    }

    @Test
    @DisplayName("Admin assigns location to Staff user; Staff can access assigned location but rejected on other locations")
    void testUserLocationAssignmentAndScopeEnforcement() throws Exception {
        // Assign only Bangalore Branch to Staff user
        AssignUserLocationsRequest assignReq = AssignUserLocationsRequest.builder()
                .locationIds(List.of(locBangaloreOrgA.getId()))
                .build();

        mockMvc.perform(put("/api/v1/users/" + staffUserOrgA.getId() + "/locations")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(locBangaloreOrgA.getId().toString()))
                .andExpect(jsonPath("$.data[0].name").value("Bangalore Branch"));

        // Staff user gets assigned locations list
        mockMvc.perform(get("/api/v1/users/" + staffUserOrgA.getId() + "/locations")
                        .header("Authorization", staffTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(locBangaloreOrgA.getId().toString()));

        // Staff accesses allowed Bangalore location -> 200 OK
        mockMvc.perform(get("/api/v1/locations/" + locBangaloreOrgA.getId())
                        .header("Authorization", staffTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Bangalore Branch"));

        // Staff attempts to access unassigned Mumbai Head Office -> 403 Forbidden
        mockMvc.perform(get("/api/v1/locations/" + locMumbaiOrgA.getId())
                        .header("Authorization", staffTokenOrgA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Reject cross-tenant location assignment: Org A Admin assigning Org B location to Org A user")
    void testCrossTenantLocationAssignmentRejection() throws Exception {
        // Attempt to assign Org B location to Org A user
        AssignUserLocationsRequest invalidReq = AssignUserLocationsRequest.builder()
                .locationIds(List.of(locDelhiOrgB.getId()))
                .build();

        mockMvc.perform(put("/api/v1/users/" + staffUserOrgA.getId() + "/locations")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Practice Admin has organization-wide ALL_LOCATIONS scope without explicit manual branch assignment")
    void testPracticeAdminOrganizationWideLocationScope() throws Exception {
        // Admin gets accessible locations -> sees both Mumbai and Bangalore
        mockMvc.perform(get("/api/v1/users/" + adminUserOrgA.getId() + "/locations")
                        .header("Authorization", adminTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // Admin accesses Mumbai location -> 200 OK
        mockMvc.perform(get("/api/v1/locations/" + locMumbaiOrgA.getId())
                        .header("Authorization", adminTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Mumbai Head Office"));

        // Admin accesses Bangalore location -> 200 OK
        mockMvc.perform(get("/api/v1/locations/" + locBangaloreOrgA.getId())
                        .header("Authorization", adminTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Bangalore Branch"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/context returns aggregated context including location scope and effective modules")
    void testCurrentUserContextEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/context")
                        .header("Authorization", adminTokenOrgA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("admin@apexca.com"))
                .andExpect(jsonPath("$.data.organizationName").value("Apex CA Practice"))
                .andExpect(jsonPath("$.data.locationScope.scopeType").value("ALL_LOCATIONS"))
                .andExpect(jsonPath("$.data.locationScope.isAllLocations").value(true))
                .andExpect(jsonPath("$.data.effectiveConfiguration").exists())
                .andExpect(jsonPath("$.data.effectiveConfiguration.navigationItems").isArray());
    }

    @Test
    @DisplayName("User lifecycle status update and sole active admin deactivation protection")
    void testUserStatusManagementAndSoleAdminProtection() throws Exception {
        // 1. Deactivate staff user -> 200 OK
        UpdateUserStatusRequest deactivateReq = UpdateUserStatusRequest.builder()
                .status(UserStatus.INACTIVE)
                .build();

        mockMvc.perform(put("/api/v1/users/" + staffUserOrgA.getId() + "/status")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        // 2. Attempt to deactivate sole remaining active admin -> 400 Bad Request
        mockMvc.perform(put("/api/v1/users/" + adminUserOrgA.getId() + "/status")
                        .header("Authorization", adminTokenOrgA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivateReq)))
                .andExpect(status().isBadRequest());
    }
}
