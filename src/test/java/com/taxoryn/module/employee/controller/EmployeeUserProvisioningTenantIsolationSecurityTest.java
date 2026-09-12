package com.taxoryn.module.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.dto.CreateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeRoleRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeStatusRequest;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security Integration Test: Employee User Provisioning Tenant Isolation (Security Fix #5).
 * <p>
 * Verifies that:
 * 1. Organization B cannot create an employee with an email belonging to an existing user in Organization A.
 * 2. Organization A cannot create an employee with an email belonging to an existing client user in Organization A.
 * 3. Organization A attempting to create an employee with an existing internal user email without explicit userId returns a conflict.
 * 4. Organization A creating an employee with explicit userId succeeds and attaches the user safely.
 * 5. Organization A cannot attach the same userId to multiple employee records.
 * 6. Organization A cannot reference or attach a userId belonging to Organization B (404/rejection).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeUserProvisioningTenantIsolationSecurityTest {

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

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity adminA;
    private UserEntity adminB;
    private String tokenAdminA;
    private String tokenAdminB;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();

        String suffixA = UUID.randomUUID().toString().substring(0, 6);
        String suffixB = UUID.randomUUID().toString().substring(0, 6);

        orgA = factory.createOrganization("Alpha Tax Practice " + suffixA, "admin.alpha." + suffixA + "@alpha.in");
        orgB = factory.createOrganization("Beta Financial Practice " + suffixB, "admin.beta." + suffixB + "@beta.in");

        adminA = factory.createAdminUser(orgA, "adminA." + suffixA + "@alpha.in", "AdminPass123!");
        adminB = factory.createAdminUser(orgB, "adminB." + suffixB + "@beta.in", "AdminPass123!");

        tokenAdminA = factory.generateBearerToken(adminA);
        tokenAdminB = factory.generateBearerToken(adminB);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("SECURITY FIX #5: Organization B cannot attach or hijack User belonging to Organization A")
    void testCrossTenantUserHijacking_IsRejected() throws Exception {
        // Step 1: Create an employee in Org A with email user.alpha@practice.in
        String sharedEmail = "user.alpha." + UUID.randomUUID().toString().substring(0, 6) + "@practice.in";

        CreateEmployeeRequest createInOrgA = CreateEmployeeRequest.builder()
                .employeeCode("EMP-A-101")
                .firstName("Rohan")
                .lastName("Deshmukh")
                .email(sharedEmail)
                .phone("+919876543210")
                .department("Taxation")
                .designation("Tax Associate")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createInOrgA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email", equalTo(sharedEmail)));

        UserEntity userInOrgA = userRepository.findByEmailIgnoreCase(sharedEmail).orElseThrow();
        assertEquals(orgA.getId(), userInOrgA.getOrganizationId());

        // Step 2: Org B attempts to create an employee with the SAME email
        CreateEmployeeRequest createInOrgB = CreateEmployeeRequest.builder()
                .employeeCode("EMP-B-101")
                .firstName("Impostor")
                .lastName("Staff")
                .email(sharedEmail)
                .phone("+919876543211")
                .department("Audit")
                .designation("Senior Associate")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createInOrgB)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already registered")));

        // Verify Org A user's organizationId was NOT mutated and no employee was created in Org B
        UserEntity userAfterAttack = userRepository.findByEmailIgnoreCase(sharedEmail).orElseThrow();
        assertEquals(orgA.getId(), userAfterAttack.getOrganizationId());
        assertFalse(employeeRepository.existsByOrganizationIdAndEmail(orgB.getId(), sharedEmail));
    }

    @Test
    @DisplayName("SECURITY FIX #5: Cannot create employee with email of an existing Client User in same org")
    void testSameOrgClientUserEmail_CannotBeAttachedAsEmployee() throws Exception {
        String clientUserEmail = "clientuser." + UUID.randomUUID().toString().substring(0, 6) + "@clientcorp.in";

        // Create a Client in Org A
        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Logistics Corp")
                .pan("ABCDE1234F")
                .email("info@abclogistics.in")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(orgA.getId());
        ClientEntity savedClient = clientRepository.save(client);

        // Create a Client User in Org A attached to this client
        RoleEntity clientRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("CLIENT_USER")
                        .name("Client User")
                        .isSystemRole(true)
                        .build()));

        UserEntity clientUser = UserEntity.builder()
                .organizationId(orgA.getId())
                .clientId(savedClient.getId())
                .email(clientUserEmail)
                .passwordHash("hashedPass")
                .firstName("Client")
                .lastName("Contact")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientRole)))
                .build();
        userRepository.save(clientUser);

        // Org A attempts to create an employee with the client user's email
        CreateEmployeeRequest empRequest = CreateEmployeeRequest.builder()
                .employeeCode("EMP-A-102")
                .firstName("Staff")
                .lastName("Member")
                .email(clientUserEmail)
                .department("Taxation")
                .designation("Tax Associate")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("client user")));
    }

    @Test
    @DisplayName("SECURITY FIX #5: Internal user in same org without explicit userId returns conflict (no silent conversion)")
    void testExistingInternalUserWithoutUserId_ReturnsConflict() throws Exception {
        String internalEmail = "partner." + UUID.randomUUID().toString().substring(0, 6) + "@alpha.in";

        RoleEntity partnerRole = roleRepository.findByCodeAndIsSystemRoleTrue("PARTNER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("PARTNER")
                        .name("Practice Partner")
                        .isSystemRole(true)
                        .build()));

        UserEntity partnerUser = UserEntity.builder()
                .organizationId(orgA.getId())
                .email(internalEmail)
                .passwordHash("hashedPass")
                .firstName("Vikram")
                .lastName("Patel")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(partnerRole)))
                .build();
        UserEntity savedUser = userRepository.save(partnerUser);

        // Attempt employee creation with existing user email WITHOUT passing userId
        CreateEmployeeRequest empRequest = CreateEmployeeRequest.builder()
                .employeeCode("EMP-A-103")
                .firstName("Vikram")
                .lastName("Patel")
                .email(internalEmail)
                .department("Audit")
                .designation("Partner")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("specify userId explicitly")));
    }

    @Test
    @DisplayName("SECURITY FIX #5: Creating employee with explicit valid userId succeeds")
    void testCreateEmployee_WithExplicitValidUserId_Success() throws Exception {
        String internalEmail = "manager." + UUID.randomUUID().toString().substring(0, 6) + "@alpha.in";

        RoleEntity managerRole = roleRepository.findByCodeAndIsSystemRoleTrue("TAX_MANAGER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("TAX_MANAGER")
                        .name("Tax Manager")
                        .isSystemRole(true)
                        .build()));

        UserEntity managerUser = UserEntity.builder()
                .organizationId(orgA.getId())
                .email(internalEmail)
                .passwordHash("hashedPass")
                .firstName("Pooja")
                .lastName("Hegde")
                .status(UserEntity.UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(managerRole)))
                .build();
        UserEntity savedUser = userRepository.save(managerUser);

        // Pass explicit userId
        CreateEmployeeRequest empRequest = CreateEmployeeRequest.builder()
                .userId(savedUser.getId())
                .employeeCode("EMP-A-104")
                .firstName("Pooja")
                .lastName("Hegde")
                .email(internalEmail)
                .department("Taxation")
                .designation("Tax Manager")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.employeeCode", equalTo("EMP-A-104")))
                .andExpect(jsonPath("$.data.userId", equalTo(savedUser.getId().toString())));

        // Attempt to create a SECOND employee with the same userId in Org A -> must fail
        CreateEmployeeRequest duplicateUserEmp = CreateEmployeeRequest.builder()
                .userId(savedUser.getId())
                .employeeCode("EMP-A-105")
                .firstName("Pooja")
                .lastName("Hegde")
                .email("pooja.alt." + UUID.randomUUID().toString().substring(0, 4) + "@alpha.in")
                .department("Taxation")
                .designation("Tax Manager")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUserEmp)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("SECURITY FIX #5: Org A cannot pass a foreign userId belonging to Org B")
    void testCreateEmployee_WithForeignUserId_ReturnsNotFound() throws Exception {
        CreateEmployeeRequest empRequest = CreateEmployeeRequest.builder()
                .userId(adminB.getId()) // Belongs to Org B
                .employeeCode("EMP-A-106")
                .firstName("Sneaky")
                .lastName("Admin")
                .email("sneaky." + UUID.randomUUID().toString().substring(0, 6) + "@alpha.in")
                .department("Taxation")
                .designation("Associate")
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SECURITY FIX #5: Org A cannot modify status or role of Org B's employee")
    void testCrossTenantEmployeeModification_IsBlocked() throws Exception {
        // Create employee in Org B
        UserEntity pracB = factory.createEmployeeUser(orgB, "prac." + UUID.randomUUID().toString().substring(0, 6) + "@beta.in", "PRACTITIONER", "StaffPass123!");
        EmployeeEntity empB = factory.createEmployee(orgB, pracB, "EMP-B-999", "Audit", "Auditor");

        // Org A attempts to update status of Org B's employee
        UpdateEmployeeStatusRequest statusReq = new UpdateEmployeeStatusRequest(EmployeeStatus.SUSPENDED);
        mockMvc.perform(patch("/api/v1/employees/" + empB.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isNotFound());

        // Org A attempts to update role of Org B's employee
        UpdateEmployeeRoleRequest roleReq = new UpdateEmployeeRoleRequest(null, "TAX_MANAGER");
        mockMvc.perform(put("/api/v1/employees/" + empB.getId() + "/role")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isNotFound());

        // Org A attempts to delete Org B's employee
        mockMvc.perform(delete("/api/v1/employees/" + empB.getId())
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isNotFound());
    }
}
