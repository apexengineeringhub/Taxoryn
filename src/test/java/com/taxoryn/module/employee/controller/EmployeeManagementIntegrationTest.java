package com.taxoryn.module.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.employee.dto.CreateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeRequest;
import com.taxoryn.module.employee.dto.UpdateEmployeeStatusRequest;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeManagementIntegrationTest {

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
    private EmployeeRepository employeeRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private String adminToken1;
    private EmployeeEntity employee1;
    private EmployeeEntity employee2;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        taskRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization 1 & 2
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Verma & Associates")
                .email("admin@vermatax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Kapadia Advisory")
                .email("admin@kapadiatax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        TenantContext.setTenantId(org1.getId());

        adminUser1 = userRepository.save(UserEntity.builder()
                .email("admin@vermatax.com")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .firstName("Suresh")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        adminToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser1.getId(),
                org1.getId(),
                adminUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("EMPLOYEE_CREATE", "EMPLOYEE_VIEW", "EMPLOYEE_UPDATE", "TASK_VIEW")
        );

        // 2. Create Employee 1 in Org 1
        employee1 = EmployeeEntity.builder()
                .employeeCode("EMP-101")
                .firstName("Amit")
                .lastName("Sharma")
                .email("amit.sharma@vermatax.com")
                .phone("+919876500001")
                .department("Taxation")
                .designation("Senior Tax Manager")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 4, 1))
                .build();
        employee1 = employeeRepository.save(employee1);

        // 3. Create Employee 2 in Org 1 (reporting to Employee 1)
        employee2 = EmployeeEntity.builder()
                .employeeCode("EMP-102")
                .firstName("Priya")
                .lastName("Patel")
                .email("priya.p@vermatax.com")
                .phone("+919876500002")
                .department("Audit")
                .designation("Audit Associate")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 6, 1))
                .managerId(employee1.getId())
                .build();
        employee2 = employeeRepository.save(employee2);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Create employee successfully")
    void testCreateEmployee() throws Exception {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .employeeCode("EMP-103")
                .firstName("Deepak")
                .lastName("Mehta")
                .email("deepak.m@vermatax.com")
                .phone("+919876500003")
                .department("Taxation")
                .designation("GST Specialist")
                .joiningDate(LocalDate.of(2024, 1, 10))
                .managerId(employee1.getId())
                .build();

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeCode").value("EMP-103"))
                .andExpect(jsonPath("$.data.fullName").value("Deepak Mehta"))
                .andExpect(jsonPath("$.data.managerName").value("Amit Sharma"));
    }

    @Test
    @DisplayName("2. List & search employees by search term and filters")
    void testSearchAndFilterEmployees() throws Exception {
        // Search by keyword 'Priya'
        mockMvc.perform(get("/api/v1/employees")
                        .param("search", "Priya")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].firstName").value("Priya"));

        // Filter by department 'Taxation'
        mockMvc.perform(get("/api/v1/employees")
                        .param("department", "Taxation")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].firstName").value("Amit"));

        // Filter by status 'ACTIVE'
        mockMvc.perform(get("/api/v1/employees")
                        .param("status", "ACTIVE")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("3. Update employee profile and reporting hierarchy")
    void testUpdateEmployee() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("Amit")
                .lastName("Sharma")
                .email("amit.sharma@vermatax.com")
                .phone("+919876599999")
                .department("Taxation & Advisory")
                .designation("Partner - Direct Tax")
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 4, 1))
                .build();

        mockMvc.perform(put("/api/v1/employees/" + employee1.getId())
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phone").value("+919876599999"))
                .andExpect(jsonPath("$.data.designation").value("Partner - Direct Tax"));
    }

    @Test
    @DisplayName("4. Deactivate employee status")
    void testDeactivateEmployee() throws Exception {
        UpdateEmployeeStatusRequest statusRequest = new UpdateEmployeeStatusRequest(EmployeeStatus.INACTIVE);

        mockMvc.perform(patch("/api/v1/employees/" + employee2.getId() + "/status")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        // Delete endpoint marks as TERMINATED
        mockMvc.perform(delete("/api/v1/employees/" + employee2.getId())
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("5. Employee Workload Endpoint computes accurate task analytics")
    void testGetEmployeeWorkload() throws Exception {
        TenantContext.setTenantId(org1.getId());
        try {
            // Create tasks assigned to employee1
            // 1. Completed Task
            taskRepository.save(TaskEntity.builder()
                    .assignedTo(employee1.getId())
                    .title("GSTR-3B Filing for ABC Ltd")
                    .taskCategory(TaskCategory.GST)
                    .status(TaskStatus.COMPLETED)
                    .priority(TaskPriority.HIGH)
                    .dueDate(LocalDate.now().minusDays(2))
                    .build());

            // 2. Pending Task (On track)
            taskRepository.save(TaskEntity.builder()
                    .assignedTo(employee1.getId())
                    .title("ITR-6 Preparation")
                    .taskCategory(TaskCategory.ITR)
                    .status(TaskStatus.IN_PROGRESS)
                    .priority(TaskPriority.URGENT)
                    .dueDate(LocalDate.now().plusDays(5))
                    .build());

            // 3. Overdue Task (Past due date)
            taskRepository.save(TaskEntity.builder()
                    .assignedTo(employee1.getId())
                    .title("Tax Audit 44AB Report")
                    .taskCategory(TaskCategory.AUDIT)
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.HIGH)
                    .dueDate(LocalDate.now().minusDays(3))
                    .build());

            // 4. Cancelled Task (Should not be counted in totalAssignedTasks)
            taskRepository.save(TaskEntity.builder()
                    .assignedTo(employee1.getId())
                    .title("Cancelled Task")
                    .taskCategory(TaskCategory.OTHER)
                    .status(TaskStatus.CANCELLED)
                    .priority(TaskPriority.LOW)
                    .dueDate(LocalDate.now().plusDays(10))
                    .build());
        } finally {
            TenantContext.clear();
        }

        // Request workload via /api/employees/{id}/workload
        mockMvc.perform(get("/api/employees/" + employee1.getId() + "/workload")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeId").value(employee1.getId().toString()))
                .andExpect(jsonPath("$.data.totalAssignedTasks").value(3))
                .andExpect(jsonPath("$.data.completedTasks").value(1))
                .andExpect(jsonPath("$.data.pendingTasks").value(2))
                .andExpect(jsonPath("$.data.overdueTasks").value(1));

        // Also test /api/v1/employees/{id}/workload route
        mockMvc.perform(get("/api/v1/employees/" + employee1.getId() + "/workload")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAssignedTasks").value(3));
    }

    @Test
    @DisplayName("6. Cross-tenant employee access is rejected")
    void testCrossTenantEmployeeAccessRejected() throws Exception {
        TenantContext.setTenantId(org2.getId());
        UserEntity adminUser2;
        try {
            adminUser2 = userRepository.save(UserEntity.builder()
                    .email("admin@kapadiatax.com")
                    .passwordHash(passwordEncoder.encode("SecretPass123!"))
                    .firstName("Ketan")
                    .status(UserStatus.ACTIVE)
                    .roles(new HashSet<>())
                    .build());
        } finally {
            TenantContext.clear();
        }

        String org2Token = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser2.getId(),
                org2.getId(),
                adminUser2.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("EMPLOYEE_VIEW", "EMPLOYEE_UPDATE")
        );

        // Org 2 user attempts to access employee from Org 1 -> 404 NOT FOUND (due to tenant filter)
        mockMvc.perform(get("/api/v1/employees/" + employee1.getId())
                        .header("Authorization", org2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
