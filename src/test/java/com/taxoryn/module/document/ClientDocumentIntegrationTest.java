package com.taxoryn.module.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.dto.UpdateDocumentRequest;
import com.taxoryn.module.document.dto.UploadDocumentRequest;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.repository.DocumentRepository;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClientDocumentIntegrationTest {

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

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity adminUserA;
    private UserEntity adminUserB;
    private ClientEntity clientA;
    private ClientEntity clientB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("CA Firm A - " + UUID.randomUUID())
                .legalName("CA Firm A LLP")
                .email("admin.a." + UUID.randomUUID() + "@firm-a.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        adminUserA = userRepository.save(UserEntity.builder()
                .organizationId(orgA.getId())
                .email("admin.a." + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("A")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenA = jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                orgA.getId(),
                adminUserA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "DOCUMENT_UPLOAD", "DOCUMENT_VIEW", "DOCUMENT_READ", "DOCUMENT_WRITE", "DOCUMENT_DELETE", "CLIENT_VIEW")
        );

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("CA Firm B - " + UUID.randomUUID())
                .legalName("CA Firm B LLP")
                .email("admin.b." + UUID.randomUUID() + "@firm-b.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        adminUserB = userRepository.save(UserEntity.builder()
                .organizationId(orgB.getId())
                .email("admin.b." + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("B")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        tokenB = jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                orgB.getId(),
                adminUserB.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "DOCUMENT_UPLOAD", "DOCUMENT_VIEW", "DOCUMENT_READ", "DOCUMENT_WRITE", "DOCUMENT_DELETE", "CLIENT_VIEW")
        );

        ClientEntity cA = ClientEntity.builder()
                .displayName("Alpha Enterprises")
                .legalName("Alpha Enterprises Pvt Ltd")
                .clientType(ClientType.COMPANY)
                .pan("ABCDE1234F")
                .email("alpha@client.com")
                .phone("9876543210")
                .status(ClientStatus.ACTIVE)
                .build();
        cA.setOrganizationId(orgA.getId());
        clientA = clientRepository.save(cA);

        ClientEntity cB = ClientEntity.builder()
                .displayName("Beta Corporation")
                .legalName("Beta Corporation Ltd")
                .clientType(ClientType.COMPANY)
                .pan("XYZPQ5678R")
                .email("beta@client.com")
                .phone("9876543211")
                .status(ClientStatus.ACTIVE)
                .build();
        cB.setOrganizationId(orgB.getId());
        clientB = clientRepository.save(cB);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Upload document with metadata and verify SHA256 checksum and clean scan status")
    void testUploadDocumentSuccess() throws Exception {
        byte[] fileContent = "%PDF-1.4 sample pdf content for client document test".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "Form16_2026.pdf", "application/pdf", fileContent);

        UploadDocumentRequest metaRequest = UploadDocumentRequest.builder()
                .clientId(clientA.getId())
                .documentType(DocumentType.FORM_16)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .notes("Original signed Form 16 from employer")
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "", "application/json",
                objectMapper.writeValueAsBytes(metaRequest)
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("Form16_2026.pdf"))
                .andExpect(jsonPath("$.data.documentType").value("FORM_16"))
                .andExpect(jsonPath("$.data.clientId").value(clientA.getId().toString()))
                .andExpect(jsonPath("$.data.financialYear").value("2025-26"))
                .andExpect(jsonPath("$.data.assessmentYear").value("2026-27"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.scanStatus").value("CLEAN"))
                .andExpect(jsonPath("$.data.checksum").isNotEmpty());
    }

    @Test
    @DisplayName("Download and preview uploaded document and verify binary streaming headers")
    void testDownloadAndPreviewDocument() throws Exception {
        byte[] fileContent = "%PDF-1.4 tax return acknowledgement".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "ITR_V_Ack.pdf", "application/pdf", fileContent);

        UploadDocumentRequest metaRequest = UploadDocumentRequest.builder()
                .clientId(clientA.getId())
                .documentType(DocumentType.ITR_ACKNOWLEDGEMENT)
                .financialYear("2025-26")
                .notes("ITR-V acknowledgement")
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "", "application/json",
                objectMapper.writeValueAsBytes(metaRequest)
        );

        String uploadResponse = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID docId = UUID.fromString(objectMapper.readTree(uploadResponse).path("data").path("id").asText());

        // Test Download (attachment disposition)
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"ITR_V_Ack.pdf\""))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        // Test Preview (inline disposition)
        mockMvc.perform(get("/api/v1/documents/" + docId + "/preview")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"ITR_V_Ack.pdf\""));
    }

    @Test
    @DisplayName("Update document metadata and verify persistence")
    void testUpdateDocumentMetadata() throws Exception {
        byte[] fileContent = "%PDF-1.4 test balance sheet".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "BS_Draft.pdf", "application/pdf", fileContent);

        UploadDocumentRequest metaRequest = UploadDocumentRequest.builder()
                .clientId(clientA.getId())
                .documentType(DocumentType.FINANCIAL_STATEMENTS)
                .financialYear("2024-25")
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "", "application/json",
                objectMapper.writeValueAsBytes(metaRequest)
        );

        String uploadResponse = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID docId = UUID.fromString(objectMapper.readTree(uploadResponse).path("data").path("id").asText());

        UpdateDocumentRequest updateRequest = UpdateDocumentRequest.builder()
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .notes("Final audited balance sheet")
                .build();

        mockMvc.perform(put("/api/v1/documents/" + docId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.financialYear").value("2025-26"))
                .andExpect(jsonPath("$.data.assessmentYear").value("2026-27"))
                .andExpect(jsonPath("$.data.notes").value("Final audited balance sheet"));
    }

    @Test
    @DisplayName("Soft delete document and verify it cannot be retrieved or downloaded")
    void testSoftDeleteDocument() throws Exception {
        byte[] fileContent = "%PDF-1.4 temporary document".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "Temp_Doc.pdf", "application/pdf", fileContent);

        UploadDocumentRequest metaRequest = UploadDocumentRequest.builder()
                .clientId(clientA.getId())
                .documentType(DocumentType.OTHER)
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "", "application/json",
                objectMapper.writeValueAsBytes(metaRequest)
        );

        String uploadResponse = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID docId = UUID.fromString(objectMapper.readTree(uploadResponse).path("data").path("id").asText());

        // Delete document
        mockMvc.perform(delete("/api/v1/documents/" + docId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Attempt to download deleted document -> 404
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Strict multi-tenant isolation: Tenant B cannot access Tenant A's document")
    void testTenantIsolation() throws Exception {
        byte[] fileContent = "%PDF-1.4 confidential audit report".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "Confidential_Audit.pdf", "application/pdf", fileContent);

        UploadDocumentRequest metaRequest = UploadDocumentRequest.builder()
                .clientId(clientA.getId())
                .documentType(DocumentType.TAX_AUDIT_REPORT)
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "", "application/json",
                objectMapper.writeValueAsBytes(metaRequest)
        );

        String uploadResponse = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID docId = UUID.fromString(objectMapper.readTree(uploadResponse).path("data").path("id").asText());

        // Tenant B attempts to access Tenant A's document metadata -> 404
        mockMvc.perform(get("/api/v1/documents/" + docId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Tenant B attempts to download Tenant A's document -> 404
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Retrieve client document vault lists active documents for client")
    void testGetClientDocuments() throws Exception {
        byte[] file1 = "%PDF-1.4 PAN Card".getBytes(StandardCharsets.UTF_8);
        byte[] file2 = "%PDF-1.4 Aadhaar Card".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile panFile = new MockMultipartFile("file", "PAN.pdf", "application/pdf", file1);
        MockMultipartFile aadhaarFile = new MockMultipartFile("file", "Aadhaar.pdf", "application/pdf", file2);

        MockMultipartFile panMeta = new MockMultipartFile("metadata", "", "application/json",
                objectMapper.writeValueAsBytes(UploadDocumentRequest.builder().clientId(clientA.getId()).documentType(DocumentType.PAN_CARD).build()));
        MockMultipartFile aadhaarMeta = new MockMultipartFile("metadata", "", "application/json",
                objectMapper.writeValueAsBytes(UploadDocumentRequest.builder().clientId(clientA.getId()).documentType(DocumentType.AADHAAR_CARD).build()));

        mockMvc.perform(multipart("/api/v1/documents/upload").file(panFile).file(panMeta).header("Authorization", "Bearer " + tokenA)).andExpect(status().isCreated());
        mockMvc.perform(multipart("/api/v1/documents/upload").file(aadhaarFile).file(aadhaarMeta).header("Authorization", "Bearer " + tokenA)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/documents/clients/" + clientA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
