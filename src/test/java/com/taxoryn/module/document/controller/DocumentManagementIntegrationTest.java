package com.taxoryn.module.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentManagementIntegrationTest {

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

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity adminUser1;
    private UserEntity adminUser2;
    private String adminToken1;
    private String adminToken2;
    private ClientEntity clientAbc;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        documentRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization 1 & 2
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Advisors")
                .email("admin@apextax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Global Tax Consultancy")
                .email("admin@globaltax.com")
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
                .email("admin@apextax.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Rajesh")
                .lastName("Verma")
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

        clientAbc = clientRepository.save(ClientEntity.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("ABC Infotech Pvt Ltd")
                .legalName("ABC Infotech Private Limited")
                .pan("AAACB1234D")
                .status(ClientStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(org2.getId());
        adminUser2 = userRepository.save(UserEntity.builder()
                .email("admin@globaltax.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .firstName("Suresh")
                .lastName("Mehta")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        adminToken2 = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUser2.getId(),
                org2.getId(),
                adminUser2.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("DOCUMENT_VIEW", "DOCUMENT_UPLOAD", "DOCUMENT_DELETE")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Upload, download, fetch metadata, and verify client vault")
    void testUploadAndDownloadDocumentLifecycle() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 Mock Tax Audit Report Content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "TaxAuditReport.pdf", "application/pdf", pdfBytes);

        UploadDocumentRequest metadataReq = UploadDocumentRequest.builder()
                .clientId(clientAbc.getId())
                .documentType(DocumentType.TAX_AUDIT_REPORT)
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .notes("Form 3CA/3CD Tax Audit Report")
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(metadataReq));

        // 1. Upload Document
        String uploadResponse = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metadataPart)
                        .header("Authorization", adminToken1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("TaxAuditReport.pdf"))
                .andExpect(jsonPath("$.data.documentType").value("TAX_AUDIT_REPORT"))
                .andExpect(jsonPath("$.data.clientName").value("ABC Infotech Pvt Ltd"))
                .andExpect(jsonPath("$.data.checksum").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String documentId = objectMapper.readTree(uploadResponse).path("data").path("id").asText();

        // 2. Download Document
        mockMvc.perform(get("/api/v1/documents/" + documentId + "/download")
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"TaxAuditReport.pdf\""))
                .andExpect(content().bytes(pdfBytes));

        // 3. Get Metadata
        mockMvc.perform(get("/api/v1/documents/" + documentId)
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(documentId))
                .andExpect(jsonPath("$.data.financialYear").value("2025-26"));

        // 4. Client Vault
        mockMvc.perform(get("/api/v1/documents/clients/" + clientAbc.getId())
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].fileName").value("TaxAuditReport.pdf"));

        // 5. Cross-Tenant Isolation: Org 2 cannot download Org 1 document
        mockMvc.perform(get("/api/v1/documents/" + documentId + "/download")
                        .header("Authorization", adminToken2))
                .andExpect(status().isNotFound());

        // 6. Delete Document
        mockMvc.perform(delete("/api/v1/documents/" + documentId)
                        .header("Authorization", adminToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
