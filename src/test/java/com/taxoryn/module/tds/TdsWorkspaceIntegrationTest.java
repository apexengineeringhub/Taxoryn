package com.taxoryn.module.tds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.WorkItemEntity;
import com.taxoryn.module.task.model.WorkItemStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.repository.WorkItemRepository;
import com.taxoryn.module.tds.entity.TdsProfileEntity;
import com.taxoryn.module.tds.entity.TdsProfileEntity.DeductorType;
import com.taxoryn.module.tds.entity.TdsProfileEntity.TdsProfileStatus;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.timetracking.entity.TimeEntryEntity;
import com.taxoryn.module.timetracking.model.TimeEntryStatus;
import com.taxoryn.module.timetracking.repository.TimeEntryRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TdsWorkspaceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TdsProfileRepository tdsProfileRepository;

    @Autowired
    private ComplianceObligationRepository complianceObligationRepository;

    @Autowired
    private ComplianceWorkflowRepository complianceWorkflowRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity org;
    private LocationEntity loc;
    private UserEntity user;
    private ClientEntity client;
    private TdsProfileEntity tdsProfile;
    private String token;

    @BeforeEach
    void setUp() {
        cleanUp();

        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        org = organizationRepository.save(OrganizationEntity.builder()
                .name("Kothari & Associates - " + UUID.randomUUID())
                .legalName("Kothari & Associates LLP")
                .email("admin." + UUID.randomUUID() + "@kothari.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        loc = LocationEntity.builder()
                .name("Bengaluru Central")
                .code("BLR-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Bengaluru")
                .state("Karnataka")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc.setOrganizationId(org.getId());
        loc = locationRepository.save(loc);

        user = userRepository.save(UserEntity.builder()
                .email("partner." + UUID.randomUUID() + "@kothari.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Manoj")
                .lastName("Kothari")
                .organizationId(org.getId())
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        token = jwtTokenProvider.generateAccessToken(
                user.getId(),
                org.getId(),
                user.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "TDS_VIEW", "TDS_CREATE", "TDS_UPDATE", "CLIENT_VIEW")
        );

        // Client
        client = ClientEntity.builder()
                .displayName("Alpha Infotech Systems")
                .legalName("Alpha Infotech Systems Private Limited")
                .pan("AABCA9876C")
                .tan("BLRA98765B")
                .clientType(ClientType.PRIVATE_LIMITED)
                .locationId(loc.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(org.getId());
        client = clientRepository.save(client);

        // TDS Profile
        tdsProfile = TdsProfileEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .tan("BLRA98765B")
                .deductorType(DeductorType.COMPANY)
                .branchDivisionName("Main Delivery Center")
                .responsiblePersonName("Rajesh Kothari")
                .responsiblePersonDesignation("Finance Director")
                .responsiblePersonPan("ABCPK5678L")
                .responsiblePersonEmail("rajesh@alphainfotech.com")
                .active(true)
                .status(TdsProfileStatus.ACTIVE)
                .build();
        tdsProfile.setOrganizationId(org.getId());
        tdsProfile = tdsProfileRepository.save(tdsProfile);

        // Compliance Obligation
        ComplianceObligationEntity ob = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .tdsProfileId(tdsProfile.getId())
                .title("Form 26Q Quarterly Return Filing (Q1 2026-27)")
                .obligationType(ComplianceObligationType.TDS_RETURN)
                .periodLabel("2026-27-Q1")
                .financialYear("2026-27")
                .statutoryDueDate(LocalDate.of(2026, 7, 31))
                .status(ComplianceObligationStatus.IN_PROGRESS)
                .build();
        ob.setOrganizationId(org.getId());
        ob = complianceObligationRepository.save(ob);

        // Compliance Workflow
        ComplianceWorkflowEntity wf = ComplianceWorkflowEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .complianceObligationId(ob.getId())
                .tdsProfileId(tdsProfile.getId())
                .workflowType("TDS")
                .statutoryDueDate(LocalDate.of(2026, 7, 31))
                .workflowStatus(ComplianceWorkflowStatus.IN_PROGRESS)
                .build();
        wf.setOrganizationId(org.getId());
        wf = complianceWorkflowRepository.save(wf);

        // Work Item & Task
        WorkItemEntity wi = WorkItemEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .workflowId(wf.getId())
                .title("Form 26Q Challan Reconciliation & Deductee Mapping")
                .status(WorkItemStatus.IN_PROGRESS)
                .build();
        wi.setOrganizationId(org.getId());
        wi = workItemRepository.save(wi);

        TaskEntity task = TaskEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .workItemId(wi.getId())
                .title("Verify OLTAS BSR challan payment credits")
                .status(TaskEntity.TaskStatus.IN_PROGRESS)
                .build();
        task.setOrganizationId(org.getId());
        task = taskRepository.save(task);

        // Time Entry
        TimeEntryEntity te = TimeEntryEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .userId(user.getId())
                .workItemId(wi.getId())
                .taskId(task.getId())
                .entryDate(LocalDate.of(2026, 7, 20))
                .durationMinutes(75)
                .billable(true)
                .status(TimeEntryStatus.SUBMITTED)
                .build();
        te.setOrganizationId(org.getId());
        timeEntryRepository.save(te);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
        TenantContext.clear();
    }

    private void cleanUp() {
        TenantContext.clear();
        timeEntryRepository.deleteAll();
        taskRepository.deleteAll();
        workItemRepository.deleteAll();
        complianceWorkflowRepository.deleteAll();
        complianceObligationRepository.deleteAll();
        tdsProfileRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("TDS Workspace Aggregation: Successfully retrieve unified client TDS workspace")
    void testGetClientTdsWorkspace() throws Exception {
        mockMvc.perform(get("/api/v1/tds/clients/" + client.getId() + "/workspace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.client.displayName").value("Alpha Infotech Systems"))
                .andExpect(jsonPath("$.data.tan").value("BLRA98765B"))
                .andExpect(jsonPath("$.data.profile.deductorType").value("COMPANY"))
                .andExpect(jsonPath("$.data.activeObligations[0].title").value("Form 26Q Quarterly Return Filing (Q1 2026-27)"))
                .andExpect(jsonPath("$.data.currentWorkflows[0].workflowType").value("TDS"))
                .andExpect(jsonPath("$.data.linkedWorkItems[0].title").value("Form 26Q Challan Reconciliation & Deductee Mapping"))
                .andExpect(jsonPath("$.data.pendingTasks[0].title").value("Verify OLTAS BSR challan payment credits"))
                .andExpect(jsonPath("$.data.timeEntries[0].durationMinutes").value(75));
    }

    @Test
    @DisplayName("TDS Obligations, Workflows & Periods Endpoints: Dedicated endpoints return filtered lists")
    void testGetDedicatedTdsEndpoints() throws Exception {
        // 1. Obligations
        mockMvc.perform(get("/api/v1/tds/clients/" + client.getId() + "/obligations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].obligationType").value("TDS_RETURN"))
                .andExpect(jsonPath("$.data[0].periodLabel").value("2026-27-Q1"));

        // 2. Workflows
        mockMvc.perform(get("/api/v1/tds/clients/" + client.getId() + "/workflows")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].workflowStatus").value("IN_PROGRESS"));

        // 3. Periods
        mockMvc.perform(get("/api/v1/tds/clients/" + client.getId() + "/periods")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].financialYear").value("2026-27"))
                .andExpect(jsonPath("$.data[0].quarter").value("Q1"))
                .andExpect(jsonPath("$.data[0].statutoryDueDate").value("2026-07-31"));
    }

    @Test
    @DisplayName("Client 360 Integration: Client 360 includes TDS profile information")
    void testClient360IncludesTdsProfile() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + client.getId() + "/360")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.displayName").value("Alpha Infotech Systems"))
                .andExpect(jsonPath("$.data.tdsProfile.tan").value("BLRA98765B"))
                .andExpect(jsonPath("$.data.tdsProfile.deductorType").value("COMPANY"))
                .andExpect(jsonPath("$.data.tdsProfile.locationName").value("Bengaluru Central"))
                .andExpect(jsonPath("$.data.tdsProfile.active").value(true));
    }
}
