package com.taxoryn.core.security;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.module.billing.dto.InvoiceDto;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.mapper.InvoiceMapper;
import com.taxoryn.module.billing.repository.InvoiceItemRepository;
import com.taxoryn.module.billing.repository.InvoicePaymentRepository;
import com.taxoryn.module.billing.repository.InvoiceRepository;
import com.taxoryn.module.billing.service.InvoiceServiceImpl;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.dto.DocumentDownloadDto;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.mapper.DocumentMapper;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.service.DocumentServiceImpl;
import com.taxoryn.module.document.storage.DocumentStorageService;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.portal.repository.ClientNotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StrongTenantIsolationAndAbacTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentStorageService storageService;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private PracticeSecurityScopeEvaluator securityScopeEvaluator;
    @Mock
    private com.taxoryn.module.audit.service.AuditService auditService;
    @Mock
    private com.taxoryn.module.subscription.service.SubscriptionService subscriptionService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceItemRepository invoiceItemRepository;
    @Mock
    private InvoicePaymentRepository invoicePaymentRepository;
    @Mock
    private ClientNotificationRepository notificationRepository;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private UUID tenantAId;
    private UUID tenantBId;
    private UUID client1Id;
    private UUID client2Id;
    private UUID staffUserId;

    @BeforeEach
    void setUp() {
        tenantAId = UUID.randomUUID();
        tenantBId = UUID.randomUUID();
        client1Id = UUID.randomUUID();
        client2Id = UUID.randomUUID();
        staffUserId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticateStaff(UUID tenantId, UUID userId) {
        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .email("staff@practice.com")
                .roles(Set.of("STAFF"))
                .permissions(Set.of("DOCUMENT_VIEW", "INVOICE_VIEW"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);
    }

    private void authenticateClientPortalUser(UUID tenantId, UUID clientId) {
        SecurityUser principal = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(tenantId)
                .clientId(clientId)
                .email("client@clientdomain.com")
                .roles(Set.of("CLIENT_USER"))
                .permissions(Set.of("PORTAL_ACCESS"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);
    }

    @Test
    @DisplayName("Cross-Tenant Document Access: Tenant A cannot download Tenant B document (IDOR Prevention)")
    void testCrossTenantDocumentAccessBlocked() {
        authenticateStaff(tenantAId, staffUserId);

        UUID docId = UUID.randomUUID();
        // Repository returns empty because findByIdAndOrganizationId scopes to tenantAId
        when(documentRepository.findByIdAndOrganizationId(docId, tenantAId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> documentService.downloadDocument(docId));
    }

    @Test
    @DisplayName("ABAC Document Isolation: Staff cannot download document of unassigned client in same tenant")
    void testAbacUnassignedClientDocumentBlocked() {
        authenticateStaff(tenantAId, staffUserId);

        UUID docId = UUID.randomUUID();
        DocumentEntity doc = DocumentEntity.builder()
                .clientId(client2Id)
                .fileName("SecretAudit.pdf")
                .storageKey("tenants/" + tenantAId + "/SecretAudit.pdf")
                .status(DocumentEntity.DocumentStatus.ACTIVE)
                .build();
        doc.setId(docId);
        doc.setOrganizationId(tenantAId);

        when(documentRepository.findByIdAndOrganizationId(docId, tenantAId)).thenReturn(Optional.of(doc));

        PracticeSecurityScope staffScope = PracticeSecurityScope.builder()
                .organizationId(tenantAId)
                .userId(staffUserId)
                .isFirmAdmin(false)
                .isStaff(true)
                .build();

        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(staffScope);
        when(securityScopeEvaluator.getAccessibleClientIds(staffScope)).thenReturn(Set.of(client1Id)); // only client1 assigned

        assertThrows(AccessDeniedException.class, () -> documentService.downloadDocument(docId));
    }

    @Test
    @DisplayName("ABAC Document Access: Staff CAN download document of assigned client in same tenant")
    void testAbacAssignedClientDocumentAllowed() {
        authenticateStaff(tenantAId, staffUserId);

        UUID docId = UUID.randomUUID();
        DocumentEntity doc = DocumentEntity.builder()
                .clientId(client1Id)
                .fileName("Client1Audit.pdf")
                .storageKey("tenants/" + tenantAId + "/Client1Audit.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .status(DocumentEntity.DocumentStatus.ACTIVE)
                .build();
        doc.setId(docId);
        doc.setOrganizationId(tenantAId);

        when(documentRepository.findByIdAndOrganizationId(docId, tenantAId)).thenReturn(Optional.of(doc));

        PracticeSecurityScope staffScope = PracticeSecurityScope.builder()
                .organizationId(tenantAId)
                .userId(staffUserId)
                .isFirmAdmin(false)
                .isStaff(true)
                .build();

        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(staffScope);
        when(securityScopeEvaluator.getAccessibleClientIds(staffScope)).thenReturn(Set.of(client1Id));
        when(storageService.retrieve(doc.getStorageKey())).thenReturn("data".getBytes());

        DocumentDownloadDto result = documentService.downloadDocument(docId);
        assertNotNull(result);
        assertEquals("Client1Audit.pdf", result.getFileName());
    }

    @Test
    @DisplayName("Cross-Tenant Invoice Access: Tenant A cannot access Tenant B invoice")
    void testCrossTenantInvoiceAccessBlocked() {
        authenticateStaff(tenantAId, staffUserId);

        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndOrganizationId(invoiceId, tenantAId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoiceById(invoiceId));
    }

    @Test
    @DisplayName("ABAC Invoice Isolation: Staff without billing permissions cannot access invoice for unassigned client")
    void testAbacInvoiceAccessDeniedForUnassignedClient() {
        authenticateStaff(tenantAId, staffUserId);

        UUID invoiceId = UUID.randomUUID();
        InvoiceEntity invoice = InvoiceEntity.builder()
                .clientId(client2Id)
                .invoiceNumber("INV-2026-0001")
                .build();
        invoice.setId(invoiceId);
        invoice.setOrganizationId(tenantAId);

        when(invoiceRepository.findByIdAndOrganizationId(invoiceId, tenantAId)).thenReturn(Optional.of(invoice));

        PracticeSecurityScope staffScope = PracticeSecurityScope.builder()
                .organizationId(tenantAId)
                .userId(staffUserId)
                .isFirmAdmin(false)
                .isStaff(true)
                .build();

        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(staffScope);
        when(securityScopeEvaluator.hasBillingAccess(staffScope)).thenReturn(false);
        when(securityScopeEvaluator.getAccessibleClientIds(staffScope)).thenReturn(Set.of(client1Id)); // client2 is unassigned

        assertThrows(AccessDeniedException.class, () -> invoiceService.getInvoiceById(invoiceId));
    }

    @Test
    @DisplayName("Client Portal Isolation: Client Portal User cannot access invoice belonging to another client")
    void testClientPortalInvoiceIsolation() {
        authenticateClientPortalUser(tenantAId, client1Id);

        UUID invoiceId = UUID.randomUUID();
        InvoiceEntity invoiceOfClient2 = InvoiceEntity.builder()
                .clientId(client2Id)
                .invoiceNumber("INV-2026-0099")
                .build();
        invoiceOfClient2.setId(invoiceId);
        invoiceOfClient2.setOrganizationId(tenantAId);

        when(invoiceRepository.findByIdAndOrganizationId(invoiceId, tenantAId)).thenReturn(Optional.of(invoiceOfClient2));

        assertThrows(AccessDeniedException.class, () -> invoiceService.getInvoiceById(invoiceId));
    }
}
