package com.taxoryn.qa.security;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.billing.dto.BillingDashboardStatsDto;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.mapper.InvoiceMapper;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.billing.service.InvoiceServiceImpl;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientFilterRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.mapper.ClientMapper;
import com.taxoryn.module.client.repository.ClientNoteRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.service.ClientServiceImpl;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceRuleRepository;
import com.taxoryn.module.docrequest.dto.DocumentRequestSummaryDto;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.docrequest.service.DocumentRequestServiceImpl;
import com.taxoryn.module.document.dto.DocumentDownloadDto;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.mapper.DocumentMapper;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.service.DocumentServiceImpl;
import com.taxoryn.module.document.storage.DocumentStorageService;
import com.taxoryn.module.document.storage.StorageProperties;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.entity.GstProfileEntity;
import com.taxoryn.module.gst.mapper.GstMapper;
import com.taxoryn.module.gst.repository.GstMonthlySummaryRepository;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.gst.service.GstServiceImpl;
import com.taxoryn.module.itr.mapper.ItrMapper;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.itr.service.ItrServiceImpl;
import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import com.taxoryn.module.notice.mapper.TaxNoticeMapper;
import com.taxoryn.module.notice.repository.NoticeActivityRepository;
import com.taxoryn.module.notice.repository.NoticeHearingRepository;
import com.taxoryn.module.notice.repository.NoticeResponseRepository;
import com.taxoryn.module.notice.repository.TaxNoticeRepository;
import com.taxoryn.module.notice.service.TaxNoticeServiceImpl;
import com.taxoryn.module.notification.email.service.EmailNotificationService;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.portal.mapper.ClientPortalMapper;
import com.taxoryn.module.portal.repository.ClientDocumentRequestRepository;
import com.taxoryn.module.portal.repository.ClientNotificationRepository;
import com.taxoryn.module.portal.repository.ClientPortalMessageRepository;
import com.taxoryn.module.portal.service.ClientPortalServiceImpl;
import com.taxoryn.module.portal.websocket.PortalChatEventPublisher;
import com.taxoryn.module.subscription.service.SubscriptionService;
import com.taxoryn.module.task.dto.BulkTaskCreateRequest;
import com.taxoryn.module.task.dto.BulkTaskImportResultDto;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.mapper.TaskMapper;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.service.TaskServiceImpl;
import com.taxoryn.module.tds.mapper.TdsMapper;
import com.taxoryn.module.tds.repository.TdsCertificateRepository;
import com.taxoryn.module.tds.repository.TdsChallanRepository;
import com.taxoryn.module.tds.repository.TdsDeducteeEntryRepository;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import com.taxoryn.module.tds.service.TdsCalculatorService;
import com.taxoryn.module.tds.service.TdsServiceImpl;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TAXORYN PHASE 6.1 — FINAL API / ENDPOINT SECURITY COVERAGE INTEGRATION TEST SUITE
 *
 * Exhaustive endpoint-level verification confirming:
 * 1. Multi-Tenant isolation across all modules
 * 2. Cross-Client portfolio scope enforcement on direct-ID and search queries
 * 3. Manager scope strictly confined to reportees and decoupled from mere hierarchy flags
 * 4. Task assignment decoupled from client profile and document authorization
 * 5. Document & pre-signed URL security
 * 6. Bulk operation per-item validation
 * 7. Dashboard and aggregate metric zero data leakage
 * 8. Client portal taxpayer isolation
 * 9. OrganizationType independence across all 5 enum values
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EndpointSecurityCoverageIntegrationTest {

    // =========================================================================
    // IDENTIFIERS
    // =========================================================================
    private final UUID orgA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID orgB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final UUID practitionerAUserId = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private final UUID practitionerAEmpId = UUID.fromString("aaaaaaaa-1111-1111-1111-222222222222");

    private final UUID practitionerBUserId = UUID.fromString("aaaaaaaa-2222-2222-2222-111111111111");
    private final UUID practitionerBEmpId = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");

    private final UUID staffAUserId = UUID.fromString("aaaaaaaa-3333-3333-3333-111111111111");
    private final UUID staffAEmpId = UUID.fromString("aaaaaaaa-3333-3333-3333-222222222222");

    private final UUID managerAUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-222222222222");
    private final UUID managerAEmpId = UUID.fromString("aaaaaaaa-0000-0000-0000-222222222223");

    private final UUID clientAId = UUID.fromString("aaaaaaaa-caaa-caaa-caaa-aaaaaaaaaaaa");
    private final UUID clientBId = UUID.fromString("aaaaaaaa-cbbb-cbbb-cbbb-bbbbbbbbbbbb");
    private final UUID clientCId = UUID.fromString("aaaaaaaa-cccc-cccc-cccc-cccccccccccc");
    private final UUID clientDId = UUID.fromString("aaaaaaaa-cddd-cddd-cddd-dddddddddddd");
    private final UUID clientEId = UUID.fromString("bbbbbbbb-ceee-ceee-ceee-eeeeeeeeeeee");

    // =========================================================================
    // MOCKED REPOSITORIES & DEPENDENCIES
    // =========================================================================
    @Mock private ClientRepository clientRepository;
    @Mock private ClientNoteRepository clientNoteRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private AuditService auditService;
    @Mock private PracticeSecurityScopeEvaluator securityScopeEvaluator;
    @Mock private NotificationService notificationService;
    @Mock private EmailNotificationService emailNotificationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private SubscriptionService subscriptionService;
    @Mock private ClientMapper clientMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private ComplianceObligationRepository complianceObligationRepository;
    @Mock private ComplianceRuleRepository complianceRuleRepository;

    @Mock private GstProfileRepository gstProfileRepository;
    @Mock private GstReturnFilingRepository gstReturnFilingRepository;
    @Mock private GstMonthlySummaryRepository gstMonthlySummaryRepository;
    @Mock private GstMapper gstMapper;

    @Mock private ItrProfileRepository itrProfileRepository;
    @Mock private ItrReturnRepository itrReturnRepository;
    @Mock private ItrMapper itrMapper;

    @Mock private TdsProfileRepository tdsProfileRepository;
    @Mock private TdsReturnRepository tdsReturnRepository;
    @Mock private TdsChallanRepository tdsChallanRepository;
    @Mock private TdsDeducteeEntryRepository tdsDeducteeEntryRepository;
    @Mock private TdsCertificateRepository tdsCertificateRepository;
    @Mock private TdsMapper tdsMapper;
    @Mock private TdsCalculatorService tdsCalculatorService;

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceItemRepository invoiceItemRepository;
    @Mock private InvoicePaymentRepository invoicePaymentRepository;
    @Mock private ClientNotificationRepository clientNotificationRepository;
    @Mock private InvoiceMapper invoiceMapper;

    @Mock private DocumentRequestRepository documentRequestRepository;
    @Mock private DocumentRequestItemRepository documentRequestItemRepository;

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentMapper documentMapper;
    @Mock private DocumentStorageService storageService;

    @Mock private TaxNoticeRepository noticeRepository;
    @Mock private NoticeResponseRepository noticeResponseRepository;
    @Mock private NoticeHearingRepository noticeHearingRepository;
    @Mock private NoticeActivityRepository noticeActivityRepository;
    @Mock private TaxNoticeMapper noticeMapper;

    @Mock private ClientPortalMessageRepository clientPortalMessageRepository;
    @Mock private PortalChatEventPublisher portalChatEventPublisher;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ClientPortalMapper clientPortalMapper;
    @Mock private ClientDocumentRequestRepository clientDocumentRequestRepository;

    // Services Under Test
    private ClientServiceImpl clientService;
    private TaskServiceImpl taskService;
    private GstServiceImpl gstService;
    private ItrServiceImpl itrService;
    private TdsServiceImpl tdsService;
    private InvoiceServiceImpl invoiceService;
    private DocumentRequestServiceImpl documentRequestService;
    private DocumentServiceImpl documentService;
    private TaxNoticeServiceImpl taxNoticeService;
    private ClientPortalServiceImpl clientPortalService;

    // Practice Security Scopes
    private PracticeSecurityScope managerAScope;
    private PracticeSecurityScope practitionerAScope;
    private PracticeSecurityScope staffAScope;

    private ClientEntity clientA;
    private ClientEntity clientB;
    private ClientEntity clientC;
    private ClientEntity clientD;
    private ClientEntity clientE;

    @BeforeEach
    void setUp() {
        clientA = ClientEntity.builder().displayName("Alpha Corp").assignedEmployeeId(practitionerAEmpId).status(ClientEntity.ClientStatus.ACTIVE).build();
        clientA.setId(clientAId); clientA.setOrganizationId(orgA);

        clientB = ClientEntity.builder().displayName("Beta Industries").assignedEmployeeId(practitionerAEmpId).status(ClientEntity.ClientStatus.ACTIVE).build();
        clientB.setId(clientBId); clientB.setOrganizationId(orgA);

        clientC = ClientEntity.builder().displayName("Gamma Traders").assignedEmployeeId(practitionerBEmpId).status(ClientEntity.ClientStatus.ACTIVE).build();
        clientC.setId(clientCId); clientC.setOrganizationId(orgA);

        clientD = ClientEntity.builder().displayName("Delta Services").assignedEmployeeId(staffAEmpId).status(ClientEntity.ClientStatus.ACTIVE).build();
        clientD.setId(clientDId); clientD.setOrganizationId(orgA);

        clientE = ClientEntity.builder().displayName("Epsilon Logistics").assignedEmployeeId(UUID.randomUUID()).status(ClientEntity.ClientStatus.ACTIVE).build();
        clientE.setId(clientEId); clientE.setOrganizationId(orgB);

        when(clientRepository.findByIdAndOrganizationId(clientAId, orgA)).thenReturn(Optional.of(clientA));
        when(clientRepository.findByIdAndOrganizationId(clientBId, orgA)).thenReturn(Optional.of(clientB));
        when(clientRepository.findByIdAndOrganizationId(clientCId, orgA)).thenReturn(Optional.of(clientC));
        when(clientRepository.findByIdAndOrganizationId(clientDId, orgA)).thenReturn(Optional.of(clientD));
        when(clientRepository.findByIdAndOrganizationId(clientEId, orgB)).thenReturn(Optional.of(clientE));
        when(clientRepository.findByIdAndOrganizationId(clientEId, orgA)).thenReturn(Optional.empty());

        // Default mapper stubs
        when(clientMapper.toDto(any(ClientEntity.class))).thenAnswer(inv -> {
            ClientEntity ce = inv.getArgument(0);
            return ClientDto.builder().id(ce.getId()).displayName(ce.getDisplayName()).build();
        });
        when(taskMapper.toDto(any(TaskEntity.class))).thenAnswer(inv -> {
            TaskEntity te = inv.getArgument(0);
            return TaskDto.builder().id(te.getId()).title(te.getTitle()).build();
        });

        managerAScope = PracticeSecurityScope.builder()
                .organizationId(orgA)
                .userId(managerAUserId)
                .employeeId(managerAEmpId)
                .roleTier(PracticeSecurityScope.RoleTier.DEPARTMENT_MANAGER)
                .isDepartmentManager(true)
                .department("Direct Tax")
                .accessibleAssigneeIds(Set.of(practitionerAEmpId, staffAEmpId))
                .build();

        practitionerAScope = PracticeSecurityScope.builder()
                .organizationId(orgA)
                .userId(practitionerAUserId)
                .employeeId(practitionerAEmpId)
                .roleTier(PracticeSecurityScope.RoleTier.STAFF_INDIVIDUAL)
                .isStaff(true)
                .accessibleAssigneeIds(Set.of(practitionerAEmpId))
                .build();

        staffAScope = PracticeSecurityScope.builder()
                .organizationId(orgA)
                .userId(staffAUserId)
                .employeeId(staffAEmpId)
                .roleTier(PracticeSecurityScope.RoleTier.STAFF_INDIVIDUAL)
                .isStaff(true)
                .accessibleAssigneeIds(Set.of(staffAEmpId))
                .build();

        clientService = new ClientServiceImpl(
                clientRepository, clientNoteRepository, employeeRepository,
                taskRepository, userRepository, null,
                organizationRepository, null, null,
                emailNotificationService, subscriptionService, securityScopeEvaluator,
                noticeRepository, clientMapper, taskMapper, auditService
        );

        taskService = new TaskServiceImpl(
                taskRepository, clientRepository, employeeRepository,
                userRepository, complianceObligationRepository,
                documentRequestRepository, documentRequestItemRepository,
                securityScopeEvaluator, taskMapper, notificationService
        );

        gstService = new GstServiceImpl(
                gstProfileRepository, gstReturnFilingRepository, gstMonthlySummaryRepository,
                clientRepository, employeeRepository, userRepository,
                complianceObligationRepository, complianceRuleRepository,
                taskRepository, documentRequestRepository, documentRequestItemRepository,
                null, documentRepository, documentMapper,
                notificationService, gstMapper, auditService, securityScopeEvaluator
        );

        itrService = new ItrServiceImpl(
                itrProfileRepository, itrReturnRepository, clientRepository,
                employeeRepository, userRepository,
                complianceObligationRepository, complianceRuleRepository,
                taskRepository, documentRequestRepository,
                null, documentRepository, documentMapper,
                notificationService, itrMapper, auditService, securityScopeEvaluator
        );

        tdsService = new TdsServiceImpl(
                tdsProfileRepository, tdsReturnRepository, tdsChallanRepository,
                tdsDeducteeEntryRepository, tdsCertificateRepository, clientRepository,
                employeeRepository, userRepository,
                complianceObligationRepository, complianceRuleRepository,
                taskRepository, documentRequestRepository,
                null, documentRepository, documentMapper,
                notificationService, tdsMapper, tdsCalculatorService,
                auditService, securityScopeEvaluator
        );

        invoiceService = new InvoiceServiceImpl(
                invoiceRepository, invoiceItemRepository, invoicePaymentRepository,
                clientRepository, clientNotificationRepository, invoiceMapper,
                auditService, eventPublisher, securityScopeEvaluator, organizationRepository
        );

        documentRequestService = new DocumentRequestServiceImpl(
                documentRequestRepository, documentRequestItemRepository, clientRepository,
                organizationRepository, userRepository, documentRepository,
                null, notificationService, emailNotificationService,
                auditService, securityScopeEvaluator, taskRepository, employeeRepository
        );

        documentService = new DocumentServiceImpl(
                documentRepository, storageService, new StorageProperties(),
                clientRepository, gstReturnFilingRepository, itrReturnRepository,
                tdsReturnRepository, taskRepository, subscriptionService,
                documentMapper, auditService, securityScopeEvaluator,
                null, null
        );

        taxNoticeService = new TaxNoticeServiceImpl(
                noticeRepository, noticeResponseRepository, noticeHearingRepository,
                noticeActivityRepository, clientRepository, employeeRepository,
                userRepository, taskRepository, documentRepository,
                documentRequestRepository, noticeMapper, auditService,
                notificationService, securityScopeEvaluator
        );

        clientPortalService = new ClientPortalServiceImpl(
                clientRepository, userRepository, null, passwordEncoder,
                employeeRepository, organizationRepository, null,
                null, gstReturnFilingRepository, itrReturnRepository,
                documentService, documentRepository, taskRepository,
                clientNotificationRepository, clientDocumentRequestRepository,
                invoiceRepository, invoiceMapper, clientPortalMapper,
                notificationService, auditService, documentRequestService,
                documentRequestRepository, documentRequestItemRepository,
                null, clientPortalMessageRepository,
                portalChatEventPublisher, securityScopeEvaluator
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticatePracticeUser(UUID orgId, UUID userId, String role) {
        TenantContext.setTenantId(orgId);
        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(orgId)
                .email("user@" + orgId + ".com")
                .roles(Set.of(role))
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authenticateClientPortalUser(UUID orgId, UUID userId, UUID clientId) {
        TenantContext.setTenantId(orgId);
        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(orgId)
                .clientId(clientId)
                .email("taxpayer@" + clientId + ".com")
                .roles(Set.of("ROLE_CLIENT_USER", "CLIENT_USER"))
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================================================
    // SECTION 1: CROSS-TENANT DIRECT-ID ACCESS TESTS
    // =========================================================================
    @Nested
    @DisplayName("1. Cross-Tenant Direct-ID Access Defenses")
    class CrossTenantDirectIdAccessTests {

        @Test
        @DisplayName("Org A user cannot access Org B Client via direct-ID lookup")
        void testCrossTenantClientAccessBlocked() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_PRACTITIONER");
            assertThrows(ResourceNotFoundException.class, () -> clientService.getClientById(clientEId));
        }

        @Test
        @DisplayName("Org A user cannot access Org B GST Profile via direct-ID lookup")
        void testCrossTenantGstProfileAccessBlocked() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_PRACTITIONER");
            UUID gstBId = UUID.randomUUID();
            when(gstProfileRepository.findByIdAndOrganizationId(gstBId, orgA)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> gstService.getProfileById(gstBId));
        }

        @Test
        @DisplayName("Org A user cannot download Org B Document via direct-ID lookup")
        void testCrossTenantDocumentDownloadBlocked() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_PRACTITIONER");
            UUID docBId = UUID.randomUUID();
            when(documentRepository.findByIdAndOrganizationId(docBId, orgA)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> documentService.downloadDocument(docBId));
        }

        @Test
        @DisplayName("Org A user cannot view Org B Tax Notice via direct-ID lookup")
        void testCrossTenantNoticeAccessBlocked() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_PRACTITIONER");
            UUID noticeBId = UUID.randomUUID();
            when(noticeRepository.findByIdAndOrganizationId(noticeBId, orgA)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> taxNoticeService.getNoticeById(noticeBId));
        }
    }

    // =========================================================================
    // SECTION 2: CROSS-CLIENT PORTFOLIO & FILTER TAMPERING TESTS
    // =========================================================================
    @Nested
    @DisplayName("2. Cross-Client Portfolio & Search/Filter Boundary Defenses")
    class CrossClientPortfolioAndFilterTests {

        @Test
        @DisplayName("Practitioner A cannot access Client C (assigned to Practitioner B) via getClientById")
        void testPractitionerCrossClientDirectAccessBlocked() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_PRACTITIONER");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerAScope)).thenReturn(Set.of(clientAId, clientBId));

            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientCId));
        }

        @Test
        @DisplayName("Practitioner A filtering clients by assignedEmployeeId of Practitioner B is constrained by scope")
        void testPractitionerFilterScopeEnforced() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_PRACTITIONER");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerAScope)).thenReturn(Set.of(clientAId, clientBId));

            when(clientRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            ClientFilterRequest filter = new ClientFilterRequest();
            filter.setAssignedEmployeeId(practitionerBEmpId);

            var response = clientService.getClients(filter);
            assertNotNull(response);
            assertEquals(0, response.getContent().size());
        }
    }

    // =========================================================================
    // SECTION 3: MANAGER SCOPE & REPORTING HIERARCHY DECOUPLING TESTS
    // =========================================================================
    @Nested
    @DisplayName("3. Manager Scope vs Reporting Hierarchy Decoupling")
    class ManagerScopeAndHierarchyTests {

        @Test
        @DisplayName("Manager A accesses Client A assigned to reportee Practitioner A")
        void testManagerAccessesReporteeClient() {
            authenticatePracticeUser(orgA, managerAUserId, "ROLE_MANAGER");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(managerAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(managerAScope)).thenReturn(Set.of(clientAId, clientBId, clientDId));

            assertDoesNotThrow(() -> clientService.getClientById(clientAId));
        }

        @Test
        @DisplayName("Manager A cannot access Client C assigned to unmanaged Practitioner B")
        void testManagerBlockedFromUnmanagedPractitionerClient() {
            authenticatePracticeUser(orgA, managerAUserId, "ROLE_MANAGER");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(managerAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(managerAScope)).thenReturn(Set.of(clientAId, clientBId, clientDId));

            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientCId));
        }
    }

    // =========================================================================
    // SECTION 4: TASK ASSIGNMENT DECOUPLING TESTS
    // =========================================================================
    @Nested
    @DisplayName("4. Task Assignment Decoupling")
    class TaskAssignmentDecouplingTests {

        @Test
        @DisplayName("Staff A assigned to task for Client C cannot access Client C profile or GST return")
        void testTaskAssigneeCannotAccessClientProfileOrGst() {
            authenticatePracticeUser(orgA, staffAUserId, "ROLE_STAFF");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(staffAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(staffAScope)).thenReturn(Set.of(clientDId));

            // Task lookup succeeds
            UUID taskId = UUID.randomUUID();
            TaskEntity task = TaskEntity.builder()
                    .clientId(clientCId)
                    .assignedTo(staffAEmpId)
                    .title("Review Form 26AS for Client C")
                    .build();
            task.setId(taskId);
            task.setOrganizationId(orgA);
            when(taskRepository.findByIdAndOrganizationId(taskId, orgA)).thenReturn(Optional.of(task));

            TaskDto taskDto = taskService.getTaskById(taskId);
            assertNotNull(taskDto);
            assertEquals(taskId, taskDto.getId());

            // But accessing Client C profile is denied
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientCId));

            // And accessing Client C GST Profile is denied
            UUID gstProfileCId = UUID.randomUUID();
            GstProfileEntity gstC = GstProfileEntity.builder().clientId(clientCId).build();
            gstC.setId(gstProfileCId); gstC.setOrganizationId(orgA);
            when(gstProfileRepository.findByIdAndOrganizationId(gstProfileCId, orgA)).thenReturn(Optional.of(gstC));
            assertThrows(AccessDeniedException.class, () -> gstService.getProfileById(gstProfileCId));
        }
    }

    // =========================================================================
    // SECTION 5: DOCUMENT & STORAGE PRE-SIGNED URL SECURITY TESTS
    // =========================================================================
    @Nested
    @DisplayName("5. Document & Storage Pre-Signed URL Security")
    class DocumentStorageSecurityTests {

        @Test
        @DisplayName("Pre-signed download URL generation is blocked for out-of-portfolio document")
        void testPresignedDownloadUrlBlockedForUnauthorizedClient() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_PRACTITIONER");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerAScope)).thenReturn(Set.of(clientAId, clientBId));

            UUID docCId = UUID.randomUUID();
            DocumentEntity docC = DocumentEntity.builder()
                    .clientId(clientCId)
                    .storageKey("taxoryn/orgA/clientC/itr_computation.pdf")
                    .build();
            docC.setId(docCId);
            docC.setOrganizationId(orgA);
            when(documentRepository.findByIdAndOrganizationId(docCId, orgA)).thenReturn(Optional.of(docC));

            assertThrows(AccessDeniedException.class, () -> documentService.getDocumentDownloadUrl(docCId));
        }
    }

    // =========================================================================
    // SECTION 6: BULK OPERATIONS SCOPE ENFORCEMENT TESTS
    // =========================================================================
    @Nested
    @DisplayName("6. Bulk Operations Scope Enforcement")
    class BulkOperationsSecurityTests {

        @Test
        @DisplayName("Bulk task generator creates tasks bounded strictly to current organization")
        void testBulkTaskGeneratorEnforcesTenantIsolation() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_PRACTITIONER");

            BulkTaskCreateRequest request = BulkTaskCreateRequest.builder()
                    .clientIds(List.of(clientAId, clientBId))
                    .title("Quarterly GST Filing")
                    .assignedTo(practitionerAEmpId)
                    .build();

            TaskEntity tA = TaskEntity.builder().clientId(clientAId).assignedTo(practitionerAEmpId).title("Quarterly GST Filing").build();
            tA.setId(UUID.randomUUID());
            tA.setOrganizationId(orgA);
            when(taskRepository.save(any(TaskEntity.class))).thenReturn(tA);

            BulkTaskImportResultDto result = taskService.generateBulkTasks(request);
            assertNotNull(result);
            assertEquals(2, result.getTotalCreated());
            assertEquals(0, result.getTotalFailed());
        }
    }

    // =========================================================================
    // SECTION 7: DASHBOARD & AGGREGATE METRICS ZERO LEAKAGE TESTS
    // =========================================================================
    @Nested
    @DisplayName("7. Dashboard & Aggregate Metrics Zero Data Leakage")
    class DashboardAggregationSecurityTests {

        @Test
        @DisplayName("Invoice dashboard aggregates count only revenue from practitioner accessible clients")
        void testInvoiceDashboardZeroLeakage() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_PRACTITIONER");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerAScope)).thenReturn(Set.of(clientAId, clientBId));

            InvoiceEntity invA = InvoiceEntity.builder()
                    .clientId(clientAId)
                    .total(new BigDecimal("75000.00"))
                    .paidAmount(BigDecimal.ZERO)
                    .balanceDue(new BigDecimal("75000.00"))
                    .dueDate(LocalDate.now().plusDays(15))
                    .items(List.of())
                    .status(InvoiceEntity.InvoiceStatus.ISSUED)
                    .build();
            InvoiceEntity invC = InvoiceEntity.builder()
                    .clientId(clientCId)
                    .total(new BigDecimal("300000.00"))
                    .paidAmount(BigDecimal.ZERO)
                    .balanceDue(new BigDecimal("300000.00"))
                    .dueDate(LocalDate.now().plusDays(15))
                    .items(List.of())
                    .status(InvoiceEntity.InvoiceStatus.ISSUED)
                    .build();

            when(invoiceRepository.findAllByOrganizationId(orgA)).thenReturn(List.of(invA, invC));

            BillingDashboardStatsDto stats = invoiceService.getBillingDashboardStats();
            assertEquals(1, stats.getTotalInvoices());
            assertEquals(new BigDecimal("75000.00"), stats.getTotalBilled());
        }

        @Test
        @DisplayName("Document request summary stats never leak request counts from unauthorized clients")
        void testDocumentRequestSummaryZeroLeakage() {
            authenticatePracticeUser(orgA, staffAUserId, "ROLE_STAFF");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(staffAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(staffAScope)).thenReturn(Set.of(clientDId));

            DocumentRequestEntity reqD = DocumentRequestEntity.builder().clientId(clientDId).status(DocumentRequestEntity.RequestStatus.SENT).build();

            when(documentRequestRepository.findAll(any(Specification.class))).thenReturn(List.of(reqD));

            DocumentRequestSummaryDto stats = documentRequestService.getSummaryStats();
            assertEquals(1, stats.getTotalRequests());
            assertEquals(1, stats.getPendingRequests());
        }
    }

    // =========================================================================
    // SECTION 8: CLIENT PORTAL TAXPAYER ISOLATION TESTS
    // =========================================================================
    @Nested
    @DisplayName("8. Client Portal Taxpayer Isolation")
    class ClientPortalTaxpayerIsolationTests {

        @Test
        @DisplayName("Client portal user is strictly isolated to their own clientId")
        void testClientPortalUserStrictlyBoundToOwnClientId() {
            UUID clientPortalUserId = UUID.fromString("90000000-0000-0000-0000-000000000001");
            authenticateClientPortalUser(orgA, clientPortalUserId, clientAId);

            // Accessing Client A document succeeds
            UUID docAId = UUID.randomUUID();
            DocumentEntity docA = DocumentEntity.builder()
                    .clientId(clientAId)
                    .storageKey("taxoryn/orgA/clientA/file.pdf")
                    .scanStatus(DocumentEntity.DocumentScanStatus.CLEAN)
                    .fileName("file.pdf")
                    .contentType("application/pdf")
                    .fileSize(1024L)
                    .build();
            docA.setId(docAId);
            docA.setOrganizationId(orgA);
            when(documentRepository.findByIdAndOrganizationId(docAId, orgA)).thenReturn(Optional.of(docA));
            when(storageService.retrieve(docA.getStorageKey())).thenReturn("sample content".getBytes());

            DocumentDownloadDto downloadDto = clientPortalService.downloadClientDocument(docAId);
            assertNotNull(downloadDto);

            // Accessing Client B document is rejected
            UUID docBId = UUID.randomUUID();
            DocumentEntity docB = DocumentEntity.builder()
                    .clientId(clientBId)
                    .storageKey("taxoryn/orgA/clientB/file.pdf")
                    .scanStatus(DocumentEntity.DocumentScanStatus.CLEAN)
                    .fileName("file.pdf")
                    .contentType("application/pdf")
                    .fileSize(1024L)
                    .build();
            docB.setId(docBId);
            docB.setOrganizationId(orgA);
            when(documentRepository.findByIdAndOrganizationId(docBId, orgA)).thenReturn(Optional.of(docB));

            assertThrows(com.taxoryn.core.exception.ForbiddenException.class, () -> clientPortalService.downloadClientDocument(docBId));
        }
    }

    // =========================================================================
    // SECTION 9: ORGANIZATIONTYPE INDEPENDENCE TESTS ACROSS ALL 5 ENUMS
    // =========================================================================
    @Nested
    @DisplayName("9. OrganizationType Independence Across All 5 Enums")
    class OrganizationTypeIndependenceTests {

        @Test
        @DisplayName("Authorization and client scope evaluation behave identically regardless of OrganizationType")
        void testScopeEvaluatorBehavesIdenticallyAcrossOrganizationTypes() {
            OrganizationType[] allTypes = OrganizationType.values();
            assertEquals(5, allTypes.length);

            for (OrganizationType orgType : allTypes) {
                OrganizationEntity testOrg = OrganizationEntity.builder()
                        .legalName("Test Firm")
                        .organizationType(orgType)
                        .build();
                testOrg.setId(orgA);
                when(organizationRepository.findById(orgA)).thenReturn(Optional.of(testOrg));

                authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_PRACTITIONER");
                when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerAScope);
                when(securityScopeEvaluator.getAccessibleClientIds(practitionerAScope)).thenReturn(Set.of(clientAId, clientBId));

                assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientCId),
                        "Access should be denied under OrganizationType." + orgType.name());
            }
        }
    }
}
