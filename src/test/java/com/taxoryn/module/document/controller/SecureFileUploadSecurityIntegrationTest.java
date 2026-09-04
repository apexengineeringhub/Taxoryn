package com.taxoryn.module.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentScanStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.entity.DocumentEntity.StorageProvider;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.document.storage.DocumentStorageService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecureFileUploadSecurityIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private DocumentStorageService storageService;

    private OrganizationEntity org1;
    private UserEntity adminUser1;
    private String adminToken1;
    private ClientEntity client1;

    // Valid file byte headers for test fixtures
    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes(StandardCharsets.UTF_8);
    private static final byte[] VALID_PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};
    private static final byte[] VALID_JPEG_BYTES = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46};
    private static final String EICAR_TEST_SIGNATURE = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        documentRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Secure Tax Associates")
                .email("admin@securetax.in")
                .status(OrganizationStatus.ACTIVE)
                .build());

        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        TenantContext.setTenantId(org1.getId());
        adminUser1 = userRepository.save(UserEntity.builder()
                .email("admin@securetax.in")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Arun")
                .lastName("Kumar")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        adminToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser1.getId(),
                org1.getId(),
                adminUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("DOCUMENT_VIEW", "DOCUMENT_UPLOAD", "DOCUMENT_DELETE")
        );

        client1 = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("AlphaTech Solutions")
                .legalName("AlphaTech Solutions Private Limited")
                .pan("AABCA1234D")
                .status(ClientStatus.ACTIVE)
                .build());

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Valid document upload: passes magic-byte inspection and malware scanner, marked CLEAN")
    void testValidDocumentUploadSuccess() throws Exception {
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "Form16_2026.pdf", "application/pdf", VALID_PDF_BYTES);

        UploadDocumentRequest metadata = UploadDocumentRequest.builder()
                .clientId(client1.getId())
                .documentType(DocumentType.FORM_16)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadata));

        String responseStr = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header("Authorization", adminToken1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("Form16_2026.pdf"))
                .andExpect(jsonPath("$.data.scanStatus").value("CLEAN"))
                .andReturn().getResponse().getContentAsString();

        String docId = objectMapper.readTree(responseStr).path("data").path("id").asText();
        assertNotNull(docId);

        DocumentEntity saved = documentRepository.findById(UUID.fromString(docId)).orElseThrow();
        assertEquals(DocumentScanStatus.CLEAN, saved.getScanStatus());
        assertNotNull(saved.getScannedAt());
    }

    @Test
    @DisplayName("2. Disallowed extension: executable .exe rejected with 400 Bad Request")
    void testDisallowedExtensionRejected() throws Exception {
        byte[] payload = new byte[]{(byte) 0x4D, (byte) 0x5A, (byte) 0x90, 0x00, 'B', 'i', 'n', 'a', 'r', 'y'};
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "malicious_script.exe", "application/octet-stream", payload);

        UploadDocumentRequest metadata = UploadDocumentRequest.builder()
                .clientId(client1.getId())
                .documentType(DocumentType.OTHER)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadata));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header("Authorization", adminToken1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Disallowed or dangerous file extension")));
    }

    @Test
    @DisplayName("3. Double extension attack: disguised .pdf.exe or .exe.pdf rejected")
    void testDoubleExtensionAttackRejected() throws Exception {
        byte[] payload = "%PDF-1.4\nfake".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "tax_notice.pdf.exe", "application/pdf", payload);

        UploadDocumentRequest metadata = UploadDocumentRequest.builder()
                .clientId(client1.getId())
                .documentType(DocumentType.OTHER)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadata));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header("Authorization", adminToken1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Disallowed or dangerous file extension")));
    }

    @Test
    @DisplayName("4. MIME/Magic-byte spoofing: non-PDF disguised as .pdf rejected")
    void testMimeMagicByteSpoofingRejected() throws Exception {
        byte[] fakePdfBytes = "This is not a PDF, it is plain text content without header".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", fakePdfBytes);

        UploadDocumentRequest metadata = UploadDocumentRequest.builder()
                .clientId(client1.getId())
                .documentType(DocumentType.GST_INVOICE_SALE)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadata));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header("Authorization", adminToken1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("File content does not match valid PDF document signature")));
    }

    @Test
    @DisplayName("5. Disguised Windows PE Executable (MZ header) disguised as .pdf rejected")
    void testDisguisedExecutableRejected() throws Exception {
        byte[] disguisedMzPdf = new byte[]{(byte) 0x4D, (byte) 0x5A, 0x25, 0x50, 0x44, 0x46, 0x2D};
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "tax_computation.pdf", "application/pdf", disguisedMzPdf);

        UploadDocumentRequest metadata = UploadDocumentRequest.builder()
                .clientId(client1.getId())
                .documentType(DocumentType.ITR_COMPUTATION_SHEET)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadata));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header("Authorization", adminToken1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Executable binary disguised as document is not allowed")));
    }

    @Test
    @DisplayName("6. Antivirus detection: EICAR standard test signature detected and upload blocked")
    void testEicarAntivirusSignatureDetected() throws Exception {
        // EICAR signature inside a nominally valid PDF
        String eicarPayload = "%PDF-1.4\n" + EICAR_TEST_SIGNATURE + "\n%%EOF";
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "infected_file.pdf", "application/pdf", eicarPayload.getBytes(StandardCharsets.UTF_8));

        UploadDocumentRequest metadata = UploadDocumentRequest.builder()
                .clientId(client1.getId())
                .documentType(DocumentType.OTHER)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadata));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header("Authorization", adminToken1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Malware detected in uploaded file")));
    }

    @Test
    @DisplayName("7. Path traversal in filename: rejected with 400 Bad Request")
    void testPathTraversalFilenameRejected() throws Exception {
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "../../etc/passwd.pdf", "application/pdf", VALID_PDF_BYTES);

        UploadDocumentRequest metadata = UploadDocumentRequest.builder()
                .clientId(client1.getId())
                .documentType(DocumentType.OTHER)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadata));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header("Authorization", adminToken1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("illegal path traversal")));
    }

    @Test
    @DisplayName("8. Null-byte injection in filename: rejected with 400 Bad Request")
    void testNullByteFilenameRejected() throws Exception {
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "report.pdf\0.exe", "application/pdf", VALID_PDF_BYTES);

        UploadDocumentRequest metadata = UploadDocumentRequest.builder()
                .clientId(client1.getId())
                .documentType(DocumentType.OTHER)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadata));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header("Authorization", adminToken1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("null byte")));
    }

    @Test
    @DisplayName("9. Zip Slip / Path Traversal inside ZIP archive: rejected with 400 Bad Request")
    void testZipSlipArchiveRejected() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("../evil.sh");
            zos.putNextEntry(entry);
            zos.write("echo hacked".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        MockMultipartFile filePart = new MockMultipartFile(
                "file", "tax_archive.zip", "application/zip", baos.toByteArray());

        UploadDocumentRequest metadata = UploadDocumentRequest.builder()
                .clientId(client1.getId())
                .documentType(DocumentType.OTHER)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadata));

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header("Authorization", adminToken1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Archive entry contains illegal path traversal")));
    }

    @Test
    @DisplayName("10. Fail-Closed Download Gate: Document with INFECTED scan status cannot be downloaded or previewed")
    void testInfectedDocumentDownloadBlocked() throws Exception {
        TenantContext.setTenantId(org1.getId());

        String storageKey = storageService.store(org1.getId(), "infected.pdf", "application/pdf", VALID_PDF_BYTES);
        DocumentEntity infectedDoc = documentRepository.save(DocumentEntity.builder()
                .clientId(client1.getId())
                .documentType(DocumentType.FORM_16)
                .fileName("infected.pdf")
                .contentType("application/pdf")
                .fileSize(VALID_PDF_BYTES.length)
                .storageKey(storageKey)
                .storageProvider(StorageProvider.LOCAL)
                .status(DocumentStatus.ACTIVE)
                .scanStatus(DocumentScanStatus.INFECTED)
                .scanResultDetails("Trojan.Generic detected")
                .build());

        TenantContext.clear();

        // 1. Download blocked
        mockMvc.perform(get("/api/v1/documents/" + infectedDoc.getId() + "/download")
                        .header("Authorization", adminToken1))
                .andExpect(status().isForbidden());

        // 2. Preview blocked
        mockMvc.perform(get("/api/v1/documents/" + infectedDoc.getId() + "/preview")
                        .header("Authorization", adminToken1))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. Fail-Closed Download Gate: Document with SCAN_FAILED status cannot be downloaded or previewed")
    void testScanFailedDocumentDownloadBlocked() throws Exception {
        TenantContext.setTenantId(org1.getId());

        String storageKey = storageService.store(org1.getId(), "unscanned.pdf", "application/pdf", VALID_PDF_BYTES);
        DocumentEntity failedDoc = documentRepository.save(DocumentEntity.builder()
                .clientId(client1.getId())
                .documentType(DocumentType.FORM_16)
                .fileName("unscanned.pdf")
                .contentType("application/pdf")
                .fileSize(VALID_PDF_BYTES.length)
                .storageKey(storageKey)
                .storageProvider(StorageProvider.LOCAL)
                .status(DocumentStatus.ACTIVE)
                .scanStatus(DocumentScanStatus.SCAN_FAILED)
                .scanResultDetails("Scanner engine timeout")
                .build());

        TenantContext.clear();

        // 1. Download blocked
        mockMvc.perform(get("/api/v1/documents/" + failedDoc.getId() + "/download")
                        .header("Authorization", adminToken1))
                .andExpect(status().isForbidden());

        // 2. Preview blocked
        mockMvc.perform(get("/api/v1/documents/" + failedDoc.getId() + "/preview")
                        .header("Authorization", adminToken1))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. Clean document download and preview: succeed with full headers")
    void testCleanDocumentDownloadSuccess() throws Exception {
        TenantContext.setTenantId(org1.getId());

        String storageKey = storageService.store(org1.getId(), "CleanAudit.pdf", "application/pdf", VALID_PDF_BYTES);
        DocumentEntity cleanDoc = documentRepository.save(DocumentEntity.builder()
                .clientId(client1.getId())
                .documentType(DocumentType.TAX_AUDIT_REPORT)
                .fileName("CleanAudit.pdf")
                .contentType("application/pdf")
                .fileSize(VALID_PDF_BYTES.length)
                .storageKey(storageKey)
                .storageProvider(StorageProvider.LOCAL)
                .status(DocumentStatus.ACTIVE)
                .scanStatus(DocumentScanStatus.CLEAN)
                .build());

        TenantContext.clear();

        // 1. Download succeeds
        mockMvc.perform(get("/api/v1/documents/" + cleanDoc.getId() + "/download")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(content().bytes(VALID_PDF_BYTES));

        // 2. Preview succeeds
        mockMvc.perform(get("/api/v1/documents/" + cleanDoc.getId() + "/preview")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(content().bytes(VALID_PDF_BYTES));
    }
}
