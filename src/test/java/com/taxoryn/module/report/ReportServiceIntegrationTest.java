package com.taxoryn.module.report;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.entity.GstProfileEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.report.dto.ClientReportDto;
import com.taxoryn.module.report.dto.FinancialReportDto;
import com.taxoryn.module.report.dto.PracticeOverviewReportDto;
import com.taxoryn.module.report.dto.TaxWorkReportDto;
import com.taxoryn.module.report.dto.WorkManagementReportDto;
import com.taxoryn.module.report.service.ReportService;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.tds.entity.TdsProfileEntity;
import com.taxoryn.module.tds.entity.TdsProfileEntity.DeductorType;
import com.taxoryn.module.tds.entity.TdsReturnEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFormType;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReportServiceIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ComplianceObligationRepository complianceObligationRepository;

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
    private DocumentRequestRepository documentRequestRepository;

    @Autowired
    private DocumentRequestItemRepository documentRequestItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private UserEntity staffUser1;
    private EmployeeEntity employee1;
    private ClientEntity client1;
    private ClientEntity client2;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax Firm " + suffix)
                .email("contact@alpha" + suffix + ".com")
                .phone("9876543210")
                .pan("AABCA1234A")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Tax Firm " + suffix)
                .email("contact@beta" + suffix + ".com")
                .phone("9876543211")
                .pan("AABCB1234B")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(org1.getId());

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("Org Admin").code("ORG_ADMIN").isSystemRole(true).build()));

        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("Staff").code("STAFF").isSystemRole(true).build()));

        adminUser1 = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("admin@" + suffix + ".com")
                .passwordHash("$2a$10$abcdefghijklmnopqrstuvwxyzABCDE")
                .firstName("Admin")
                .lastName("Sharma")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        staffUser1 = userRepository.save(UserEntity.builder()
                .organizationId(org1.getId())
                .email("staff@" + suffix + ".com")
                .passwordHash("$2a$10$abcdefghijklmnopqrstuvwxyzABCDE")
                .firstName("Staff")
                .lastName("Verma")
                .roles(new HashSet<>(Set.of(staffRole)))
                .build());

        EmployeeEntity emp = EmployeeEntity.builder()
                .userId(staffUser1.getId())
                .employeeCode("EMP-" + suffix)
                .firstName("Staff")
                .lastName("Verma")
                .email(staffUser1.getEmail())
                .phone("9876543212")
                .department("Direct Tax")
                .designation("Senior Associate")
                .status(EmployeeStatus.ACTIVE)
                .build();
        emp.setOrganizationId(org1.getId());
        employee1 = employeeRepository.save(emp);

        ClientEntity c1 = ClientEntity.builder()
                .displayName("Acme Pvt Ltd " + suffix)
                .legalName("Acme Technologies Private Limited")
                .pan("AABCA" + suffix.substring(0, 4) + "A")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(employee1.getId())
                .build();
        c1.setOrganizationId(org1.getId());
        client1 = clientRepository.save(c1);

        ClientEntity c2 = ClientEntity.builder()
                .displayName("John Doe " + suffix)
                .legalName("John Doe")
                .pan("ABCDE" + suffix.substring(0, 4) + "F")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(employee1.getId())
                .build();
        c2.setOrganizationId(org1.getId());
        client2 = clientRepository.save(c2);

        authenticateAs(adminUser1, "ORG_ADMIN", "CLIENT_READ", "TASK_READ", "COMPLIANCE_READ", "BILLING_VIEW", "BILLING_READ");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticateAs(UserEntity user, String roleCode, String... permissions) {
        Set<String> roles = Set.of(roleCode);
        Set<String> perms = permissions.length > 0 ? Set.of(permissions) : Set.of();
        SecurityUser securityUser = SecurityUser.builder()
                .userId(user.getId())
                .organizationId(user.getOrganizationId())
                .email(user.getEmail())
                .roles(roles)
                .permissions(perms)
                .enabled(true)
                .build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(user.getOrganizationId());
    }

    @Test
    @DisplayName("Practice Overview Report aggregates cross-module KPIs correctly")
    void testGetPracticeOverviewReport() {
        LocalDate today = LocalDate.now();

        // 1. Create a task
        TaskEntity task = TaskEntity.builder()
                .clientId(client1.getId())
                .title("Prepare Q1 GST")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .taskCategory(TaskCategory.GST)
                .dueDate(today.plusDays(2))
                .assignedTo(employee1.getId())
                .build();
        task.setOrganizationId(org1.getId());
        taskRepository.save(task);

        // 2. Create a GST filing
        GstProfileEntity gstProfile = GstProfileEntity.builder()
                .clientId(client1.getId())
                .gstin("27AABCA" + UUID.randomUUID().toString().substring(0, 7).toUpperCase())
                .legalName(client1.getLegalName())
                .tradeName(client1.getDisplayName())
                .build();
        gstProfile.setOrganizationId(org1.getId());
        gstProfile = gstProfileRepository.save(gstProfile);

        GstReturnFilingEntity gstFiling = GstReturnFilingEntity.builder()
                .clientId(client1.getId())
                .gstProfileId(gstProfile.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod("042026")
                .financialYear("2026-27")
                .dueDate(today.plusDays(5))
                .filingStatus(GstFilingStatus.PENDING)
                .build();
        gstFiling.setOrganizationId(org1.getId());
        gstReturnFilingRepository.save(gstFiling);

        // 3. Create an ITR return
        ItrProfileEntity itrProfile = ItrProfileEntity.builder()
                .clientId(client2.getId())
                .pan(client2.getPan())
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .defaultItrType(ItrType.ITR_1)
                .build();
        itrProfile.setOrganizationId(org1.getId());
        itrProfileRepository.save(itrProfile);

        ItrReturnEntity itrReturn = ItrReturnEntity.builder()
                .clientId(client2.getId())
                .assessmentYear("2026-27")
                .financialYear("2025-26")
                .taxpayerType(TaxpayerType.INDIVIDUAL)
                .itrType(ItrType.ITR_1)
                .status(ItrStatus.DATA_ENTRY)
                .dueDate(today.plusDays(20))
                .build();
        itrReturn.setOrganizationId(org1.getId());
        itrReturnRepository.save(itrReturn);

        // 4. Create an Invoice
        InvoiceEntity invoice = InvoiceEntity.builder()
                .clientId(client1.getId())
                .invoiceNumber("INV-2026-001")
                .invoiceDate(today)
                .dueDate(today.plusDays(15))
                .total(BigDecimal.valueOf(15000))
                .paidAmount(BigDecimal.valueOf(5000))
                .balanceDue(BigDecimal.valueOf(10000))
                .status(InvoiceStatus.PARTIALLY_PAID)
                .build();
        invoice.setOrganizationId(org1.getId());
        invoiceRepository.save(invoice);

        PracticeOverviewReportDto report = reportService.getPracticeOverviewReport(null, null);

        assertThat(report).isNotNull();
        assertThat(report.getTotalClients()).isEqualTo(2);
        assertThat(report.getActiveClients()).isEqualTo(2);
        assertThat(report.getOpenTasks()).isGreaterThanOrEqualTo(1);
        assertThat(report.getActiveTaxJobs()).isGreaterThanOrEqualTo(2);
        assertThat(report.isHasBillingAccess()).isTrue();
        assertThat(report.getTotalInvoiced()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(report.getTotalCollected()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(report.getTotalOutstanding()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("Tax Work Report breaks down GST, ITR, TDS and statutory compliance")
    void testGetTaxWorkReport() {
        LocalDate today = LocalDate.now();

        // GST
        GstProfileEntity gstProfile = GstProfileEntity.builder()
                .clientId(client1.getId())
                .gstin("27AABCA9999Z1")
                .legalName(client1.getLegalName())
                .tradeName(client1.getDisplayName())
                .build();
        gstProfile.setOrganizationId(org1.getId());
        gstProfile = gstProfileRepository.save(gstProfile);

        GstReturnFilingEntity gstFiling = GstReturnFilingEntity.builder()
                .clientId(client1.getId())
                .gstProfileId(gstProfile.getId())
                .returnType(GstReturnType.GSTR3B)
                .returnPeriod("052026")
                .financialYear("2026-27")
                .dueDate(today.minusDays(2))
                .filingStatus(GstFilingStatus.PENDING)
                .build();
        gstFiling.setOrganizationId(org1.getId());
        gstReturnFilingRepository.save(gstFiling);

        // TDS
        TdsProfileEntity tdsProfile = TdsProfileEntity.builder()
                .clientId(client1.getId())
                .tan("MUMA12345A")
                .deductorType(DeductorType.COMPANY)
                .build();
        tdsProfile.setOrganizationId(org1.getId());
        tdsProfile = tdsProfileRepository.save(tdsProfile);

        TdsReturnEntity tdsReturn = TdsReturnEntity.builder()
                .clientId(client1.getId())
                .tdsProfileId(tdsProfile.getId())
                .formType(TdsFormType.FORM_26Q)
                .quarter(TdsQuarter.Q1)
                .financialYear("2026-27")
                .assessmentYear("2027-28")
                .filingStatus(TdsFilingStatus.FILED)
                .dueDate(today.minusDays(10))
                .build();
        tdsReturn.setOrganizationId(org1.getId());
        tdsReturnRepository.save(tdsReturn);

        // Compliance
        ComplianceObligationEntity obligation = ComplianceObligationEntity.builder()
                .clientId(client1.getId())
                .title("GSTR-3B May 2026")
                .complianceType(ComplianceType.GST)
                .period("052026")
                .dueDate(today.minusDays(2))
                .status(ComplianceStatus.PENDING)
                .build();
        obligation.setOrganizationId(org1.getId());
        complianceObligationRepository.save(obligation);

        TaxWorkReportDto report = reportService.getTaxWorkReport("2026-27", null, null, null, null);

        assertThat(report).isNotNull();
        assertThat(report.getTaxWorkSummary()).hasSize(3);
        assertThat(report.getGstOverdue()).isGreaterThanOrEqualTo(1);
        assertThat(report.getTdsFiled()).isGreaterThanOrEqualTo(1);
        assertThat(report.getComplianceTotal()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Client Report tracks pending follow-up items and attention list")
    void testGetClientReport() {
        LocalDate today = LocalDate.now();

        // Add overdue task to client1
        TaskEntity task = TaskEntity.builder()
                .clientId(client1.getId())
                .title("Audit Queries Pending")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.URGENT)
                .taskCategory(TaskCategory.AUDIT)
                .dueDate(today.minusDays(3))
                .assignedTo(employee1.getId())
                .build();
        task.setOrganizationId(org1.getId());
        taskRepository.save(task);

        // Add document request item
        DocumentRequestEntity docReq = DocumentRequestEntity.builder()
                .clientId(client1.getId())
                .requestNumber("DOC-REQ-001")
                .purpose("Bank Statements Q1")
                .dueDate(today.minusDays(1))
                .status(RequestStatus.SENT)
                .build();
        docReq.setOrganizationId(org1.getId());
        docReq = documentRequestRepository.save(docReq);

        DocumentRequestItemEntity item = DocumentRequestItemEntity.builder()
                .clientId(client1.getId())
                .request(docReq)
                .title("HDFC Current Account Statement")
                .status(ItemStatus.PENDING)
                .required(true)
                .build();
        item.setOrganizationId(org1.getId());
        documentRequestItemRepository.save(item);

        ClientReportDto report = reportService.getClientReport(null, null);

        assertThat(report).isNotNull();
        assertThat(report.getTotalClients()).isEqualTo(2);
        assertThat(report.getClientsWithOverdueWork()).isGreaterThanOrEqualTo(1);
        assertThat(report.getClientsRequiringAttention()).isNotEmpty();
        assertThat(report.getClientsRequiringAttention().get(0).getClientId()).isEqualTo(client1.getId());
    }

    @Test
    @DisplayName("Work Management Report computes employee task completion productivity")
    void testGetWorkManagementReport() {
        LocalDate today = LocalDate.now();

        // 1 completed task, 1 open task for employee1
        TaskEntity t1 = TaskEntity.builder()
                .clientId(client1.getId())
                .title("Task 1 Done")
                .status(TaskStatus.COMPLETED)
                .priority(TaskPriority.MEDIUM)
                .taskCategory(TaskCategory.GST)
                .assignedTo(employee1.getId())
                .build();
        t1.setOrganizationId(org1.getId());
        taskRepository.save(t1);

        TaskEntity t2 = TaskEntity.builder()
                .clientId(client1.getId())
                .title("Task 2 Open")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .taskCategory(TaskCategory.ITR)
                .dueDate(today.plusDays(3))
                .assignedTo(employee1.getId())
                .build();
        t2.setOrganizationId(org1.getId());
        taskRepository.save(t2);

        WorkManagementReportDto report = reportService.getWorkManagementReport(null, null);

        assertThat(report).isNotNull();
        assertThat(report.getTotalTasks()).isGreaterThanOrEqualTo(2);
        assertThat(report.getEmployeeProductivity()).isNotEmpty();
        var empMetric = report.getEmployeeProductivity().stream()
                .filter(e -> employee1.getId().equals(e.getEmployeeId()))
                .findFirst()
                .orElse(null);

        assertThat(empMetric).isNotNull();
        assertThat(empMetric.getAssignedTasks()).isGreaterThanOrEqualTo(2);
        assertThat(empMetric.getCompletedTasks()).isGreaterThanOrEqualTo(1);
        assertThat(empMetric.getCompletionRate()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Financial Report calculates outstanding invoices and protects staff view")
    void testGetFinancialReport() {
        LocalDate today = LocalDate.now();

        InvoiceEntity invoice = InvoiceEntity.builder()
                .clientId(client1.getId())
                .invoiceNumber("INV-FIN-001")
                .invoiceDate(today.minusDays(20))
                .dueDate(today.minusDays(5))
                .total(BigDecimal.valueOf(25000))
                .paidAmount(BigDecimal.valueOf(10000))
                .balanceDue(BigDecimal.valueOf(15000))
                .status(InvoiceStatus.PARTIALLY_PAID)
                .build();
        invoice.setOrganizationId(org1.getId());
        invoice = invoiceRepository.save(invoice);

        InvoicePaymentEntity payment = InvoicePaymentEntity.builder()
                .clientId(client1.getId())
                .invoice(invoice)
                .amount(BigDecimal.valueOf(10000))
                .paymentDate(today.minusDays(10))
                .paymentMethod(InvoicePaymentEntity.PaymentMethod.NEFT_RTGS)
                .referenceNumber("NEFT123456")
                .build();
        payment.setOrganizationId(org1.getId());
        invoicePaymentRepository.save(payment);

        FinancialReportDto adminReport = reportService.getFinancialReport(null, null);
        assertThat(adminReport.isHasBillingAccess()).isTrue();
        assertThat(adminReport.getTotalInvoiced()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        assertThat(adminReport.getTotalCollected()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(adminReport.getTotalOutstanding()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(adminReport.getOutstandingInvoices()).isNotEmpty();
        assertThat(adminReport.getOutstandingInvoices().get(0).isOverdue()).isTrue();

        // Authenticate as Staff (no billing access)
        authenticateAs(staffUser1, "STAFF");
        FinancialReportDto staffReport = reportService.getFinancialReport(null, null);
        assertThat(staffReport.isHasBillingAccess()).isFalse();
        assertThat(staffReport.getTotalInvoiced()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Multi-tenant isolation prevents cross-organization data leakage in reports")
    void testMultiTenantIsolation() {
        // Create client and tasks in org2
        TenantContext.setTenantId(org2.getId());
        ClientEntity org2Client = ClientEntity.builder()
                .displayName("Beta Client")
                .legalName("Beta Client Pvt Ltd")
                .pan("AABCB9999B")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .build();
        org2Client.setOrganizationId(org2.getId());
        org2Client = clientRepository.save(org2Client);

        TaskEntity task = TaskEntity.builder()
                .clientId(org2Client.getId())
                .title("Org 2 Secret Task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .taskCategory(TaskCategory.OTHER)
                .build();
        task.setOrganizationId(org2.getId());
        taskRepository.save(task);

        // Authenticate as Org 1 Admin
        authenticateAs(adminUser1, "ORG_ADMIN", "CLIENT_READ", "TASK_READ");
        PracticeOverviewReportDto org1Overview = reportService.getPracticeOverviewReport(null, null);

        // Org 1 report must only count Org 1 clients
        assertThat(org1Overview.getTotalClients()).isEqualTo(2);
    }
}
