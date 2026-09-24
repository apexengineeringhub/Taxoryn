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
import com.taxoryn.module.billing.mapper.InvoiceMapper;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.billing.service.InvoiceServiceImpl;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.docrequest.dto.DocumentRequestDto;
import com.taxoryn.module.docrequest.dto.DocumentRequestSummaryDto;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.docrequest.service.DocumentRequestServiceImpl;
import com.taxoryn.module.document.mapper.DocumentMapper;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.service.DocumentService;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.dto.GstWorkloadDashboardDto;
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
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.portal.entity.ClientNotificationEntity;
import com.taxoryn.module.portal.mapper.ClientPortalMapper;
import com.taxoryn.module.portal.repository.ClientDocumentRequestRepository;
import com.taxoryn.module.portal.repository.ClientNotificationRepository;
import com.taxoryn.module.portal.repository.ClientPortalMessageRepository;
import com.taxoryn.module.portal.service.ClientPortalServiceImpl;
import com.taxoryn.module.portal.websocket.PortalChatEventPublisher;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.tds.dto.TdsWorkloadDashboardDto;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrossModuleScopeSecurityIntegrationTest {

    private final UUID orgA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID orgB = UUID.fromString("22222222-2222-2222-2222-222222222222");

    // Organization A Users & Practitioners
    private final UUID ownerAUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-111111111111");
    private final UUID practitionerA1UserId = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private final UUID practitionerA1EmpId = UUID.fromString("aaaaaaaa-1111-1111-1111-222222222222");

    private final UUID practitionerA2UserId = UUID.fromString("aaaaaaaa-2222-2222-2222-111111111111");
    private final UUID practitionerA2EmpId = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");

    // Organization A Clients
    private final UUID clientA1Id = UUID.fromString("aaaaaaaa-1111-1111-1111-333333333333"); // Assigned to A1
    private final UUID clientA2Id = UUID.fromString("aaaaaaaa-2222-2222-2222-333333333333"); // Assigned to A2

    // Organization B Entities
    private final UUID orgBUserId = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");
    private final UUID clientB1Id = UUID.fromString("bbbbbbbb-1111-1111-1111-333333333333");

    // Common Repositories
    @Mock private ClientRepository clientRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private AuditService auditService;
    @Mock private PracticeSecurityScopeEvaluator securityScopeEvaluator;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private com.taxoryn.module.compliance.repository.ComplianceObligationRepository complianceObligationRepository;
    @Mock private com.taxoryn.module.compliance.repository.ComplianceRuleRepository complianceRuleRepository;

    // GST Mocks & Service
    @Mock private GstProfileRepository gstProfileRepository;
    @Mock private GstReturnFilingRepository gstReturnFilingRepository;
    @Mock private GstMonthlySummaryRepository gstMonthlySummaryRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentMapper documentMapper;
    @Mock private GstMapper gstMapper;
    private GstServiceImpl gstService;

    // ITR Mocks & Service
    @Mock private ItrProfileRepository itrProfileRepository;
    @Mock private ItrReturnRepository itrReturnRepository;
    @Mock private ItrMapper itrMapper;
    private ItrServiceImpl itrService;

    // TDS Mocks & Service
    @Mock private TdsProfileRepository tdsProfileRepository;
    @Mock private TdsReturnRepository tdsReturnRepository;
    @Mock private TdsChallanRepository tdsChallanRepository;
    @Mock private TdsDeducteeEntryRepository tdsDeducteeEntryRepository;
    @Mock private TdsCertificateRepository tdsCertificateRepository;
    @Mock private TdsMapper tdsMapper;
    @Mock private TdsCalculatorService tdsCalculatorService;
    private TdsServiceImpl tdsService;

    // Billing Mocks & Service
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceItemRepository invoiceItemRepository;
    @Mock private InvoicePaymentRepository invoicePaymentRepository;
    @Mock private ClientNotificationRepository clientNotificationRepository;
    @Mock private InvoiceMapper invoiceMapper;
    private InvoiceServiceImpl invoiceService;

    // Document Requests Mocks & Service
    @Mock private DocumentRequestRepository documentRequestRepository;
    @Mock private DocumentRequestItemRepository documentRequestItemRepository;
    @Mock private DocumentService documentService;
    @Mock private com.taxoryn.module.notification.email.service.EmailNotificationService emailNotificationService;
    private DocumentRequestServiceImpl documentRequestService;

    // Tax Notice Mocks & Service
    @Mock private TaxNoticeRepository noticeRepository;
    @Mock private NoticeResponseRepository noticeResponseRepository;
    @Mock private NoticeHearingRepository noticeHearingRepository;
    @Mock private NoticeActivityRepository noticeActivityRepository;
    @Mock private TaxNoticeMapper noticeMapper;
    @Mock private com.taxoryn.module.notice.service.TaxNoticeConfigurationService noticeConfigurationService;
    private TaxNoticeServiceImpl taxNoticeService;

    // Client Portal Mocks & Service
    @Mock private ClientPortalMessageRepository clientPortalMessageRepository;
    @Mock private PortalChatEventPublisher portalChatEventPublisher;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ProfileImageService profileImageService;
    @Mock private ClientPortalMapper clientPortalMapper;
    @Mock private ClientDocumentRequestRepository clientDocumentRequestRepository;
    private ClientPortalServiceImpl clientPortalService;

    private ClientEntity clientA1;
    private ClientEntity clientA2;
    private ClientEntity clientB1;

    private PracticeSecurityScope practitionerA1Scope;
    private PracticeSecurityScope practitionerA2Scope;
    private PracticeSecurityScope firmAdminScope;

    @BeforeEach
    void setUp() {
        clientA1 = ClientEntity.builder()
                .displayName("Alpha Traders")
                .assignedEmployeeId(practitionerA1EmpId)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientA1.setId(clientA1Id);
        clientA1.setOrganizationId(orgA);

        clientA2 = ClientEntity.builder()
                .displayName("Beta Logistics")
                .assignedEmployeeId(practitionerA2EmpId)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientA2.setId(clientA2Id);
        clientA2.setOrganizationId(orgA);

        clientB1 = ClientEntity.builder()
                .displayName("Gamma Industries")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        clientB1.setId(clientB1Id);
        clientB1.setOrganizationId(orgB);

        when(clientRepository.findByIdAndOrganizationId(clientA1Id, orgA)).thenReturn(Optional.of(clientA1));
        when(clientRepository.findByIdAndOrganizationId(clientA2Id, orgA)).thenReturn(Optional.of(clientA2));
        when(clientRepository.findByIdAndOrganizationId(clientB1Id, orgB)).thenReturn(Optional.of(clientB1));

        practitionerA1Scope = PracticeSecurityScope.builder()
                .organizationId(orgA)
                .userId(practitionerA1UserId)
                .employeeId(practitionerA1EmpId)
                .roleTier(PracticeSecurityScope.RoleTier.STAFF_INDIVIDUAL)
                .isStaff(true)
                .department(null)
                .accessibleAssigneeIds(Set.of(practitionerA1UserId))
                .build();

        practitionerA2Scope = PracticeSecurityScope.builder()
                .organizationId(orgA)
                .userId(practitionerA2UserId)
                .employeeId(practitionerA2EmpId)
                .roleTier(PracticeSecurityScope.RoleTier.STAFF_INDIVIDUAL)
                .isStaff(true)
                .department(null)
                .accessibleAssigneeIds(Set.of(practitionerA2UserId))
                .build();

        firmAdminScope = PracticeSecurityScope.builder()
                .organizationId(orgA)
                .userId(ownerAUserId)
                .roleTier(PracticeSecurityScope.RoleTier.FIRM_ADMIN)
                .isFirmAdmin(true)
                .build();

        // Instantiate Services
        gstService = new GstServiceImpl(
                gstProfileRepository, gstReturnFilingRepository, gstMonthlySummaryRepository,
                clientRepository, employeeRepository, userRepository,
                complianceObligationRepository, complianceRuleRepository,
                taskRepository, documentRequestRepository, documentRequestItemRepository,
                documentRequestService, documentRepository, documentMapper,
                notificationService, gstMapper, auditService, securityScopeEvaluator
        );

        itrService = new ItrServiceImpl(
                itrProfileRepository, itrReturnRepository, clientRepository,
                employeeRepository, userRepository,
                complianceObligationRepository, complianceRuleRepository,
                taskRepository, documentRequestRepository,
                documentRequestService, documentRepository, documentMapper,
                notificationService, itrMapper, auditService, securityScopeEvaluator
        );

        tdsService = new TdsServiceImpl(
                tdsProfileRepository, tdsReturnRepository, tdsChallanRepository,
                tdsDeducteeEntryRepository, tdsCertificateRepository, clientRepository,
                employeeRepository, userRepository,
                complianceObligationRepository, complianceRuleRepository,
                taskRepository, documentRequestRepository,
                documentRequestService, documentRepository, documentMapper,
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
                documentService, notificationService, emailNotificationService,
                auditService, securityScopeEvaluator, taskRepository, employeeRepository
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

    private void authenticate(UUID orgId, UUID userId, String role) {
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

    @Nested
    @DisplayName("1. Cross-Tenant Multi-Tenant Isolation")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Practitioner in Org A cannot access GST filing in Org B")
        void testCrossTenantGstAccessBlocked() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");
            UUID foreignFilingId = UUID.randomUUID();

            when(gstReturnFilingRepository.findByIdAndOrganizationId(foreignFilingId, orgA)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> gstService.getFilingById(foreignFilingId));
        }

        @Test
        @DisplayName("Practitioner in Org A cannot access ITR return in Org B")
        void testCrossTenantItrAccessBlocked() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");
            UUID foreignReturnId = UUID.randomUUID();

            when(itrReturnRepository.findByIdAndOrganizationId(foreignReturnId, orgA)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> itrService.getReturnById(foreignReturnId));
        }

        @Test
        @DisplayName("Practitioner in Org A cannot access Invoice in Org B")
        void testCrossTenantInvoiceAccessBlocked() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");
            UUID foreignInvoiceId = UUID.randomUUID();

            when(invoiceRepository.findByIdAndOrganizationId(foreignInvoiceId, orgA)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoiceById(foreignInvoiceId));
        }
    }

    @Nested
    @DisplayName("2. Cross-Practitioner Client Access Scope inside same Organization")
    class CrossPractitionerScopeTests {

        @BeforeEach
        void setupScopeEvaluator() {
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerA1Scope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerA1Scope)).thenReturn(Set.of(clientA1Id));
        }

        @Test
        @DisplayName("Practitioner A1 is rejected with 403 when trying to access GST profile of Client A2")
        void testGstCrossPractitionerRejected() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");

            GstProfileEntity profileA2 = GstProfileEntity.builder().clientId(clientA2Id).gstin("29AABCU9603R1Z2").build();
            profileA2.setId(UUID.randomUUID());
            profileA2.setOrganizationId(orgA);

            when(gstProfileRepository.findByIdAndOrganizationId(profileA2.getId(), orgA)).thenReturn(Optional.of(profileA2));

            assertThrows(AccessDeniedException.class, () -> gstService.getProfileById(profileA2.getId()));
        }

        @Test
        @DisplayName("Practitioner A1 is rejected with 403 when trying to access ITR return of Client A2")
        void testItrCrossPractitionerRejected() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");

            ItrReturnEntity returnA2 = ItrReturnEntity.builder().clientId(clientA2Id).assessmentYear("2026-27").build();
            returnA2.setId(UUID.randomUUID());
            returnA2.setOrganizationId(orgA);

            when(itrReturnRepository.findByIdAndOrganizationId(returnA2.getId(), orgA)).thenReturn(Optional.of(returnA2));

            assertThrows(AccessDeniedException.class, () -> itrService.getReturnById(returnA2.getId()));
        }

        @Test
        @DisplayName("Practitioner A1 is rejected with 403 when trying to access TDS profile of Client A2")
        void testTdsCrossPractitionerRejected() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");

            TdsProfileEntity profileA2 = TdsProfileEntity.builder().clientId(clientA2Id).tan("BLRA12345B").build();
            profileA2.setId(UUID.randomUUID());
            profileA2.setOrganizationId(orgA);

            when(tdsProfileRepository.findByIdAndOrganizationId(profileA2.getId(), orgA)).thenReturn(Optional.of(profileA2));

            assertThrows(AccessDeniedException.class, () -> tdsService.getProfileById(profileA2.getId()));
        }

        @Test
        @DisplayName("Practitioner A1 is rejected with 403 when trying to access Document Request of Client A2")
        void testDocRequestCrossPractitionerRejected() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");

            DocumentRequestEntity docReqA2 = DocumentRequestEntity.builder().clientId(clientA2Id).purpose("Annual Audit").build();
            docReqA2.setId(UUID.randomUUID());
            docReqA2.setOrganizationId(orgA);

            when(documentRequestRepository.findByIdAndOrganizationId(docReqA2.getId(), orgA)).thenReturn(Optional.of(docReqA2));

            assertThrows(AccessDeniedException.class, () -> documentRequestService.getRequestById(docReqA2.getId()));
        }

        @Test
        @DisplayName("Practitioner A1 is rejected with 403 when trying to access Invoice of Client A2")
        void testInvoiceCrossPractitionerRejected() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");

            InvoiceEntity invA2 = InvoiceEntity.builder().clientId(clientA2Id).invoiceNumber("INV-2026-0002").build();
            invA2.setId(UUID.randomUUID());
            invA2.setOrganizationId(orgA);

            when(invoiceRepository.findByIdAndOrganizationId(invA2.getId(), orgA)).thenReturn(Optional.of(invA2));

            assertThrows(AccessDeniedException.class, () -> invoiceService.getInvoiceById(invA2.getId()));
        }

        @Test
        @DisplayName("Practitioner A1 is rejected when trying to access Tax Notice of Client A2")
        void testNoticeCrossPractitionerRejected() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");

            TaxNoticeEntity noticeA2 = TaxNoticeEntity.builder().clientId(clientA2Id).noticeNumber("DIN-2026-001").build();
            noticeA2.setId(UUID.randomUUID());
            noticeA2.setOrganizationId(orgA);

            when(noticeRepository.findByIdAndOrganizationId(noticeA2.getId(), orgA)).thenReturn(Optional.of(noticeA2));

            assertThrows(ForbiddenException.class, () -> taxNoticeService.getNoticeById(noticeA2.getId()));
        }

        @Test
        @DisplayName("Practitioner A1 can successfully access Client A1 GST, ITR, TDS, and Invoice data")
        void testAuthorizedPortfolioAccessAllowed() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerA1Scope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerA1Scope)).thenReturn(Set.of(clientA1Id));

            GstProfileEntity profileA1 = GstProfileEntity.builder().clientId(clientA1Id).gstin("29AABCU9603R1Z1").build();
            profileA1.setId(UUID.randomUUID());
            profileA1.setOrganizationId(orgA);
            when(gstProfileRepository.findByIdAndOrganizationId(profileA1.getId(), orgA)).thenReturn(Optional.of(profileA1));
            when(gstMapper.toDto(any(GstProfileEntity.class))).thenReturn(new com.taxoryn.module.gst.dto.GstProfileDto());

            assertDoesNotThrow(() -> gstService.getProfileById(profileA1.getId()));
        }
    }

    @Nested
    @DisplayName("3. Task Assignment Decoupling from Client Access Scope")
    class TaskAssignmentDecouplingTests {

        @Test
        @DisplayName("Task assigned to Practitioner A2 for Client A1 does NOT grant portfolio scope to Client A1")
        void testTaskAssignmentDoesNotGrantClientScope() {
            authenticate(orgA, practitionerA2UserId, "ROLE_ACCOUNTANT");

            // Practitioner A2 only has access to Client A2 in client portfolio
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerA2Scope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerA2Scope)).thenReturn(Set.of(clientA2Id));

            // Attempt to access Client A1's GST profile
            GstProfileEntity profileA1 = GstProfileEntity.builder().clientId(clientA1Id).gstin("29AABCU9603R1Z1").build();
            profileA1.setId(UUID.randomUUID());
            profileA1.setOrganizationId(orgA);
            when(gstProfileRepository.findByIdAndOrganizationId(profileA1.getId(), orgA)).thenReturn(Optional.of(profileA1));

            // Must be denied despite any task assignments
            assertThrows(AccessDeniedException.class, () -> gstService.getProfileById(profileA1.getId()));
        }
    }

    @Nested
    @DisplayName("4. Aggregate Dashboard Metrics Security (Zero Data Leakage)")
    class DashboardMetricsSecurityTests {

        @Test
        @DisplayName("Billing dashboard excludes unauthorized clients for non-admin staff")
        void testBillingDashboardScopedToAccessiblePortfolio() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerA1Scope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerA1Scope)).thenReturn(Set.of(clientA1Id));
            when(securityScopeEvaluator.hasBillingAccess(practitionerA1Scope)).thenReturn(false);

            InvoiceEntity invA1 = InvoiceEntity.builder().clientId(clientA1Id).total(new BigDecimal("1000.00")).paidAmount(BigDecimal.ZERO).balanceDue(new BigDecimal("1000.00")).status(InvoiceEntity.InvoiceStatus.ISSUED).dueDate(LocalDate.now().plusDays(5)).items(List.of()).build();
            InvoiceEntity invA2 = InvoiceEntity.builder().clientId(clientA2Id).total(new BigDecimal("5000.00")).paidAmount(BigDecimal.ZERO).balanceDue(new BigDecimal("5000.00")).status(InvoiceEntity.InvoiceStatus.ISSUED).dueDate(LocalDate.now().plusDays(5)).items(List.of()).build();

            when(invoiceRepository.findAllByOrganizationId(orgA)).thenReturn(List.of(invA1, invA2));

            BillingDashboardStatsDto stats = invoiceService.getBillingDashboardStats();
            assertEquals(1, stats.getTotalInvoices());
            assertEquals(new BigDecimal("1000.00"), stats.getTotalBilled());
        }

        @Test
        @DisplayName("Document request summary statistics exclude unauthorized clients")
        void testDocRequestSummaryScopedToAccessiblePortfolio() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerA1Scope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerA1Scope)).thenReturn(Set.of(clientA1Id));

            DocumentRequestEntity reqA1 = DocumentRequestEntity.builder().clientId(clientA1Id).status(DocumentRequestEntity.RequestStatus.SENT).build();

            when(documentRequestRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of(reqA1));

            DocumentRequestSummaryDto stats = documentRequestService.getSummaryStats();
            assertEquals(1, stats.getTotalRequests());
            assertEquals(1, stats.getPendingRequests());
        }

        @Test
        @DisplayName("TDS workload dashboard excludes unauthorized clients for non-admin staff")
        void testTdsWorkloadDashboardScopedToAccessiblePortfolio() {
            authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");
            when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerA1Scope);
            when(securityScopeEvaluator.getAccessibleClientIds(practitionerA1Scope)).thenReturn(Set.of(clientA1Id));

            TdsProfileEntity p1 = TdsProfileEntity.builder().clientId(clientA1Id).status(TdsProfileEntity.TdsProfileStatus.ACTIVE).build();
            TdsProfileEntity p2 = TdsProfileEntity.builder().clientId(clientA2Id).status(TdsProfileEntity.TdsProfileStatus.ACTIVE).build();
            when(tdsProfileRepository.findAllByOrganizationId(orgA)).thenReturn(List.of(p1, p2));

            TdsReturnEntity r1 = TdsReturnEntity.builder().clientId(clientA1Id).filingStatus(TdsReturnEntity.TdsFilingStatus.PENDING).totalTaxDeducted(new BigDecimal("500")).totalTaxDeposited(BigDecimal.ZERO).build();
            TdsReturnEntity r2 = TdsReturnEntity.builder().clientId(clientA2Id).filingStatus(TdsReturnEntity.TdsFilingStatus.PENDING).totalTaxDeducted(new BigDecimal("9500")).totalTaxDeposited(BigDecimal.ZERO).build();
            when(tdsReturnRepository.findAllByOrganizationIdAndQuarterAndFinancialYear(orgA, TdsReturnEntity.TdsQuarter.Q1, "2026-27")).thenReturn(List.of(r1, r2));

            TdsWorkloadDashboardDto dashboard = tdsService.getWorkloadDashboard("Q1", "2026-27", null);
            assertEquals(1, dashboard.getTotalTanClients());
            assertEquals(1, dashboard.getTotalScheduledReturns());
            assertEquals(new BigDecimal("500"), dashboard.getTotalPracticeTdsDeducted());
        }
    }

    @Nested
    @DisplayName("5. OrganizationType Independence")
    class OrganizationTypeIndependenceTests {

        @Test
        @DisplayName("Authorization rules remain identically strict across all OrganizationType values")
        void testSecurityRulesIdenticalAcrossOrgTypes() {
            for (OrganizationType orgType : OrganizationType.values()) {
                OrganizationEntity org = OrganizationEntity.builder()
                        .name("Test Practice " + orgType)
                        .organizationType(orgType)
                        .build();
                org.setId(orgA);

                when(organizationRepository.findById(orgA)).thenReturn(Optional.of(org));

                authenticate(orgA, practitionerA1UserId, "ROLE_ACCOUNTANT");
                when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(practitionerA1Scope);
                when(securityScopeEvaluator.getAccessibleClientIds(practitionerA1Scope)).thenReturn(Set.of(clientA1Id));

                // Direct-ID access to Client A2 GST filing must always throw AccessDeniedException regardless of orgType
                GstReturnFilingEntity filingA2 = GstReturnFilingEntity.builder().clientId(clientA2Id).returnType(GstReturnFilingEntity.GstReturnType.GSTR1).build();
                filingA2.setId(UUID.randomUUID());
                filingA2.setOrganizationId(orgA);
                when(gstReturnFilingRepository.findByIdAndOrganizationId(filingA2.getId(), orgA)).thenReturn(Optional.of(filingA2));

                assertThrows(AccessDeniedException.class, () -> gstService.getFilingById(filingA2.getId()),
                        "Security must remain strictly enforced under OrganizationType: " + orgType);
            }
        }
    }
}
