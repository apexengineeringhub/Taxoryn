package com.taxoryn.module.worktemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.worktemplate.entity.WorkInstanceEntity;
import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import com.taxoryn.module.worktemplate.model.WorkInventoryStatus;
import com.taxoryn.module.worktemplate.model.WorkInventoryType;
import com.taxoryn.module.worktemplate.repository.WorkInstanceRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkInventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EngagementRepository engagementRepository;

    @Autowired
    private WorkInstanceRepository workInstanceRepository;

    @Autowired
    private ComplianceObligationRepository obligationRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity userA;
    private UserEntity userB;
    private String tokenA;
    private String tokenB;
    private ClientEntity clientA;
    private ClientEntity clientB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        // Setup Tenant A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax Practice Org - " + UUID.randomUUID())
                .legalName("Alpha Tax Advisory LLP")
                .email("admin." + UUID.randomUUID() + "@alphatax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        userA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("admin." + UUID.randomUUID() + "@alpha-practice.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Alpha")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(
                userA.getId(),
                orgA.getId(),
                userA.getEmail(),
                Set.of("ORG_ADMIN", "PRACTICE_ADMIN"),
                Set.of("CLIENT_VIEW", "TASK_VIEW", "COMPLIANCE_VIEW", "ROLE_ORG_ADMIN")
        );

        ClientEntity clientAEntity = ClientEntity.builder()
                .displayName("Alpha Client Industries")
                .legalName("Alpha Client Industries Private Limited")
                .pan("AAACA1234A")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build();
        clientAEntity.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(clientAEntity);

        TenantContext.clear();

        // Setup Tenant B (for multi-tenant isolation testing)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Advisory Org - " + UUID.randomUUID())
                .legalName("Beta Advisory LLP")
                .email("admin." + UUID.randomUUID() + "@betatax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgB.getId());

        userB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin." + UUID.randomUUID() + "@beta-practice.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Beta")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                userB.getId(),
                orgB.getId(),
                userB.getEmail(),
                Set.of("ORG_ADMIN", "PRACTICE_ADMIN"),
                Set.of("CLIENT_VIEW", "TASK_VIEW", "COMPLIANCE_VIEW", "ROLE_ORG_ADMIN")
        );

        ClientEntity clientBEntity = ClientEntity.builder()
                .displayName("Beta Client Corp")
                .legalName("Beta Client Corp LLP")
                .pan("BBBCB5678B")
                .clientType(ClientType.LLP)
                .status(ClientStatus.ACTIVE)
                .build();
        clientBEntity.setOrganizationId(orgB.getId());
        clientB = clientRepository.save(clientBEntity);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Unified Work Inventory queries Work Instances, Obligations, and Standalone Tasks correctly without duplicating covered obligations")
    void testUnifiedWorkInventoryProjection() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // 1. Create Engagement for Client A
        EngagementEntity engagement = EngagementEntity.builder()
                .clientId(clientA.getId())
                .name("FY2026 Retainer Tax Services")
                .status(EngagementStatus.ACTIVE)
                .priority(EngagementPriority.HIGH)
                .build();
        engagement.setOrganizationId(orgA.getId());
        engagement = engagementRepository.save(engagement);

        // 2. Create Statutory Compliance Obligation 1 (will be linked to a WorkInstance)
        ComplianceObligationEntity obligationCovered = ComplianceObligationEntity.builder()
                .clientId(clientA.getId())
                .title("GST GSTR-3B Monthly Return - April 2026")
                .ruleCode("GST_GSTR3B_MONTHLY")
                .statutoryDueDate(LocalDate.of(2026, 5, 20))
                .status(ComplianceObligationStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .build();
        obligationCovered.setOrganizationId(orgA.getId());
        obligationCovered = obligationRepository.save(obligationCovered);

        // 3. Create Work Instance referencing obligationCovered
        WorkInstanceEntity workInstance = WorkInstanceEntity.builder()
                .engagementId(engagement.getId())
                .title("GSTR-3B Filing Occurrence — April 2026")
                .periodStart(LocalDate.of(2026, 4, 1))
                .periodEnd(LocalDate.of(2026, 4, 30))
                .dueDate(LocalDate.of(2026, 5, 20))
                .status(WorkInstanceStatus.IN_PROGRESS)
                .assignedUserId(userA.getId())
                .complianceObligationId(obligationCovered.getId())
                .build();
        workInstance.setOrganizationId(orgA.getId());
        workInstance = workInstanceRepository.save(workInstance);

        // Create 2 subtasks under this Work Instance
        TaskEntity subtask1 = TaskEntity.builder()
                .workInstanceId(workInstance.getId())
                .clientId(clientA.getId())
                .title("Reconcile Sales Invoices with GSTR-1")
                .status(TaskStatus.COMPLETED)
                .build();
        subtask1.setOrganizationId(orgA.getId());
        taskRepository.save(subtask1);

        TaskEntity subtask2 = TaskEntity.builder()
                .workInstanceId(workInstance.getId())
                .clientId(clientA.getId())
                .title("Compute Eligible ITC under Section 16")
                .status(TaskStatus.TODO)
                .build();
        subtask2.setOrganizationId(orgA.getId());
        taskRepository.save(subtask2);

        // 4. Create Standalone Statutory Compliance Obligation 2 (NOT linked to any work instance)
        ComplianceObligationEntity obligationStandalone = ComplianceObligationEntity.builder()
                .clientId(clientA.getId())
                .title("TDS 26Q Quarterly Return - Q1 FY26")
                .ruleCode("TDS_26Q_Q1")
                .statutoryDueDate(LocalDate.of(2026, 7, 31))
                .status(ComplianceObligationStatus.UPCOMING)
                .priority(TaskPriority.MEDIUM)
                .build();
        obligationStandalone.setOrganizationId(orgA.getId());
        obligationStandalone = obligationRepository.save(obligationStandalone);

        // 5. Create Standalone Task (not tied to any work instance or obligation)
        TaskEntity standaloneTask = TaskEntity.builder()
                .clientId(clientA.getId())
                .title("Collect physical board resolution for authorized signatory")
                .dueDate(LocalDate.of(2026, 6, 15))
                .status(TaskStatus.TODO)
                .priority(TaskPriority.LOW)
                .assignedTo(userA.getId())
                .build();
        standaloneTask.setOrganizationId(orgA.getId());
        taskRepository.save(standaloneTask);

        TenantContext.clear();

        // Query Unified Work Inventory for Tenant A
        mockMvc.perform(get("/api/v1/work/inventory")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(3)) // 1 WorkInstance + 1 Standalone Obligation + 1 Standalone Task (Covered Obligation is suppressed!)
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                // Verify Work Instance entry
                .andExpect(jsonPath("$.data.content[?(@.id == '" + workInstance.getId() + "')].workType").value(WorkInventoryType.WORK_INSTANCE.name()))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + workInstance.getId() + "')].totalTasks").value(2))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + workInstance.getId() + "')].completedTasks").value(1))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + workInstance.getId() + "')].nextActionableStep").value("Compute Eligible ITC under Section 16"))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + workInstance.getId() + "')].clientName").value("Alpha Client Industries"))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + workInstance.getId() + "')].engagementName").value("FY2026 Retainer Tax Services"))
                // Verify Standalone Obligation entry
                .andExpect(jsonPath("$.data.content[?(@.id == '" + obligationStandalone.getId() + "')].workType").value(WorkInventoryType.COMPLIANCE_OBLIGATION.name()))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + obligationStandalone.getId() + "')].ruleCode").value("TDS_26Q_Q1"))
                .andExpect(jsonPath("$.data.content[?(@.id == '" + obligationStandalone.getId() + "')].normalizedStatus").value(WorkInventoryStatus.NOT_STARTED.name()))
                // Verify Standalone Task entry
                .andExpect(jsonPath("$.data.content[?(@.title == 'Collect physical board resolution for authorized signatory')].workType").value(WorkInventoryType.TASK.name()));
    }

    @Test
    @DisplayName("Tenant Isolation: Work inventory strictly segregates items across different practice organizations")
    void testTenantIsolation() throws Exception {
        TenantContext.setTenantId(orgB.getId());

        // Create an item for Tenant B
        EngagementEntity engagementB = EngagementEntity.builder()
                .clientId(clientB.getId())
                .name("Beta Advisory Contract")
                .status(EngagementStatus.ACTIVE)
                .build();
        engagementB.setOrganizationId(orgB.getId());
        engagementB = engagementRepository.save(engagementB);

        WorkInstanceEntity workInstanceB = WorkInstanceEntity.builder()
                .engagementId(engagementB.getId())
                .title("Beta Private Tax Review")
                .periodStart(LocalDate.of(2026, 4, 1))
                .periodEnd(LocalDate.of(2026, 4, 30))
                .dueDate(LocalDate.of(2026, 5, 10))
                .status(WorkInstanceStatus.IN_PROGRESS)
                .build();
        workInstanceB.setOrganizationId(orgB.getId());
        workInstanceB = workInstanceRepository.save(workInstanceB);

        TenantContext.clear();

        // Tenant A must NOT see Tenant B's work instance
        mockMvc.perform(get("/api/v1/work/inventory")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + workInstanceB.getId() + "')]").doesNotExist())
                .andExpect(jsonPath("$.data.content[?(@.title == 'Beta Private Tax Review')]").doesNotExist());

        // Tenant B must see Tenant B's work instance
        mockMvc.perform(get("/api/v1/work/inventory")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + workInstanceB.getId() + "')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.title == 'Beta Private Tax Review')].clientName").value("Beta Client Corp"));
    }

    @Test
    @DisplayName("Filters: Filtering work inventory by workType, blockedOnly, and search behaves correctly")
    void testWorkInventoryFilters() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        EngagementEntity engagement = EngagementEntity.builder()
                .clientId(clientA.getId())
                .name("Filter Test Retainer")
                .status(EngagementStatus.ACTIVE)
                .build();
        engagement.setOrganizationId(orgA.getId());
        engagement = engagementRepository.save(engagement);

        // 1. Blocked work instance
        WorkInstanceEntity blockedWi = WorkInstanceEntity.builder()
                .engagementId(engagement.getId())
                .title("Blocked GST Assessment Work")
                .periodStart(LocalDate.of(2026, 4, 1))
                .periodEnd(LocalDate.of(2026, 4, 30))
                .status(WorkInstanceStatus.ON_HOLD)
                .notes("Waiting for client bank statement")
                .build();
        blockedWi.setOrganizationId(orgA.getId());
        workInstanceRepository.save(blockedWi);

        // 2. Normal work instance
        WorkInstanceEntity activeWi = WorkInstanceEntity.builder()
                .engagementId(engagement.getId())
                .title("Active Income Tax Audit")
                .periodStart(LocalDate.of(2026, 5, 1))
                .periodEnd(LocalDate.of(2026, 5, 31))
                .status(WorkInstanceStatus.IN_PROGRESS)
                .build();
        activeWi.setOrganizationId(orgA.getId());
        workInstanceRepository.save(activeWi);

        TenantContext.clear();

        // Test filter blockedOnly=true
        mockMvc.perform(get("/api/v1/work/inventory")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("blockedOnly", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Blocked GST Assessment Work"))
                .andExpect(jsonPath("$.data.content[0].blocked").value(true))
                .andExpect(jsonPath("$.data.content[0].blockedReason").value("Waiting for client bank statement"));

        // Test filter search keyword
        mockMvc.perform(get("/api/v1/work/inventory")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("search", "Income Tax Audit")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Active Income Tax Audit"));
    }
}
