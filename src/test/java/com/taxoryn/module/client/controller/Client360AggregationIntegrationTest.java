package com.taxoryn.module.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientNoteEntity;
import com.taxoryn.module.client.entity.ClientNoteEntity.NoteType;
import com.taxoryn.module.client.repository.ClientNoteRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Client360AggregationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientNoteRepository clientNoteRepository;

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

    @Autowired
    private GstProfileRepository gstProfileRepository;

    @Autowired
    private GstReturnFilingRepository gstReturnFilingRepository;

    @Autowired
    private ItrProfileRepository itrProfileRepository;

    @Autowired
    private ItrReturnRepository itrReturnRepository;

    @Autowired
    private TdsProfileRepository tdsProfileRepository;

    @Autowired
    private TdsReturnRepository tdsReturnRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentRequestRepository documentRequestRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private TaxNoticeRepository taxNoticeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private String adminToken1;
    private UserEntity staffUser1;
    private String staffToken1;
    private EmployeeEntity employee1;
    private ClientEntity client1;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        taskRepository.deleteAll();
        clientNoteRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization 1 & 2
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Consultants")
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

        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .code("STAFF")
                .name("Staff Member")
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
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "BILLING_VIEW", "DOC_VIEW", "TASK_VIEW", "NOTICE_VIEW")
        );

        staffUser1 = userRepository.save(UserEntity.builder()
                .email("staff@apextax.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Priya")
                .lastName("Patel")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        // Staff without BILLING_VIEW or admin role
        staffToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                staffUser1.getId(),
                org1.getId(),
                staffUser1.getEmail(),
                Set.of("STAFF"),
                Set.of("CLIENT_VIEW", "DOC_VIEW", "TASK_VIEW")
        );

        // 2. Create Employees in Org 1
        employee1 = EmployeeEntity.builder()
                .userId(adminUser1.getId())
                .employeeCode("EMP-001")
                .firstName("Vikram")
                .lastName("Sharma")
                .email("vikram@apextax.com")
                .department("Taxation")
                .designation("Senior Manager")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employee1 = employeeRepository.save(employee1);

        EmployeeEntity employee2 = EmployeeEntity.builder()
                .userId(staffUser1.getId())
                .employeeCode("EMP-002")
                .firstName("Priya")
                .lastName("Patel")
                .email("priya@apextax.com")
                .department("Taxation")
                .designation("Tax Associate")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employee2 = employeeRepository.save(employee2);

        // 3. Create Sample Client in Org 1 assigned to Employee 2 (Staff)
        client1 = ClientEntity.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Zenith Infotech Pvt Ltd")
                .legalName("Zenith Information Technologies Private Limited")
                .tradeName("Zenith Software")
                .pan("AAACZ1234D")
                .gstin("27AAACZ1234D1Z8")
                .tan("MUMZ12345A")
                .cin("U72200MH2018PTC312345")
                .email("finance@zenithinfo.com")
                .phone("+919811122233")
                .city("Mumbai")
                .state("Maharashtra")
                .assignedEmployeeId(employee2.getId())
                .status(ClientStatus.ACTIVE)
                .build();
        client1 = clientRepository.save(client1);

        // Add Notes
        ClientNoteEntity note = new ClientNoteEntity();
        note.setOrganizationId(org1.getId());
        note.setClientId(client1.getId());
        note.setAuthorId(adminUser1.getId());
        note.setAuthorName("Rajesh Verma");
        note.setTitle("Client Urgent Request");
        note.setNoteType(NoteType.GENERAL);
        note.setContent("Client requested urgent advance tax assessment assistance.");
        clientNoteRepository.save(note);

        // Add Tasks
        TaskEntity task = TaskEntity.builder()
                .clientId(client1.getId())
                .title("Prepare Q2 Advance Tax Calculation")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(LocalDate.now().plusDays(5))
                .build();
        task.setOrganizationId(org1.getId());
        taskRepository.save(task);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        taskRepository.deleteAll();
        clientNoteRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/v1/clients/{clientId}/overview returns comprehensive 360 aggregation for Org Admin")
    void testGetClientOverview_AsAdmin_Success() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientId}/overview", client1.getId())
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.client.id").value(client1.getId().toString()))
                .andExpect(jsonPath("$.data.client.displayName").value("Zenith Infotech Pvt Ltd"))
                .andExpect(jsonPath("$.data.statutory.pan").value("AAACZ1234D"))
                .andExpect(jsonPath("$.data.statutory.gstin").value("27AAACZ1234D1Z8"))
                .andExpect(jsonPath("$.data.statutory.tan").value("MUMZ12345A"))
                .andExpect(jsonPath("$.data.client.assignedEmployeeName").value("Priya Patel"))
                .andExpect(jsonPath("$.data.taskSummary.totalTasks").value(1))
                .andExpect(jsonPath("$.data.taskSummary.inProgressTasks").value(1))
                .andExpect(jsonPath("$.data.recentNotes[0].content").value("Client requested urgent advance tax assessment assistance."))
                .andExpect(jsonPath("$.data.billingSummary").exists())
                .andExpect(jsonPath("$.data.complianceSummary").exists())
                .andExpect(jsonPath("$.data.documentsSummary").exists())
                .andExpect(jsonPath("$.data.docRequestsSummary").exists())
                .andExpect(jsonPath("$.data.noticeSummary").exists());
    }

    @Test
    @DisplayName("GET /api/v1/clients/{clientId}/360 alias returns same 360 overview")
    void testGetClient360Alias_Success() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientId}/360", client1.getId())
                        .header("Authorization", adminToken1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.client.id").value(client1.getId().toString()))
                .andExpect(jsonPath("$.data.client.displayName").value("Zenith Infotech Pvt Ltd"));
    }

    @Test
    @DisplayName("GET /api/v1/clients/{clientId}/overview redacts billingSummary for non-billing staff")
    void testGetClientOverview_NonBillingStaff_RedactsBilling() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientId}/overview", client1.getId())
                        .header("Authorization", staffToken1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.client.id").value(client1.getId().toString()))
                .andExpect(jsonPath("$.data.billingSummary").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/clients/{clientId}/overview from different tenant returns 404")
    void testGetClientOverview_CrossTenant_Blocked() throws Exception {
        String otherOrgToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(),
                org2.getId(),
                "admin@globaltax.com",
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW")
        );

        mockMvc.perform(get("/api/v1/clients/{clientId}/overview", client1.getId())
                        .header("Authorization", otherOrgToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
