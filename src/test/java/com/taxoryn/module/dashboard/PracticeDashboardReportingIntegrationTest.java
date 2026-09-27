package com.taxoryn.module.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoiceItemEntity;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeStatus;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
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
import com.taxoryn.module.task.entity.WorkItemEntity;
import com.taxoryn.module.task.model.WorkItemPriority;
import com.taxoryn.module.task.model.WorkItemStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.repository.WorkItemRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PracticeDashboardReportingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EngagementRepository engagementRepository;

    @Autowired
    private ComplianceObligationRepository complianceObligationRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentRequestRepository documentRequestRepository;

    @Autowired
    private TaxNoticeRepository taxNoticeRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private LocationEntity locA1;
    private LocationEntity locA2;
    private UserEntity adminUserA;
    private UserEntity staffUserA;
    private String adminTokenA;
    private String staffTokenA;
    private String adminTokenB;

    private ClientEntity clientA1;
    private ClientEntity clientA2;
    private ClientEntity clientB1;

    @BeforeEach
    void setUp() {
        invoicePaymentRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        taxNoticeRepository.deleteAll();
        documentRequestRepository.deleteAll();
        documentRepository.deleteAll();
        taskRepository.deleteAll();
        workItemRepository.deleteAll();
        complianceObligationRepository.deleteAll();
        engagementRepository.deleteAll();
        clientRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();
        organizationRepository.deleteAll();

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build())
        );

        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("STAFF").name("Staff Member").isSystemRole(true).build())
        );

        // 1. Setup Org A
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Tax Practice Alpha " + UUID.randomUUID())
                .legalName("Alpha Practice LLP")
                .email("admin@" + UUID.randomUUID() + ".com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        LocationEntity l1 = LocationEntity.builder()
                .name("Mumbai HQ")
                .code("MUM")
                .isHeadOffice(true)
                .city("Mumbai")
                .state("Maharashtra")
                .isActive(true)
                .build();
        l1.setOrganizationId(orgA.getId());
        locA1 = locationRepository.save(l1);

        LocationEntity l2 = LocationEntity.builder()
                .name("Delhi Branch")
                .code("DEL")
                .isHeadOffice(false)
                .city("Delhi")
                .state("Delhi")
                .isActive(true)
                .build();
        l2.setOrganizationId(orgA.getId());
        locA2 = locationRepository.save(l2);

        UserEntity uAdminA = UserEntity.builder()
                .email("admin." + UUID.randomUUID() + "@alpha.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Alpha")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build();
        uAdminA.setOrganizationId(orgA.getId());
        adminUserA = userRepository.save(uAdminA);

        adminTokenA = jwtTokenProvider.generateAccessToken(
                adminUserA.getId(), orgA.getId(), locA1.getId(), adminUserA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("DASHBOARD_VIEW", "CLIENT_VIEW", "COMPLIANCE_VIEW", "TASK_VIEW", "DOCUMENT_VIEW", "NOTICE_VIEW", "BILLING_VIEW", "REPORT_VIEW")
        );

        UserEntity uStaffA = UserEntity.builder()
                .email("staff." + UUID.randomUUID() + "@alpha.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Alpha")
                .lastName("Staff")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(staffRole)))
                .build();
        uStaffA.setOrganizationId(orgA.getId());
        staffUserA = userRepository.save(uStaffA);

        staffTokenA = jwtTokenProvider.generateAccessToken(
                staffUserA.getId(), orgA.getId(), locA1.getId(), staffUserA.getEmail(),
                Set.of("STAFF"),
                Set.of("DASHBOARD_VIEW", "CLIENT_VIEW", "COMPLIANCE_VIEW", "TASK_VIEW", "DOCUMENT_VIEW", "NOTICE_VIEW")
        );

        // Employee for staffUserA
        EmployeeEntity empA = EmployeeEntity.builder()
                .userId(staffUserA.getId())
                .employeeCode("EMP-001")
                .firstName("Alpha")
                .lastName("Staff")
                .email(staffUserA.getEmail())
                .department("Tax")
                .designation("Senior Associate")
                .status(EmployeeStatus.ACTIVE)
                .build();
        empA.setOrganizationId(orgA.getId());
        employeeRepository.save(empA);

        // 2. Setup Clients for Org A
        ClientEntity c1 = ClientEntity.builder()
                .locationId(locA1.getId())
                .assignedEmployeeId(empA.getId())
                .displayName("Alpha Client One")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .pan("AAAAA1111A")
                .gstin("27AAAAA1111A1Z1")
                .build();
        c1.setOrganizationId(orgA.getId());
        clientA1 = clientRepository.save(c1);

        ClientEntity c2 = ClientEntity.builder()
                .locationId(locA2.getId())
                .displayName("Alpha Client Two")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .pan("BBBBB2222B")
                .build();
        c2.setOrganizationId(orgA.getId());
        clientA2 = clientRepository.save(c2);

        // 3. Setup Org B (Competitor for tenant isolation testing)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Competitor Practice Beta " + UUID.randomUUID())
                .legalName("Beta Practice LLP")
                .email("admin@" + UUID.randomUUID() + ".com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgB.getId());

        UserEntity uAdminB = UserEntity.builder()
                .email("admin." + UUID.randomUUID() + "@beta.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Beta")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build();
        uAdminB.setOrganizationId(orgB.getId());
        UserEntity adminUserB = userRepository.save(uAdminB);

        adminTokenB = jwtTokenProvider.generateAccessToken(
                adminUserB.getId(), orgB.getId(), null, adminUserB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("DASHBOARD_VIEW", "CLIENT_VIEW", "COMPLIANCE_VIEW", "TASK_VIEW", "DOCUMENT_VIEW", "NOTICE_VIEW", "BILLING_VIEW", "REPORT_VIEW")
        );

        ClientEntity cb1 = ClientEntity.builder()
                .displayName("Beta Competitor Client")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .pan("CCCCC3333C")
                .build();
        cb1.setOrganizationId(orgB.getId());
        clientB1 = clientRepository.save(cb1);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Overview Dashboard: Aggregates all practice KPIs with multi-tenant isolation")
    void testOverviewDashboard_TenantIsolation() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // Seed Org A Engagement
        EngagementEntity eng = EngagementEntity.builder()
                .locationId(locA1.getId())
                .clientId(clientA1.getId())
                .name("GST Filing Retainer")
                .status(EngagementStatus.ACTIVE)
                .startDate(LocalDate.now().minusMonths(1))
                .build();
        eng.setOrganizationId(orgA.getId());
        engagementRepository.save(eng);

        // Seed Org A Compliance Obligation
        ComplianceObligationEntity comp = ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .title("GSTR-3B July 2026")
                .obligationType(ComplianceObligationType.GST_RETURN)
                .periodLabel("2026-07")
                .statutoryDueDate(LocalDate.now().plusDays(5))
                .status(ComplianceObligationStatus.READY)
                .build();
        comp.setOrganizationId(orgA.getId());
        complianceObligationRepository.save(comp);

        // Seed Org A Work Item (Overdue)
        WorkItemEntity wi = WorkItemEntity.builder()
                .clientId(clientA1.getId())
                .assignedUserId(staffUserA.getId())
                .title("Reconcile ITC Ledger")
                .status(WorkItemStatus.IN_PROGRESS)
                .priority(WorkItemPriority.HIGH)
                .dueDate(LocalDate.now().minusDays(3))
                .build();
        wi.setOrganizationId(orgA.getId());
        workItemRepository.save(wi);

        // Seed Org A Task
        TaskEntity t = TaskEntity.builder()
                .clientId(clientA1.getId())
                .assignedTo(staffUserA.getId())
                .title("Verify Sales Register")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.now().plusDays(2))
                .build();
        t.setOrganizationId(orgA.getId());
        taskRepository.save(t);

        // Seed Org A Doc Request
        DocumentRequestEntity req = DocumentRequestEntity.builder()
                .clientId(clientA1.getId())
                .requestNumber("REQ-001")
                .purpose("Bank Statements July 2026")
                .status(RequestStatus.REQUESTED)
                .dueDate(LocalDate.now().plusDays(4))
                .build();
        req.setOrganizationId(orgA.getId());
        documentRequestRepository.save(req);

        // Seed Org A Tax Notice
        TaxNoticeEntity notice = TaxNoticeEntity.builder()
                .clientId(clientA1.getId())
                .noticeNumber("NOT-2026-9011")
                .dinNumber("DIN-2026-9011")
                .department(NoticeDepartment.GST)
                .noticeType("SCRUTINY")
                .subject("GST Scrutiny Notice July 2026")
                .receivedDate(LocalDate.now().minusDays(5))
                .status(NoticeStatus.RECEIVED)
                .priority(NoticePriority.HIGH)
                .responseRequired(true)
                .responseDueDate(LocalDate.now().plusDays(10))
                .demandAmount(new BigDecimal("75000.00"))
                .build();
        notice.setOrganizationId(orgA.getId());
        taxNoticeRepository.save(notice);

        // Seed Org A Invoice
        InvoiceEntity inv = InvoiceEntity.builder()
                .locationId(locA1.getId())
                .clientId(clientA1.getId())
                .invoiceNumber("INV-ALPHA-001")
                .invoiceDate(LocalDate.now().minusDays(5))
                .dueDate(LocalDate.now().plusDays(25))
                .status(InvoiceStatus.ISSUED)
                .subtotal(new BigDecimal("10000.00"))
                .tax(new BigDecimal("1800.00"))
                .total(new BigDecimal("11800.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("11800.00"))
                .build();
        inv.setOrganizationId(orgA.getId());
        invoiceRepository.save(inv);

        // Seed Org B Data (Should NEVER be visible in Org A)
        TenantContext.setTenantId(orgB.getId());
        InvoiceEntity invB = InvoiceEntity.builder()
                .clientId(clientB1.getId())
                .invoiceNumber("INV-BETA-999")
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(InvoiceStatus.ISSUED)
                .subtotal(new BigDecimal("100000.00"))
                .tax(new BigDecimal("18000.00"))
                .total(new BigDecimal("118000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("118000.00"))
                .build();
        invB.setOrganizationId(orgB.getId());
        invoiceRepository.save(invB);

        // Execute GET /api/v1/dashboard/overview as Org A Admin
        mockMvc.perform(get("/api/v1/dashboard/overview")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalClients").value(2))
                .andExpect(jsonPath("$.data.activeClients").value(2))
                .andExpect(jsonPath("$.data.activeEngagements").value(1))
                .andExpect(jsonPath("$.data.openComplianceObligations").value(1))
                .andExpect(jsonPath("$.data.openWorkItems").value(1))
                .andExpect(jsonPath("$.data.overdueWorkItems").value(1))
                .andExpect(jsonPath("$.data.pendingTasks").value(1))
                .andExpect(jsonPath("$.data.pendingDocumentRequests").value(1))
                .andExpect(jsonPath("$.data.openTaxNotices").value(1))
                .andExpect(jsonPath("$.data.outstandingBillingAmount").value(11800.00));
    }

    @Test
    @DisplayName("2. Compliance Dashboard: Breakdown by GST, ITR, TDS and upcoming deadlines")
    void testComplianceDashboard() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // GST Obligation (due in 5 days -> upcoming)
        ComplianceObligationEntity c1 = ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .title("GSTR-1 Monthly")
                .obligationType(ComplianceObligationType.GST_RETURN)
                .statutoryDueDate(LocalDate.now().plusDays(5))
                .status(ComplianceObligationStatus.IN_PROGRESS)
                .build();
        c1.setOrganizationId(orgA.getId());
        complianceObligationRepository.save(c1);

        // ITR Obligation (overdue)
        ComplianceObligationEntity c2 = ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .title("ITR-6 Filing")
                .obligationType(ComplianceObligationType.ITR_FILING)
                .statutoryDueDate(LocalDate.now().minusDays(10))
                .status(ComplianceObligationStatus.OVERDUE)
                .build();
        c2.setOrganizationId(orgA.getId());
        complianceObligationRepository.save(c2);

        // TDS Obligation (completed)
        ComplianceObligationEntity c3 = ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .title("TDS 26Q Q1")
                .obligationType(ComplianceObligationType.TDS_RETURN)
                .statutoryDueDate(LocalDate.now().minusMonths(1))
                .status(ComplianceObligationStatus.COMPLETED)
                .build();
        c3.setOrganizationId(orgA.getId());
        complianceObligationRepository.save(c3);

        mockMvc.perform(get("/api/v1/dashboard/compliance")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalObligations").value(3))
                .andExpect(jsonPath("$.data.inProgress").value(1))
                .andExpect(jsonPath("$.data.overdue").value(1))
                .andExpect(jsonPath("$.data.completed").value(1))
                .andExpect(jsonPath("$.data.upcomingDeadlines").value(1))
                .andExpect(jsonPath("$.data.gst.total").value(1))
                .andExpect(jsonPath("$.data.itr.total").value(1))
                .andExpect(jsonPath("$.data.tds.total").value(1));
    }

    @Test
    @DisplayName("3. Work Dashboard: Work items, tasks and user workload distribution")
    void testWorkDashboard() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        WorkItemEntity wi = WorkItemEntity.builder()
                .clientId(clientA1.getId())
                .assignedUserId(staffUserA.getId())
                .title("Tax Audit 44AB")
                .status(WorkItemStatus.TODO)
                .priority(WorkItemPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(10))
                .build();
        wi.setOrganizationId(orgA.getId());
        workItemRepository.save(wi);

        TaskEntity t = TaskEntity.builder()
                .clientId(clientA1.getId())
                .assignedTo(staffUserA.getId())
                .title("Depreciation Schedule Check")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.now().minusDays(1)) // Overdue task
                .build();
        t.setOrganizationId(orgA.getId());
        taskRepository.save(t);

        mockMvc.perform(get("/api/v1/dashboard/work")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalWorkItems").value(1))
                .andExpect(jsonPath("$.data.workItemsTodo").value(1))
                .andExpect(jsonPath("$.data.totalTasks").value(1))
                .andExpect(jsonPath("$.data.inProgressTasks").value(1))
                .andExpect(jsonPath("$.data.overdueTasks").value(1))
                .andExpect(jsonPath("$.data.userWorkloads", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.userWorkloads[0].assignedWorkItems").value(1))
                .andExpect(jsonPath("$.data.userWorkloads[0].assignedTasks").value(1))
                .andExpect(jsonPath("$.data.userWorkloads[0].overdueTasks").value(1));
    }

    @Test
    @DisplayName("4. Document Dashboard: Document repository and client document requests status")
    void testDocumentDashboard() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        DocumentEntity doc = DocumentEntity.builder()
                .clientId(clientA1.getId())
                .taskId(UUID.randomUUID())
                .fileName("AuditReport.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .storageKey("/storage/docs/audit.pdf")
                .build();
        doc.setOrganizationId(orgA.getId());
        documentRepository.save(doc);

        DocumentRequestEntity r1 = DocumentRequestEntity.builder()
                .clientId(clientA1.getId())
                .requestNumber("REQ-002")
                .purpose("Form 16A TDS Certificates")
                .status(RequestStatus.REQUESTED)
                .dueDate(LocalDate.now().minusDays(2)) // Overdue request
                .build();
        r1.setOrganizationId(orgA.getId());
        documentRequestRepository.save(r1);

        DocumentRequestEntity r2 = DocumentRequestEntity.builder()
                .clientId(clientA1.getId())
                .requestNumber("REQ-003")
                .purpose("Purchase Register Q1")
                .status(RequestStatus.COMPLETED)
                .build();
        r2.setOrganizationId(orgA.getId());
        documentRequestRepository.save(r2);

        mockMvc.perform(get("/api/v1/dashboard/documents")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalDocuments").value(1))
                .andExpect(jsonPath("$.data.activeWorkflowDocuments").value(1))
                .andExpect(jsonPath("$.data.totalRequests").value(2))
                .andExpect(jsonPath("$.data.pendingRequests").value(1))
                .andExpect(jsonPath("$.data.fulfilledRequests").value(1))
                .andExpect(jsonPath("$.data.overdueRequests").value(1));
    }

    @Test
    @DisplayName("5. Notice Dashboard: Tax notice response demands, hearing deadlines")
    void testNoticeDashboard() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        TaxNoticeEntity n = TaxNoticeEntity.builder()
                .clientId(clientA1.getId())
                .noticeNumber("NOT-GST-001")
                .dinNumber("DIN-GST-001")
                .department(NoticeDepartment.GST)
                .noticeType("SCRUTINY")
                .subject("GST Annual Return Notice")
                .receivedDate(LocalDate.now().minusDays(10))
                .status(NoticeStatus.RESPONSE_PREPARATION)
                .priority(NoticePriority.HIGH)
                .responseRequired(true)
                .responseDueDate(LocalDate.now().minusDays(1)) // Overdue response
                .hearingRequired(true)
                .hearingDate(LocalDate.now().plusDays(10)) // Upcoming hearing
                .demandAmount(new BigDecimal("120000.00"))
                .build();
        n.setOrganizationId(orgA.getId());
        taxNoticeRepository.save(n);

        mockMvc.perform(get("/api/v1/dashboard/notices")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOpenNotices").value(1))
                .andExpect(jsonPath("$.data.noticesRequiringResponse").value(1))
                .andExpect(jsonPath("$.data.noticesRequiringHearing").value(1))
                .andExpect(jsonPath("$.data.upcomingHearings").value(1))
                .andExpect(jsonPath("$.data.overdueResponseDeadlines").value(1))
                .andExpect(jsonPath("$.data.totalDemandAmount").value(120000.00));
    }

    @Test
    @DisplayName("6. Billing Dashboard: Invoices status, collected revenue, service breakdowns")
    void testBillingDashboard() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        InvoiceEntity inv1 = InvoiceEntity.builder()
                .clientId(clientA1.getId())
                .invoiceNumber("INV-BILL-001")
                .invoiceDate(LocalDate.now().minusDays(10))
                .dueDate(LocalDate.now().minusDays(2)) // Overdue invoice
                .status(InvoiceStatus.PARTIALLY_PAID)
                .subtotal(new BigDecimal("10000.00"))
                .tax(new BigDecimal("1800.00"))
                .total(new BigDecimal("11800.00"))
                .paidAmount(new BigDecimal("5000.00"))
                .balanceDue(new BigDecimal("6800.00"))
                .build();
        inv1.setOrganizationId(orgA.getId());
        inv1 = invoiceRepository.save(inv1);

        InvoiceItemEntity item = InvoiceItemEntity.builder()
                .invoice(inv1)
                .service(BillingServiceType.GST_FILING)
                .description("GST Return Filing Q1")
                .amount(new BigDecimal("10000.00"))
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("10000.00"))
                .build();
        invoiceItemRepository.save(item);

        mockMvc.perform(get("/api/v1/dashboard/billing")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalInvoices").value(1))
                .andExpect(jsonPath("$.data.partiallyPaidInvoices").value(1))
                .andExpect(jsonPath("$.data.overdueInvoices").value(1))
                .andExpect(jsonPath("$.data.totalInvoicedAmount").value(11800.00))
                .andExpect(jsonPath("$.data.totalCollectedAmount").value(5000.00))
                .andExpect(jsonPath("$.data.totalOutstandingAmount").value(6800.00))
                .andExpect(jsonPath("$.data.totalOverdueAmount").value(6800.00))
                .andExpect(jsonPath("$.data.revenueByService.GST_FILING").value(10000.00));
    }

    @Test
    @DisplayName("7. Client Dashboard & Attention List: Prioritization of clients requiring action")
    void testClientDashboard_AttentionList() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // Client A1 has open notice and overdue work item
        TaxNoticeEntity notice = TaxNoticeEntity.builder()
                .clientId(clientA1.getId())
                .noticeNumber("NOT-ATTN-001")
                .dinNumber("DIN-ATTN-001")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("SCRUTINY")
                .subject("ITR 143(2) Notice")
                .receivedDate(LocalDate.now().minusDays(3))
                .responseDueDate(LocalDate.now().plusDays(15))
                .status(NoticeStatus.RECEIVED)
                .priority(NoticePriority.CRITICAL)
                .build();
        notice.setOrganizationId(orgA.getId());
        taxNoticeRepository.save(notice);

        WorkItemEntity wi = WorkItemEntity.builder()
                .clientId(clientA1.getId())
                .assignedUserId(staffUserA.getId())
                .title("Urgent Audit Action")
                .status(WorkItemStatus.TODO)
                .priority(WorkItemPriority.URGENT)
                .dueDate(LocalDate.now().minusDays(5))
                .build();
        wi.setOrganizationId(orgA.getId());
        workItemRepository.save(wi);

        mockMvc.perform(get("/api/v1/dashboard/clients")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalClients").value(2))
                .andExpect(jsonPath("$.data.activeClients").value(2))
                .andExpect(jsonPath("$.data.clientsWithOpenNotices").value(1))
                .andExpect(jsonPath("$.data.clientsWithOverdueWork").value(1))
                .andExpect(jsonPath("$.data.attentionList", hasSize(1)))
                .andExpect(jsonPath("$.data.attentionList[0].clientId").value(clientA1.getId().toString()))
                .andExpect(jsonPath("$.data.attentionList[0].openNoticesCount").value(1))
                .andExpect(jsonPath("$.data.attentionList[0].overdueWorkCount").value(1));
    }

    @Test
    @DisplayName("8. Location Scoping: Location filter filters metrics, unauthorized location returns 404")
    void testLocationScoping() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // Client A1 is in Location 1 (Mumbai), Client A2 is in Location 2 (Delhi)
        mockMvc.perform(get("/api/v1/dashboard/clients?locationId=" + locA1.getId())
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClients").value(1));

        // Unknown location in another tenant / random UUID returns 404 Not Found
        mockMvc.perform(get("/api/v1/dashboard/clients?locationId=" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("9. Staff Portfolio Scoping: Staff member scoped to their assigned clients only")
    void testStaffPortfolioScoping() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // staffUserA is primaryManager for ClientA1, but NOT ClientA2
        mockMvc.perform(get("/api/v1/dashboard/overview?clientId=" + clientA1.getId())
                        .header("Authorization", "Bearer " + staffTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClients").value(1));
    }
}
