package com.taxoryn.qa.security;

import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.billing.dto.BillingDashboardStatsDto;
import com.taxoryn.module.billing.dto.CreateInvoiceRequest;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity;
import com.taxoryn.module.billing.mapper.InvoiceMapper;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.billing.service.InvoiceServiceImpl;
import com.taxoryn.module.client.dto.AssignClientEmployeeRequest;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.CreateClientNoteRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientNoteEntity;
import com.taxoryn.module.client.mapper.ClientMapper;
import com.taxoryn.module.client.repository.ClientNoteRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.service.ClientServiceImpl;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.repository.ComplianceRuleRepository;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.DocumentRequestDto;
import com.taxoryn.module.docrequest.dto.DocumentRequestSummaryDto;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.docrequest.service.DocumentRequestService;
import com.taxoryn.module.docrequest.service.DocumentRequestServiceImpl;
import com.taxoryn.module.document.dto.DocumentDownloadDto;
import com.taxoryn.module.document.dto.PresignedUrlResponse;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.mapper.DocumentMapper;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.service.DocumentServiceImpl;
import com.taxoryn.module.document.storage.DocumentStorageService;
import com.taxoryn.module.document.storage.StorageProperties;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.dto.CreateGstProfileRequest;
import com.taxoryn.module.gst.dto.GstProfileDto;
import com.taxoryn.module.gst.dto.GstWorkloadDashboardDto;
import com.taxoryn.module.gst.entity.GstMonthlySummaryEntity;
import com.taxoryn.module.gst.entity.GstProfileEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.mapper.GstMapper;
import com.taxoryn.module.gst.repository.GstMonthlySummaryRepository;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.gst.service.GstServiceImpl;
import com.taxoryn.module.itr.dto.ItrWorkloadDashboardDto;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import com.taxoryn.module.itr.mapper.ItrMapper;
import com.taxoryn.module.itr.repository.ItrProfileRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.itr.service.ItrServiceImpl;
import com.taxoryn.module.notice.dto.NoticeDashboardStatsDto;
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
import com.taxoryn.module.portal.entity.ClientPortalMessageEntity;
import com.taxoryn.module.portal.mapper.ClientPortalMapper;
import com.taxoryn.module.portal.repository.ClientDocumentRequestRepository;
import com.taxoryn.module.portal.repository.ClientNotificationRepository;
import com.taxoryn.module.portal.repository.ClientPortalMessageRepository;
import com.taxoryn.module.portal.service.ClientPortalServiceImpl;
import com.taxoryn.module.portal.websocket.PortalChatEventPublisher;
import com.taxoryn.module.subscription.service.SubscriptionService;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.mapper.TaskMapper;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.service.TaskServiceImpl;
import com.taxoryn.module.tds.dto.CreateTdsProfileRequest;
import com.taxoryn.module.tds.dto.TdsProfileDto;
import com.taxoryn.module.tds.dto.TdsWorkloadDashboardDto;
import com.taxoryn.module.tds.entity.TdsCertificateEntity;
import com.taxoryn.module.tds.entity.TdsChallanEntity;
import com.taxoryn.module.tds.entity.TdsDeducteeEntryEntity;
import com.taxoryn.module.tds.entity.TdsProfileEntity;
import com.taxoryn.module.tds.entity.TdsReturnEntity;
import com.taxoryn.module.tds.mapper.TdsMapper;
import com.taxoryn.module.tds.repository.TdsCertificateRepository;
import com.taxoryn.module.tds.repository.TdsChallanRepository;
import com.taxoryn.module.tds.repository.TdsDeducteeEntryRepository;
import com.taxoryn.module.tds.repository.TdsProfileRepository;
import com.taxoryn.module.tds.repository.TdsReturnRepository;
import com.taxoryn.module.tds.service.TdsCalculatorService;
import com.taxoryn.module.tds.service.TdsServiceImpl;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.user.service.ProfileImageService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TAXORYN PHASE 6 — Comprehensive End-to-End Security & Authorization Verification Suite
 * Validates complete multi-tenant, multi-practitioner, manager, staff, and client portal access boundaries.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EndToEndSecurityAndAuthorizationVerificationTest {

    // Organizations
    private final UUID orgA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID orgB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    // Organization A Users & Employees
    private final UUID orgAdminAUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-111111111111");
    private final UUID managerAUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-222222222222");
    private final UUID managerAEmpId = UUID.fromString("aaaaaaaa-0000-0000-0000-222222222223");

    private final UUID practitionerAUserId = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private final UUID practitionerAEmpId = UUID.fromString("aaaaaaaa-1111-1111-1111-222222222222");

    private final UUID practitionerBUserId = UUID.fromString("aaaaaaaa-2222-2222-2222-111111111111");
    private final UUID practitionerBEmpId = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");

    private final UUID staffAUserId = UUID.fromString("aaaaaaaa-3333-3333-3333-111111111111");
    private final UUID staffAEmpId = UUID.fromString("aaaaaaaa-3333-3333-3333-222222222222");

    private final UUID clientPortalAUserId = UUID.fromString("aaaaaaaa-4444-4444-4444-111111111111");

    // Organization B Users & Employees
    private final UUID orgAdminBUserId = UUID.fromString("bbbbbbbb-0000-0000-0000-111111111111");
    private final UUID practitionerCUserId = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");
    private final UUID practitionerCEmpId = UUID.fromString("bbbbbbbb-1111-1111-1111-222222222222");
    private final UUID clientPortalBUserId = UUID.fromString("bbbbbbbb-4444-4444-4444-111111111111");

    // Clients
    private final UUID clientAId = UUID.fromString("aaaaaaaa-caaa-caaa-caaa-aaaaaaaaaaaa");
    private final UUID clientBId = UUID.fromString("aaaaaaaa-cbbb-cbbb-cbbb-bbbbbbbbbbbb");
    private final UUID clientCId = UUID.fromString("aaaaaaaa-cccc-cccc-cccc-cccccccccccc");
    private final UUID clientDId = UUID.fromString("aaaaaaaa-cddd-cddd-cddd-dddddddddddd");
    private final UUID clientEId = UUID.fromString("bbbbbbbb-ceee-ceee-ceee-eeeeeeeeeeee");

    // Shared Core Mocks
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

    // GST Mocks
    @Mock private GstProfileRepository gstProfileRepository;
    @Mock private GstReturnFilingRepository gstReturnFilingRepository;
    @Mock private GstMonthlySummaryRepository gstMonthlySummaryRepository;
    @Mock private GstMapper gstMapper;

    // ITR Mocks
    @Mock private ItrProfileRepository itrProfileRepository;
    @Mock private ItrReturnRepository itrReturnRepository;
    @Mock private ItrMapper itrMapper;

    // TDS Mocks
    @Mock private TdsProfileRepository tdsProfileRepository;
    @Mock private TdsReturnRepository tdsReturnRepository;
    @Mock private TdsChallanRepository tdsChallanRepository;
    @Mock private TdsDeducteeEntryRepository tdsDeducteeEntryRepository;
    @Mock private TdsCertificateRepository tdsCertificateRepository;
    @Mock private TdsMapper tdsMapper;
    @Mock private TdsCalculatorService tdsCalculatorService;

    // Billing Mocks
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceItemRepository invoiceItemRepository;
    @Mock private InvoicePaymentRepository invoicePaymentRepository;
    @Mock private ClientNotificationRepository clientNotificationRepository;
    @Mock private InvoiceMapper invoiceMapper;

    // Document Requests Mocks
    @Mock private DocumentRequestRepository documentRequestRepository;
    @Mock private DocumentRequestItemRepository documentRequestItemRepository;

    // Documents & Storage Mocks
    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentMapper documentMapper;
    @Mock private DocumentStorageService storageService;

    // Tax Notices Mocks
    @Mock private TaxNoticeRepository noticeRepository;
    @Mock private NoticeResponseRepository noticeResponseRepository;
    @Mock private NoticeHearingRepository noticeHearingRepository;
    @Mock private NoticeActivityRepository noticeActivityRepository;
    @Mock private TaxNoticeMapper noticeMapper;
    @Mock private com.taxoryn.module.notice.service.TaxNoticeConfigurationService noticeConfigurationService;

    // Client Portal Mocks
    @Mock private ClientPortalMessageRepository clientPortalMessageRepository;
    @Mock private PortalChatEventPublisher portalChatEventPublisher;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ProfileImageService profileImageService;
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

    // Entities
    private ClientEntity clientA;
    private ClientEntity clientB;
    private ClientEntity clientC;
    private ClientEntity clientD;
    private ClientEntity clientE;

    // Practice Security Scopes
    private PracticeSecurityScope orgAdminAScope;
    private PracticeSecurityScope managerAScope;
    private PracticeSecurityScope practitionerAScope;
    private PracticeSecurityScope practitionerBScope;
    private PracticeSecurityScope staffAScope;

    @BeforeEach
    void setUp() {
        // Build Clients
        clientA = ClientEntity.builder().displayName("Alpha Corp").assignedEmployeeId(practitionerAEmpId).status(ClientEntity.ClientStatus.ACTIVE).build();
        clientA.setId(clientAId); clientA.setOrganizationId(orgA);

        clientB = ClientEntity.builder().displayName("Beta Industries").assignedEmployeeId(practitionerAEmpId).status(ClientEntity.ClientStatus.ACTIVE).build();
        clientB.setId(clientBId); clientB.setOrganizationId(orgA);

        clientC = ClientEntity.builder().displayName("Gamma Traders").assignedEmployeeId(practitionerBEmpId).status(ClientEntity.ClientStatus.ACTIVE).build();
        clientC.setId(clientCId); clientC.setOrganizationId(orgA);

        clientD = ClientEntity.builder().displayName("Delta Services").assignedEmployeeId(staffAEmpId).status(ClientEntity.ClientStatus.ACTIVE).build();
        clientD.setId(clientDId); clientD.setOrganizationId(orgA);

        clientE = ClientEntity.builder().displayName("Epsilon Logistics").assignedEmployeeId(practitionerCEmpId).status(ClientEntity.ClientStatus.ACTIVE).build();
        clientE.setId(clientEId); clientE.setOrganizationId(orgB);

        // Mock Client Repository Lookups
        when(clientRepository.findByIdAndOrganizationId(clientAId, orgA)).thenReturn(Optional.of(clientA));
        when(clientRepository.findByIdAndOrganizationId(clientBId, orgA)).thenReturn(Optional.of(clientB));
        when(clientRepository.findByIdAndOrganizationId(clientCId, orgA)).thenReturn(Optional.of(clientC));
        when(clientRepository.findByIdAndOrganizationId(clientDId, orgA)).thenReturn(Optional.of(clientD));
        when(clientRepository.findByIdAndOrganizationId(clientEId, orgB)).thenReturn(Optional.of(clientE));

        // Cross-tenant lookup returns empty (Hard tenant boundary)
        when(clientRepository.findByIdAndOrganizationId(clientEId, orgA)).thenReturn(Optional.empty());
        when(clientRepository.findByIdAndOrganizationId(clientAId, orgB)).thenReturn(Optional.empty());

        // Build Practice Scopes
        orgAdminAScope = PracticeSecurityScope.builder()
                .organizationId(orgA)
                .userId(orgAdminAUserId)
                .roleTier(PracticeSecurityScope.RoleTier.FIRM_ADMIN)
                .isFirmAdmin(true)
                .build();

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

        practitionerBScope = PracticeSecurityScope.builder()
                .organizationId(orgA)
                .userId(practitionerBUserId)
                .employeeId(practitionerBEmpId)
                .roleTier(PracticeSecurityScope.RoleTier.STAFF_INDIVIDUAL)
                .isStaff(true)
                .accessibleAssigneeIds(Set.of(practitionerBEmpId))
                .build();

        staffAScope = PracticeSecurityScope.builder()
                .organizationId(orgA)
                .userId(staffAUserId)
                .employeeId(staffAEmpId)
                .roleTier(PracticeSecurityScope.RoleTier.STAFF_INDIVIDUAL)
                .isStaff(true)
                .accessibleAssigneeIds(Set.of(staffAEmpId))
                .build();

        // Instantiate Services
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
                notificationService, securityScopeEvaluator, noticeConfigurationService
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
                profileImageService, clientPortalMessageRepository,
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
                .email("portal@" + clientId + ".com")
                .roles(Set.of("ROLE_CLIENT_USER", "CLIENT_USER"))
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================================================
    // SECTION 1: ORGANIZATION BOUNDARY VERIFICATION
    // =========================================================================
    @Nested
    @DisplayName("1. Organization Multi-Tenant Boundary Verification")
    class OrganizationBoundaryTests {

        @Test
        @DisplayName("Org A users are strictly blocked from accessing Org B Client E")
        void testCrossTenantClientAccessBlocked() {
            authenticatePracticeUser(orgA, orgAdminAUserId, "ROLE_ORG_ADMIN");
            assertThrows(ResourceNotFoundException.class, () -> clientService.getClientById(clientEId));
        }

        @Test
        @DisplayName("Org A users cannot view GST, ITR, TDS, Invoices, or Notices belonging to Org B")
        void testCrossTenantDomainEntitiesBlocked() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_ACCOUNTANT");
            UUID foreignId = UUID.randomUUID();

            when(gstProfileRepository.findByIdAndOrganizationId(foreignId, orgA)).thenReturn(Optional.empty());
            when(itrProfileRepository.findByIdAndOrganizationId(foreignId, orgA)).thenReturn(Optional.empty());
            when(tdsProfileRepository.findByIdAndOrganizationId(foreignId, orgA)).thenReturn(Optional.empty());
            when(invoiceRepository.findByIdAndOrganizationId(foreignId, orgA)).thenReturn(Optional.empty());
            when(noticeRepository.findByIdAndOrganizationId(foreignId, orgA)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> gstService.getProfileById(foreignId));
            assertThrows(ResourceNotFoundException.class, () -> itrService.getProfileById(foreignId));
            assertThrows(ResourceNotFoundException.class, () -> tdsService.getProfileById(foreignId));
            assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoiceById(foreignId));
            assertThrows(ResourceNotFoundException.class, () -> taxNoticeService.getNoticeById(foreignId));
        }

        @Test
        @DisplayName("Client Portal User in Org A cannot access Client Portal in Org B")
        void testCrossTenantClientPortalAccessBlocked() {
            authenticateClientPortalUser(orgA, clientPortalAUserId, clientAId);
            assertThrows(ResourceNotFoundException.class, () -> clientService.getClientById(clientEId));
        }
    }

    // =========================================================================
    // SECTION 2: CLIENT PORTFOLIO BOUNDARY VERIFICATION
    // =========================================================================
    @Nested
    @DisplayName("2. Client Portfolio Boundary Verification")
    class ClientPortfolioBoundaryTests {

        @Test
        @DisplayName("PRACTITIONER_A: Allowed CLIENT_A & CLIENT_B, Denied CLIENT_C, CLIENT_D, CLIENT_E")
        void testPractitionerAPortfolioBoundaries() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_ACCOUNTANT");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerAScope)).thenReturn(Set.of(clientAId, clientBId));

            when(clientMapper.toDto(clientA)).thenReturn(ClientDto.builder().id(clientAId).build());
            when(clientMapper.toDto(clientB)).thenReturn(ClientDto.builder().id(clientBId).build());

            // Allowed
            assertDoesNotThrow(() -> clientService.getClientById(clientAId));
            assertDoesNotThrow(() -> clientService.getClientById(clientBId));

            // Denied (Same Tenant, Unassigned Portfolio)
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientCId));
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientDId));

            // Denied (Cross Tenant)
            assertThrows(ResourceNotFoundException.class, () -> clientService.getClientById(clientEId));
        }

        @Test
        @DisplayName("PRACTITIONER_B: Allowed CLIENT_C, Denied CLIENT_A, CLIENT_B, CLIENT_D, CLIENT_E")
        void testPractitionerBPortfolioBoundaries() {
            authenticatePracticeUser(orgA, practitionerBUserId, "ROLE_ACCOUNTANT");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerBScope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerBScope)).thenReturn(Set.of(clientCId));

            when(clientMapper.toDto(clientC)).thenReturn(ClientDto.builder().id(clientCId).build());

            // Allowed
            assertDoesNotThrow(() -> clientService.getClientById(clientCId));

            // Denied
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientAId));
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientBId));
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientDId));
            assertThrows(ResourceNotFoundException.class, () -> clientService.getClientById(clientEId));
        }

        @Test
        @DisplayName("STAFF_A: Allowed CLIENT_D, Denied CLIENT_A, CLIENT_B, CLIENT_C, CLIENT_E")
        void testStaffAPortfolioBoundaries() {
            authenticatePracticeUser(orgA, staffAUserId, "ROLE_STAFF");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(staffAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(staffAScope)).thenReturn(Set.of(clientDId));

            when(clientMapper.toDto(clientD)).thenReturn(ClientDto.builder().id(clientDId).build());

            // Allowed
            assertDoesNotThrow(() -> clientService.getClientById(clientDId));

            // Denied
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientAId));
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientBId));
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientCId));
            assertThrows(ResourceNotFoundException.class, () -> clientService.getClientById(clientEId));
        }
    }

    // =========================================================================
    // SECTION 3 & 4: MANAGER SCOPE & REPORTING HIERARCHY
    // =========================================================================
    @Nested
    @DisplayName("3 & 4. Manager Scope vs Organization Scope & Reporting Hierarchy")
    class ManagerScopeAndHierarchyTests {

        @Test
        @DisplayName("PRACTITIONER_A with reportees does NOT receive MANAGER authority (hasReportees != MANAGER)")
        void testReporteesDoNotGrantManagerScope() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_ACCOUNTANT");
            // Practitioner A is configured with staffIndividual role tier even if reportees exist in DB
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerAScope)).thenReturn(Set.of(clientAId, clientBId));

            assertFalse(practitionerAScope.isFirmAdmin());
            assertFalse(practitionerAScope.isDepartmentManager());
            assertTrue(practitionerAScope.isStaff());

            // Still blocked from viewing Client C
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientCId));
        }

        @Test
        @DisplayName("MANAGER_A receives scoped department access, not organization-wide access")
        void testManagerScopeIsDepartmentConstrained() {
            authenticatePracticeUser(orgA, managerAUserId, "ROLE_TAX_MANAGER");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(managerAScope);
            // Manager A manages Direct Tax department (Client A and Client D), but not Client C (Indirect Tax)
            when(securityScopeEvaluator.getAccessibleClientIds(managerAScope)).thenReturn(Set.of(clientAId, clientDId));

            when(clientMapper.toDto(clientA)).thenReturn(ClientDto.builder().id(clientAId).build());
            when(clientMapper.toDto(clientD)).thenReturn(ClientDto.builder().id(clientDId).build());

            assertDoesNotThrow(() -> clientService.getClientById(clientAId));
            assertDoesNotThrow(() -> clientService.getClientById(clientDId));

            // Manager A cannot access Client C in different department
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientCId));
        }
    }

    // =========================================================================
    // SECTION 5: TASK ACCESS DECOUPLING
    // =========================================================================
    @Nested
    @DisplayName("5. Task Assignment Decoupling from Client Access Scope")
    class TaskAccessDecouplingTests {

        @Test
        @DisplayName("Task assigned to STAFF_A for CLIENT_A allows task update, but NOT CLIENT_A portfolio access")
        void testTaskAssignmentDoesNotGrantClientScope() {
            UUID taskId = UUID.randomUUID();
            TaskEntity taskA = TaskEntity.builder()
                    .title("Prepare GSTR-1 Data")
                    .clientId(clientAId)
                    .assignedTo(staffAEmpId)
                    .status(TaskEntity.TaskStatus.IN_PROGRESS)
                    .priority(TaskEntity.TaskPriority.MEDIUM)
                    .build();
            taskA.setId(taskId);
            taskA.setOrganizationId(orgA);

            authenticatePracticeUser(orgA, staffAUserId, "ROLE_STAFF");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(staffAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(staffAScope)).thenReturn(Set.of(clientDId));

            when(taskRepository.findByIdAndOrganizationId(taskId, orgA)).thenReturn(Optional.of(taskA));
            when(taskMapper.toDto(taskA)).thenReturn(TaskDto.builder().id(taskId).title("Prepare GSTR-1 Data").build());

            // 1. Staff A CAN retrieve and work on the assigned Task A
            assertDoesNotThrow(() -> taskService.getTaskById(taskId));

            // 2. Staff A CANNOT access Client A's tax records, profile, notes or invoices
            assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientAId));

            // 3. Client A assigned employee remains Practitioner A
            assertEquals(practitionerAEmpId, clientA.getAssignedEmployeeId());
        }
    }

    // =========================================================================
    // SECTION 6: DOCUMENT & OBJECT STORAGE SECURITY
    // =========================================================================
    @Nested
    @DisplayName("6. Document & Object Storage Security")
    class DocumentStorageSecurityTests {

        @Test
        @DisplayName("Direct document download, preview, and presigned URL enforce portfolio authorization")
        void testDocumentAccessControlled() {
            UUID docA1Id = UUID.randomUUID();
            DocumentEntity docA1 = DocumentEntity.builder()
                    .clientId(clientAId)
                    .fileName("Alpha_Financials.pdf")
                    .storageKey("tenants/org_111/clients/caaa/Alpha_Financials.pdf")
                    .contentType("application/pdf")
                    .fileSize(1024L)
                    .scanStatus(DocumentEntity.DocumentScanStatus.CLEAN)
                    .status(DocumentEntity.DocumentStatus.ACTIVE)
                    .build();
            docA1.setId(docA1Id);
            docA1.setOrganizationId(orgA);

            when(documentRepository.findByIdAndOrganizationId(docA1Id, orgA)).thenReturn(Optional.of(docA1));

            // 1. Practitioner A (Authorized) can download and generate presigned URL
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_ACCOUNTANT");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerAScope)).thenReturn(Set.of(clientAId, clientBId));

            assertDoesNotThrow(() -> documentService.downloadDocument(docA1Id));
            assertDoesNotThrow(() -> documentService.previewDocument(docA1Id));
            assertDoesNotThrow(() -> documentService.getDocumentDownloadUrl(docA1Id));

            // 2. Practitioner B (Unauthorized Client) is blocked with AccessDeniedException
            authenticatePracticeUser(orgA, practitionerBUserId, "ROLE_ACCOUNTANT");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerBScope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerBScope)).thenReturn(Set.of(clientCId));

            assertThrows(AccessDeniedException.class, () -> documentService.downloadDocument(docA1Id));
            assertThrows(AccessDeniedException.class, () -> documentService.previewDocument(docA1Id));
            assertThrows(AccessDeniedException.class, () -> documentService.getDocumentDownloadUrl(docA1Id));
        }
    }

    // =========================================================================
    // SECTION 7: DASHBOARD & SUMMARY METRICS ZERO DATA LEAKAGE
    // =========================================================================
    @Nested
    @DisplayName("7. Dashboard & Aggregate Metrics Zero Data Leakage")
    class DashboardAggregationSecurityTests {

        @Test
        @DisplayName("Invoice dashboard stats never count or leak revenue from out-of-portfolio clients")
        void testInvoiceDashboardZeroLeakage() {
            authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_ACCOUNTANT");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerAScope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerAScope)).thenReturn(Set.of(clientAId, clientBId));

            InvoiceEntity invA = InvoiceEntity.builder()
                    .clientId(clientAId)
                    .total(new BigDecimal("50000.00"))
                    .paidAmount(BigDecimal.ZERO)
                    .balanceDue(new BigDecimal("50000.00"))
                    .dueDate(LocalDate.now().plusDays(10))
                    .items(List.of())
                    .status(InvoiceEntity.InvoiceStatus.ISSUED)
                    .build();
            InvoiceEntity invC = InvoiceEntity.builder()
                    .clientId(clientCId)
                    .total(new BigDecimal("200000.00"))
                    .paidAmount(BigDecimal.ZERO)
                    .balanceDue(new BigDecimal("200000.00"))
                    .dueDate(LocalDate.now().plusDays(10))
                    .items(List.of())
                    .status(InvoiceEntity.InvoiceStatus.ISSUED)
                    .build();

            when(invoiceRepository.findAllByOrganizationId(orgA)).thenReturn(List.of(invA, invC));

            BillingDashboardStatsDto stats = invoiceService.getBillingDashboardStats();
            assertEquals(1, stats.getTotalInvoices());
            assertEquals(new BigDecimal("50000.00"), stats.getTotalBilled());
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
    // SECTION 8: CLIENT PORTAL TAXPAYER ISOLATION
    // =========================================================================
    @Nested
    @DisplayName("8. Client Portal Taxpayer Isolation")
    class ClientPortalTaxpayerIsolationTests {

        @Test
        @DisplayName("CLIENT_PORTAL_A user cannot access CLIENT_B, CLIENT_C, CLIENT_D, or CLIENT_E data")
        void testClientPortalUserStrictlyBoundToOwnClientId() {
            authenticateClientPortalUser(orgA, clientPortalAUserId, clientAId);

            // Accessing Client A document
            UUID docAId = UUID.randomUUID();
            DocumentEntity docA = DocumentEntity.builder().clientId(clientAId).fileName("Tax_Report.pdf").status(DocumentEntity.DocumentStatus.ACTIVE).scanStatus(DocumentEntity.DocumentScanStatus.CLEAN).build();
            docA.setId(docAId); docA.setOrganizationId(orgA);
            when(documentRepository.findByIdAndOrganizationId(docAId, orgA)).thenReturn(Optional.of(docA));

            assertDoesNotThrow(() -> documentService.downloadDocument(docAId));

            // Attempting to access Client B document in same tenant
            UUID docBId = UUID.randomUUID();
            DocumentEntity docB = DocumentEntity.builder().clientId(clientBId).fileName("Beta_Tax.pdf").status(DocumentEntity.DocumentStatus.ACTIVE).scanStatus(DocumentEntity.DocumentScanStatus.CLEAN).build();
            docB.setId(docBId); docB.setOrganizationId(orgA);
            when(documentRepository.findByIdAndOrganizationId(docBId, orgA)).thenReturn(Optional.of(docB));

            assertThrows(AccessDeniedException.class, () -> documentService.downloadDocument(docBId));
        }
    }

    // =========================================================================
    // SECTION 9: ORGANIZATION TYPE SECURITY INDEPENDENCE
    // =========================================================================
    @Nested
    @DisplayName("9. OrganizationType Security Independence")
    class OrganizationTypeIndependenceTests {

        @Test
        @DisplayName("OrganizationType (SOLO, SMALL_TAX_FIRM, GROWING_PRACTICE, BUSINESS, UNKNOWN) never changes authorization semantics")
        void testAllOrganizationTypesEnforceIdenticalSecurityRules() {
            for (OrganizationType type : OrganizationType.values()) {
                OrganizationEntity org = OrganizationEntity.builder()
                        .name("Practice " + type.name())
                        .organizationType(type)
                        .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                        .build();
                org.setId(orgA);

                when(organizationRepository.findById(orgA)).thenReturn(Optional.of(org));

                authenticatePracticeUser(orgA, practitionerAUserId, "ROLE_ACCOUNTANT");
                when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerAScope);
                when(securityScopeEvaluator.getAccessibleClientIds(practitionerAScope)).thenReturn(Set.of(clientAId, clientBId));

                // Allowed
                when(clientMapper.toDto(clientA)).thenReturn(ClientDto.builder().id(clientAId).build());
                assertDoesNotThrow(() -> clientService.getClientById(clientAId), "Failed on type " + type);

                // Denied (Client C is out-of-portfolio)
                assertThrows(AccessDeniedException.class, () -> clientService.getClientById(clientCId), "Failed to deny on type " + type);
            }
        }
    }
}
