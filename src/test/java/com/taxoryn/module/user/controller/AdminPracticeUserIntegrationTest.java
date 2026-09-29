package com.taxoryn.module.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.entity.OrganizationEntity.SubscriptionPlan;
import com.taxoryn.module.organization.entity.OrganizationType;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminPracticeUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity practiceA;
    private OrganizationEntity practiceB;
    private OrganizationEntity platformOrg;

    private UserEntity superAdminUser;
    private UserEntity practiceAAdmin1;
    private UserEntity practiceAAdmin2;
    private UserEntity practiceAStaff1;
    private UserEntity practiceAStaff2;
    private UserEntity practiceBAdmin1;
    private UserEntity practiceBStaff1;
    private UserEntity standardClientUser;

    private String superAdminToken;
    private String clientToken;

    private RoleEntity superAdminRole;
    private RoleEntity practiceAdminRole;
    private RoleEntity practiceOwnerRole;
    private RoleEntity staffRole;
    private RoleEntity clientRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Resolve or Create System Roles
        superAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("TAXORYN_SUPERADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("TAXORYN_SUPERADMIN")
                        .name("Taxoryn SuperAdmin")
                        .isSystemRole(true)
                        .build()));

        practiceAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("PRACTICE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("PRACTICE_ADMIN")
                        .name("Practice Administrator")
                        .isSystemRole(true)
                        .build()));

        practiceOwnerRole = roleRepository.findByCodeAndIsSystemRoleTrue("PRACTICE_OWNER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("PRACTICE_OWNER")
                        .name("Practice Owner")
                        .isSystemRole(true)
                        .build()));

        staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("STAFF")
                        .name("Practice Staff")
                        .isSystemRole(true)
                        .build()));

        clientRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("CLIENT_USER")
                        .name("Client User")
                        .isSystemRole(true)
                        .build()));

        // 2. Create Organizations
        platformOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Taxoryn Platform Operations")
                .email("admin@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .organizationType(OrganizationType.ENTERPRISE)
                .subscriptionPlan(SubscriptionPlan.ENTERPRISE)
                .build());

        practiceA = organizationRepository.save(OrganizationEntity.builder()
                .name("IshnAI InfoTech Practice")
                .legalName("IshnAI InfoTech LLP")
                .email("contact@ishnai.com")
                .city("Varanasi")
                .state("Uttar Pradesh")
                .status(OrganizationStatus.ACTIVE)
                .organizationType(OrganizationType.FIRM)
                .subscriptionPlan(SubscriptionPlan.BUSINESS)
                .build());

        practiceB = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Consultants")
                .legalName("Apex Tax Solutions Pvt Ltd")
                .email("info@apextax.com")
                .city("Mumbai")
                .state("Maharashtra")
                .status(OrganizationStatus.INACTIVE)
                .organizationType(OrganizationType.SOLO_PRACTITIONER)
                .subscriptionPlan(SubscriptionPlan.PROFESSIONAL)
                .build());

        // 3. Create SuperAdmin
        superAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(platformOrg.getId())
                .firstName("Platform")
                .lastName("SuperAdmin")
                .email("superadmin@taxoryn.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(superAdminRole)))
                .build());

        // 4. Create Practice A Users (2 Admins, 2 Staff: 3 Active, 1 Inactive)
        practiceAAdmin1 = userRepository.save(UserEntity.builder()
                .organizationId(practiceA.getId())
                .firstName("Anjani")
                .lastName("Pathak")
                .email("anjani@ishnai.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practiceAdminRole)))
                .build());

        practiceAAdmin2 = userRepository.save(UserEntity.builder()
                .organizationId(practiceA.getId())
                .firstName("Ravi")
                .lastName("Kumar")
                .email("ravi@ishnai.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practiceOwnerRole)))
                .build());

        practiceAStaff1 = userRepository.save(UserEntity.builder()
                .organizationId(practiceA.getId())
                .firstName("Rahul")
                .lastName("Sharma")
                .email("rahul@ishnai.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        practiceAStaff2 = userRepository.save(UserEntity.builder()
                .organizationId(practiceA.getId())
                .firstName("Neha")
                .lastName("Gupta")
                .email("neha@ishnai.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.INACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        // 5. Create Practice B Users (1 Admin, 1 Staff: 2 Active)
        practiceBAdmin1 = userRepository.save(UserEntity.builder()
                .organizationId(practiceB.getId())
                .firstName("Priya")
                .lastName("Singh")
                .email("priya@apextax.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(practiceAdminRole)))
                .build());

        practiceBStaff1 = userRepository.save(UserEntity.builder()
                .organizationId(practiceB.getId())
                .firstName("Amit")
                .lastName("Verma")
                .email("amit@apextax.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        // 6. Create Non-Admin Client User
        standardClientUser = userRepository.save(UserEntity.builder()
                .organizationId(practiceA.getId())
                .firstName("Client")
                .lastName("Consumer")
                .email("client@customer.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientRole)))
                .build());

        // 7. Generate Tokens
        superAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                superAdminUser.getId(), platformOrg.getId(), superAdminUser.getEmail(),
                Set.of("ROLE_TAXORYN_SUPERADMIN", "TAXORYN_SUPERADMIN"),
                Set.of("PLATFORM_USER_VIEW", "PLATFORM_USER_CREATE", "PLATFORM_USER_UPDATE")
        );

        clientToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                standardClientUser.getId(), practiceA.getId(), standardClientUser.getEmail(),
                Set.of("ROLE_CLIENT_USER", "CLIENT_USER"),
                Set.of("PORTAL_VIEW")
        );
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("SuperAdmin can retrieve practice summaries with aggregated counts and multiple admins")
    void testGetPracticeSummaries() throws Exception {
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("page", "0")
                        .param("size", "25")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(3))) // platformOrg, practiceA, practiceB
                .andExpect(jsonPath("$.data.totalElements").value(3));

        // Find Practice A summary
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("search", "IshnAI")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].organizationName").value("IshnAI InfoTech Practice"))
                .andExpect(jsonPath("$.data.content[0].totalUserCount").value(5)) // 2 admins + 2 staff + 1 client
                .andExpect(jsonPath("$.data.content[0].activeUserCount").value(4)) // 4 active, 1 inactive
                .andExpect(jsonPath("$.data.content[0].adminCount").value(2)) // Anjani & Ravi
                .andExpect(jsonPath("$.data.content[0].admins", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].admins[*].email", containsInAnyOrder("anjani@ishnai.com", "ravi@ishnai.com")));
    }

    @Test
    @DisplayName("Search handles null, empty, whitespace, and special characters cleanly")
    void testSearchWithEdgeCaseInputs() throws Exception {
        // 1. Empty string search returns all
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("search", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(3)));

        // 2. Whitespace-only search returns all
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("search", "   ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(3)));

        // 3. One character search
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("search", "I")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())));

        // 4. No result search
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("search", "NonExistentOrgNameXYZ123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("Search matches case-insensitively across practice name, city, email, phone, and legal name")
    void testSearchMatchesAcrossPracticeFields() throws Exception {
        // 1. Practice Name (mixed case)
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("search", "iSHNAI")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].organizationName").value("IshnAI InfoTech Practice"));

        // 2. City
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("search", "varanasi")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].city").value("Varanasi"));

        // 3. Email
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("search", "info@apextax.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].organizationName").value("Apex Tax Consultants"));

        // 4. Legal Name
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("search", "Solutions Pvt Ltd")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].legalName").value("Apex Tax Solutions Pvt Ltd"));
    }

    @Test
    @DisplayName("SuperAdmin can search practices by user email or user name")
    void testSearchPracticesByUserEmailOrName() throws Exception {
        // Search by user name 'Rahul' inside Practice A
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("search", "Rahul")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].organizationName").value("IshnAI InfoTech Practice"));

        // Search by user email 'priya@apextax.com' inside Practice B
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("search", "priya@apextax.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].organizationName").value("Apex Tax Consultants"));

        // Search by user last name 'Kumar'
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("search", "kumar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].organizationName").value("IshnAI InfoTech Practice"));
    }

    @Test
    @DisplayName("SuperAdmin can filter practices by status and combine with search")
    void testFilterPracticesByStatus() throws Exception {
        // Status filter alone
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("status", "INACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].organizationName").value("Apex Tax Consultants"));

        // Status filter combined with matching search
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("status", "ACTIVE")
                        .param("search", "IshnAI")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].organizationName").value("IshnAI InfoTech Practice"));

        // Status filter combined with non-matching status
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", superAdminToken)
                        .param("status", "ACTIVE")
                        .param("search", "Apex")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("SuperAdmin can lazy load users for a specific practice with pagination and filters")
    void testGetPracticeUsersLazyLoad() throws Exception {
        mockMvc.perform(get("/api/v1/admin/practices/{organizationId}/users", practiceA.getId())
                        .header("Authorization", superAdminToken)
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3));

        // Filter practice users by status INACTIVE
        mockMvc.perform(get("/api/v1/admin/practices/{organizationId}/users", practiceA.getId())
                        .header("Authorization", superAdminToken)
                        .param("status", "INACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].email").value("neha@ishnai.com"));

        // Filter practice users by role STAFF
        mockMvc.perform(get("/api/v1/admin/practices/{organizationId}/users", practiceA.getId())
                        .header("Authorization", superAdminToken)
                        .param("role", "STAFF")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[*].email", containsInAnyOrder("rahul@ishnai.com", "neha@ishnai.com")));

        // Filter practice users by search
        mockMvc.perform(get("/api/v1/admin/practices/{organizationId}/users", practiceA.getId())
                        .header("Authorization", superAdminToken)
                        .param("search", "Anjani")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].email").value("anjani@ishnai.com"));
    }

    @Test
    @DisplayName("Non-SuperAdmin cannot access practice summaries or practice users")
    void testUnauthorizedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/practices")
                        .header("Authorization", clientToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/practices/{organizationId}/users", practiceA.getId())
                        .header("Authorization", clientToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Requesting users for a non-existent organization returns 404")
    void testNonExistentOrganizationReturns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/admin/practices/{organizationId}/users", nonExistentId)
                        .header("Authorization", superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
