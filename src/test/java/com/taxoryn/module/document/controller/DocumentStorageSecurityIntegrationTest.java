package com.taxoryn.module.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.dto.UpdateDocumentRequest;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.storage.DocumentStorageService;
import com.taxoryn.module.gst.entity.GstProfileEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstFilingStatus;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity.GstReturnType;
import com.taxoryn.module.gst.repository.GstProfileRepository;
import com.taxoryn.module.gst.repository.GstReturnFilingRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentStorageSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private GstProfileRepository gstProfileRepository;

    @Autowired
    private GstReturnFilingRepository gstReturnFilingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private DocumentStorageService storageService;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private UserEntity adminUser2;
    private UserEntity clientPortalUser1;
    private UserEntity clientPortalUser2;
    private String tokenOrg1;
    private String tokenOrg2;
    private String tokenClient1;
    private String tokenClient2;
    private ClientEntity client1;
    private ClientEntity client2;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        documentRepository.deleteAll();
        gstReturnFilingRepository.deleteAll();
        gstProfileRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Setup Tenant Organizations
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Tax Firm")
                .email("alpha" + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Beta Tax Firm")
                .email("beta" + UUID.randomUUID() + "@taxoryn.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Setup Roles
        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity clientAdminRole = roleRepository.save(RoleEntity.builder()
                .code("CLIENT_ADMIN")
                .name("Client Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 3. Setup Clients
        TenantContext.setTenantId(org1.getId());
        client1 = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Alpha Client One Pvt Ltd")
                .legalName("Alpha Client One Private Limited")
                .pan("AAACA1111A")
                .status(ClientStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(org2.getId());
        client2 = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Beta Client Two Pvt Ltd")
                .legalName("Beta Client Two Private Limited")
                .pan("BBBCB2222B")
                .status(ClientStatus.ACTIVE)
                .build());

        // 4. Setup Users
        TenantContext.setTenantId(org1.getId());
        adminUser1 = userRepository.save(UserEntity.builder()
                .email("admin@alpha.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Alpha")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        clientPortalUser1 = userRepository.save(UserEntity.builder()
                .email("portal@alphaclient.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Alpha")
                .lastName("ClientUser")
                .clientId(client1.getId())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientAdminRole)))
                .build());

        TenantContext.setTenantId(org2.getId());
        adminUser2 = userRepository.save(UserEntity.builder()
                .email("admin@beta.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Beta")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        clientPortalUser2 = userRepository.save(UserEntity.builder()
                .email("portal@betaclient.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Beta")
                .lastName("ClientUser")
                .clientId(client2.getId())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientAdminRole)))
                .build());

        // 5. Generate Auth Tokens
        tokenOrg1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser1.getId(),
                org1.getId(),
                adminUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("DOCUMENT_VIEW", "DOCUMENT_UPLOAD", "DOCUMENT_DELETE", "DOCUMENT_UPDATE")
        );

        tokenOrg2 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser2.getId(),
                org2.getId(),
                adminUser2.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("DOCUMENT_VIEW", "DOCUMENT_UPLOAD", "DOCUMENT_DELETE", "DOCUMENT_UPDATE")
        );

        tokenClient1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientPortalUser1.getId(),
                org1.getId(),
                client1.getId(),
                clientPortalUser1.getEmail(),
                Set.of("CLIENT_ADMIN"),
                Set.of("CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_DOCUMENT_UPLOAD")
        );

        tokenClient2 = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientPortalUser2.getId(),
                org2.getId(),
                client2.getId(),
                clientPortalUser2.getEmail(),
                Set.of("CLIENT_ADMIN"),
                Set.of("CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_DOCUMENT_UPLOAD")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private String uploadTestDocument(String authHeader, UUID clientId, String filename, byte[] content) throws Exception {
        MockMultipartFile filePart = new MockMultipartFile("file", filename, "application/pdf", content);
        UploadDocumentRequest metadataReq = UploadDocumentRequest.builder()
                .clientId(clientId)
                .documentType(DocumentType.FORM_16)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .notes("Confidential Tax Record")
                .build();
        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadataReq));

        MvcResult result = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    @Test
    @DisplayName("1. Cross-Tenant Document Access: Tenant B cannot download, preview, get, update, or delete Tenant A document")
    void crossTenantDocumentAccessBlocked() throws Exception {
        byte[] confidentialBytes = "%PDF-1.4 Tenant A Confidential Tax Records".getBytes(StandardCharsets.UTF_8);
        String docId = uploadTestDocument(tokenOrg1, client1.getId(), "ITR_Computation_Alpha.pdf", confidentialBytes);

        // Tenant A can download successfully
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg1))
                .andExpect(status().isOk())
                .andExpect(content().bytes(confidentialBytes));

        // Tenant B cannot download (returns 404 ResourceNotFound)
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg2))
                .andExpect(status().isNotFound());

        // Tenant B cannot preview
        mockMvc.perform(get("/api/v1/documents/" + docId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg2))
                .andExpect(status().isNotFound());

        // Tenant B cannot view metadata
        mockMvc.perform(get("/api/v1/documents/" + docId)
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg2))
                .andExpect(status().isNotFound());

        // Tenant B cannot update metadata
        UpdateDocumentRequest updateReq = UpdateDocumentRequest.builder()
                .notes("Hacked Notes")
                .build();
        mockMvc.perform(put("/api/v1/documents/" + docId)
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Tenant B cannot delete
        mockMvc.perform(delete("/api/v1/documents/" + docId)
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("2. Metadata Privacy: Internal storageKey and storageProvider are not leaked in API responses")
    void storageMetadataNotExposedInJson() throws Exception {
        byte[] content = "%PDF-1.4 Form 26AS Statement".getBytes(StandardCharsets.UTF_8);
        String docId = uploadTestDocument(tokenOrg1, client1.getId(), "Form26AS.pdf", content);

        mockMvc.perform(get("/api/v1/documents/" + docId)
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("Form26AS.pdf"))
                .andExpect(jsonPath("$.data.storageKey").doesNotExist())
                .andExpect(jsonPath("$.data.storageProvider").doesNotExist());
    }

    @Test
    @DisplayName("3. HTTP Security Headers: Strict cache-control and nosniff headers enforced on downloads & previews")
    void downloadAndPreviewSecurityHeadersEnforced() throws Exception {
        byte[] content = "%PDF-1.4 GST Audit Report".getBytes(StandardCharsets.UTF_8);
        String docId = uploadTestDocument(tokenOrg1, client1.getId(), "GST_Audit.pdf", content);

        // Test Download Headers
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg1))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"GST_Audit.pdf\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate"))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                .andExpect(header().string(HttpHeaders.EXPIRES, "0"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        // Test Preview Headers
        mockMvc.perform(get("/api/v1/documents/" + docId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg1))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"GST_Audit.pdf\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate"))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                .andExpect(header().string(HttpHeaders.EXPIRES, "0"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("4. Relational Tenant Boundary: Upload fails if clientId belongs to another tenant")
    void uploadWithCrossTenantClientIdRejected() throws Exception {
        MockMultipartFile filePart = new MockMultipartFile("file", "Attack.pdf", "application/pdf", "attack".getBytes());
        // Org 1 admin tries to attach document to Org 2 client
        UploadDocumentRequest metadataReq = UploadDocumentRequest.builder()
                .clientId(client2.getId())
                .documentType(DocumentType.PAN_CARD)
                .build();
        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadataReq));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5. Relational Tenant Boundary: Upload fails if gstFilingId belongs to another tenant")
    void uploadWithCrossTenantGstFilingIdRejected() throws Exception {
        TenantContext.setTenantId(org2.getId());
        GstProfileEntity gstProfile2 = gstProfileRepository.save(GstProfileEntity.builder()
                .clientId(client2.getId())
                .gstin("27BBBCB2222B1Z5")
                .legalName("Beta Client Two Private Limited")
                .stateCode("27")
                .status(com.taxoryn.module.gst.entity.GstProfileEntity.GstProfileStatus.ACTIVE)
                .build());

        GstReturnFilingEntity filing2 = gstReturnFilingRepository.save(GstReturnFilingEntity.builder()
                .gstProfileId(gstProfile2.getId())
                .clientId(client2.getId())
                .returnType(GstReturnType.GSTR1)
                .returnPeriod("082026")
                .financialYear("2026-27")
                .dueDate(LocalDate.now().plusDays(10))
                .filingStatus(GstFilingStatus.PENDING)
                .build());
        TenantContext.clear();

        MockMultipartFile filePart = new MockMultipartFile("file", "Attack.pdf", "application/pdf", "attack".getBytes());
        // Org 1 admin tries to bind filing belonging to Org 2
        UploadDocumentRequest metadataReq = UploadDocumentRequest.builder()
                .clientId(client1.getId())
                .gstFilingId(filing2.getId())
                .documentType(DocumentType.GST_INVOICE_SALE)
                .build();
        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadataReq));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("6. Client Portal Security: Client Portal User 1 cannot access Client 2's document")
    void clientPortalCrossClientAccessBlocked() throws Exception {
        byte[] content = "%PDF-1.4 Alpha Client Document".getBytes(StandardCharsets.UTF_8);
        String docId = uploadTestDocument(tokenOrg1, client1.getId(), "Client1_Doc.pdf", content);

        // Client 1 can download via portal
        mockMvc.perform(get("/api/v1/portal/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, tokenClient1))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));

        // Client 2 cannot download Client 1 document (returns 403 or 404)
        mockMvc.perform(get("/api/v1/portal/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, tokenClient2))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("7. Path Traversal Rejection: Storage service rejects traversal attacks")
    void storagePathTraversalAttacksRejected() {
        assertThrows(BadRequestException.class, () -> storageService.retrieve("../etc/passwd"));
        assertThrows(BadRequestException.class, () -> storageService.retrieve("..\\windows\\win.ini"));
        assertThrows(BadRequestException.class, () -> storageService.retrieve("org_1/../../secret.txt"));
        assertThrows(BadRequestException.class, () -> storageService.retrieve("%2e%2e%2fsecret.txt"));
        assertThrows(BadRequestException.class, () -> storageService.retrieve("file\0.txt"));
    }

    @Test
    @DisplayName("8. Soft-Deleted Document Security: Deleted documents return 404 upon download and preview")
    void deletedDocumentCannotBeDownloaded() throws Exception {
        byte[] content = "%PDF-1.4 Temporary File".getBytes(StandardCharsets.UTF_8);
        String docId = uploadTestDocument(tokenOrg1, client1.getId(), "TempFile.pdf", content);

        // Delete document
        mockMvc.perform(delete("/api/v1/documents/" + docId)
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg1))
                .andExpect(status().isOk());

        // Download must return 404
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg1))
                .andExpect(status().isNotFound());

        // Preview must return 404
        mockMvc.perform(get("/api/v1/documents/" + docId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("9. Anonymous Access Denied: Unauthenticated requests to document endpoints return 401")
    void anonymousAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/documents/" + UUID.randomUUID() + "/download"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/documents/" + UUID.randomUUID() + "/download-url"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/portal/documents"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/portal/documents/" + UUID.randomUUID() + "/download-url"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("10. Download URL Endpoint Security: Firm and Portal users receive authorized download URL response")
    void testDownloadUrlEndpoints() throws Exception {
        byte[] content = "%PDF-1.4 Vault Document".getBytes(StandardCharsets.UTF_8);
        String docId = uploadTestDocument(tokenOrg1, client1.getId(), "VaultDoc.pdf", content);

        // Firm user requests download URL
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download-url")
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.fileName").value("VaultDoc.pdf"));

        // Client 1 portal user requests download URL
        mockMvc.perform(get("/api/v1/portal/documents/" + docId + "/download-url")
                        .header(HttpHeaders.AUTHORIZATION, tokenClient1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.fileName").value("VaultDoc.pdf"));

        // Client 2 cannot get download URL for Client 1 document
        mockMvc.perform(get("/api/v1/portal/documents/" + docId + "/download-url")
                        .header(HttpHeaders.AUTHORIZATION, tokenClient2))
                .andExpect(status().is4xxClientError());

        // Org 2 user cannot get download URL for Org 1 document
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download-url")
                        .header(HttpHeaders.AUTHORIZATION, tokenOrg2))
                .andExpect(status().isNotFound());
    }
}
