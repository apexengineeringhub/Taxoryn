package com.taxoryn.module.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.employee.dto.CreateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeRoleRequest;
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
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security Integration Test: Separate RBAC Role From Designation (Security Fix #6).
 * <p>
 * Verifies that:
 * 1. An employee created with designation "Partner" does NOT automatically get the PARTNER security role.
 * 2. An employee created with designation "Manager" does NOT automatically get the TAX_MANAGER security role.
 * 3. Updating an employee's designation to "Partner" or "Managing Partner" never changes their security role.
 * 4. Only explicit authorized RBAC role assignments grant practice roles.
 * 5. Platform roles (e.g. SUPER_ADMIN) cannot be assigned by practice admins.
 * 6. Client roles (e.g. CLIENT_USER) cannot be assigned as practice employee roles.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacRoleDesignationSeparationSecurityTest {

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

    private OrganizationEntity orgA;
    private UserEntity adminA;
    private String tokenAdminA;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        orgA = factory.createOrganization("Chartered Practice " + suffix, "admin." + suffix + "@practice.in");
        adminA = factory.createAdminUser(orgA, "admin." + suffix + "@practice.in", "AdminPass123!");
        tokenAdminA = factory.generateBearerToken(adminA);

        // Ensure baseline system roles exist
        ensureSystemRole("TAX_ASSOCIATE", "Tax Associate");
        ensureSystemRole("PARTNER", "Practice Partner");
        ensureSystemRole("TAX_MANAGER", "Tax Manager");
        ensureSystemRole("PRACTITIONER", "Practitioner");
        ensureSystemRole("ACCOUNTANT", "Staff Accountant");
    }

    private void ensureSystemRole(String code, String name) {
        roleRepository.findByCodeAndIsSystemRoleTrue(code).orElseGet(() ->
                roleRepository.save(RoleEntity.builder()
                        .code(code)
                        .name(name)
                        .isSystemRole(true)
                        .permissions(Set.of())
                        .build()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("SECURITY FIX #6: Designation 'Partner' does NOT automatically grant PARTNER role")
    void testDesignationPartner_DoesNotGrantPartnerRole() throws Exception {
        String empEmail = "partner.title." + UUID.randomUUID().toString().substring(0, 6) + "@practice.in";

        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-PAR-01")
                .firstName("Aditya")
                .lastName("Birla")
                .email(empEmail)
                .phone("+919876543210")
                .department("Taxation")
                .designation("Partner") // Designation is Partner, but NO roleCode specified!
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.designation", equalTo("Partner")))
                .andExpect(jsonPath("$.data.roleCode", equalTo("TAX_ASSOCIATE"))); // Must default to TAX_ASSOCIATE, NOT PARTNER!

        UserEntity user = userRepository.findByEmailIgnoreCase(empEmail).orElseThrow();
        Set<RoleEntity> roles = user.getRoles();
        assertNotNull(roles);
        assertFalse(roles.stream().anyMatch(r -> "PARTNER".equalsIgnoreCase(r.getCode())),
                "User must NOT have PARTNER role inferred from designation");
        assertTrue(roles.stream().anyMatch(r -> "TAX_ASSOCIATE".equalsIgnoreCase(r.getCode())),
                "User must have standard base TAX_ASSOCIATE role");
    }

    @Test
    @DisplayName("SECURITY FIX #6: Designation 'Tax Manager' does NOT automatically grant TAX_MANAGER role")
    void testDesignationManager_DoesNotGrantManagerRole() throws Exception {
        String empEmail = "mgr.title." + UUID.randomUUID().toString().substring(0, 6) + "@practice.in";

        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-MGR-01")
                .firstName("Sneha")
                .lastName("Patil")
                .email(empEmail)
                .department("Audit")
                .designation("Senior Tax Manager & Lead") // Manager in title, NO roleCode specified
                .status(EmployeeStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.designation", equalTo("Senior Tax Manager & Lead")))
                .andExpect(jsonPath("$.data.roleCode", equalTo("TAX_ASSOCIATE"))); // Must default to TAX_ASSOCIATE, NOT TAX_MANAGER!

        UserEntity user = userRepository.findByEmailIgnoreCase(empEmail).orElseThrow();
        assertFalse(user.getRoles().stream().anyMatch(r -> "TAX_MANAGER".equalsIgnoreCase(r.getCode())),
                "User must NOT have TAX_MANAGER role inferred from designation");
    }

    @Test
    @DisplayName("SECURITY FIX #6: Explicit role assignment is honored regardless of designation")
    void testExplicitRoleAssignment_IsHonored() throws Exception {
        String empEmail = "explicit.role." + UUID.randomUUID().toString().substring(0, 6) + "@practice.in";

        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-EXP-01")
                .firstName("Raj")
                .lastName("Malhotra")
                .email(empEmail)
                .department("Taxation")
                .designation("Junior Article Intern") // Intern in title
                .roleCode("PRACTITIONER") // Explicitly assigned PRACTITIONER role!
                .status(EmployeeStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.designation", equalTo("Junior Article Intern")))
                .andExpect(jsonPath("$.data.roleCode", equalTo("PRACTITIONER")));

        UserEntity user = userRepository.findByEmailIgnoreCase(empEmail).orElseThrow();
        assertTrue(user.getRoles().stream().anyMatch(r -> "PRACTITIONER".equalsIgnoreCase(r.getCode())),
                "Explicitly requested role PRACTITIONER must be assigned");
    }

    @Test
    @DisplayName("SECURITY FIX #6: Updating designation does NOT elevate security role")
    void testUpdateDesignation_DoesNotElevateSecurityRole() throws Exception {
        String empEmail = "staff.promo." + UUID.randomUUID().toString().substring(0, 6) + "@practice.in";

        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-PRO-01")
                .firstName("Karan")
                .lastName("Kapoor")
                .email(empEmail)
                .department("Taxation")
                .designation("Staff Associate")
                .status(EmployeeStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        EmployeeEntity employee = employeeRepository.findByOrganizationIdAndEmail(orgA.getId(), empEmail).orElseThrow();

        // Admin updates employee designation to "Senior Managing Partner" via PUT /api/v1/employees/{id} without changing role
        UpdateEmployeeRequest updateRequest = UpdateEmployeeRequest.builder()
                .firstName("Karan")
                .lastName("Kapoor")
                .email(empEmail)
                .department("Taxation")
                .designation("Senior Managing Partner")
                .build();

        mockMvc.perform(put("/api/v1/employees/" + employee.getId())
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.designation", equalTo("Senior Managing Partner")))
                .andExpect(jsonPath("$.data.roleCode", equalTo("TAX_ASSOCIATE")));

        UserEntity user = userRepository.findByEmailIgnoreCase(empEmail).orElseThrow();
        assertFalse(user.getRoles().stream().anyMatch(r -> "PARTNER".equalsIgnoreCase(r.getCode())),
                "Role must NOT become PARTNER when designation is changed to Partner");
        assertTrue(user.getRoles().stream().anyMatch(r -> "TAX_ASSOCIATE".equalsIgnoreCase(r.getCode())));
    }

    @Test
    @DisplayName("SECURITY FIX #6: Practice Admin cannot assign platform role during employee creation or update")
    void testAssignPlatformRole_IsDenied() throws Exception {
        String empEmail = "escalate." + UUID.randomUUID().toString().substring(0, 6) + "@practice.in";

        // 1. Attempt to assign SUPER_ADMIN platform role during creation
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-ESC-01")
                .firstName("Attacker")
                .lastName("Admin")
                .email(empEmail)
                .department("IT")
                .designation("System Admin")
                .roleCode("SUPER_ADMIN") // Platform role
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Privilege escalation denied")));

        // 2. Create standard employee first
        request.setRoleCode("TAX_ASSOCIATE");
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        EmployeeEntity employee = employeeRepository.findByOrganizationIdAndEmail(orgA.getId(), empEmail).orElseThrow();

        // 3. Attempt to escalate role to TAXORYN_SUPERADMIN via role update endpoint
        UpdateEmployeeRoleRequest roleReq = new UpdateEmployeeRoleRequest(null, "TAXORYN_SUPERADMIN");
        mockMvc.perform(put("/api/v1/employees/" + employee.getId() + "/role")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Privilege escalation denied")));
    }

    @Test
    @DisplayName("SECURITY FIX #6: Practice Admin cannot assign client role to practice employee")
    void testAssignClientRole_IsDenied() throws Exception {
        String empEmail = "clientrole." + UUID.randomUUID().toString().substring(0, 6) + "@practice.in";

        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-CLI-01")
                .firstName("Client")
                .lastName("Staff")
                .email(empEmail)
                .department("Taxation")
                .designation("Staff")
                .roleCode("CLIENT_USER") // Client role
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Invalid role assignment")));
    }
}
