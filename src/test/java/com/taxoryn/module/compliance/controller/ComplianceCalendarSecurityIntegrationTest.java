package com.taxoryn.module.compliance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.AssignObligationRequest;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.UpdateObligationStatusRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComplianceCalendarSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplianceObligationRepository obligationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Tenant A
    private OrganizationEntity tenantA;
    private UserEntity tenantAAdminUser;
    private String tenantAAdminToken;
    private ClientEntity clientA1;
    private ClientEntity clientA2;
    private EmployeeEntity employeeA;

    // Tenant B
    private OrganizationEntity tenantB;
    private UserEntity tenantBAdminUser;
    private String tenantBAdminToken;
    private ClientEntity clientB1;

    @BeforeEach
    void setUp() {
        tearDown();

        // 1. Setup Tenant A
        tenantA = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant A Practice")
                .email("admin-a-" + UUID.randomUUID() + "@taxoryn.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        RoleEntity adminRoleA = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        tenantAAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(tenantA.getId())
                .email("admin@tenant-a-" + UUID.randomUUID() + ".com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("TenantA")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRoleA)))
                .build());

        tenantAAdminToken = jwtTokenProvider.generateAccessToken(
                tenantAAdminUser.getId(),
                tenantA.getId(),
                tenantAAdminUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("COMPLIANCE_READ", "COMPLIANCE_WRITE", "CLIENT_READ", "CLIENT_WRITE")
        );

        TenantContext.setTenantId(tenantA.getId());
        employeeA = employeeRepository.save(EmployeeEntity.builder()
                .userId(tenantAAdminUser.getId())
                .employeeCode("EMP-A1")
                .firstName("Admin")
                .lastName("TenantA")
                .email(tenantAAdminUser.getEmail())
                .designation("Senior Manager")
                .status(EmployeeStatus.ACTIVE)
                .build());

        clientA1 = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.INDIVIDUAL)
                .legalName("Client A1 Pvt Ltd")
                .displayName("Client A1")
                .pan("AAACA1111A")
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(employeeA.getId())
                .build());

        clientA2 = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.INDIVIDUAL)
                .legalName("Client A2 Individual")
                .displayName("Client A2")
                .pan("BBBCB2222B")
                .status(ClientStatus.ACTIVE)
                .build());
        TenantContext.clear();

        // 2. Setup Tenant B
        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("Tenant B Practice")
                .email("admin-b-" + UUID.randomUUID() + "@taxoryn.test")
                .status(OrganizationStatus.ACTIVE)
                .build());

        RoleEntity adminRoleB = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        tenantBAdminUser = userRepository.save(UserEntity.builder()
                .organizationId(tenantB.getId())
                .email("admin@tenant-b-" + UUID.randomUUID() + ".com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("TenantB")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRoleB)))
                .build());

        tenantBAdminToken = jwtTokenProvider.generateAccessToken(
                tenantBAdminUser.getId(),
                tenantB.getId(),
                tenantBAdminUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("COMPLIANCE_READ", "COMPLIANCE_WRITE", "CLIENT_READ", "CLIENT_WRITE")
        );

        TenantContext.setTenantId(tenantB.getId());
        clientB1 = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.INDIVIDUAL)
                .legalName("Client B1 Corp")
                .displayName("Client B1")
                .pan("CCCC1111C")
                .status(ClientStatus.ACTIVE)
                .build());
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        obligationRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        organizationRepository.deleteAll();
        auditLogRepository.deleteAll();
    }

    @Test
    @DisplayName("Tenant A creates obligation and Tenant B cannot view or access it")
    void testTenantIsolationOnObligations() throws Exception {
        LocalDate today = LocalDate.now();

        CreateComplianceObligationRequest createRequest = CreateComplianceObligationRequest.builder()
                .clientId(clientA1.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B Current Month")
                .periodLabel("Current Month")
                .statutoryDueDate(today.plusDays(10))
                .priority(TaskPriority.HIGH)
                .build();

        // Tenant A creates obligation
        String response = mockMvc.perform(post("/api/compliance/calendar/obligations")
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("GSTR-3B Current Month"))
                .andExpect(jsonPath("$.data.clientDisplayName").value("Client A1"))
                .andReturn().getResponse().getContentAsString();

        String obligationId = objectMapper.readTree(response).path("data").path("id").asText();

        // Tenant A can retrieve it
        mockMvc.perform(get("/api/compliance/calendar/obligations/" + obligationId)
                        .header("Authorization", "Bearer " + tenantAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(obligationId));

        // Tenant B cannot retrieve it (returns 404/not found)
        mockMvc.perform(get("/api/compliance/calendar/obligations/" + obligationId)
                        .header("Authorization", "Bearer " + tenantBAdminToken))
                .andExpect(status().isNotFound());

        // Tenant B's calendar query returns 0 items
        mockMvc.perform(get("/api/compliance/calendar")
                        .header("Authorization", "Bearer " + tenantBAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        // Tenant A's calendar query returns 1 item
        mockMvc.perform(get("/api/compliance/calendar")
                        .header("Authorization", "Bearer " + tenantAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("Summary statistics should be strictly isolated and match statutory counts")
    void testSummaryMetricsIsolation() throws Exception {
        LocalDate today = LocalDate.now();

        // Create 1 Due Today, 1 Upcoming, 1 Overdue in Tenant A
        ComplianceObligationEntity dueToday = ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("Due Today GSTR-1")
                .periodLabel("Current Month")
                .statutoryDueDate(today)
                .internalTargetDate(today.minusDays(3))
                .status(ComplianceObligationStatus.UPCOMING)
                .priority(TaskPriority.HIGH)
                .build();
        dueToday.setOrganizationId(tenantA.getId());
        dueToday.syncLegacyFields();
        obligationRepository.save(dueToday);

        ComplianceObligationEntity upcoming = ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .obligationType(ComplianceObligationType.TDS_RETURN)
                .title("Upcoming 26Q")
                .periodLabel("Q2")
                .statutoryDueDate(today.plusDays(7))
                .internalTargetDate(today.plusDays(4))
                .status(ComplianceObligationStatus.UPCOMING)
                .priority(TaskPriority.MEDIUM)
                .build();
        upcoming.setOrganizationId(tenantA.getId());
        upcoming.syncLegacyFields();
        obligationRepository.save(upcoming);

        ComplianceObligationEntity overdue = ComplianceObligationEntity.builder()
                .clientId(clientA2.getId())
                .obligationType(ComplianceObligationType.ITR_FILING)
                .title("Overdue ITR")
                .periodLabel("AY 2025-26")
                .statutoryDueDate(today.minusDays(5))
                .internalTargetDate(today.minusDays(8))
                .status(ComplianceObligationStatus.OVERDUE)
                .priority(TaskPriority.URGENT)
                .build();
        overdue.setOrganizationId(tenantA.getId());
        overdue.syncLegacyFields();
        obligationRepository.save(overdue);

        // Check Tenant A summary
        mockMvc.perform(get("/api/compliance/calendar/summary")
                        .header("Authorization", "Bearer " + tenantAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalObligations").value(3))
                .andExpect(jsonPath("$.data.dueToday").value(1))
                .andExpect(jsonPath("$.data.upcoming").value(1))
                .andExpect(jsonPath("$.data.overdue").value(1))
                .andExpect(jsonPath("$.data.completed").value(0));

        // Check Tenant B summary (must be all 0s)
        mockMvc.perform(get("/api/compliance/calendar/summary")
                        .header("Authorization", "Bearer " + tenantBAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalObligations").value(0))
                .andExpect(jsonPath("$.data.dueToday").value(0))
                .andExpect(jsonPath("$.data.upcoming").value(0))
                .andExpect(jsonPath("$.data.overdue").value(0));
    }

    @Test
    @DisplayName("Should update obligation status and log audit event")
    void testUpdateObligationStatusAndAudit() throws Exception {
        LocalDate today = LocalDate.now();

        ComplianceObligationEntity entity = ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B Test")
                .periodLabel("Test Period")
                .statutoryDueDate(today.plusDays(10))
                .status(ComplianceObligationStatus.UPCOMING)
                .priority(TaskPriority.HIGH)
                .build();
        entity.setOrganizationId(tenantA.getId());
        entity.syncLegacyFields();
        entity = obligationRepository.save(entity);

        UpdateObligationStatusRequest statusRequest = UpdateObligationStatusRequest.builder()
                .status(ComplianceObligationStatus.FILED)
                .remarks("Successfully filed via GSTN portal")
                .build();

        mockMvc.perform(patch("/api/compliance/calendar/obligations/" + entity.getId() + "/status")
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FILED"))
                .andExpect(jsonPath("$.data.filedDate").isNotEmpty())
                .andExpect(jsonPath("$.data.notes").value("Successfully filed via GSTN portal"));

        // Verify audit log exists
        Page<AuditLogEntity> auditPage = auditLogRepository.findAllByOrganizationId(tenantA.getId(), PageRequest.of(0, 10));
        assertThat(auditPage.getContent()).anyMatch(log -> "COMPLIANCE_OBLIGATION_STATUS_UPDATED".equals(log.getAction()));
    }

    @Test
    @DisplayName("Should reject assigning obligation to employee of a different tenant")
    void testCrossTenantAssigneeRejection() throws Exception {
        LocalDate today = LocalDate.now();

        TenantContext.setTenantId(tenantB.getId());
        // Create employee in Tenant B
        EmployeeEntity employeeB = employeeRepository.save(EmployeeEntity.builder()
                .userId(tenantBAdminUser.getId())
                .employeeCode("EMP-B1")
                .firstName("Staff")
                .lastName("TenantB")
                .email(tenantBAdminUser.getEmail())
                .designation("Staff")
                .status(EmployeeStatus.ACTIVE)
                .build());
        TenantContext.clear();

        ComplianceObligationEntity entity = ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .obligationType(ComplianceObligationType.GST_RETURN)
                .title("GSTR-3B Assignment Test")
                .periodLabel("Test Period")
                .statutoryDueDate(today.plusDays(5))
                .status(ComplianceObligationStatus.UPCOMING)
                .priority(TaskPriority.HIGH)
                .build();
        entity.setOrganizationId(tenantA.getId());
        entity.syncLegacyFields();
        entity = obligationRepository.save(entity);

        AssignObligationRequest assignRequest = AssignObligationRequest.builder()
                .employeeId(employeeB.getId())
                .build();

        // Tenant A admin tries to assign Tenant B's employee
        mockMvc.perform(patch("/api/compliance/calendar/obligations/" + entity.getId() + "/assign")
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isBadRequest());
    }
}
