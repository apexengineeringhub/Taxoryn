package com.taxoryn.module.compliance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.CreateComplianceRuleRequest;
import com.taxoryn.module.compliance.dto.GenerateComplianceRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceStatusRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.CompliancePriority;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceFrequency;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceRuleRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComplianceManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplianceObligationRepository obligationRepository;

    @Autowired
    private ComplianceRuleRepository ruleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TaskRepository taskRepository;

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

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private String adminToken1;
    private EmployeeEntity employeeRahul;
    private ClientEntity clientAbc;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        obligationRepository.deleteAll();
        taskRepository.deleteAll();
        ruleRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization 1 & 2
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Advisors")
                .email("admin@apextax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Global Tax Advisory")
                .email("admin@globaltax.com")
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
                .email("admin@apextax.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Rajesh")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        adminToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser1.getId(),
                org1.getId(),
                adminUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("TASK_CREATE", "TASK_VIEW", "TASK_UPDATE", "TASK_ASSIGN", "GST_VIEW", "ITR_VIEW")
        );

        // 2. Create Employee Rahul
        employeeRahul = EmployeeEntity.builder()
                .employeeCode("EMP-001")
                .firstName("Rahul")
                .lastName("Sharma")
                .email("rahul@apextax.com")
                .department("Tax & Compliance")
                .designation("Senior Associate")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employeeRahul = employeeRepository.save(employeeRahul);

        // 3. Create Client ABC Traders
        clientAbc = ClientEntity.builder()
                .clientType(ClientType.PROPRIETORSHIP)
                .displayName("ABC Traders")
                .legalName("ABC Traders Proprietorship")
                .pan("AAACB1234D")
                .gstin("27AAACB1234D1Z5")
                .email("contact@abctraders.com")
                .assignedEmployeeId(employeeRahul.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        clientAbc = clientRepository.save(clientAbc);

        // 4. Seed a system rule
        ruleRepository.save(ComplianceRuleEntity.builder()
                .ruleCode("GST_GSTR3B_MONTHLY")
                .name("GSTR-3B Monthly Return & Tax Payment")
                .complianceType(ComplianceType.GST)
                .frequency(ComplianceFrequency.MONTHLY)
                .dueDay(20)
                .dueMonthOffset(1)
                .active(true)
                .systemRule(true)
                .build());

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Create custom compliance rule")
    void testCreateCustomRule() throws Exception {
        CreateComplianceRuleRequest request = CreateComplianceRuleRequest.builder()
                .ruleCode("ROC_MGT7_ANNUAL")
                .name("ROC Annual Return (MGT-7)")
                .complianceType(ComplianceType.ROC)
                .frequency(ComplianceFrequency.ANNUALLY)
                .dueDay(29)
                .fixedDueMonth(11)
                .descriptionTemplate("Annual filing of ROC Form MGT-7 for {period}")
                .build();

        mockMvc.perform(post("/api/v1/compliance/rules")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ruleCode").value("ROC_MGT7_ANNUAL"))
                .andExpect(jsonPath("$.data.dueDay").value(29));
    }

    @Test
    @DisplayName("2. Create custom compliance obligation and update status")
    void testCreateAndCompleteObligation() throws Exception {
        CreateComplianceObligationRequest request = CreateComplianceObligationRequest.builder()
                .clientId(clientAbc.getId())
                .title("TDS Deposit Challan 281 for August 2026")
                .complianceType(ComplianceType.TDS)
                .period("2026-08")
                .dueDate(LocalDate.of(2026, 9, 7))
                .priority(CompliancePriority.HIGH)
                .assignedEmployeeId(employeeRahul.getId())
                .build();

        String response = mockMvc.perform(post("/api/v1/compliance/obligations")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("TDS Deposit Challan 281 for August 2026"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        String obligationId = objectMapper.readTree(response).path("data").path("id").asText();

        // Update status to COMPLETED
        mockMvc.perform(patch("/api/v1/compliance/obligations/" + obligationId + "/status")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateComplianceStatusRequest(ComplianceStatus.COMPLETED, "Tax deposited via net banking"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("3. Batch generate compliance obligations from rules")
    void testBatchGenerateCompliance() throws Exception {
        GenerateComplianceRequest req = GenerateComplianceRequest.builder()
                .period("2026-08")
                .build();

        mockMvc.perform(post("/api/v1/compliance/generate")
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].dueDate").value("2026-09-20"))
                .andExpect(jsonPath("$.data[0].assignedEmployeeName").value("Rahul Sharma"));
    }

    @Test
    @DisplayName("4. Convert compliance obligation to actionable Task")
    void testConvertObligationToTask() throws Exception {
        TenantContext.setTenantId(org1.getId());
        var ob = obligationRepository.save(ComplianceObligationEntity.builder()
                .clientId(clientAbc.getId())
                .title("GSTR-3B Monthly Return - ABC Traders")
                .complianceType(ComplianceType.GST)
                .period("2026-08")
                .dueDate(LocalDate.of(2026, 9, 20))
                .status(ComplianceStatus.PENDING)
                .priority(CompliancePriority.HIGH)
                .assignedEmployeeId(employeeRahul.getId())
                .build());
        TenantContext.clear();

        mockMvc.perform(post("/api/v1/compliance/obligations/" + ob.getId() + "/create-task")
                        .header("Authorization", adminToken1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("5. Executive Dashboard stats calculate correctly")
    void testGetDashboardStats() throws Exception {
        LocalDate today = LocalDate.now();

        TenantContext.setTenantId(org1.getId());
        try {
            // Due today
            obligationRepository.save(ComplianceObligationEntity.builder()
                    .clientId(clientAbc.getId())
                    .title("Due Today Compliance")
                    .complianceType(ComplianceType.GST)
                    .period("2026-08")
                    .dueDate(today)
                    .status(ComplianceStatus.PENDING)
                    .build());

            // Overdue
            obligationRepository.save(ComplianceObligationEntity.builder()
                    .clientId(clientAbc.getId())
                    .title("Overdue Compliance")
                    .complianceType(ComplianceType.ITR)
                    .period("2025-26")
                    .dueDate(today.minusDays(10))
                    .status(ComplianceStatus.OVERDUE)
                    .build());
        } finally {
            TenantContext.clear();
        }

        mockMvc.perform(get("/api/v1/compliance/dashboard/stats")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dueTodayCount").value(1))
                .andExpect(jsonPath("$.data.overdueCount").value(1))
                .andExpect(jsonPath("$.data.totalActiveCount").value(2));

        // Test Today endpoint
        mockMvc.perform(get("/api/v1/compliance/today")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // Test Overdue endpoint
        mockMvc.perform(get("/api/v1/compliance/overdue")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
