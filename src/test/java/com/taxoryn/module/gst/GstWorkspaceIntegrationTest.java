package com.taxoryn.module.gst;

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
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.gst.entity.GstRegistrationEntity;
import com.taxoryn.module.gst.model.GstFilingFrequency;
import com.taxoryn.module.gst.model.GstRegistrationStatus;
import com.taxoryn.module.gst.model.GstRegistrationType;
import com.taxoryn.module.gst.repository.GstRegistrationRepository;
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
public class GstWorkspaceIntegrationTest {

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
    private GstRegistrationRepository gstRegistrationRepository;

    @Autowired
    private ComplianceObligationRepository complianceObligationRepository;

    @Autowired
    private ComplianceWorkflowRepository complianceWorkflowRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DocumentRequestRepository documentRequestRepository;

    @Autowired
    private DocumentRepository documentRepository;

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
    private GstRegistrationEntity gstReg;
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
                .name("Verma Tax Advisors - " + UUID.randomUUID())
                .legalName("Verma Tax Advisors LLP")
                .email("admin." + UUID.randomUUID() + "@vermatax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        LocationEntity l = LocationEntity.builder()
                .name("Delhi HQ")
                .code("DEL-" + UUID.randomUUID().toString().substring(0, 4))
                .city("Delhi")
                .state("Delhi")
                .isHeadOffice(true)
                .isActive(true)
                .build();
        l.setOrganizationId(org.getId());
        loc = locationRepository.save(l);

        user = userRepository.save(UserEntity.builder()
                .organizationId(org.getId())
                .email("lead." + UUID.randomUUID() + "@vermatax.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Rohan")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        token = jwtTokenProvider.generateAccessToken(
                user.getId(),
                org.getId(),
                user.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "GST_VIEW", "GST_CREATE", "GST_UPDATE", "CLIENT_VIEW", "TASK_VIEW", "DOC_VIEW")
        );

        ClientEntity c = ClientEntity.builder()
                .displayName("Cosmo Retail Pvt Ltd")
                .legalName("Cosmo Retail Private Limited")
                .clientType(ClientType.COMPANY)
                .pan("AABCC1234A")
                .locationId(loc.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(org.getId());
        client = clientRepository.save(c);

        // 1. GST Registration
        GstRegistrationEntity reg = GstRegistrationEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .gstin("07AABCC1234A1Z8")
                .legalName("Cosmo Retail Private Limited")
                .tradeName("Cosmo Retail")
                .registrationType(GstRegistrationType.REGULAR)
                .registrationStatus(GstRegistrationStatus.ACTIVE)
                .filingFrequency(GstFilingFrequency.MONTHLY)
                .stateCode("07")
                .build();
        reg.setOrganizationId(org.getId());
        gstReg = gstRegistrationRepository.save(reg);

        // 2. GST Compliance Obligations
        ComplianceObligationEntity ob1 = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .gstRegistrationId(gstReg.getId())
                .title("GSTR-1 Monthly Return for July 2026")
                .obligationType(ComplianceObligationType.GSTR_1)
                .periodLabel("July 2026")
                .statutoryDueDate(LocalDate.of(2026, 8, 11))
                .status(ComplianceObligationStatus.IN_PROGRESS)
                .build();
        ob1.setOrganizationId(org.getId());
        ob1 = complianceObligationRepository.save(ob1);

        ComplianceObligationEntity ob2 = ComplianceObligationEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .gstRegistrationId(gstReg.getId())
                .title("GSTR-3B Monthly Return for July 2026")
                .obligationType(ComplianceObligationType.GSTR_3B)
                .periodLabel("July 2026")
                .statutoryDueDate(LocalDate.of(2026, 8, 20))
                .status(ComplianceObligationStatus.UPCOMING)
                .build();
        ob2.setOrganizationId(org.getId());
        ob2 = complianceObligationRepository.save(ob2);

        // 3. GST Compliance Workflow
        ComplianceWorkflowEntity wf = ComplianceWorkflowEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .complianceObligationId(ob1.getId())
                .gstRegistrationId(gstReg.getId())
                .workflowStatus(ComplianceWorkflowStatus.IN_PROGRESS)
                .statutoryDueDate(LocalDate.of(2026, 8, 11))
                .targetDate(LocalDate.of(2026, 8, 9))
                .build();
        wf.setOrganizationId(org.getId());
        wf = complianceWorkflowRepository.save(wf);

        // 4. Work Item & Task
        WorkItemEntity wi = WorkItemEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .workflowId(wf.getId())
                .title("GSTR-1 Sales Invoices Reconciliation")
                .status(WorkItemStatus.IN_PROGRESS)
                .build();
        wi.setOrganizationId(org.getId());
        wi = workItemRepository.save(wi);

        TaskEntity task = TaskEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .workItemId(wi.getId())
                .title("Reconcile B2B Credit Notes")
                .status(TaskEntity.TaskStatus.IN_PROGRESS)
                .build();
        task.setOrganizationId(org.getId());
        task = taskRepository.save(task);

        // 5. Time Entry
        TimeEntryEntity te = TimeEntryEntity.builder()
                .clientId(client.getId())
                .locationId(loc.getId())
                .userId(user.getId())
                .workItemId(wi.getId())
                .taskId(task.getId())
                .entryDate(LocalDate.of(2026, 8, 5))
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
        timeEntryRepository.deleteAll();
        taskRepository.deleteAll();
        workItemRepository.deleteAll();
        complianceWorkflowRepository.deleteAll();
        complianceObligationRepository.deleteAll();
        documentRequestRepository.deleteAll();
        documentRepository.deleteAll();
        gstRegistrationRepository.deleteAll();
        clientRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Retrieve aggregated GST Workspace for client")
    void testGetClientGstWorkspace() throws Exception {
        mockMvc.perform(get("/api/v1/gst/clients/" + client.getId() + "/workspace")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.client.displayName").value("Cosmo Retail Pvt Ltd"))
                .andExpect(jsonPath("$.data.registrations.length()").value(1))
                .andExpect(jsonPath("$.data.registrations[0].gstin").value("07AABCC1234A1Z8"))
                .andExpect(jsonPath("$.data.activeObligations.length()").value(2))
                .andExpect(jsonPath("$.data.currentWorkflows.length()").value(1))
                .andExpect(jsonPath("$.data.linkedWorkItems.length()").value(1))
                .andExpect(jsonPath("$.data.linkedWorkItems[0].title").value("GSTR-1 Sales Invoices Reconciliation"))
                .andExpect(jsonPath("$.data.pendingTasks.length()").value(1))
                .andExpect(jsonPath("$.data.timeEntries.length()").value(1));
    }

    @Test
    @DisplayName("Retrieve GST obligations and workflows via dedicated endpoints")
    void testGetGstObligationsAndWorkflows() throws Exception {
        // GST Obligations
        mockMvc.perform(get("/api/v1/gst/clients/" + client.getId() + "/obligations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));

        // GST Workflows
        mockMvc.perform(get("/api/v1/gst/clients/" + client.getId() + "/workflows")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].workflowStatus").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Client 360 exposes GST registrations when GST module is enabled")
    void testClient360GstIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + client.getId() + "/360")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.client.displayName").value("Cosmo Retail Pvt Ltd"))
                .andExpect(jsonPath("$.data.gstRegistrations.length()").value(1))
                .andExpect(jsonPath("$.data.gstRegistrations[0].gstin").value("07AABCC1234A1Z8"));
    }
}
