package com.taxoryn.qa.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.employee.dto.CreateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeStatusRequest;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.qa.factory.OrganizationTestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationTestDataFactory factory;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity adminA;
    private UserEntity adminB;
    private UserEntity practitionerA;
    private EmployeeEntity employeeA;
    private EmployeeEntity employeeB;
    private String tokenAdminA;
    private String tokenAdminB;
    private String tokenPractitionerA;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        orgA = factory.createOrganization("Alpha Advisors " + UUID.randomUUID().toString().substring(0, 5), "admin.alpha." + UUID.randomUUID().toString().substring(0, 5) + "@alpha.in");
        orgB = factory.createOrganization("Beta Financial " + UUID.randomUUID().toString().substring(0, 5), "admin.beta." + UUID.randomUUID().toString().substring(0, 5) + "@beta.in");
        adminA = factory.createAdminUser(orgA, "adminA." + UUID.randomUUID().toString().substring(0, 5) + "@alpha.in", "AdminPass123!");
        adminB = factory.createAdminUser(orgB, "adminB." + UUID.randomUUID().toString().substring(0, 5) + "@beta.in", "AdminPass123!");

        practitionerA = factory.createEmployeeUser(orgA, "pracA." + UUID.randomUUID().toString().substring(0, 5) + "@alpha.in", "PRACTITIONER", "StaffPass123!");
        employeeA = factory.createEmployee(orgA, practitionerA, "EMP-A-" + UUID.randomUUID().toString().substring(0, 4), "Tax", "Senior Tax Manager");

        UserEntity practitionerB = factory.createEmployeeUser(orgB, "pracB." + UUID.randomUUID().toString().substring(0, 5) + "@beta.in", "PRACTITIONER", "StaffPass123!");
        employeeB = factory.createEmployee(orgB, practitionerB, "EMP-B-" + UUID.randomUUID().toString().substring(0, 4), "Audit", "Audit Senior");

        tokenAdminA = factory.generateBearerToken(adminA);
        tokenAdminB = factory.generateBearerToken(adminB);
        tokenPractitionerA = factory.generateBearerToken(practitionerA);
    }

    @Autowired
    private com.taxoryn.module.authentication.repository.OrganizationActivationTokenRepository activationTokenRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("EMP-001: Add employee - Admin creates employee successfully with activation token and INACTIVE status")
    void shouldAddEmployeeSuccessfully() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-" + unique)
                .firstName("Vikram")
                .lastName("Mehta")
                .email("vikram." + unique + "@alpha.in")
                .phone("+919876543220")
                .department("Direct Tax")
                .designation("Chartered Accountant")
                .joiningDate(LocalDate.now())
                .status(EmployeeStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.employeeCode", is("EMP-" + unique)))
                .andExpect(jsonPath("$.data.email", is("vikram." + unique + "@alpha.in")));

        assertTrue(employeeRepository.existsByOrganizationIdAndEmail(orgA.getId(), "vikram." + unique + "@alpha.in"));
        UserEntity user = userRepository.findByEmailIgnoreCase("vikram." + unique + "@alpha.in").orElseThrow();
        assertEquals(UserEntity.UserStatus.INVITED, user.getStatus());

        // Verify activation token created for user and org
        var tokens = activationTokenRepository.findAllByUserIdAndUsedAtIsNull(user.getId());
        assertFalse(tokens.isEmpty());
        assertEquals(orgA.getId(), tokens.get(0).getOrganizationId());
    }

    @Test
    @DisplayName("EMP-002: Employee Invitation & Activation Full Lifecycle with Password Setup")
    void shouldCompleteEmployeeInvitationAndActivationFlow() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        String employeeEmail = "invitee." + unique + "@alpha.in";

        // 1. Practice Admin creates employee
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-" + unique)
                .firstName("Pooja")
                .lastName("Nair")
                .email(employeeEmail)
                .phone("+919876543221")
                .department("Audit")
                .designation("Audit Senior")
                .joiningDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        UserEntity user = userRepository.findByEmailIgnoreCase(employeeEmail).orElseThrow();
        assertEquals(UserEntity.UserStatus.INVITED, user.getStatus());

        // 2. INACTIVE employee cannot log in
        LoginRequest failedLogin = LoginRequest.builder()
                .email(employeeEmail)
                .password("AnyPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failedLogin)))
                .andExpect(status().isUnauthorized());

        // 3. Obtain raw token by creating a deterministic activation token test case
        String rawToken = com.taxoryn.core.security.PasswordSecurityUtils.generateSecureToken();
        String tokenHash = com.taxoryn.core.security.PasswordSecurityUtils.hashSha256(rawToken);

        com.taxoryn.module.authentication.entity.OrganizationActivationTokenEntity testToken =
                com.taxoryn.module.authentication.entity.OrganizationActivationTokenEntity.builder()
                        .userId(user.getId())
                        .organizationId(orgA.getId())
                        .tokenHash(tokenHash)
                        .expiresAt(java.time.Instant.now().plus(24, java.time.temporal.ChronoUnit.HOURS))
                        .build();
        activationTokenRepository.save(testToken);

        // 4. Validate activation token via validation endpoint
        mockMvc.perform(get("/api/v1/auth/validate-activation-token")
                        .param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.valid", is(true)))
                .andExpect(jsonPath("$.data.email", is(employeeEmail)))
                .andExpect(jsonPath("$.data.userFullName", is("Pooja Nair")))
                .andExpect(jsonPath("$.data.requiresPasswordSetup", is(true)));

        // 5. Invalid token validation returns 401
        mockMvc.perform(get("/api/v1/auth/validate-activation-token")
                        .param("token", "invalid_random_token_123"))
                .andExpect(status().isUnauthorized());

        // 6. Employee sets password and activates account via activation endpoint
        String newPassword = "NewEmployeePass123!";
        com.taxoryn.module.authentication.dto.ActivateOrganizationRequest activateReq =
                new com.taxoryn.module.authentication.dto.ActivateOrganizationRequest(rawToken, newPassword, newPassword);

        mockMvc.perform(post("/api/v1/auth/activate-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 7. Verify user status is now ACTIVE and password is updated
        UserEntity activatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(UserEntity.UserStatus.ACTIVE, activatedUser.getStatus());
        assertTrue(passwordEncoder.matches(newPassword, activatedUser.getPasswordHash()));

        // 8. Employee can now successfully log in with new password
        LoginRequest successfulLogin = LoginRequest.builder()
                .email(employeeEmail)
                .password(newPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(successfulLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is(employeeEmail)));

        // 9. Token reuse is rejected
        mockMvc.perform(post("/api/v1/auth/activate-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activateReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("EMP-003: Resend employee invitation generates new token")
    void shouldResendEmployeeInvitationSuccessfully() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        String employeeEmail = "resend." + unique + "@alpha.in";

        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-" + unique)
                .firstName("Ankit")
                .lastName("Sharma")
                .email(employeeEmail)
                .phone("+919876543222")
                .department("GST")
                .designation("Consultant")
                .build();

        String res = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID employeeId = UUID.fromString(objectMapper.readTree(res).path("data").path("id").asText());

        // Resend invitation as Admin A
        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/resend-invitation")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Resend from another org Admin B is rejected (404 not found)
        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/resend-invitation")
                        .header("Authorization", "Bearer " + tokenAdminB))
                .andExpect(status().isNotFound());

        // Resend by non-admin employee is forbidden (403)
        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/resend-invitation")
                        .header("Authorization", "Bearer " + tokenPractitionerA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EMP-004: Employee login - Employee logs in with credentials")
    void shouldAllowEmployeeLoginWithCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(practitionerA.getEmail())
                .password("StaffPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is(practitionerA.getEmail())));
    }

    @Test
    @DisplayName("EMP-005: Employee cross-tenant isolation - Employee A cannot access Org B employees")
    void shouldNotAllowEmployeeToAccessAnotherOrganization() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + employeeB.getId())
                        .header("Authorization", "Bearer " + tokenPractitionerA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("EMP-007: Employee ID manipulation - Admin A cannot update Employee B")
    void shouldRejectEmployeeIdManipulationCrossTenant() throws Exception {
        UpdateEmployeeRequest updateReq = UpdateEmployeeRequest.builder()
                .firstName("Hacked First Name")
                .email("hacked@beta.in")
                .department("Hacked Dept")
                .designation("Hacked Desig")
                .build();

        mockMvc.perform(put("/api/v1/employees/" + employeeB.getId())
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Verify Employee B is unchanged
        EmployeeEntity unchanged = employeeRepository.findById(employeeB.getId()).orElseThrow();
        assertNotEquals("Hacked First Name", unchanged.getFirstName());
    }

    @Test
    @DisplayName("EMP-008: Privilege escalation prevention - Non-admin employee cannot create new employees")
    void shouldPreventNonAdminEmployeeFromCreatingEmployees() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-" + unique)
                .firstName("Unauthorized")
                .email("unauthorized." + unique + "@alpha.in")
                .department("Tax")
                .designation("Article")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenPractitionerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EMP-009: Duplicate employee prevention - Duplicate code or email in same tenant rejected")
    void shouldRejectDuplicateEmployeeInSameOrg() throws Exception {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode(employeeA.getEmployeeCode()) // duplicate code
                .firstName("Duplicate")
                .email("new.email." + UUID.randomUUID().toString().substring(0, 4) + "@alpha.in")
                .department("Tax")
                .designation("Senior")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("EMP-012: Employee deactivation - Terminated employee status update")
    void shouldDeactivateEmployeeSuccessfully() throws Exception {
        UpdateEmployeeStatusRequest statusReq = UpdateEmployeeStatusRequest.builder()
                .status(EmployeeStatus.TERMINATED)
                .build();

        mockMvc.perform(patch("/api/v1/employees/" + employeeA.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("TERMINATED")));

        EmployeeEntity updated = employeeRepository.findById(employeeA.getId()).orElseThrow();
        assertEquals(EmployeeStatus.TERMINATED, updated.getStatus());
    }
}
