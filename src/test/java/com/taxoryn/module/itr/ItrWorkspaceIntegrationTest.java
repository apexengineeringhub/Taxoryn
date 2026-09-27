package com.taxoryn.module.itr;

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
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrProfileStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ResidentialStatus;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
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
public class ItrWorkspaceIntegrationTest {

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
    private ItrProfileRepository itrProfileRepository;

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
    private ItrProfileEntity itrProfile;
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
                .name("Verma Tax Consultants - " + UUID.randomUUID())
                .legalName("Verma Tax Consultants LLP")
                .email("admin." + UUID.randomUUID() + "@vermatax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        loc = LocationEntity.builder()
                .name("New Delhi Branch")
                .code("DEL-" + UUID.randomUUID().toString().substring(0, 4))
                .city("New Delhi")
                .state("Delhi")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        loc.setOrganizationId(org.getId());
        loc = locationRepository.save(loc);

        user = userRepository.save(UserEntity.builder()
                .email("partner." + UUID.randomUUID() + "@vermatax.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Rajiv")
                .lastName("Verma")
                .organizationId(org.getId())
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        token = jwtTokenProvider.generateAccessToken(
                user.getId(),
                org.getId(),
                user.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "ITR_VIEW", "ITR_CREATE", "ITR_UPDATE", "CLIENT_VIEW")
        );

        // Client
        client = ClientEntity.builder()
                .displayName("Omega Enterprises")
                .legalName("Omega Enterprises LLP")
                .pan("AAAFO1234M")
                .clientType(ClientType.LLP)
                .locationId(loc.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        client.setOrganizationId(org.getId());
        client = clientRepository.save(client);

        // ITR Profile
        itrProfile = ItrProfileEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .pan("AAAFO1234M")
                .taxpayerType(TaxpayerType.LLP)
                .defaultItrType(ItrType.ITR_5)
                .applicableReturnType(ItrType.ITR_5)
                .defaultAssessmentYear("2026-27")
                .assessmentCategory("REGULAR")
                .residentialStatus(ResidentialStatus.RESIDENT)
                .active(true)
                .status(ItrProfileStatus.ACTIVE)
                .build();
        itrProfile.setOrganizationId(org.getId());
        itrProfile = itrProfileRepository.save(itrProfile);

        // Compliance Obligation
        ComplianceObligationEntity ob = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .itrProfileId(itrProfile.getId())
                .title("ITR-5 Annual Return Filing (AY 2026-27)")
                .obligationType(ComplianceObligationType.ITR_FILING)
                .periodLabel("2026-27")
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .statutoryDueDate(LocalDate.of(2026, 10, 31))
                .status(ComplianceObligationStatus.IN_PROGRESS)
                .build();
        ob.setOrganizationId(org.getId());
        ob = complianceObligationRepository.save(ob);

        // Compliance Workflow
        ComplianceWorkflowEntity wf = ComplianceWorkflowEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .complianceObligationId(ob.getId())
                .itrProfileId(itrProfile.getId())
                .workflowType("ITR")
                .statutoryDueDate(LocalDate.of(2026, 10, 31))
                .workflowStatus(ComplianceWorkflowStatus.IN_PROGRESS)
                .build();
        wf.setOrganizationId(org.getId());
        wf = complianceWorkflowRepository.save(wf);

        // Work Item & Task
        WorkItemEntity wi = WorkItemEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .workflowId(wf.getId())
                .title("ITR-5 Financial Statements & Computation")
                .status(WorkItemStatus.IN_PROGRESS)
                .build();
        wi.setOrganizationId(org.getId());
        wi = workItemRepository.save(wi);

        TaskEntity task = TaskEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .workItemId(wi.getId())
                .title("Verify Form 26AS & AIS tax credits")
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
                .entryDate(LocalDate.of(2026, 8, 10))
                .durationMinutes(90)
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
        itrProfileRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("ITR Workspace Aggregation: Successfully retrieve unified client ITR workspace")
    void testGetClientItrWorkspace() throws Exception {
        mockMvc.perform(get("/api/v1/itr/clients/" + client.getId() + "/workspace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.client.displayName").value("Omega Enterprises"))
                .andExpect(jsonPath("$.data.pan").value("AAAFO1234M"))
                .andExpect(jsonPath("$.data.profile.taxpayerType").value("LLP"))
                .andExpect(jsonPath("$.data.defaultAssessmentYear").value("2026-27"))
                .andExpect(jsonPath("$.data.activeObligations[0].title").value("ITR-5 Annual Return Filing (AY 2026-27)"))
                .andExpect(jsonPath("$.data.currentWorkflows[0].workflowType").value("ITR"))
                .andExpect(jsonPath("$.data.linkedWorkItems[0].title").value("ITR-5 Financial Statements & Computation"))
                .andExpect(jsonPath("$.data.pendingTasks[0].title").value("Verify Form 26AS & AIS tax credits"))
                .andExpect(jsonPath("$.data.timeEntries[0].durationMinutes").value(90));
    }

    @Test
    @DisplayName("ITR Obligations, Workflows & AY Endpoints: Dedicated endpoints return filtered lists")
    void testGetDedicatedItrEndpoints() throws Exception {
        // 1. Obligations
        mockMvc.perform(get("/api/v1/itr/clients/" + client.getId() + "/obligations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].obligationType").value("ITR_FILING"))
                .andExpect(jsonPath("$.data[0].periodLabel").value("2026-27"));

        // 2. Workflows
        mockMvc.perform(get("/api/v1/itr/clients/" + client.getId() + "/workflows")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].workflowStatus").value("IN_PROGRESS"));

        // 3. Assessment Years
        mockMvc.perform(get("/api/v1/itr/clients/" + client.getId() + "/assessment-years")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("2026-27"));
    }

    @Test
    @DisplayName("Client 360 Integration: Client 360 includes ITR profile information")
    void testClient360IncludesItrProfile() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + client.getId() + "/360")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.client.displayName").value("Omega Enterprises"))
                .andExpect(jsonPath("$.data.itrProfile.pan").value("AAAFO1234M"))
                .andExpect(jsonPath("$.data.itrProfile.taxpayerType").value("LLP"))
                .andExpect(jsonPath("$.data.itrProfile.defaultAssessmentYear").value("2026-27"));
    }
}
