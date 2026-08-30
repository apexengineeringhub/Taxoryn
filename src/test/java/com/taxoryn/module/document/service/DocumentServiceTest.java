package com.taxoryn.module.document.service;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.dto.DocumentDownloadDto;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.entity.DocumentEntity.StorageProvider;
import com.taxoryn.module.document.mapper.DocumentMapper;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.storage.DocumentStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentStorageService storageService;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private com.taxoryn.module.subscription.service.SubscriptionService subscriptionService;

    @Mock
    private com.taxoryn.module.audit.service.AuditService auditService;

    @Mock
    private com.taxoryn.core.security.PracticeSecurityScopeEvaluator securityScopeEvaluator;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private UUID tenantId;
    private UUID clientId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .email("admin@taxpractice.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("DOCUMENT_VIEW", "DOCUMENT_UPLOAD", "DOCUMENT_DELETE"))
                .enabled(true)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(tenantId);

        org.mockito.Mockito.lenient().when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(com.taxoryn.core.security.PracticeSecurityScope.firmAdmin(userId));
        org.mockito.Mockito.lenient().when(securityScopeEvaluator.hasBillingAccess(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Upload document successfully")
    void testUploadDocumentSuccess() {
        byte[] content = "Form 16 sample data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "Form16.pdf", "application/pdf", content);

        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .clientId(clientId)
                .documentType(DocumentType.FORM_16)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .notes("Employee Form 16 Part A & B")
                .build();

        ClientEntity client = ClientEntity.builder().displayName("Anand Joshi").build();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(storageService.store(eq(tenantId), eq("Form16.pdf"), eq("application/pdf"), any(byte[].class)))
                .thenReturn("org_" + tenantId + "/2026/08/Form16.pdf");
        when(storageService.getStorageProviderName()).thenReturn("LOCAL");

        DocumentEntity saved = DocumentEntity.builder()
                .clientId(clientId)
                .documentType(DocumentType.FORM_16)
                .fileName("Form16.pdf")
                .contentType("application/pdf")
                .fileSize(content.length)
                .storageKey("org_" + tenantId + "/2026/08/Form16.pdf")
                .storageProvider(StorageProvider.LOCAL)
                .status(DocumentStatus.ACTIVE)
                .build();
        saved.setId(documentId);
        saved.setOrganizationId(tenantId);

        when(documentRepository.save(any(DocumentEntity.class))).thenReturn(saved);
        when(documentMapper.toDto(saved)).thenReturn(DocumentDto.builder()
                .id(documentId)
                .fileName("Form16.pdf")
                .documentType(DocumentType.FORM_16)
                .build());

        DocumentDto result = documentService.uploadDocument(file, request);

        assertNotNull(result);
        assertEquals("Form16.pdf", result.getFileName());
        assertEquals(DocumentType.FORM_16, result.getDocumentType());
    }

    @Test
    @DisplayName("Download document successfully")
    void testDownloadDocumentSuccess() {
        byte[] content = "Binary document content".getBytes(StandardCharsets.UTF_8);

        DocumentEntity document = DocumentEntity.builder()
                .fileName("Invoice.pdf")
                .contentType("application/pdf")
                .fileSize(content.length)
                .storageKey("key123")
                .status(DocumentStatus.ACTIVE)
                .build();
        document.setId(documentId);
        document.setOrganizationId(tenantId);

        when(documentRepository.findByIdAndOrganizationId(documentId, tenantId)).thenReturn(Optional.of(document));
        when(storageService.retrieve("key123")).thenReturn(content);

        DocumentDownloadDto download = documentService.downloadDocument(documentId);

        assertNotNull(download);
        assertEquals("Invoice.pdf", download.getFileName());
        assertEquals("application/pdf", download.getContentType());
        assertArrayEquals(content, download.getData());
    }

    @Test
    @DisplayName("Delete document soft-deletes and removes from storage")
    void testDeleteDocumentSuccess() {
        DocumentEntity document = DocumentEntity.builder()
                .storageKey("key123")
                .status(DocumentStatus.ACTIVE)
                .build();
        document.setId(documentId);
        document.setOrganizationId(tenantId);

        when(documentRepository.findByIdAndOrganizationId(documentId, tenantId)).thenReturn(Optional.of(document));

        documentService.deleteDocument(documentId);

        verify(storageService).delete("key123");
        assertEquals(DocumentStatus.DELETED, document.getStatus());
        verify(documentRepository).save(document);
    }

    @Test
    @DisplayName("SECURITY: firm admin can view a client's document vault")
    void testGetClientDocumentsAllowedForFirmAdmin() {
        ClientEntity client = new ClientEntity();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(documentRepository.findAllByOrganizationIdAndClientIdAndStatus(tenantId, clientId, DocumentStatus.ACTIVE))
                .thenReturn(java.util.List.of());

        // setUp() already stubs securityScopeEvaluator.evaluateCurrentScope() -> firmAdmin
        documentService.getClientDocuments(clientId);

        verify(documentRepository).findAllByOrganizationIdAndClientIdAndStatus(tenantId, clientId, DocumentStatus.ACTIVE);
    }

    @Test
    @DisplayName("SECURITY: restricted staff cannot view another employee's client document vault (same org)")
    void testGetClientDocumentsDeniedForOutOfScopeStaff() {
        ClientEntity client = new ClientEntity();
        client.setId(clientId);
        client.setOrganizationId(tenantId);

        UUID restrictedUserId = UUID.randomUUID();
        UUID restrictedEmployeeId = UUID.randomUUID();
        // Staff scope whose accessible client set does NOT include this clientId
        // (e.g. clientId belongs to a colleague's portfolio in the same organization).
        com.taxoryn.core.security.PracticeSecurityScope restrictedScope =
                com.taxoryn.core.security.PracticeSecurityScope.staffIndividual(
                        restrictedUserId, restrictedEmployeeId, Set.of(UUID.randomUUID()));

        org.mockito.Mockito.reset(securityScopeEvaluator);
        when(securityScopeEvaluator.evaluateCurrentScope()).thenReturn(restrictedScope);
        when(securityScopeEvaluator.getAccessibleClientIds(restrictedScope)).thenReturn(Set.of());
        when(clientRepository.findByIdAndOrganizationId(clientId, tenantId)).thenReturn(Optional.of(client));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> documentService.getClientDocuments(clientId),
                "A restricted employee must not be able to pull another employee's client document vault " +
                "just because both clients belong to the same organization.");

        org.mockito.Mockito.verify(documentRepository, org.mockito.Mockito.never())
                .findAllByOrganizationIdAndClientIdAndStatus(any(), any(), any());
    }
}
