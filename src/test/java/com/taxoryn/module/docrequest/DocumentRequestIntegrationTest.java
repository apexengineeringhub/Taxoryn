package com.taxoryn.module.docrequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequestItem;
import com.taxoryn.module.docrequest.dto.RejectDocumentItemRequest;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentRequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRequestRepository docRequestRepository;

    @Autowired
    private DocumentRequestItemRepository docRequestItemRepository;

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
        docRequestItemRepository.deleteAll();
        docRequestRepository.deleteAll();
        documentRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

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
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "DOCUMENT_VIEW", "DOCUMENT_WRITE")
        );

        // 4. Client 1 in Org 1
        client1 = clientRepository.save(ClientEntity.builder()
                .displayName("Shree Ganesh Logistics")
                .pan("AABCS1234D")
                .gstin("27AABCS1234D1Z8")
                .clientType(ClientType.PRIVATE_LIMITED)
                .email("accounts@shreeganesh.com")
                .phone("9811122233")
                .build());

        clientUser1 = userRepository.save(UserEntity.builder()
                .email("portal@shreeganesh.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Suresh")
                .lastName("Patil")
                .clientId(client1.getId())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientRole)))
                .build());

        clientToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientUser1.getId(),
                org1.getId(),
                client1.getId(),
                clientUser1.getEmail(),
                Set.of("CLIENT_ADMIN"),
                Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_DOCUMENT_UPLOAD")
        );

        // 5. Client 2 in Org 2
        TenantContext.setTenantId(org2.getId());

        client2 = clientRepository.save(ClientEntity.builder()
                .displayName("Global Traders LLP")
                .pan("AAACG9999D")
                .clientType(ClientType.LLP)
                .email("finance@globaltraders.com")
                .build());

        clientUser2 = userRepository.save(UserEntity.builder()
                .email("portal@globaltraders.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Vikas")
                .lastName("Shah")
                .clientId(client2.getId())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientRole)))
                .build());

        clientToken2 = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientUser2.getId(),
                org2.getId(),
                client2.getId(),
                clientUser2.getEmail(),
                Set.of("CLIENT_ADMIN"),
                Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_DOCUMENT_UPLOAD")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Document Request: Practitioner creates multi-item request for client")
    void testCreateDocumentRequest_Success() throws Exception {
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(client1.getId())
                .purpose("ITR FY 2026-27 Filing Preparation")
                .dueDate(LocalDate.now().plusDays(15))
                .message("Please upload your Form 16, AIS, and Bank Statements.")
                .financialYear("2026-27")
                .assessmentYear("2027-28")
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.FORM_16)
                                .title("Form 16 Part A & B")
                                .description("Issued by employer with TDS deducted")
                                .required(true)
                                .build(),
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.AIS_TIS)
                                .title("Annual Information Statement (AIS)")
                                .required(true)
                                .build(),
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.BANK_STATEMENT)
                                .title("Savings Bank Account Statement")
                                .description("All pages from 01-Apr-2026 to 31-Mar-2027")
                                .required(false)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", practitionerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.requestNumber", startsWith("REQ-")))
                .andExpect(jsonPath("$.data.purpose", is("ITR FY 2026-27 Filing Preparation")))
                .andExpect(jsonPath("$.data.status", is("SENT")))
                .andExpect(jsonPath("$.data.totalItems", is(3)))
                .andExpect(jsonPath("$.data.pendingItems", is(3)))
                .andExpect(jsonPath("$.data.items", hasSize(3)))
                .andExpect(jsonPath("$.data.items[0].title", is("Form 16 Part A & B")))
                .andExpect(jsonPath("$.data.items[0].status", is("PENDING")));
    }

    @Test
    @DisplayName("Document Request: Client portal view & document upload on requested item")
    void testClientPortal_UploadDocumentItem() throws Exception {
        // 1. Practitioner creates request
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(client1.getId())
                .purpose("GST Audit FY 2026-27")
                .dueDate(LocalDate.now().plusDays(10))
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.GST_INVOICE_PURCHASE)
                                .title("Purchase Invoices Register")
                                .required(true)
                                .build()
                ))
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", practitionerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID itemId = UUID.fromString(objectMapper.readTree(responseStr).path("data").path("items").get(0).path("id").asText());

        // 2. Client views requests in client portal
        mockMvc.perform(get("/api/v1/portal/document-requests/v1")
                        .header("Authorization", clientToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].purpose", is("GST Audit FY 2026-27")));

        // 3. Client uploads file for item
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Purchase_Register_Aug_2026.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "mock-excel-binary-data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/portal/document-requests/v1/items/" + itemId + "/upload")
                        .file(file)
                        .header("Authorization", clientToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PARTIALLY_COMPLETED")))
                .andExpect(jsonPath("$.data.uploadedItems", is(1)))
                .andExpect(jsonPath("$.data.items[0].status", is("UPLOADED")))
                .andExpect(jsonPath("$.data.items[0].uploadedDocumentName", is("Purchase_Register_Aug_2026.xlsx")));
    }

    @Test
    @DisplayName("Document Request: Practitioner review - Accept item completes request")
    void testPractitionerAcceptAndCompleteRequest() throws Exception {
        // 1. Create request with 1 required item
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(client1.getId())
                .purpose("ITR Verification")
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.FORM_16)
                                .title("Form 16")
                                .required(true)
                                .build()
                ))
                .build();

        String createStr = mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", practitionerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        UUID itemId = UUID.fromString(objectMapper.readTree(createStr).path("data").path("items").get(0).path("id").asText());

        // 2. Upload file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Form16_Signed.pdf",
                "application/pdf",
                "%PDF-1.4 mock".getBytes()
        );
        mockMvc.perform(multipart("/api/v1/portal/document-requests/v1/items/" + itemId + "/upload")
                        .file(file)
                        .header("Authorization", clientToken1))
                .andExpect(status().isOk());

        // 3. Practitioner accepts item -> request transitions to COMPLETED
        mockMvc.perform(post("/api/v1/document-requests/items/" + itemId + "/accept")
                        .header("Authorization", practitionerToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                .andExpect(jsonPath("$.data.acceptedItems", is(1)))
                .andExpect(jsonPath("$.data.items[0].status", is("ACCEPTED")));
    }

    @Test
    @DisplayName("Document Request: Practitioner reject item requires reason & allows client re-upload")
    void testPractitionerRejectItem_RequiresReason() throws Exception {
        // 1. Create request & upload
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(client1.getId())
                .purpose("Bank Verification")
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.BANK_STATEMENT)
                                .title("Annual Bank Statement")
                                .required(true)
                                .build()
                ))
                .build();

        String createStr = mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", practitionerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        UUID itemId = UUID.fromString(objectMapper.readTree(createStr).path("data").path("items").get(0).path("id").asText());

        MockMultipartFile file1 = new MockMultipartFile("file", "statement_incomplete.pdf", "application/pdf", "data".getBytes());
        mockMvc.perform(multipart("/api/v1/portal/document-requests/v1/items/" + itemId + "/upload")
                        .file(file1)
                        .header("Authorization", clientToken1))
                .andExpect(status().isOk());

        // 2. Reject without reason -> 400 Bad Request
        mockMvc.perform(post("/api/v1/document-requests/items/" + itemId + "/reject")
                        .header("Authorization", practitionerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectDocumentItemRequest(""))))
                .andExpect(status().isBadRequest());

        // 3. Reject with reason -> 200 OK
        RejectDocumentItemRequest rejectReq = new RejectDocumentItemRequest("Bank statement is missing months Oct-Mar. Please upload full 12 months statement.");
        mockMvc.perform(post("/api/v1/document-requests/items/" + itemId + "/reject")
                        .header("Authorization", practitionerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rejectedItems", is(1)))
                .andExpect(jsonPath("$.data.items[0].status", is("REJECTED")))
                .andExpect(jsonPath("$.data.items[0].rejectionReason", containsString("missing months Oct-Mar")));

        // 4. Client re-uploads replacement document
        MockMultipartFile file2 = new MockMultipartFile("file", "statement_full_12months.pdf", "application/pdf", "data".getBytes());
        mockMvc.perform(multipart("/api/v1/portal/document-requests/v1/items/" + itemId + "/upload")
                        .file(file2)
                        .header("Authorization", clientToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status", is("UPLOADED")))
                .andExpect(jsonPath("$.data.items[0].uploadedDocumentName", is("statement_full_12months.pdf")));
    }

    @Test
    @DisplayName("Document Request: Manual reminder dispatch & summary metrics")
    void testSendReminderAndSummaryMetrics() throws Exception {
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(client1.getId())
                .purpose("TDS Compliance Q2")
                .dueDate(LocalDate.now().minusDays(2)) // Overdue date
                .items(List.of(
                        CreateDocumentRequestItem.builder().title("Salary Register").build()
                ))
                .build();

        String createStr = mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", practitionerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        UUID reqId = UUID.fromString(objectMapper.readTree(createStr).path("data").path("id").asText());

        // Send reminder
        mockMvc.perform(post("/api/v1/document-requests/" + reqId + "/remind")
                        .header("Authorization", practitionerToken1))
                .andExpect(status().isOk());

        // Get summary stats
        mockMvc.perform(get("/api/v1/document-requests/summary/stats")
                        .header("Authorization", practitionerToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRequests", is(1)))
                .andExpect(jsonPath("$.data.overdueRequests", is(1)));
    }

    @Test
    @DisplayName("Security & Isolation: Foreign client cannot view or upload to another client's request")
    void testClientIsolation_ForeignClientBlocked() throws Exception {
        // Request created for Client 1 in Org 1
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(client1.getId())
                .purpose("Confidential Audit")
                .items(List.of(CreateDocumentRequestItem.builder().title("Ledger").build()))
                .build();

        String createStr = mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", practitionerToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        UUID reqId = UUID.fromString(objectMapper.readTree(createStr).path("data").path("id").asText());
        UUID itemId = UUID.fromString(objectMapper.readTree(createStr).path("data").path("items").get(0).path("id").asText());

        // Client 2 (different client & org) attempts to view Client 1's request -> 404
        mockMvc.perform(get("/api/v1/portal/document-requests/v1/" + reqId)
                        .header("Authorization", clientToken2))
                .andExpect(status().isNotFound());

        // Client 2 attempts to upload to Client 1's item -> 404
        MockMultipartFile file = new MockMultipartFile("file", "leak.pdf", "application/pdf", "data".getBytes());
        mockMvc.perform(multipart("/api/v1/portal/document-requests/v1/items/" + itemId + "/upload")
                        .file(file)
                        .header("Authorization", clientToken2))
                .andExpect(status().isNotFound());
    }
}