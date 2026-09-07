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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("EMP-001: Add employee - Admin creates employee successfully")
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
        assertTrue(userRepository.findByEmailIgnoreCase("vikram." + unique + "@alpha.in").isPresent());
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
