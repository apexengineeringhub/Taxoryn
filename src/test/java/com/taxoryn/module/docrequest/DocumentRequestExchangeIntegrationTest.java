package com.taxoryn.module.docrequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.docrequest.dto.CreateClientAcknowledgementRequest;
import com.taxoryn.module.docrequest.dto.DeclineDocumentRequest;
import com.taxoryn.module.docrequest.dto.SendDocumentToClientRequest;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.DocumentCategory;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.ExchangeType;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestDirection;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentScanStatus;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentStatus;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentRequestExchangeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRequestRepository docRequestRepository;

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
    private AuditLogRepository auditLogRepository;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private UserEntity practitionerUser1;
    private String practitionerToken1;
    private ClientEntity client1;
    private UserEntity clientUser1;
    private String clientToken1;

    private ClientEntity client2;
    private UserEntity clientUser2;
    private String clientToken2;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        docRequestRepository.deleteAll();
        documentRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
        auditLogRepository.deleteAll();

        // 1. Setup Organization 1 & 2
        org1 = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Tax Consultants")
                .email("admin@apextax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        org2 = organizationRepository.save(OrganizationEntity.builder()
                .name("Zenith Advisory")
                .email("admin@zenith.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        // 2. Roles
        RoleEntity adminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        RoleEntity clientRole = roleRepository.save(RoleEntity.builder()
                .code("CLIENT_ADMIN")
                .name("Client Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 3. Practitioner in Org 1
        TenantContext.setTenantId(org1.getId());

        practitionerUser1 = userRepository.save(UserEntity.builder()
                .email("practitioner@apextax.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Rajesh")
                .lastName("Verma")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build());

        practitionerToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                practitionerUser1.getId(),
                org1.getId(),
                practitionerUser1.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_UPDATE", "DOCUMENT_WRITE", "CLIENT_VIEW", "DOCUMENT_VIEW")
        );

        // 4. Client 1 in Org 1
        client1 = clientRepository.save(ClientEntity.builder()
                .displayName("Acme Global Pvt Ltd")
                .pan("ABCDE1234F")
                .email("finance@acmeglobal.com")
                .clientType(ClientType.PRIVATE_LIMITED)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());

        clientUser1 = userRepository.save(UserEntity.builder()
                .email("finance@acmeglobal.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Suresh")
                .lastName("Kumar")
                .status(UserStatus.ACTIVE)
                .clientId(client1.getId())
                .roles(new HashSet<>(Set.of(clientRole)))
                .build());

        clientToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientUser1.getId(),
                org1.getId(),
                client1.getId(),
                clientUser1.getEmail(),
                Set.of("CLIENT_ADMIN"),
                Set.of("CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_DOCUMENT_UPLOAD", "CLIENT_PORTAL_ACCESS")
        );

        // 5. Client 2 in Org 1
        client2 = clientRepository.save(ClientEntity.builder()
                .displayName("Beta Enterprises")
                .pan("XYZPQ5678R")
                .email("contact@beta.com")
                .clientType(ClientType.INDIVIDUAL)
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build());

        clientUser2 = userRepository.save(UserEntity.builder()
                .email("contact@beta.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Anita")
                .lastName("Roy")
                .status(UserStatus.ACTIVE)
                .clientId(client2.getId())
                .roles(new HashSet<>(Set.of(clientRole)))
                .build());

        clientToken2 = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientUser2.getId(),
                org1.getId(),
                client2.getId(),
                clientUser2.getEmail(),
                Set.of("CLIENT_ADMIN"),
                Set.of("CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_DOCUMENT_UPLOAD", "CLIENT_PORTAL_ACCESS")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Client can request an acknowledgement or document from practitioner")
    void testClientCanRequestAcknowledgement() throws Exception {
        CreateClientAcknowledgementRequest request = CreateClientAcknowledgementRequest.builder()
                .category(DocumentCategory.ACKNOWLEDGEMENT)
                .documentType(DocumentType.ITR_ACKNOWLEDGEMENT)
                .purpose("ITR-V Acknowledgement AY 2026-27")
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .message("Please provide my ITR acknowledgement for loan processing.")
                .build();

        mockMvc.perform(post("/api/v1/portal/document-requests/v1/request-acknowledgement")
                        .header("Authorization", clientToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.purpose").value("ITR-V Acknowledgement AY 2026-27"))
                .andExpect(jsonPath("$.data.exchangeType").value("ACKNOWLEDGEMENT_REQUEST"))
                .andExpect(jsonPath("$.data.direction").value("CLIENT_TO_PRACTITIONER"))
                .andExpect(jsonPath("$.data.category").value("ACKNOWLEDGEMENT"))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.clientId").value(client1.getId().toString()));

        // Verify audit log
        TenantContext.setTenantId(org1.getId());
        boolean hasAudit = auditLogRepository.findAll().stream()
                .anyMatch(a -> "ACKNOWLEDGEMENT_REQUESTED".equals(a.getAction()));
        assertTrue(hasAudit, "Audit event ACKNOWLEDGEMENT_REQUESTED should be recorded");
    }

    @Test
    @DisplayName("Practitioner can fulfill client request by uploading delivered document")
    void testPractitionerCanFulfillClientAcknowledgementRequestWithNewUpload() throws Exception {
        // 1. Client creates request
        CreateClientAcknowledgementRequest ackReq = CreateClientAcknowledgementRequest.builder()
                .category(DocumentCategory.ACKNOWLEDGEMENT)
                .documentType(DocumentType.ITR_ACKNOWLEDGEMENT)
                .purpose("ITR-V AY 2026-27")
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .build();

        String clientResp = mockMvc.perform(post("/api/v1/portal/document-requests/v1/request-acknowledgement")
                        .header("Authorization", clientToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ackReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String requestId = objectMapper.readTree(clientResp).path("data").path("id").asText();

        // 2. Practitioner fulfills with upload
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ITR-V_Ack_AY2026-27.pdf",
                "application/pdf",
                "%PDF-1.4 sample ITR content".getBytes()
        );

        SendDocumentToClientRequest sendReq = SendDocumentToClientRequest.builder()
                .requestId(UUID.fromString(requestId))
                .category(DocumentCategory.ACKNOWLEDGEMENT)
                .documentType(DocumentType.ITR_ACKNOWLEDGEMENT)
                .title("ITR-V Acknowledgement AY 2026-27 (Signed)")
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .message("Your ITR has been successfully filed and verified.")
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(sendReq)
        );

        mockMvc.perform(multipart("/api/v1/document-requests/send-document")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", practitionerToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.deliveredDocumentId").isNotEmpty())
                .andExpect(jsonPath("$.data.deliveredDocumentName").value("ITR-V_Ack_AY2026-27.pdf"));

        // Verify audit log
        TenantContext.setTenantId(org1.getId());
        boolean hasAckSent = auditLogRepository.findAll().stream()
                .anyMatch(a -> "ACKNOWLEDGEMENT_SENT".equals(a.getAction()));
        assertTrue(hasAckSent, "Audit event ACKNOWLEDGEMENT_SENT should be recorded");
    }

    @Test
    @DisplayName("Practitioner can deliver document proactively using clean vault document")
    void testPractitionerCanDeliverDocumentProactivelyFromExistingCleanVaultDocument() throws Exception {
        TenantContext.setTenantId(org1.getId());

        // Create clean document in client1's vault
        DocumentEntity cleanDoc = DocumentEntity.builder()
                .clientId(client1.getId())
                .fileName("Tax_Audit_Report_FY2025-26.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .storageKey("org1/client1/audit.pdf")
                .documentType(DocumentType.TAX_AUDIT_REPORT)
                .status(DocumentStatus.ACTIVE)
                .scanStatus(DocumentScanStatus.CLEAN)
                .build();
        cleanDoc.setOrganizationId(org1.getId());
        DocumentEntity savedDoc = documentRepository.save(cleanDoc);

        SendDocumentToClientRequest sendReq = SendDocumentToClientRequest.builder()
                .clientId(client1.getId())
                .existingDocumentId(savedDoc.getId())
                .category(DocumentCategory.TAX_DOCUMENT)
                .title("Form 3CD Tax Audit Report")
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .message("Certified copy of your tax audit report.")
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(sendReq)
        );

        mockMvc.perform(multipart("/api/v1/document-requests/send-document")
                        .file(metadata)
                        .header("Authorization", practitionerToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deliveredDocumentId").value(savedDoc.getId().toString()))
                .andExpect(jsonPath("$.data.exchangeType").value("DOCUMENT_DELIVERY"))
                .andExpect(jsonPath("$.data.direction").value("PRACTITIONER_TO_CLIENT"))
                .andExpect(jsonPath("$.data.category").value("TAX_DOCUMENT"));
    }

    @Test
    @DisplayName("Security: Prevent delivering a document belonging to a different client")
    void testPreventCrossClientDocumentDeliveryVulnerability() throws Exception {
        TenantContext.setTenantId(org1.getId());

        // Document belongs to Client 2
        DocumentEntity client2Doc = DocumentEntity.builder()
                .clientId(client2.getId())
                .fileName("Confidential_Client2_Doc.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .storageKey("org1/client2/doc.pdf")
                .documentType(DocumentType.OTHER)
                .status(DocumentStatus.ACTIVE)
                .scanStatus(DocumentScanStatus.CLEAN)
                .build();
        client2Doc.setOrganizationId(org1.getId());
        DocumentEntity savedDoc = documentRepository.save(client2Doc);

        // Practitioner attempts to deliver Client 2's document to Client 1
        SendDocumentToClientRequest maliciousReq = SendDocumentToClientRequest.builder()
                .clientId(client1.getId())
                .existingDocumentId(savedDoc.getId())
                .category(DocumentCategory.ACKNOWLEDGEMENT)
                .title("Unauthorized Cross-Client Document")
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(maliciousReq)
        );

        mockMvc.perform(multipart("/api/v1/document-requests/send-document")
                        .file(metadata)
                        .header("Authorization", practitionerToken1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Selected document does not belong to this client")));
    }

    @Test
    @DisplayName("Security: Prevent delivering an infected or unverified document")
    void testPreventInfectedOrUnscannedDocumentDelivery() throws Exception {
        TenantContext.setTenantId(org1.getId());

        // Document is infected
        DocumentEntity infectedDoc = DocumentEntity.builder()
                .clientId(client1.getId())
                .fileName("Trojan_Ack.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .storageKey("org1/client1/trojan.pdf")
                .documentType(DocumentType.OTHER)
                .status(DocumentStatus.ACTIVE)
                .scanStatus(DocumentScanStatus.INFECTED)
                .build();
        infectedDoc.setOrganizationId(org1.getId());
        DocumentEntity savedDoc = documentRepository.save(infectedDoc);

        SendDocumentToClientRequest sendReq = SendDocumentToClientRequest.builder()
                .clientId(client1.getId())
                .existingDocumentId(savedDoc.getId())
                .category(DocumentCategory.ACKNOWLEDGEMENT)
                .title("Infected Delivery")
                .build();

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(sendReq)
        );

        mockMvc.perform(multipart("/api/v1/document-requests/send-document")
                        .file(metadata)
                        .header("Authorization", practitionerToken1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot deliver a document that has not passed malware scanning")));
    }

    @Test
    @DisplayName("Practitioner can decline client request with explanation")
    void testPractitionerCanDeclineClientRequestWithReason() throws Exception {
        CreateClientAcknowledgementRequest ackReq = CreateClientAcknowledgementRequest.builder()
                .category(DocumentCategory.RETURN_COPY)
                .purpose("GSTR-9 Annual Return Copy FY 2025-26")
                .build();

        String clientResp = mockMvc.perform(post("/api/v1/portal/document-requests/v1/request-acknowledgement")
                        .header("Authorization", clientToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ackReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String requestId = objectMapper.readTree(clientResp).path("data").path("id").asText();

        DeclineDocumentRequest declineReq = DeclineDocumentRequest.builder()
                .declineReason("GSTR-9 filing has not been initiated yet as audited financials are pending.")
                .build();

        mockMvc.perform(post("/api/v1/document-requests/" + requestId + "/decline")
                        .header("Authorization", practitionerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(declineReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DECLINED"))
                .andExpect(jsonPath("$.data.declineReason").value("GSTR-9 filing has not been initiated yet as audited financials are pending."))
                .andExpect(jsonPath("$.data.declinedAt").isNotEmpty());

        // Verify audit log
        TenantContext.setTenantId(org1.getId());
        boolean hasDeclineAudit = auditLogRepository.findAll().stream()
                .anyMatch(a -> "REQUEST_DECLINED".equals(a.getAction()));
        assertTrue(hasDeclineAudit, "Audit event REQUEST_DECLINED should be recorded");
    }

    @Test
    @DisplayName("Client can view delivered documents and status transitions to VIEWED and DOWNLOADED")
    void testClientCanViewAndDownloadDeliveredDocumentAndTrackEvents() throws Exception {
        TenantContext.setTenantId(org1.getId());

        // Create delivered document
        DocumentEntity cleanDoc = DocumentEntity.builder()
                .clientId(client1.getId())
                .fileName("ITR_Ack_AY2026-27.pdf")
                .contentType("application/pdf")
                .fileSize(2048L)
                .storageKey("org1/client1/itr_ack.pdf")
                .documentType(DocumentType.ITR_ACKNOWLEDGEMENT)
                .status(DocumentStatus.ACTIVE)
                .scanStatus(DocumentScanStatus.CLEAN)
                .build();
        cleanDoc.setOrganizationId(org1.getId());
        DocumentEntity savedDoc = documentRepository.save(cleanDoc);

        DocumentRequestEntity deliveredReq = DocumentRequestEntity.builder()
                .clientId(client1.getId())
                .requestNumber("REQ-2026-009999")
                .purpose("ITR-V Acknowledgement AY 2026-27")
                .exchangeType(ExchangeType.DOCUMENT_DELIVERY)
                .direction(RequestDirection.PRACTITIONER_TO_CLIENT)
                .category(DocumentCategory.ACKNOWLEDGEMENT)
                .status(RequestStatus.SENT)
                .deliveredDocumentId(savedDoc.getId())
                .deliveredAt(java.time.Instant.now())
                .build();
        deliveredReq.setOrganizationId(org1.getId());
        DocumentRequestEntity savedReq = docRequestRepository.save(deliveredReq);

        // 1. Client fetches delivered list
        mockMvc.perform(get("/api/v1/portal/document-requests/v1/delivered")
                        .header("Authorization", clientToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].purpose").value("ITR-V Acknowledgement AY 2026-27"))
                .andExpect(jsonPath("$.data[0].deliveredDocumentName").value("ITR_Ack_AY2026-27.pdf"));

        // 2. Client records document VIEWED
        mockMvc.perform(post("/api/v1/portal/document-requests/v1/" + savedReq.getId() + "/viewed")
                        .header("Authorization", clientToken1))
                .andExpect(status().isOk());

        DocumentRequestEntity viewedReq = docRequestRepository.findById(savedReq.getId()).orElseThrow();
        assertEquals(RequestStatus.VIEWED, viewedReq.getStatus());

        // 3. Client records document DOWNLOADED
        mockMvc.perform(post("/api/v1/portal/document-requests/v1/" + savedReq.getId() + "/downloaded")
                        .header("Authorization", clientToken1))
                .andExpect(status().isOk());

        DocumentRequestEntity downloadedReq = docRequestRepository.findById(savedReq.getId()).orElseThrow();
        assertEquals(RequestStatus.DOWNLOADED, downloadedReq.getStatus());

        // Verify audit logs
        boolean hasViewedAudit = auditLogRepository.findAll().stream().anyMatch(a -> "DOCUMENT_VIEWED".equals(a.getAction()));
        boolean hasDownloadedAudit = auditLogRepository.findAll().stream().anyMatch(a -> "DOCUMENT_DOWNLOADED".equals(a.getAction()));
        assertTrue(hasViewedAudit, "Audit event DOCUMENT_VIEWED should be logged");
        assertTrue(hasDownloadedAudit, "Audit event DOCUMENT_DOWNLOADED should be logged");
    }
}
