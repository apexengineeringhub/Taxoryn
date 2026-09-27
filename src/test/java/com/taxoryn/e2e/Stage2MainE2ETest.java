package com.taxoryn.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.billing.dto.CreateInvoiceItemRequest;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.dto.RecordPaymentRequest;
import com.taxoryn.module.billing.entity.InvoiceEntity.InvoiceStatus;
import com.taxoryn.module.billing.entity.InvoiceItemEntity.BillingServiceType;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity.PaymentMethod;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.CreateComplianceObligationRequest;
import com.taxoryn.module.compliance.dto.UpdateObligationStatusRequest;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequestItem;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.engagement.dto.CreateEngagementRequest;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.moduleconfig.dto.UpdateOrganizationModuleRequest;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository;
import com.taxoryn.module.notice.dto.CloseNoticeRequest;
import com.taxoryn.module.notice.dto.CreateNoticeResponseRequest;
import com.taxoryn.module.notice.dto.CreateTaxNoticeRequest;
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
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.CreateWorkItemRequest;
import com.taxoryn.module.task.dto.UpdateTaskRequest;
import com.taxoryn.module.task.dto.UpdateWorkItemStatusRequest;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class Stage2MainE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private OrganizationModuleRepository organizationModuleRepository;

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
    private EmployeeEntity empStaffA;
    private String adminTokenA;
    private String staffTokenA;

    private UserEntity adminUserB;
    private String adminTokenB;

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
                .name("Apex CA & Tax Consultants " + UUID.randomUUID())
                .legalName("Apex Advisory LLP")
                .email("admin@" + UUID.randomUUID() + ".com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgA.getId());

        LocationEntity l1 = LocationEntity.builder()
                .name("Mumbai HQ")
                .code("MUM-01")
                .isHeadOffice(true)
                .city("Mumbai")
                .state("Maharashtra")
                .isActive(true)
                .build();
        l1.setOrganizationId(orgA.getId());
        locA1 = locationRepository.save(l1);

        LocationEntity l2 = LocationEntity.builder()
                .name("Bengaluru Branch")
                .code("BLR-01")
                .isHeadOffice(false)
                .city("Bengaluru")
                .state("Karnataka")
                .isActive(true)
                .build();
        l2.setOrganizationId(orgA.getId());
        locA2 = locationRepository.save(l2);

        UserEntity uAdminA = UserEntity.builder()
                .email("admin." + UUID.randomUUID() + "@apex.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Apex")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build();
        uAdminA.setOrganizationId(orgA.getId());
        adminUserA = userRepository.save(uAdminA);

        adminTokenA = jwtTokenProvider.generateAccessToken(
                adminUserA.getId(), orgA.getId(), null, adminUserA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of(
                        "ORGANIZATION_VIEW", "ORGANIZATION_UPDATE", "ORG_READ", "ORG_WRITE",
                        "CLIENT_VIEW", "CLIENT_READ", "CLIENT_CREATE", "CLIENT_WRITE", "CLIENT_UPDATE",
                        "TASK_VIEW", "TASK_READ", "TASK_CREATE", "TASK_WRITE", "TASK_UPDATE",
                        "COMPLIANCE_VIEW", "COMPLIANCE_CREATE", "COMPLIANCE_UPDATE",
                        "DOCUMENT_VIEW", "DOCUMENT_READ", "DOCUMENT_CREATE", "DOCUMENT_WRITE", "DOCUMENT_UPLOAD",
                        "NOTICE_VIEW", "NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_RESPONSE_CREATE", "NOTICE_RESPONSE_REVIEW", "NOTICE_SUBMIT", "NOTICE_CLOSE",
                        "BILLING_VIEW", "BILLING_CREATE", "BILLING_UPDATE",
                        "DASHBOARD_VIEW", "REPORT_VIEW", "GST_VIEW", "ITR_VIEW", "TDS_VIEW"
                )
        );

        UserEntity uStaffA = UserEntity.builder()
                .email("staff." + UUID.randomUUID() + "@apex.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Rohan")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(staffRole)))
                .build();
        uStaffA.setOrganizationId(orgA.getId());
        staffUserA = userRepository.save(uStaffA);

        empStaffA = EmployeeEntity.builder()
                .userId(staffUserA.getId())
                .employeeCode("EMP-APEX-01")
                .firstName("Rohan")
                .lastName("Sharma")
                .email(staffUserA.getEmail())
                .department("Direct Tax")
                .designation("Senior Associate")
                .status(EmployeeStatus.ACTIVE)
                .build();
        empStaffA.setOrganizationId(orgA.getId());
        empStaffA = employeeRepository.save(empStaffA);

        staffTokenA = jwtTokenProvider.generateAccessToken(
                staffUserA.getId(), orgA.getId(), null, staffUserA.getEmail(),
                Set.of("STAFF"),
                Set.of(
                        "CLIENT_VIEW", "CLIENT_READ",
                        "TASK_VIEW", "TASK_READ", "TASK_CREATE", "TASK_WRITE",
                        "COMPLIANCE_VIEW",
                        "DOCUMENT_VIEW", "DOCUMENT_READ", "DOCUMENT_CREATE", "DOCUMENT_UPLOAD",
                        "NOTICE_VIEW", "NOTICE_CREATE"
                )
        );

        // 2. Setup Org B (Competitor for tenant isolation testing)
        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Zenith Tax Advisors " + UUID.randomUUID())
                .legalName("Zenith Tax LLP")
                .email("admin@" + UUID.randomUUID() + ".com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(orgB.getId());

        UserEntity uAdminB = UserEntity.builder()
                .email("admin." + UUID.randomUUID() + "@zenith.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Zenith")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build();
        uAdminB.setOrganizationId(orgB.getId());
        adminUserB = userRepository.save(uAdminB);

        adminTokenB = jwtTokenProvider.generateAccessToken(
                adminUserB.getId(), orgB.getId(), null, adminUserB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of(
                        "ORGANIZATION_VIEW", "CLIENT_VIEW", "CLIENT_READ", "CLIENT_CREATE", "CLIENT_WRITE",
                        "TASK_VIEW", "TASK_READ", "COMPLIANCE_VIEW", "DOCUMENT_VIEW", "NOTICE_VIEW",
                        "BILLING_VIEW", "BILLING_CREATE", "BILLING_UPDATE", "DASHBOARD_VIEW"
                )
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // =========================================================================
    // E2E-01: Stage 2 Golden Path — Complete Cross-Module Business Journey
    // =========================================================================

    @Test
    @DisplayName("E2E-01: Stage 2 Golden Path: Org -> Client -> Engagement -> Obligation -> Work -> Task -> DocRequest -> Doc -> Notice -> Invoice -> Payment -> Dashboard")
    void shouldCompleteStage2GoldenPath() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // 1. Create Client
        CreateClientRequest createClientReq = CreateClientRequest.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Alpha Innovations Pvt Ltd")
                .legalName("Alpha Innovations Private Limited")
                .pan("AAACA1234A")
                .gstin("27AAACA1234A1Z5")
                .email("accounts@alphainno.com")
                .locationId(locA1.getId())
                .assignedEmployeeId(empStaffA.getId())
                .status(ClientStatus.ACTIVE)
                .build();

        MvcResult clientResult = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createClientReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Alpha Innovations Pvt Ltd"))
                .andReturn();

        UUID clientId = UUID.fromString(extractJsonField(clientResult, "$.data.id"));
        assertThat(clientId).isNotNull();

        // 2. Create Engagement
        CreateEngagementRequest createEngReq = CreateEngagementRequest.builder()
                .clientId(clientId)
                .locationId(locA1.getId())
                .name("Comprehensive Tax & GST Retainer FY 2026-27")
                .status(EngagementStatus.ACTIVE)
                .startDate(LocalDate.now().minusMonths(1))
                .build();

        MvcResult engResult = mockMvc.perform(post("/api/v1/engagements")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEngReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        UUID engagementId = UUID.fromString(extractJsonField(engResult, "$.data.id"));
        assertThat(engagementId).isNotNull();

        // 3. Create Compliance Obligation
        CreateComplianceObligationRequest createCompReq = CreateComplianceObligationRequest.builder()
                .clientId(clientId)
                .title("GSTR-3B Monthly Filing - July 2026")
                .obligationType(ComplianceObligationType.GST_RETURN)
                .periodLabel("July 2026")
                .financialYear("2026-27")
                .statutoryDueDate(LocalDate.now().plusDays(10))
                .status(ComplianceObligationStatus.UPCOMING)
                .priority(TaskPriority.HIGH)
                .assignedUserId(staffUserA.getId())
                .locationId(locA1.getId())
                .build();

        MvcResult compResult = mockMvc.perform(post("/api/v1/compliance/obligations")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCompReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        UUID obligationId = UUID.fromString(extractJsonField(compResult, "$.data.id"));
        assertThat(obligationId).isNotNull();

        // 4. Create Work Item
        CreateWorkItemRequest createWiReq = CreateWorkItemRequest.builder()
                .clientId(clientId)
                .locationId(locA1.getId())
                .title("GSTR-3B ITC Reconciliation & Filing Execution")
                .status(WorkItemStatus.TODO)
                .priority(WorkItemPriority.HIGH)
                .assignedUserId(staffUserA.getId())
                .dueDate(LocalDate.now().plusDays(8))
                .build();

        MvcResult wiResult = mockMvc.perform(post("/api/v1/work-items")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWiReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        UUID workItemId = UUID.fromString(extractJsonField(wiResult, "$.data.id"));
        assertThat(workItemId).isNotNull();

        // 5. Create Task
        CreateTaskRequest createTaskReq = CreateTaskRequest.builder()
                .clientId(clientId)
                .workItemId(workItemId)
                .locationId(locA1.getId())
                .assignedTo(staffUserA.getId())
                .title("Verify 2B vs Purchase Register ITC Mismatches")
                .priority(TaskPriority.HIGH)
                .taskCategory(TaskCategory.GST)
                .dueDate(LocalDate.now().plusDays(5))
                .build();

        MvcResult taskResult = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTaskReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        UUID taskId = UUID.fromString(extractJsonField(taskResult, "$.data.id"));
        assertThat(taskId).isNotNull();

        // 6. Create Document Request
        CreateDocumentRequest createDocReq = CreateDocumentRequest.builder()
                .clientId(clientId)
                .locationId(locA1.getId())
                .taskId(taskId)
                .purpose("July 2026 Purchase Register & Bank Statements")
                .dueDate(LocalDate.now().plusDays(3))
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.OTHER)
                                .title("July 2026 Purchase Invoices Summary")
                                .required(true)
                                .build()
                ))
                .build();

        MvcResult docReqResult = mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDocReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        UUID docRequestId = UUID.fromString(extractJsonField(docReqResult, "$.data.id"));
        assertThat(docRequestId).isNotNull();

        // 7. Upload Document
        UploadDocumentRequest uploadMetadata = UploadDocumentRequest.builder()
                .clientId(clientId)
                .locationId(locA1.getId())
                .taskId(taskId)
                .requestId(docRequestId)
                .documentType(DocumentType.OTHER)
                .financialYear("2026-27")
                .notes("Uploaded purchase register from client")
                .build();

        MockMultipartFile filePart = new MockMultipartFile(
                "file", "PurchaseRegister_July2026.pdf", "application/pdf", "%PDF-1.4 Dummy PDF content".getBytes());
        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(uploadMetadata));

        MvcResult docUploadResult = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        UUID documentId = UUID.fromString(extractJsonField(docUploadResult, "$.data.id"));
        assertThat(documentId).isNotNull();

        // 8. Create Tax Notice
        CreateTaxNoticeRequest createNoticeReq = CreateTaxNoticeRequest.builder()
                .clientId(clientId)
                .locationId(locA1.getId())
                .noticeNumber("NOT-GST-2026-001")
                .dinNumber("DIN-2026-GST-001")
                .department(NoticeDepartment.GST)
                .noticeType("SCRUTINY")
                .subject("ITC Discrepancy Notice ASMT-10")
                .receivedDate(LocalDate.now().minusDays(2))
                .responseDueDate(LocalDate.now().plusDays(20))
                .demandAmount(new BigDecimal("50000.00"))
                .priority(NoticePriority.HIGH)
                .build();

        MvcResult noticeResult = mockMvc.perform(post("/api/v1/notices")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createNoticeReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        UUID noticeId = UUID.fromString(extractJsonField(noticeResult, "$.data.id"));
        assertThat(noticeId).isNotNull();

        // 9. Create Invoice
        CreateInvoiceRequest createInvReq = CreateInvoiceRequest.builder()
                .clientId(clientId)
                .locationId(locA1.getId())
                .engagementId(engagementId)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(InvoiceStatus.ISSUED)
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.GST_FILING)
                                .description("Monthly GST Return Preparation & Scrutiny Advisory")
                                .quantity(BigDecimal.ONE)
                                .unitPrice(new BigDecimal("10000.00"))
                                .taxRate(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        MvcResult invResult = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createInvReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(11800.00))
                .andReturn();

        UUID invoiceId = UUID.fromString(extractJsonField(invResult, "$.data.id"));
        assertThat(invoiceId).isNotNull();

        // 10. Record Payment
        RecordPaymentRequest recordPaymentReq = RecordPaymentRequest.builder()
                .amount(new BigDecimal("11800.00"))
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .referenceNumber("NEFT-UTR-202607-001")
                .notes("Full settlement for July 2026 retainership")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordPaymentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(11800.00));

        // 11. Verify Practice Dashboard Overview reflects all created operational entities
        mockMvc.perform(get("/api/v1/dashboard/overview")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalClients").value(1))
                .andExpect(jsonPath("$.data.activeClients").value(1))
                .andExpect(jsonPath("$.data.activeEngagements").value(1))
                .andExpect(jsonPath("$.data.openComplianceObligations").value(1))
                .andExpect(jsonPath("$.data.openWorkItems").value(1))
                .andExpect(jsonPath("$.data.pendingTasks").value(2))
                .andExpect(jsonPath("$.data.pendingDocumentRequests").value(1))
                .andExpect(jsonPath("$.data.openTaxNotices").value(1))
                .andExpect(jsonPath("$.data.periodInvoicedAmount").value(11800.00))
                .andExpect(jsonPath("$.data.periodCollectedAmount").value(11800.00))
                .andExpect(jsonPath("$.data.outstandingBillingAmount").value(0.00));
    }

    // =========================================================================
    // E2E-02: Compliance -> Work Item -> Task Full Lifecycle
    // =========================================================================

    @Test
    @DisplayName("E2E-02: Compliance -> Work -> Task: Verifies creation, task completion, state transitions and dashboard updates")
    void shouldCreateComplianceWorkAndCompleteTask() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // Create Client
        ClientEntity client = clientRepository.save(createActiveClient(orgA.getId(), locA1.getId(), "Beta Traders Ltd"));

        // 1. Create Compliance Obligation
        CreateComplianceObligationRequest compReq = CreateComplianceObligationRequest.builder()
                .clientId(client.getId())
                .title("ITR-6 Corporate Filing FY 2025-26")
                .obligationType(ComplianceObligationType.ITR_FILING)
                .periodLabel("AY 2026-27")
                .statutoryDueDate(LocalDate.now().plusDays(15))
                .status(ComplianceObligationStatus.UPCOMING)
                .priority(TaskPriority.HIGH)
                .build();

        MvcResult compResult = mockMvc.perform(post("/api/v1/compliance/obligations")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID obligationId = UUID.fromString(extractJsonField(compResult, "$.data.id"));

        // 2. Create Work Item
        CreateWorkItemRequest wiReq = CreateWorkItemRequest.builder()
                .clientId(client.getId())
                .title("ITR-6 Preparation & Depreciation Computation")
                .status(WorkItemStatus.TODO)
                .priority(WorkItemPriority.HIGH)
                .assignedUserId(staffUserA.getId())
                .dueDate(LocalDate.now().plusDays(10))
                .build();

        MvcResult wiResult = mockMvc.perform(post("/api/v1/work-items")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wiReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID workItemId = UUID.fromString(extractJsonField(wiResult, "$.data.id"));

        // 3. Create Task
        CreateTaskRequest taskReq = CreateTaskRequest.builder()
                .clientId(client.getId())
                .workItemId(workItemId)
                .assignedTo(staffUserA.getId())
                .title("Compute Section 32 Depreciation Schedule")
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(5))
                .build();

        MvcResult taskResult = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID taskId = UUID.fromString(extractJsonField(taskResult, "$.data.id"));

        // 4. Update Task Status -> COMPLETED
        UpdateTaskRequest updateTask = UpdateTaskRequest.builder()
                .title("Compute Section 32 Depreciation Schedule")
                .status(TaskStatus.COMPLETED)
                .priority(TaskPriority.HIGH)
                .assignedTo(staffUserA.getId())
                .dueDate(LocalDate.now().plusDays(5))
                .build();

        mockMvc.perform(put("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 5. Update Work Item Status -> COMPLETED
        UpdateWorkItemStatusRequest updateWiStatus = UpdateWorkItemStatusRequest.builder()
                .status(WorkItemStatus.COMPLETED)
                .notes("All calculations and review finished")
                .build();

        mockMvc.perform(patch("/api/v1/work-items/" + workItemId + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateWiStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 6. Update Obligation Status -> COMPLETED
        UpdateObligationStatusRequest updateCompStatus = UpdateObligationStatusRequest.builder()
                .status(ComplianceObligationStatus.COMPLETED)
                .remarks("Filing confirmed on portal")
                .build();

        mockMvc.perform(patch("/api/v1/compliance/obligations/" + obligationId + "/status")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCompStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 7. Verify Work Dashboard reflects completion metrics
        mockMvc.perform(get("/api/v1/dashboard/work")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalWorkItems").value(1))
                .andExpect(jsonPath("$.data.workItemsCompleted").value(1))
                .andExpect(jsonPath("$.data.workItemsTodo").value(0))
                .andExpect(jsonPath("$.data.totalTasks").value(1))
                .andExpect(jsonPath("$.data.completedTasks").value(1))
                .andExpect(jsonPath("$.data.pendingTasks").value(0));

        // 8. Verify Compliance Dashboard reflects completed obligation
        mockMvc.perform(get("/api/v1/dashboard/compliance")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalObligations").value(1))
                .andExpect(jsonPath("$.data.completed").value(1))
                .andExpect(jsonPath("$.data.pending").value(0));
    }

    // =========================================================================
    // E2E-03: Document Workflow — Request, Upload, Metadata & Review
    // =========================================================================

    @Test
    @DisplayName("E2E-03: Document Workflow: Request -> Upload -> Status -> Dashboard Reflection")
    void shouldCompleteDocumentRequestWorkflow() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        ClientEntity client = clientRepository.save(createActiveClient(orgA.getId(), locA1.getId(), "Gamma Logistics LLP"));

        // 1. Create Document Request
        CreateDocumentRequest docReq = CreateDocumentRequest.builder()
                .clientId(client.getId())
                .purpose("TDS Q1 Verification")
                .dueDate(LocalDate.now().plusDays(7))
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.OTHER)
                                .title("Form 16A TDS Certificates")
                                .required(true)
                                .build()
                ))
                .build();

        MvcResult reqResult = mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andReturn();

        UUID requestId = UUID.fromString(extractJsonField(reqResult, "$.data.id"));
        UUID itemId = UUID.fromString(extractJsonField(reqResult, "$.data.items[0].id"));

        // 2. Upload Document against request item
        MockMultipartFile file = new MockMultipartFile(
                "file", "Form16A_Q1.pdf", "application/pdf", "%PDF-1.4 Sample PDF Data".getBytes());

        mockMvc.perform(multipart("/api/v1/document-requests/items/" + itemId + "/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("UPLOADED"));

        // 3. Accept Item to fulfill and complete Document Request
        mockMvc.perform(post("/api/v1/document-requests/items/" + itemId + "/accept")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 4. Verify Document Dashboard
        mockMvc.perform(get("/api/v1/dashboard/documents")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRequests").value(1))
                .andExpect(jsonPath("$.data.fulfilledRequests").value(1))
                .andExpect(jsonPath("$.data.pendingRequests").value(0));
    }

    // =========================================================================
    // E2E-04: Notice Workflow — Creation, Response, Hearing & Resolution
    // =========================================================================

    @Test
    @DisplayName("E2E-04: Notice Workflow: Notice Intake -> Response Draft -> Closure -> Notice Dashboard")
    void shouldCompleteNoticeResponseWorkflow() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        ClientEntity client = clientRepository.save(createActiveClient(orgA.getId(), locA1.getId(), "Delta Builders Pvt Ltd"));

        // 1. Create Tax Notice
        CreateTaxNoticeRequest noticeReq = CreateTaxNoticeRequest.builder()
                .clientId(client.getId())
                .noticeNumber("ITD-148-2026-99")
                .dinNumber("DIN-ITD-148-99")
                .department(NoticeDepartment.INCOME_TAX)
                .noticeType("REASSESSMENT")
                .subject("Section 148 Reassessment Notice for AY 2024-25")
                .receivedDate(LocalDate.now().minusDays(5))
                .responseDueDate(LocalDate.now().plusDays(25))
                .demandAmount(new BigDecimal("250000.00"))
                .priority(NoticePriority.CRITICAL)
                .responseRequired(true)
                .build();

        MvcResult noticeResult = mockMvc.perform(post("/api/v1/notices")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noticeReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andReturn();

        UUID noticeId = UUID.fromString(extractJsonField(noticeResult, "$.data.id"));

        // 2. Draft Response
        CreateNoticeResponseRequest responseReq = CreateNoticeResponseRequest.builder()
                .responseTitle("Objections to Reassessment under Section 148A")
                .responseSummary("We submit that the escapement of income is unfounded...")
                .legalGrounds("Section 148A(d) procedure not followed")
                .factsOfCase("Return was timely filed and all receipts declared")
                .build();

        mockMvc.perform(post("/api/v1/notices/" + noticeId + "/responses")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(responseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Close Notice
        CloseNoticeRequest closeReq = CloseNoticeRequest.builder()
                .closureStatus(NoticeStatus.RESOLVED)
                .closureDate(LocalDate.now())
                .closureRemarks("Assessing Officer dropped proceedings after hearing")
                .build();

        mockMvc.perform(post("/api/v1/notices/" + noticeId + "/close")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        // 4. Verify Notice Dashboard reflects resolved notice
        mockMvc.perform(get("/api/v1/dashboard/notices")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOpenNotices").value(0))
                .andExpect(jsonPath("$.data.noticesRequiringResponse").value(0));
    }

    // =========================================================================
    // E2E-05: Billing Workflow — Invoice Generation, Issue & Payment Lifecycle
    // =========================================================================

    @Test
    @DisplayName("E2E-05: Billing Workflow: Draft -> Issue -> Partial Payment -> Full Payment -> Billing Dashboard")
    void shouldCompleteInvoiceAndPaymentWorkflow() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        ClientEntity client = clientRepository.save(createActiveClient(orgA.getId(), locA1.getId(), "Epsilon Pharma Tech Ltd"));

        // 1. Create Draft Invoice
        CreateInvoiceRequest createInv = CreateInvoiceRequest.builder()
                .clientId(client.getId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .status(InvoiceStatus.DRAFT)
                .items(List.of(
                        CreateInvoiceItemRequest.builder()
                                .service(BillingServiceType.CONSULTING)
                                .description("Transfer Pricing & Cross-Border Tax Advisory")
                                .quantity(BigDecimal.ONE)
                                .unitPrice(new BigDecimal("50000.00"))
                                .taxRate(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        MvcResult invResult = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createInv)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.total").value(59000.00))
                .andReturn();

        UUID invoiceId = UUID.fromString(extractJsonField(invResult, "$.data.id"));

        // 2. Issue Invoice
        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/issue")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ISSUED"));

        // 3. Record Partial Payment (30,000 INR)
        RecordPaymentRequest partialPmt = RecordPaymentRequest.builder()
                .amount(new BigDecimal("30000.00"))
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .referenceNumber("NEFT-PARTIAL-001")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialPmt)))
                .andExpect(status().isCreated());

        // Verify invoice balance after partial payment
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.data.paidAmount").value(30000.00))
                .andExpect(jsonPath("$.data.balanceDue").value(29000.00));

        // 4. Record Final Settlement Payment (29,000 INR)
        RecordPaymentRequest finalPmt = RecordPaymentRequest.builder()
                .amount(new BigDecimal("29000.00"))
                .paymentDate(LocalDate.now())
                .paymentMethod(PaymentMethod.UPI)
                .referenceNumber("UPI-FINAL-002")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + adminTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalPmt)))
                .andExpect(status().isCreated());

        // Verify invoice is fully paid
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.paidAmount").value(59000.00))
                .andExpect(jsonPath("$.data.balanceDue").value(0.00));

        // 5. Verify Billing Dashboard reflects full realization
        mockMvc.perform(get("/api/v1/dashboard/billing")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalInvoices").value(1))
                .andExpect(jsonPath("$.data.paidInvoices").value(1))
                .andExpect(jsonPath("$.data.totalInvoicedAmount").value(59000.00))
                .andExpect(jsonPath("$.data.totalCollectedAmount").value(59000.00))
                .andExpect(jsonPath("$.data.totalOutstandingAmount").value(0.00))
                .andExpect(jsonPath("$.data.revenueByService.CONSULTING").value(59000.00));
    }

    // =========================================================================
    // E2E-06: Practice Dashboard Endpoints & Client Attention List
    // =========================================================================

    @Test
    @DisplayName("E2E-06: Practice Dashboard: Endpoints aggregation, operational KPIs and attention list sorting")
    void shouldReflectOperationalDataInPracticeDashboard() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // Client 1: Has open notice and overdue work item (High Attention)
        ClientEntity client1 = clientRepository.save(createActiveClient(orgA.getId(), locA1.getId(), "Urgent Attention Corp"));

        taxNoticeRepository.save(createNotice(orgA.getId(), client1.getId(), "NOT-URGENT-001", NoticeDepartment.GST, NoticeStatus.RECEIVED, true, LocalDate.now().minusDays(2)));
        workItemRepository.save(createWorkItem(orgA.getId(), client1.getId(), staffUserA.getId(), WorkItemStatus.IN_PROGRESS, LocalDate.now().minusDays(3)));

        // Client 2: Healthy client with completed items
        ClientEntity client2 = clientRepository.save(createActiveClient(orgA.getId(), locA1.getId(), "Healthy Compliant Corp"));
        complianceObligationRepository.save(createObligation(orgA.getId(), client2.getId(), ComplianceObligationStatus.COMPLETED, LocalDate.now().minusDays(10)));

        // 1. Check Overview
        mockMvc.perform(get("/api/v1/dashboard/overview")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClients").value(2))
                .andExpect(jsonPath("$.data.activeClients").value(2))
                .andExpect(jsonPath("$.data.openTaxNotices").value(1))
                .andExpect(jsonPath("$.data.overdueWorkItems").value(1));

        // 2. Check Client Dashboard & Attention List
        mockMvc.perform(get("/api/v1/dashboard/clients")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClients").value(2))
                .andExpect(jsonPath("$.data.clientsWithOpenNotices").value(1))
                .andExpect(jsonPath("$.data.clientsWithOverdueWork").value(1))
                .andExpect(jsonPath("$.data.attentionList", hasSize(1)))
                .andExpect(jsonPath("$.data.attentionList[0].clientId").value(client1.getId().toString()))
                .andExpect(jsonPath("$.data.attentionList[0].openNoticesCount").value(1))
                .andExpect(jsonPath("$.data.attentionList[0].overdueWorkCount").value(1));

        // 3. Check Legacy Organization Dashboard compatibility
        mockMvc.perform(get("/api/v1/dashboard/organization")
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clients.total").value(2));
    }

    // =========================================================================
    // E2E-07: Security, Tenant Isolation, Location & Portfolio Scoping
    // =========================================================================

    @Test
    @DisplayName("E2E-07: Security & Isolation: Tenant isolation, location scoping, portfolio boundary and billing access gates")
    void shouldEnforceSecurityTenantLocationAndPortfolioScoping() throws Exception {
        // Setup: Org A has Client A1 (Loc 1) and Client A2 (Loc 2)
        TenantContext.setTenantId(orgA.getId());
        ClientEntity clientA1 = clientRepository.save(createActiveClient(orgA.getId(), locA1.getId(), "Org A Client 1"));
        ClientEntity clientA2 = clientRepository.save(createActiveClient(orgA.getId(), locA2.getId(), "Org A Client 2"));

        // Setup: Org B has Client B1
        TenantContext.setTenantId(orgB.getId());
        ClientEntity clientB1 = clientRepository.save(createActiveClient(orgB.getId(), null, "Org B Competitor Client"));

        // 1. Cross-Tenant Data Isolation (Org A Admin trying to access Org B Client) -> Must be 404
        mockMvc.perform(get("/api/v1/clients/" + clientB1.getId())
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isNotFound());

        // 2. Cross-Tenant Data Isolation (Org B Admin trying to access Org A Client) -> Must be 404
        mockMvc.perform(get("/api/v1/clients/" + clientA1.getId())
                        .header("Authorization", "Bearer " + adminTokenB))
                .andExpect(status().isNotFound());

        // 3. Location Scoping: Org A Admin querying unknown / cross-tenant location ID -> 404 Not Found
        mockMvc.perform(get("/api/v1/dashboard/clients?locationId=" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminTokenA))
                .andExpect(status().isNotFound());

        // 4. Billing Permission Gate: Staff user without BILLING_VIEW authority attempting to access billing dashboard -> 403 Forbidden
        mockMvc.perform(get("/api/v1/dashboard/billing")
                        .header("Authorization", "Bearer " + staffTokenA))
                .andExpect(status().isForbidden());

        // 5. Unauthenticated request -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String extractJsonField(MvcResult result, String jsonPath) throws Exception {
        String content = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(content);
        // Supports simple $.data.id extraction
        if (jsonPath.equals("$.data.id")) {
            return root.path("data").path("id").asText();
        }
        String pointer = jsonPath.replace("$.", "/")
                .replace(".", "/")
                .replaceAll("\\[(\\d+)\\]", "/$1");
        return root.at(pointer).asText();
    }

    private ClientEntity createActiveClient(UUID orgId, UUID locationId, String name) {
        ClientEntity c = ClientEntity.builder()
                .locationId(locationId)
                .displayName(name)
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientStatus.ACTIVE)
                .pan("AAAAA" + (int)(1000 + Math.random() * 8999) + "A")
                .gstin("27AAAAA" + (int)(1000 + Math.random() * 8999) + "A1Z1")
                .build();
        c.setOrganizationId(orgId);
        return c;
    }

    private TaxNoticeEntity createNotice(UUID orgId, UUID clientId, String number, NoticeDepartment dept, NoticeStatus status, boolean respReq, LocalDate dueDate) {
        TaxNoticeEntity n = TaxNoticeEntity.builder()
                .clientId(clientId)
                .noticeNumber(number)
                .dinNumber("DIN-" + number)
                .department(dept)
                .noticeType("SCRUTINY")
                .subject("Statutory Scrutiny Notice")
                .receivedDate(LocalDate.now().minusDays(5))
                .responseDueDate(dueDate)
                .status(status)
                .priority(NoticePriority.HIGH)
                .responseRequired(respReq)
                .build();
        n.setOrganizationId(orgId);
        return n;
    }

    private WorkItemEntity createWorkItem(UUID orgId, UUID clientId, UUID assignedUserId, WorkItemStatus status, LocalDate dueDate) {
        WorkItemEntity wi = WorkItemEntity.builder()
                .clientId(clientId)
                .assignedUserId(assignedUserId)
                .title("Statutory Action Item")
                .status(status)
                .priority(WorkItemPriority.HIGH)
                .dueDate(dueDate)
                .build();
        wi.setOrganizationId(orgId);
        return wi;
    }

    private ComplianceObligationEntity createObligation(UUID orgId, UUID clientId, ComplianceObligationStatus status, LocalDate dueDate) {
        ComplianceObligationEntity c = ComplianceObligationEntity.builder()
                .clientId(clientId)
                .title("GST Return Obligation")
                .obligationType(ComplianceObligationType.GST_RETURN)
                .periodLabel("Monthly")
                .statutoryDueDate(dueDate)
                .status(status)
                .build();
        c.setOrganizationId(orgId);
        return c;
    }
}
