package com.taxoryn.module.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientAssignmentRole;
import com.taxoryn.module.client.entity.ClientBranchEntity;
import com.taxoryn.module.client.entity.ClientBranchType;
import com.taxoryn.module.client.entity.ClientContactEntity;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.entity.ClientServiceEntity;
import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.client.entity.ClientUserAssignmentEntity;
import com.taxoryn.module.client.entity.ContactRole;
import com.taxoryn.module.client.repository.ClientBranchRepository;
import com.taxoryn.module.client.repository.ClientContactRepository;
import com.taxoryn.module.client.repository.ClientRelationshipRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.client.repository.ClientUserAssignmentRepository;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeRisk;
import com.taxoryn.module.notice.enums.NoticeStatus;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Client360ConnectedViewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientServiceRepository clientServiceRepository;

    @Autowired
    private ClientUserAssignmentRepository clientUserAssignmentRepository;

    @Autowired
    private ClientContactRepository clientContactRepository;

    @Autowired
    private ClientBranchRepository clientBranchRepository;

    @Autowired
    private ClientRelationshipRepository clientRelationshipRepository;

    @Autowired
    private EngagementRepository engagementRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DocumentRequestRepository documentRequestRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private TaxNoticeRepository taxNoticeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationModuleRepository organizationModuleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private UserEntity staffUser1;
    private UserEntity adminUser2;
    private String adminToken1;
    private String staffToken1;
    private String adminToken2;

    private ClientEntity client1;
    private ClientEntity client2;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        auditLogRepository.deleteAll();
        taxNoticeRepository.deleteAll();
        invoiceRepository.deleteAll();
        documentRequestRepository.deleteAll();
        taskRepository.deleteAll();
        engagementRepository.deleteAll();
        clientRelationshipRepository.deleteAll();
        clientContactRepository.deleteAll();
        clientBranchRepository.deleteAll();
        clientServiceRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization 1
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA & Advisors LLP")
                .legalName("Apex Chartered Accountants LLP")
                .email("admin@apexca.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Create Organization 2
        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Other Tax Firm")
                .legalName("Other Tax Firm Pvt Ltd")
                .email("admin@othertax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 3. Roles
        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity staffRole = roleRepository.save(RoleEntity.builder()
                .code("STAFF")
                .name("Practice Staff")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        TenantContext.setTenantId(org1.getId());

        OrganizationModuleEntity mod1 = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build();
        mod1.setOrganizationId(org1.getId());
        organizationModuleRepository.save(mod1);

        OrganizationModuleEntity modBilling = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.BILLING)
                .enabled(true)
                .build();
        modBilling.setOrganizationId(org1.getId());
        organizationModuleRepository.save(modBilling);

        // 4. Admin User
        adminUser1 = UserEntity.builder()
                .email("partner@apexca.com")
                .firstName("Rajesh")
                .lastName("Sharma")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        adminUser1.setOrganizationId(org1.getId());
        adminUser1 = userRepository.save(adminUser1);

        staffUser1 = UserEntity.builder()
                .email("staff@apexca.com")
                .firstName("Rohan")
                .lastName("Staff")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(staffRole)))
                .build();
        staffUser1.setOrganizationId(org1.getId());
        staffUser1 = userRepository.save(staffUser1);

        adminToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser1.getId(),
                org1.getId(),
                adminUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_READ", "CLIENT_WRITE", "CLIENT_CREATE", "CLIENT_UPDATE", "BILLING_VIEW", "BILLING_READ")
        );

        staffToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                staffUser1.getId(),
                org1.getId(),
                staffUser1.getEmail(),
                Set.of("STAFF"),
                Set.of("CLIENT_VIEW", "CLIENT_READ")
        );

        // Org 2 Setup
        TenantContext.setTenantId(org2.getId());

        OrganizationModuleEntity mod2 = OrganizationModuleEntity.builder()
                .moduleCode(ProductModuleCode.CLIENTS)
                .enabled(true)
                .build();
        mod2.setOrganizationId(org2.getId());
        organizationModuleRepository.save(mod2);

        adminUser2 = UserEntity.builder()
                .email("admin@othertax.com")
                .firstName("Vikram")
                .lastName("Other")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build();
        adminUser2.setOrganizationId(org2.getId());
        adminUser2 = userRepository.save(adminUser2);

        adminToken2 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser2.getId(),
                org2.getId(),
                adminUser2.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_READ")
        );

        // Create Clients
        TenantContext.setTenantId(org1.getId());
        ClientEntity c1 = ClientEntity.builder()
                .displayName("Nexus Tech Solutions Private Limited")
                .legalName("Nexus Tech Solutions Private Limited")
                .clientCode("NEX-001")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .pan("AAACA1234A")
                .gstin("27AAACA1234A1Z5")
                .tan("PNEB12345C")
                .cin("U72900PN2020PTC123456")
                .email("finance@nexustech.com")
                .phone("9876543210")
                .city("Pune")
                .state("Maharashtra")
                .stateCode("27")
                .pincode("411001")
                .businessActivity("Information Technology Software Services")
                .industry("IT / SaaS")
                .businessScale("MEDIUM")
                .dateOfIncorporation(LocalDate.of(2020, 1, 15))
                .build();
        c1.setOrganizationId(org1.getId());
        client1 = clientRepository.save(c1);

        ClientUserAssignmentEntity assignment = ClientUserAssignmentEntity.builder()
                .clientId(client1.getId())
                .userId(staffUser1.getId())
                .assignmentRole(ClientAssignmentRole.PRIMARY)
                .primaryResponsible(true)
                .active(true)
                .assignedAt(java.time.Instant.now())
                .build();
        assignment.setOrganizationId(org1.getId());
        clientUserAssignmentRepository.save(assignment);

        TenantContext.setTenantId(org2.getId());
        ClientEntity c2 = ClientEntity.builder()
                .displayName("Foreign Corp B")
                .legalName("Foreign Corp B Pvt Ltd")
                .clientCode("FCB-001")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build();
        c2.setOrganizationId(org2.getId());
        client2 = clientRepository.save(c2);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        auditLogRepository.deleteAll();
        taxNoticeRepository.deleteAll();
        invoiceRepository.deleteAll();
        documentRequestRepository.deleteAll();
        taskRepository.deleteAll();
        engagementRepository.deleteAll();
        clientRelationshipRepository.deleteAll();
        clientContactRepository.deleteAll();
        clientBranchRepository.deleteAll();
        clientUserAssignmentRepository.deleteAll();
        clientServiceRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationModuleRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @DisplayName("Connected View 360: Full aggregation of identity, services, work, compliance, billing, contacts, and intelligence")
    void testClient360ConnectedViewAggregation() throws Exception {
        TenantContext.setTenantId(org1.getId());

        // 1. Add Contacts
        ClientContactEntity contact = ClientContactEntity.builder()
                .clientId(client1.getId())
                .firstName("Vikram")
                .lastName("Mehta")
                .displayName("Vikram Mehta")
                .designation("Managing Director")
                .email("vikram@nexustech.com")
                .phone("9876543211")
                .contactRole(ContactRole.AUTHORIZED_REPRESENTATIVE)
                .primaryContact(true)
                .active(true)
                .build();
        contact.setOrganizationId(org1.getId());
        clientContactRepository.save(contact);

        // 2. Add Branches
        ClientBranchEntity branch = ClientBranchEntity.builder()
                .clientId(client1.getId())
                .branchName("Mumbai Registered Office")
                .branchCode("MUM-HQ")
                .branchType(ClientBranchType.REGISTERED_OFFICE)
                .city("Mumbai")
                .state("Maharashtra")
                .stateCode("27")
                .primaryBranch(true)
                .active(true)
                .build();
        branch.setOrganizationId(org1.getId());
        clientBranchRepository.save(branch);

        // 3. Add Services
        ClientServiceEntity service = ClientServiceEntity.builder()
                .clientId(client1.getId())
                .serviceType(ClientServiceType.GST_COMPLIANCE)
                .status(ClientServiceStatus.ACTIVE)
                .notes("Monthly GSTR-1 and GSTR-3B filings")
                .build();
        service.setOrganizationId(org1.getId());
        clientServiceRepository.save(service);

        // 4. Add Engagements
        EngagementEntity engagement = EngagementEntity.builder()
                .clientId(client1.getId())
                .name("FY 2024-25 Statutory GST Audit & Monthly Compliance")
                .engagementCode("ENG-2024-GST-001")
                .status(EngagementStatus.ACTIVE)
                .priority(EngagementPriority.HIGH)
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusMonths(5))
                .build();
        engagement.setOrganizationId(org1.getId());
        engagementRepository.save(engagement);

        // 5. Add Tasks
        TaskEntity task = TaskEntity.builder()
                .clientId(client1.getId())
                .title("Reconcile GSTR-2B with Purchase Register")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(3))
                .build();
        task.setOrganizationId(org1.getId());
        taskRepository.save(task);

        // 6. Add Invoices
        InvoiceEntity invoice = InvoiceEntity.builder()
                .clientId(client1.getId())
                .invoiceNumber("INV-2024-001")
                .invoiceDate(LocalDate.now().minusDays(15))
                .dueDate(LocalDate.now().plusDays(15))
                .subtotal(new BigDecimal("50000.00"))
                .tax(new BigDecimal("9000.00"))
                .total(new BigDecimal("59000.00"))
                .paidAmount(new BigDecimal("20000.00"))
                .balanceDue(new BigDecimal("39000.00"))
                .status(InvoiceEntity.InvoiceStatus.PARTIALLY_PAID)
                .build();
        invoice.setOrganizationId(org1.getId());
        invoiceRepository.save(invoice);

        // 7. Add Doc Requests
        DocumentRequestEntity docReq = DocumentRequestEntity.builder()
                .clientId(client1.getId())
                .requestNumber("REQ-2024-001")
                .purpose("September Bank Statements")
                .status(DocumentRequestEntity.RequestStatus.REQUESTED)
                .dueDate(LocalDate.now().plusDays(5))
                .build();
        docReq.setOrganizationId(org1.getId());
        documentRequestRepository.save(docReq);

        // 8. Add Tax Notices
        TaxNoticeEntity notice = TaxNoticeEntity.builder()
                .clientId(client1.getId())
                .noticeNumber("NOT-GST-2024-09")
                .department(NoticeDepartment.GST)
                .noticeType("SCRUTINY")
                .section("Sec 61")
                .subject("Discrepancy in GSTR-3B vs GSTR-1")
                .noticeDate(LocalDate.now().minusDays(5))
                .receivedDate(LocalDate.now().minusDays(3))
                .demandAmount(new BigDecimal("150000.00"))
                .status(NoticeStatus.UNDER_REVIEW)
                .priority(NoticePriority.HIGH)
                .riskLevel(NoticeRisk.MEDIUM)
                .responseDueDate(LocalDate.now().plusDays(10))
                .build();
        notice.setOrganizationId(org1.getId());
        taxNoticeRepository.save(notice);

        TenantContext.clear();

        mockMvc.perform(get("/api/v1/clients/{clientId}/360", client1.getId())
                        .header("Authorization", adminToken1)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                // Master Identity & Statutory
                .andExpect(jsonPath("$.data.client.displayName", equalTo("Nexus Tech Solutions Private Limited")))
                .andExpect(jsonPath("$.data.client.clientCode", equalTo("NEX-001")))
                .andExpect(jsonPath("$.data.identifiers.pan", equalTo("AAACA1234A")))
                .andExpect(jsonPath("$.data.identifiers.gstin", equalTo("27AAACA1234A1Z5")))
                .andExpect(jsonPath("$.data.identifiers.isPanValid", equalTo(true)))
                // Primary Contact & Branch
                .andExpect(jsonPath("$.data.primaryContact.displayName", equalTo("Vikram Mehta")))
                .andExpect(jsonPath("$.data.primaryBranch.branchName", equalTo("Mumbai Registered Office")))
                .andExpect(jsonPath("$.data.keyContacts", hasSize(1)))
                .andExpect(jsonPath("$.data.keyBranches", hasSize(1)))
                // Services & Engagements
                .andExpect(jsonPath("$.data.services", hasSize(1)))
                .andExpect(jsonPath("$.data.engagements", hasSize(1)))
                .andExpect(jsonPath("$.data.engagements[0].name", equalTo("FY 2024-25 Statutory GST Audit & Monthly Compliance")))
                // Work Summary & Tasks
                .andExpect(jsonPath("$.data.taskSummary.totalTasks", equalTo(1)))
                .andExpect(jsonPath("$.data.taskSummary.inProgressTasks", equalTo(1)))
                // Doc Requests
                .andExpect(jsonPath("$.data.docRequestsSummary.totalRequests", equalTo(1)))
                .andExpect(jsonPath("$.data.docRequestsSummary.pendingRequests", equalTo(1)))
                // Billing (Authorized Admin)
                .andExpect(jsonPath("$.data.billingSummary", notNullValue()))
                .andExpect(jsonPath("$.data.billingSummary.outstandingBalance", equalTo(39000.0)))
                // Notices
                .andExpect(jsonPath("$.data.noticeSummary.totalNotices", equalTo(1)))
                .andExpect(jsonPath("$.data.noticeSummary.activeNotices", equalTo(1)))
                // Intelligence & Completeness
                .andExpect(jsonPath("$.data.completeness.completionPercentage", greaterThanOrEqualTo(50)))
                .andExpect(jsonPath("$.data.intelligenceSummary", notNullValue()));
    }

    @Test
    @DisplayName("Zero-Trust Security: Staff user without billing access receives redacted billing summary")
    void testClient360ZeroTrustBillingRedaction() throws Exception {
        TenantContext.setTenantId(org1.getId());

        InvoiceEntity invoice = InvoiceEntity.builder()
                .clientId(client1.getId())
                .invoiceNumber("INV-SECRET-001")
                .invoiceDate(LocalDate.now().minusDays(10))
                .dueDate(LocalDate.now().plusDays(20))
                .subtotal(new BigDecimal("100000.00"))
                .tax(new BigDecimal("18000.00"))
                .total(new BigDecimal("118000.00"))
                .balanceDue(new BigDecimal("118000.00"))
                .status(InvoiceEntity.InvoiceStatus.ISSUED)
                .build();
        invoice.setOrganizationId(org1.getId());
        invoiceRepository.save(invoice);

        TenantContext.clear();

        // Staff user gets null billingSummary
        mockMvc.perform(get("/api/v1/clients/{clientId}/360", client1.getId())
                        .header("Authorization", staffToken1)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.data.client.displayName", equalTo("Nexus Tech Solutions Private Limited")))
                .andExpect(jsonPath("$.data.billingSummary", nullValue()));
    }

    @Test
    @DisplayName("Tenant Isolation: Tenant 2 cannot access Client 360 of Tenant 1")
    void testClient360StrictTenantIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientId}/360", client1.getId())
                        .header("Authorization", adminToken2)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Audit Purity: Querying Client 360 Connected View generates ZERO audit log entries")
    void testClient360ReadModelDoesNotGenerateAuditLogs() throws Exception {
        TenantContext.setTenantId(org1.getId());
        long auditsBefore = auditLogRepository.count();
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/clients/{clientId}/360", client1.getId())
                        .header("Authorization", adminToken1)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        TenantContext.setTenantId(org1.getId());
        long auditsAfter = auditLogRepository.count();
        TenantContext.clear();

        assertThat(auditsAfter).isEqualTo(auditsBefore);
    }
}
