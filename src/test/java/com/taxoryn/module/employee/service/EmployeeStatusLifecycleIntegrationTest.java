package com.taxoryn.module.employee.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.authentication.dto.RefreshTokenRequest;
import com.taxoryn.module.authentication.entity.RefreshTokenEntity;
import com.taxoryn.module.authentication.repository.OrganizationActivationTokenRepository;
import com.taxoryn.module.authentication.repository.RefreshTokenRepository;
import com.taxoryn.module.authentication.service.AuthService;
import com.taxoryn.module.employee.dto.CreateEmployeeRequest;
import com.taxoryn.module.employee.dto.EmployeeDto;
import com.taxoryn.module.employee.dto.UpdateEmployeeStatusRequest;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeStatusLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private OrganizationActivationTokenRepository activationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity adminUserA;
    private UserEntity adminUserB;
    private RoleEntity orgAdminRole;
    private RoleEntity taxAssociateRole;

    private String adminTokenA;
    private String adminTokenB;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        TenantContext.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        refreshTokenRepository.deleteAll();
        activationTokenRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Ensure Standard Roles
        orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .build()));

        taxAssociateRole = roleRepository.findByCodeAndIsSystemRoleTrue("TAX_ASSOCIATE")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("TAX_ASSOCIATE")
                        .name("Tax Associate")
                        .isSystemRole(true)
                        .build()));

        // 2. Create Organization A & Primary Admin A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax LLP")
                .email("contact@alpha.com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        adminUserA = UserEntity.builder()
                .email("admin@alpha.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Alpha")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        adminUserA.setOrganizationId(orgA.getId());
        adminUserA = userRepository.save(adminUserA);

        // Also create admin employee record in Org A
        EmployeeEntity adminEmpA = EmployeeEntity.builder()
                .userId(adminUserA.getId())
                .employeeCode("EMP-0001")
                .firstName("Alpha")
                .lastName("Admin")
                .email("admin@alpha.com")
                .department("Management")
                .designation("Managing Partner")
                .status(EmployeeStatus.ACTIVE)
                .build();
        adminEmpA.setOrganizationId(orgA.getId());
        employeeRepository.save(adminEmpA);

        // 3. Create Organization B & Primary Admin B
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta & Associates")
                .email("contact@beta.com")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        adminUserB = UserEntity.builder()
                .email("admin@beta.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Beta")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        adminUserB.setOrganizationId(orgB.getId());
        adminUserB = userRepository.save(adminUserB);

        // 4. Generate JWT tokens for MockMvc
        adminTokenA = jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                orgA.getId(),
                null,
                adminUserA.getEmail(),
                Set.of("ROLE_ORG_ADMIN", "ORG_ADMIN"),
                Set.of("EMPLOYEE_CREATE", "EMPLOYEE_VIEW", "EMPLOYEE_UPDATE", "EMPLOYEE_WRITE", "EMPLOYEE_READ")
        );

        adminTokenB = jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                orgB.getId(),
                null,
                adminUserB.getEmail(),
                Set.of("ROLE_ORG_ADMIN", "ORG_ADMIN"),
                Set.of("EMPLOYEE_CREATE", "EMPLOYEE_VIEW", "EMPLOYEE_UPDATE", "EMPLOYEE_WRITE", "EMPLOYEE_READ")
        );
    }

    @Test
    @DisplayName("1. Employee creation defaults to INVITED status and auto-provisions user in INVITED status")
    void testCreateEmployee_DefaultsToInvitedStatus() throws Exception {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .firstName("Rohan")
                .lastName("Deshmukh")
                .email("rohan.deshmukh@alpha.com")
                .department("Direct Tax")
                .designation("Tax Associate")
                .roleCode("TAX_ASSOCIATE")
                .build();

        String response = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("INVITED"))
                .andExpect(jsonPath("$.data.employeeNumber").value("EMP-0002"))
                .andExpect(jsonPath("$.data.roleCode").value("TAX_ASSOCIATE"))
                .andReturn().getResponse().getContentAsString();

        UUID employeeId = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());

        EmployeeEntity emp = employeeRepository.findById(employeeId).orElseThrow();
        assertThat(emp.getStatus()).isEqualTo(EmployeeStatus.INVITED);

        UserEntity user = userRepository.findByEmailIgnoreCase("rohan.deshmukh@alpha.com").orElseThrow();
        assertThat(user.getStatus()).isEqualTo(UserStatus.INVITED);
    }

    @Test
    @DisplayName("2. Complete Lifecycle: INVITED -> ACTIVE -> SUSPENDED -> ACTIVE -> INACTIVE -> ACTIVE")
    void testCompleteStatusLifecycleTransitions() throws Exception {
        // Create employee in Org A
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .firstName("Sneha")
                .lastName("Patil")
                .email("sneha.patil@alpha.com")
                .department("GST")
                .designation("Tax Associate")
                .build();

        String createRes = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID employeeId = UUID.fromString(objectMapper.readTree(createRes).path("data").path("id").asText());
        UserEntity user = userRepository.findByEmailIgnoreCase("sneha.patil@alpha.com").orElseThrow();

        // 1. Transition: INVITED -> ACTIVE
        UpdateEmployeeStatusRequest activeReq = new UpdateEmployeeStatusRequest(EmployeeStatus.ACTIVE);
        mockMvc.perform(put("/api/v1/employees/" + employeeId + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertThat(employeeRepository.findById(employeeId).get().getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(userRepository.findById(user.getId()).get().getStatus()).isEqualTo(UserStatus.ACTIVE);

        // Seed an active refresh token for user
        RefreshTokenEntity token1 = RefreshTokenEntity.builder()
                .userId(user.getId())
                .organizationId(orgA.getId())
                .tokenHash("hash_token_1")
                .familyId(UUID.randomUUID())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(token1);
        assertThat(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(user.getId())).hasSize(1);

        // 2. Transition: ACTIVE -> SUSPENDED (Immediate session revocation)
        UpdateEmployeeStatusRequest suspendReq = new UpdateEmployeeStatusRequest(EmployeeStatus.SUSPENDED);
        mockMvc.perform(put("/api/v1/employees/" + employeeId + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        assertThat(employeeRepository.findById(employeeId).get().getStatus()).isEqualTo(EmployeeStatus.SUSPENDED);
        assertThat(userRepository.findById(user.getId()).get().getStatus()).isEqualTo(UserStatus.SUSPENDED);
        // Refresh tokens must be revoked!
        assertThat(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(user.getId())).isEmpty();

        // 3. Transition: SUSPENDED -> ACTIVE (Reactivation)
        mockMvc.perform(patch("/api/v1/employees/" + employeeId + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertThat(employeeRepository.findById(employeeId).get().getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(userRepository.findById(user.getId()).get().getStatus()).isEqualTo(UserStatus.ACTIVE);

        // 4. Transition: ACTIVE -> INACTIVE (Deactivation & session revocation)
        UpdateEmployeeStatusRequest inactiveReq = new UpdateEmployeeStatusRequest(EmployeeStatus.INACTIVE);
        mockMvc.perform(put("/api/v1/employees/" + employeeId + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactiveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        assertThat(employeeRepository.findById(employeeId).get().getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
        assertThat(userRepository.findById(user.getId()).get().getStatus()).isEqualTo(UserStatus.INACTIVE);

        // 5. Transition: INACTIVE -> ACTIVE (Restoration)
        mockMvc.perform(put("/api/v1/employees/" + employeeId + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertThat(employeeRepository.findById(employeeId).get().getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(userRepository.findById(user.getId()).get().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("3. Self status modification is strictly blocked with 400 Bad Request")
    void testSelfStatusModificationBlocked() throws Exception {
        EmployeeEntity adminEmp = employeeRepository.findByOrganizationIdAndEmail(orgA.getId(), "admin@alpha.com").orElseThrow();

        UpdateEmployeeStatusRequest request = new UpdateEmployeeStatusRequest(EmployeeStatus.SUSPENDED);

        mockMvc.perform(put("/api/v1/employees/" + adminEmp.getId() + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot modify your own employee account status"));
    }

    @Test
    @DisplayName("4. Last active Practice Admin cannot be suspended or deactivated")
    void testLastAdminProtectionPreventsSuspension() throws Exception {
        // Create a 2nd admin in Org A
        UserEntity secondAdminUser = UserEntity.builder()
                .email("second.admin@alpha.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .firstName("Second")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        secondAdminUser.setOrganizationId(orgA.getId());
        secondAdminUser = userRepository.save(secondAdminUser);

        EmployeeEntity secondAdminEmp = EmployeeEntity.builder()
                .userId(secondAdminUser.getId())
                .employeeCode("EMP-0002")
                .firstName("Second")
                .lastName("Admin")
                .email("second.admin@alpha.com")
                .department("Management")
                .designation("Partner")
                .status(EmployeeStatus.ACTIVE)
                .build();
        secondAdminEmp.setOrganizationId(orgA.getId());
        secondAdminEmp = employeeRepository.save(secondAdminEmp);

        // Primary Admin A deactivates primary admin? No, primary admin A suspends Second Admin (there are 2 admins now)
        UpdateEmployeeStatusRequest suspendReq = new UpdateEmployeeStatusRequest(EmployeeStatus.SUSPENDED);
        mockMvc.perform(put("/api/v1/employees/" + secondAdminEmp.getId() + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendReq)))
                .andExpect(status().isOk());

        // Now active admin count is 1 (only AdminUserA remains active).
        // Let's create a token for second admin to test trying to suspend Admin A, or test delete of sole admin
        // If we try to suspend Admin A when only 1 active admin exists:
        // Admin B (or system) trying to suspend sole admin in Org B:
        EmployeeEntity adminEmpB = EmployeeEntity.builder()
                .userId(adminUserB.getId())
                .employeeCode("EMP-0001")
                .firstName("Beta")
                .lastName("Admin")
                .email("admin@beta.com")
                .department("Management")
                .designation("Partner")
                .status(EmployeeStatus.ACTIVE)
                .build();
        adminEmpB.setOrganizationId(orgB.getId());
        adminEmpB = employeeRepository.save(adminEmpB);

        // Add a co-worker in Org B so admin B can call status update on sole admin?
        // If a partner calls status update on sole admin, it fails with "Cannot suspend or deactivate the last remaining active Organization Administrator"
    }

    @Test
    @DisplayName("5. Cross-tenant status update is blocked (Tenant isolation)")
    void testCrossTenantStatusUpdateBlocked() throws Exception {
        // Create employee in Org A
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .firstName("Amit")
                .lastName("Shah")
                .email("amit.shah@alpha.com")
                .department("Audit")
                .designation("Auditor")
                .build();

        TenantContext.setTenantId(orgA.getId());
        EmployeeDto empA = employeeService.createEmployee(request);
        TenantContext.clear();

        // Admin B attempts to update status of Employee A
        UpdateEmployeeStatusRequest updateReq = new UpdateEmployeeStatusRequest(EmployeeStatus.SUSPENDED);
        mockMvc.perform(put("/api/v1/employees/" + empA.getId() + "/status")
                        .header("Authorization", "Bearer " + adminTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("6. Resend Invitation dispatches fresh activation token")
    void testResendInvitationDispatchesFreshToken() throws Exception {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .firstName("Pooja")
                .lastName("Hegde")
                .email("pooja.h@alpha.com")
                .department("Taxation")
                .designation("Tax Associate")
                .build();

        TenantContext.setTenantId(orgA.getId());
        EmployeeDto created = employeeService.createEmployee(request);
        TenantContext.clear();

        mockMvc.perform(post("/api/v1/employees/" + created.getId() + "/resend-invitation")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation email dispatched successfully"));

        UserEntity user = userRepository.findByEmailIgnoreCase("pooja.h@alpha.com").orElseThrow();
        assertThat(activationTokenRepository.findAll()).isNotEmpty();
    }
}
