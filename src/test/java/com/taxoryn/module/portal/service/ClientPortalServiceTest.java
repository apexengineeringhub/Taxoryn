package com.taxoryn.module.portal.service;

import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.dto.DocumentDownloadDto;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.service.DocumentService;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.itr.repository.ItrReturnRepository;
import com.taxoryn.module.portal.dto.ClientDocumentRequestDto;
import com.taxoryn.module.portal.dto.ClientPortalDashboardDto;
import com.taxoryn.module.portal.dto.ClientPortalProfileDto;
import com.taxoryn.module.portal.dto.ClientPortalUserDto;
import com.taxoryn.module.portal.dto.ClientTaskDto;
import com.taxoryn.module.portal.dto.RegisterClientPortalUserRequest;
import com.taxoryn.module.portal.dto.UpdateClientPortalProfileRequest;
import com.taxoryn.module.portal.mapper.ClientPortalMapper;
import com.taxoryn.module.portal.repository.ClientDocumentRequestRepository;
import com.taxoryn.module.portal.repository.ClientNotificationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientPortalServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private GstReturnFilingRepository gstReturnFilingRepository;
    @Mock
    private ItrReturnRepository itrReturnRepository;
    @Mock
    private DocumentService documentService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private com.taxoryn.module.organization.repository.OrganizationRepository organizationRepository;
    @Mock
    private com.taxoryn.module.authentication.repository.OrganizationActivationTokenRepository organizationActivationTokenRepository;
    @Mock
    private com.taxoryn.module.notification.email.service.EmailNotificationService emailNotificationService;
    @Mock
    private com.taxoryn.module.audit.service.AuditService auditService;
    @Mock
    private ClientNotificationRepository notificationRepository;
    @Mock
    private ClientDocumentRequestRepository docRequestRepository;
    @Mock
    private com.taxoryn.module.billing.repository.InvoiceRepository invoiceRepository;
    @Mock
    private com.taxoryn.module.billing.mapper.InvoiceMapper invoiceMapper;
    @Mock
    private ClientPortalMapper mapper;
    @Mock
    private com.taxoryn.module.notification.service.NotificationService notificationService;
    @Mock
    private com.taxoryn.module.docrequest.service.DocumentRequestService multiItemDocRequestService;
    @Mock
    private com.taxoryn.module.docrequest.repository.DocumentRequestRepository multiItemDocRequestRepository;
    @Mock
    private com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository multiItemDocRequestItemRepository;

    @InjectMocks
    private ClientPortalServiceImpl portalService;

    private UUID tenantId;
    private UUID clientId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        userId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .clientId(clientId)
                .email("client@abctraders.com")
                .roles(Set.of("CLIENT_ADMIN"))
                .permissions(Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_DOCUMENT_UPLOAD", "CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_PROFILE_VIEW", "CLIENT_PORTAL_STATUS_VIEW"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Register client portal user successfully for new email")
    void testRegisterClientPortalUser() {
        RegisterClientPortalUserRequest request = RegisterClientPortalUserRequest.builder()
                .clientId(clientId)
                .email("finance@abctraders.com")
                .password("SecretPass123!")
                .firstName("Rohan")
                .lastName("Verma")
                .role("CLIENT_USER")
                .build();

        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .build();
        client.setId(clientId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findByOrganizationIdAndEmailIgnoreCase(tenantId, "finance@abctraders.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("finance@abctraders.com")).thenReturn(Optional.empty());
        when(roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_USER")).thenReturn(Optional.of(RoleEntity.builder().code("CLIENT_USER").build()));
        when(passwordEncoder.encode("SecretPass123!")).thenReturn("encodedHash");

        UserEntity savedUser = UserEntity.builder()
                .email("finance@abctraders.com")
                .firstName("Rohan")
                .lastName("Verma")
                .clientId(clientId)
                .build();
        savedUser.setId(UUID.randomUUID());
        savedUser.setOrganizationId(tenantId);

        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        ClientPortalUserDto result = portalService.registerClientPortalUser(request);

        assertNotNull(result);
        assertEquals("finance@abctraders.com", result.getEmail());
        assertEquals("ABC Traders", result.getClientName());
    }

    @Test
    @DisplayName("Case B: Register client portal user reconciles existing orphan CLIENT_USER")
    void testRegisterClientPortalUser_CaseB_OrphanReconcile() {
        RegisterClientPortalUserRequest request = RegisterClientPortalUserRequest.builder()
                .clientId(clientId)
                .email("orphan@abctraders.com")
                .firstName("Rohan")
                .lastName("Verma")
                .role("CLIENT_USER")
                .build();

        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .build();
        client.setId(clientId);

        UserEntity orphanUser = UserEntity.builder()
                .email("orphan@abctraders.com")
                .firstName("Rohan")
                .lastName("Verma")
                .clientId(null) // Orphan!
                .status(UserEntity.UserStatus.INVITED)
                .roles(Set.of(RoleEntity.builder().code("CLIENT_USER").build()))
                .build();
        orphanUser.setId(UUID.randomUUID());
        orphanUser.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findByOrganizationIdAndEmailIgnoreCase(tenantId, "orphan@abctraders.com")).thenReturn(Optional.of(orphanUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientPortalUserDto result = portalService.registerClientPortalUser(request);

        assertNotNull(result);
        assertEquals("orphan@abctraders.com", result.getEmail());
        assertEquals(clientId, result.getClientId());
        assertEquals(clientId, orphanUser.getClientId());
    }

    @Test
    @DisplayName("Case C: Register client portal user throws when email is already linked to another client in same practice")
    void testRegisterClientPortalUser_CaseC_LinkedToAnotherClient() {
        UUID otherClientId = UUID.randomUUID();
        RegisterClientPortalUserRequest request = RegisterClientPortalUserRequest.builder()
                .clientId(clientId)
                .email("otherclient@abctraders.com")
                .firstName("Rohan")
                .build();

        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .build();
        client.setId(clientId);

        UserEntity existingUser = UserEntity.builder()
                .email("otherclient@abctraders.com")
                .clientId(otherClientId)
                .roles(Set.of(RoleEntity.builder().code("CLIENT_USER").build()))
                .build();
        existingUser.setId(UUID.randomUUID());
        existingUser.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findByOrganizationIdAndEmailIgnoreCase(tenantId, "otherclient@abctraders.com")).thenReturn(Optional.of(existingUser));

        com.taxoryn.core.exception.DuplicateResourceException ex = assertThrows(
                com.taxoryn.core.exception.DuplicateResourceException.class,
                () -> portalService.registerClientPortalUser(request)
        );
        assertEquals("This email address is already associated with another client portal account in this practice. Please use the existing account or another email address.", ex.getMessage());
    }

    @Test
    @DisplayName("Case D: Register client portal user throws when email belongs to internal staff")
    void testRegisterClientPortalUser_CaseD_InternalStaff() {
        RegisterClientPortalUserRequest request = RegisterClientPortalUserRequest.builder()
                .clientId(clientId)
                .email("admin@practice.com")
                .firstName("Admin")
                .build();

        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .build();
        client.setId(clientId);

        UserEntity staffUser = UserEntity.builder()
                .email("admin@practice.com")
                .clientId(null)
                .roles(Set.of(RoleEntity.builder().code("ORG_ADMIN").build()))
                .build();
        staffUser.setId(UUID.randomUUID());
        staffUser.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findByOrganizationIdAndEmailIgnoreCase(tenantId, "admin@practice.com")).thenReturn(Optional.of(staffUser));

        com.taxoryn.core.exception.DuplicateResourceException ex = assertThrows(
                com.taxoryn.core.exception.DuplicateResourceException.class,
                () -> portalService.registerClientPortalUser(request)
        );
        assertEquals("This email address is already registered as an internal practice user. A separate client portal email address is required.", ex.getMessage());
    }

    @Test
    @DisplayName("Case F: Register client portal user throws when email is registered in different organization")
    void testRegisterClientPortalUser_CaseF_DifferentOrganization() {
        UUID foreignOrgId = UUID.randomUUID();
        RegisterClientPortalUserRequest request = RegisterClientPortalUserRequest.builder()
                .clientId(clientId)
                .email("foreign@external.com")
                .firstName("Foreign")
                .build();

        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .build();
        client.setId(clientId);

        UserEntity foreignUser = UserEntity.builder()
                .email("foreign@external.com")
                .organizationId(foreignOrgId)
                .build();
        foreignUser.setId(UUID.randomUUID());
        foreignUser.setOrganizationId(foreignOrgId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(userRepository.findByOrganizationIdAndEmailIgnoreCase(tenantId, "foreign@external.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("foreign@external.com")).thenReturn(Optional.of(foreignUser));

        com.taxoryn.core.exception.DuplicateResourceException ex = assertThrows(
                com.taxoryn.core.exception.DuplicateResourceException.class,
                () -> portalService.registerClientPortalUser(request)
        );
        assertEquals("The email address is already registered and cannot be used for this client portal account.", ex.getMessage());
    }

    @Test
    @DisplayName("Get client dashboard successfully")
    void testGetDashboard() {
        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .legalName("ABC Traders Private Limited")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("AAACB1234D")
                .gstin("27AAACB1234D1Z5")
                .build();
        client.setId(clientId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(multiItemDocRequestRepository.findAllByOrganizationIdAndClientIdAndStatusIn(eq(tenantId), eq(clientId), any()))
                .thenReturn(List.of());
        when(gstReturnFilingRepository.findAllByOrganizationIdAndClientIdOrderByDueDateDesc(tenantId, clientId)).thenReturn(List.of());
        when(itrReturnRepository.findAllByOrganizationIdAndClientIdOrderByAssessmentYearDesc(tenantId, clientId)).thenReturn(List.of());
        when(taskRepository.findAllByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(List.of());

        ClientPortalDashboardDto dashboard = portalService.getDashboard();

        assertNotNull(dashboard);
        assertEquals("ABC Traders", dashboard.getDisplayName());
        assertEquals("AAACB1234D", dashboard.getPan());
        assertEquals("27AAACB1234D1Z5", dashboard.getGstin());
    }

    @Test
    @DisplayName("Update profile details successfully")
    void testUpdateProfile() {
        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .email("old@abctraders.com")
                .build();
        client.setId(clientId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(client);
        when(mapper.toProfileDto(client)).thenReturn(ClientPortalProfileDto.builder()
                .clientId(clientId)
                .displayName("ABC Traders")
                .email("updated@abctraders.com")
                .build());

        UpdateClientPortalProfileRequest updateReq = UpdateClientPortalProfileRequest.builder()
                .email("updated@abctraders.com")
                .phone("+91 9876543210")
                .city("Mumbai")
                .build();

        ClientPortalProfileDto updated = portalService.updateProfile(updateReq);

        assertNotNull(updated);
        assertEquals("updated@abctraders.com", updated.getEmail());
    }

    @Test
    @DisplayName("Client cannot download document belonging to another client (throws ForbiddenException)")
    void testDownloadForeignClientDocumentThrowsForbidden() {
        UUID otherClientId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        DocumentEntity foreignDoc = DocumentEntity.builder()
                .clientId(otherClientId)
                .documentType(DocumentType.PAN_CARD)
                .build();
        foreignDoc.setId(docId);
        foreignDoc.setOrganizationId(tenantId);

        when(documentRepository.findByIdAndOrganizationId(docId, tenantId)).thenReturn(Optional.of(foreignDoc));

        assertThrows(ForbiddenException.class, () -> portalService.downloadClientDocument(docId));
    }

    @Test
    @DisplayName("Get client dashboard with multi-item document requests counts pending items accurately")
    void testGetDashboardWithMultiItemDocRequestsPendingCount() {
        ClientEntity client = ClientEntity.builder()
                .displayName("Vedanta Solutions")
                .legalName("Vedanta Solutions Pvt Ltd")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("AVAPI7181W")
                .build();
        client.setId(clientId);

        UUID reqId = UUID.randomUUID();
        com.taxoryn.module.docrequest.entity.DocumentRequestEntity docReqEntity = com.taxoryn.module.docrequest.entity.DocumentRequestEntity.builder()
                .requestNumber("DOC-2026-001")
                .purpose("FY 2025-26 Tax Audit")
                .status(com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.SENT)
                .clientId(clientId)
                .build();
        docReqEntity.setId(reqId);
        docReqEntity.setOrganizationId(tenantId);

        com.taxoryn.module.docrequest.dto.DocumentRequestItemDto item1 = com.taxoryn.module.docrequest.dto.DocumentRequestItemDto.builder()
                .id(UUID.randomUUID())
                .title("Bank Statement")
                .status(com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.PENDING)
                .build();
        com.taxoryn.module.docrequest.dto.DocumentRequestItemDto item2 = com.taxoryn.module.docrequest.dto.DocumentRequestItemDto.builder()
                .id(UUID.randomUUID())
                .title("Form 26AS")
                .status(com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.REJECTED)
                .build();
        com.taxoryn.module.docrequest.dto.DocumentRequestItemDto item3 = com.taxoryn.module.docrequest.dto.DocumentRequestItemDto.builder()
                .id(UUID.randomUUID())
                .title("GST Annual Return")
                .status(com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.ACCEPTED)
                .build();

        com.taxoryn.module.docrequest.dto.DocumentRequestDto docReqDto = com.taxoryn.module.docrequest.dto.DocumentRequestDto.builder()
                .id(reqId)
                .requestNumber("DOC-2026-001")
                .purpose("FY 2025-26 Tax Audit")
                .status(com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.SENT)
                .totalItems(3)
                .pendingItems(1)
                .rejectedItems(1)
                .acceptedItems(1)
                .items(List.of(item1, item2, item3))
                .build();

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(multiItemDocRequestRepository.findAllByOrganizationIdAndClientIdAndStatusIn(eq(tenantId), eq(clientId), any()))
                .thenReturn(List.of(docReqEntity));
        when(multiItemDocRequestService.getRequestById(reqId)).thenReturn(docReqDto);
        when(docRequestRepository.countByOrganizationIdAndClientIdAndStatus(tenantId, clientId, com.taxoryn.module.portal.entity.ClientDocumentRequestEntity.RequestStatus.PENDING)).thenReturn(0L);
        when(gstReturnFilingRepository.findAllByOrganizationIdAndClientIdOrderByDueDateDesc(tenantId, clientId)).thenReturn(List.of());
        when(itrReturnRepository.findAllByOrganizationIdAndClientIdOrderByAssessmentYearDesc(tenantId, clientId)).thenReturn(List.of());
        when(taskRepository.findAllByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(List.of());

        ClientPortalDashboardDto dashboard = portalService.getDashboard();

        assertNotNull(dashboard);
        assertEquals(1, dashboard.getActiveMultiItemRequests().size());
        assertEquals("DOC-2026-001", dashboard.getActiveMultiItemRequests().get(0).getRequestNumber());
        // 1 pending + 1 rejected = 2 pending items
        assertEquals(2, dashboard.getPendingDocumentsCount());
    }

    @Test
    @DisplayName("Priority A Security: Internal practitioner tasks and notes are never returned to portal users")
    void testInternalStaffTasksAreNeverReturnedToPortalUsers() {
        TaskEntity internalTask = TaskEntity.builder()
                .title("Internal Review: Preparation for scrutiny")
                .description("Internal staff checklist: Review bank statements for fee dispute")
                .notes("Confidential practitioner notes: Client owes Rs 50,000")
                .assignedTo(UUID.randomUUID())
                .status(TaskEntity.TaskStatus.IN_PROGRESS)
                .build();
        internalTask.setId(UUID.randomUUID());
        internalTask.setOrganizationId(tenantId);
        internalTask.setClientId(clientId);

        when(taskRepository.findAllByOrganizationIdAndClientId(tenantId, clientId))
                .thenReturn(List.of(internalTask));

        List<ClientTaskDto> clientTasks = portalService.getClientTasks();

        assertNotNull(clientTasks);
        org.junit.jupiter.api.Assertions.assertTrue(clientTasks.isEmpty(),
                "Internal staff tasks must never be exposed to client portal users");
    }

    @Test
    @DisplayName("Priority A Security: Dashboard pendingTasksCount and pendingTasks never leak internal practitioner tasks")
    void testDashboardDoesNotLeakInternalTasks() {
        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .pan("AAACB1234D")
                .build();
        client.setId(clientId);

        TaskEntity internalTask = TaskEntity.builder()
                .title("Internal staff task - do not disclose")
                .description("Private memo")
                .status(TaskEntity.TaskStatus.TODO)
                .build();
        internalTask.setId(UUID.randomUUID());
        internalTask.setOrganizationId(tenantId);
        internalTask.setClientId(clientId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(multiItemDocRequestRepository.findAllByOrganizationIdAndClientIdAndStatusIn(eq(tenantId), eq(clientId), any()))
                .thenReturn(List.of());
        when(gstReturnFilingRepository.findAllByOrganizationIdAndClientIdOrderByDueDateDesc(tenantId, clientId)).thenReturn(List.of());
        when(itrReturnRepository.findAllByOrganizationIdAndClientIdOrderByAssessmentYearDesc(tenantId, clientId)).thenReturn(List.of());
        when(taskRepository.findAllByOrganizationIdAndClientId(tenantId, clientId)).thenReturn(List.of(internalTask));

        ClientPortalDashboardDto dashboard = portalService.getDashboard();

        assertNotNull(dashboard);
        assertEquals(0L, dashboard.getPendingTasksCount(), "Internal task counts must not leak to client portal");
        org.junit.jupiter.api.Assertions.assertTrue(dashboard.getPendingTasks().isEmpty(),
                "Internal task list must never be returned on client portal dashboard");
    }

    @Test
    @DisplayName("Priority B Consolidation: requestDocumentFromClient delegates to canonical MultiItemDocRequestService")
    void testRequestDocumentFromClientDelegatesToCanonicalService() {
        ClientEntity client = ClientEntity.builder()
                .displayName("ABC Traders")
                .build();
        client.setId(clientId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));

        UUID canonicalRequestId = UUID.randomUUID();
        UUID canonicalItemId = UUID.randomUUID();
        com.taxoryn.module.docrequest.dto.DocumentRequestItemDto itemDto = com.taxoryn.module.docrequest.dto.DocumentRequestItemDto.builder()
                .id(canonicalItemId)
                .title("Form 16 Part A & B")
                .status(com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.PENDING)
                .build();

        com.taxoryn.module.docrequest.dto.DocumentRequestDto createdDto = com.taxoryn.module.docrequest.dto.DocumentRequestDto.builder()
                .id(canonicalRequestId)
                .requestNumber("DOC-2026-099")
                .purpose("Form 16 Part A & B")
                .status(com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.SENT)
                .items(List.of(itemDto))
                .build();

        when(multiItemDocRequestService.createAndSendRequest(any(com.taxoryn.module.docrequest.dto.CreateDocumentRequest.class)))
                .thenReturn(createdDto);

        com.taxoryn.module.portal.dto.CreateClientDocumentRequest request = com.taxoryn.module.portal.dto.CreateClientDocumentRequest.builder()
                .clientId(clientId)
                .documentType(DocumentType.FORM_16)
                .title("Form 16 Part A & B")
                .description("Please provide signed Form 16")
                .build();

        ClientDocumentRequestDto result = portalService.requestDocumentFromClient(request);

        assertNotNull(result);
        assertEquals(canonicalItemId, result.getId());
        assertEquals("Form 16 Part A & B", result.getTitle());
        assertEquals(com.taxoryn.module.portal.entity.ClientDocumentRequestEntity.RequestStatus.PENDING, result.getStatus());
        org.mockito.Mockito.verify(multiItemDocRequestService).createAndSendRequest(any(com.taxoryn.module.docrequest.dto.CreateDocumentRequest.class));
    }

    @Test
    @DisplayName("Priority B Consolidation: getPendingDocuments derives checklist items from canonical requests")
    void testGetPendingDocumentsDerivesFromCanonicalMultiItemRequests() {
        UUID reqId = UUID.randomUUID();
        UUID itemId1 = UUID.randomUUID();
        UUID itemId2 = UUID.randomUUID();

        com.taxoryn.module.docrequest.entity.DocumentRequestEntity canonicalReq = com.taxoryn.module.docrequest.entity.DocumentRequestEntity.builder()
                .requestNumber("REQ-2026-999")
                .purpose("FY 2026-27 Tax Compliance")
                .status(com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.SENT)
                .clientId(clientId)
                .build();
        canonicalReq.setId(reqId);
        canonicalReq.setOrganizationId(tenantId);

        com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity item1 = com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.builder()
                .request(canonicalReq)
                .title("Bank Statement Q1")
                .documentType(DocumentType.BANK_STATEMENT)
                .status(com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.PENDING)
                .clientId(clientId)
                .build();
        item1.setId(itemId1);
        item1.setOrganizationId(tenantId);

        com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity item2 = com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.builder()
                .request(canonicalReq)
                .title("GST Sales Summary")
                .documentType(DocumentType.GST_INVOICE_SALE)
                .status(com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.REJECTED)
                .rejectionReason("Page 2 columns truncated")
                .clientId(clientId)
                .build();
        item2.setId(itemId2);
        item2.setOrganizationId(tenantId);

        when(multiItemDocRequestRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(tenantId, clientId))
                .thenReturn(List.of(canonicalReq));
        when(multiItemDocRequestItemRepository.findAllByRequestIdOrderByCreatedAtAsc(reqId))
                .thenReturn(List.of(item1, item2));
        when(docRequestRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(tenantId, clientId))
                .thenReturn(List.of());

        List<ClientDocumentRequestDto> pendingList = portalService.getPendingDocuments();

        assertNotNull(pendingList);
        assertEquals(2, pendingList.size());
        assertEquals("Bank Statement Q1", pendingList.get(0).getTitle());
        assertEquals(com.taxoryn.module.portal.entity.ClientDocumentRequestEntity.RequestStatus.PENDING, pendingList.get(0).getStatus());
        assertEquals("GST Sales Summary", pendingList.get(1).getTitle());
        assertEquals(com.taxoryn.module.portal.entity.ClientDocumentRequestEntity.RequestStatus.REJECTED, pendingList.get(1).getStatus());
        assertEquals("Page 2 columns truncated", pendingList.get(1).getRejectionReason());
        assertEquals(reqId, pendingList.get(1).getRequestId());
        assertEquals("REQ-2026-999", pendingList.get(1).getRequestNumber());
    }

    @Test
    @DisplayName("Priority B Consolidation: uploadClientDocument fulfills canonical item and advances parent status")
    void testUploadClientDocumentFulfillsCanonicalItemAndAdvancesParentStatus() {
        UUID reqId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        com.taxoryn.module.docrequest.entity.DocumentRequestEntity parentReq = com.taxoryn.module.docrequest.entity.DocumentRequestEntity.builder()
                .purpose("FY 2026-27 Audit")
                .status(com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.SENT)
                .clientId(clientId)
                .build();
        parentReq.setId(reqId);
        parentReq.setOrganizationId(tenantId);

        com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity item = com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.builder()
                .request(parentReq)
                .title("Form 16")
                .documentType(DocumentType.FORM_16)
                .status(com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.PENDING)
                .clientId(clientId)
                .build();
        item.setId(itemId);
        item.setOrganizationId(tenantId);

        DocumentDto uploadedDoc = DocumentDto.builder()
                .id(docId)
                .fileName("Form16.pdf")
                .documentType(DocumentType.FORM_16)
                .build();

        com.taxoryn.module.document.dto.UploadDocumentRequest uploadReq = com.taxoryn.module.document.dto.UploadDocumentRequest.builder()
                .documentType(DocumentType.FORM_16)
                .build();

        org.springframework.web.multipart.MultipartFile mockFile = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);

        when(documentService.uploadDocument(eq(mockFile), any(com.taxoryn.module.document.dto.UploadDocumentRequest.class)))
                .thenReturn(uploadedDoc);
        when(multiItemDocRequestItemRepository.findByIdAndOrganizationIdAndClientId(itemId, tenantId, clientId))
                .thenReturn(Optional.of(item));

        DocumentDto result = portalService.uploadClientDocument(mockFile, uploadReq, itemId);

        assertNotNull(result);
        assertEquals(docId, result.getId());
        assertEquals(com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus.UPLOADED, item.getStatus());
        assertEquals(docId, item.getUploadedDocumentId());
        assertEquals(com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus.PARTIALLY_COMPLETED, parentReq.getStatus());
        org.mockito.Mockito.verify(multiItemDocRequestItemRepository).save(item);
        org.mockito.Mockito.verify(multiItemDocRequestRepository).save(parentReq);
    }
}
