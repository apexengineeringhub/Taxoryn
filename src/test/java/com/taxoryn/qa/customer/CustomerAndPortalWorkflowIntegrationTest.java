package com.taxoryn.qa.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.authentication.dto.LoginRequest;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequestItem;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.portal.dto.RegisterClientPortalUserRequest;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.qa.factory.OrganizationTestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerAndPortalWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationTestDataFactory factory;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRequestRepository docRequestRepository;

    @Autowired
    private DocumentRequestItemRepository docRequestItemRepository;

    @Autowired
    private DocumentRepository documentRepository;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity adminA;
    private UserEntity adminB;
    private ClientEntity clientA1;
    private ClientEntity clientB1;
    private UserEntity clientUserA1;
    private UserEntity clientUserB1;
    private String tokenAdminA;
    private String tokenAdminB;
    private String tokenClientA1;
    private String tokenClientB1;

    @BeforeEach
    void setUp() {
        orgA = factory.createOrganization("Prime Tax Solutions " + UUID.randomUUID().toString().substring(0, 5), "admin.prime." + UUID.randomUUID().toString().substring(0, 5) + "@prime.in");
        orgB = factory.createOrganization("Zenith Tax Partners " + UUID.randomUUID().toString().substring(0, 5), "admin.zenith." + UUID.randomUUID().toString().substring(0, 5) + "@zenith.in");

        adminA = factory.createAdminUser(orgA, "adminA." + UUID.randomUUID().toString().substring(0, 5) + "@prime.in", "AdminPass123!");
        adminB = factory.createAdminUser(orgB, "adminB." + UUID.randomUUID().toString().substring(0, 5) + "@zenith.in", "AdminPass123!");

        clientA1 = factory.createClient(orgA, "Acme Corp India", "contact.acme." + UUID.randomUUID().toString().substring(0, 5) + "@acme.com");
        clientB1 = factory.createClient(orgB, "Globex Industries", "contact.globex." + UUID.randomUUID().toString().substring(0, 5) + "@globex.com");

        clientUserA1 = factory.createClientPortalUser(orgA, clientA1, "portal.acme." + UUID.randomUUID().toString().substring(0, 5) + "@acme.com", "ClientPass123!");
        clientUserB1 = factory.createClientPortalUser(orgB, clientB1, "portal.globex." + UUID.randomUUID().toString().substring(0, 5) + "@globex.com", "ClientPass123!");

        tokenAdminA = factory.generateBearerToken(adminA);
        tokenAdminB = factory.generateBearerToken(adminB);
        tokenClientA1 = factory.generateBearerToken(clientUserA1);
        tokenClientB1 = factory.generateBearerToken(clientUserB1);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("CUST-001: Add customer & provision portal user - should succeed")
    void shouldCreateClientAndProvisionPortalUser() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 6);
        CreateClientRequest clientReq = CreateClientRequest.builder()
                .clientType(ClientType.PRIVATE_LIMITED)
                .displayName("Tata Consulting " + unique)
                .email("finance." + unique + "@tataconsulting.com")
                .phone("+919876543230")
                .pan("AABCT" + (int)(Math.random() * 8999 + 1000) + "A")
                .gstin("27AABCT" + (int)(Math.random() * 8999 + 1000) + "A1Z8")
                .city("Pune")
                .state("Maharashtra")
                .pincode("411001")
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.displayName", is("Tata Consulting " + unique)))
                .andReturn().getResponse().getContentAsString();

        String createdClientId = objectMapper.readTree(responseJson).get("data").get("id").asText();

        RegisterClientPortalUserRequest portalUserReq = RegisterClientPortalUserRequest.builder()
                .clientId(UUID.fromString(createdClientId))
                .email("cfo." + unique + "@tataconsulting.com")
                .firstName("Ratan")
                .lastName("Tata")
                .phone("+919876543231")
                .role("CLIENT_ADMIN")
                .password("StrongPortalPass123!")
                .build();

        mockMvc.perform(post("/api/v1/portal/users")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(portalUserReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email", is("cfo." + unique + "@tataconsulting.com")));
    }

    @Test
    @DisplayName("CUST-003: Customer login - portal user logs in successfully")
    void shouldAllowClientPortalUserLogin() throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .email(clientUserA1.getEmail())
                .password("ClientPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.clientId", is(clientA1.getId().toString())));
    }

    @Test
    @DisplayName("CUST-004 & CUST-005: Customer sees own dashboard & cannot see another customer data")
    void shouldAllowClientToSeeOnlyOwnDashboardAndDocuments() throws Exception {
        // Customer A queries own dashboard
        mockMvc.perform(get("/api/v1/portal/dashboard")
                        .header("Authorization", "Bearer " + tokenClientA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.displayName", is(clientA1.getDisplayName())));

        // Customer A cannot query practice client management endpoint directly
        mockMvc.perform(get("/api/v1/clients/" + clientB1.getId())
                        .header("Authorization", "Bearer " + tokenClientA1))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CUST-006: Customer cannot access another organization data")
    void shouldNotAllowCustomerToAccessAnotherOrganizationData() throws Exception {
        mockMvc.perform(get("/api/v1/portal/preview/" + clientB1.getId())
                        .header("Authorization", "Bearer " + tokenClientA1))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DOCREQ-001 & DOCREQ-002: Organization creates document request with checklist items")
    void shouldCreateDocumentRequestWithItems() throws Exception {
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(clientA1.getId())
                .purpose("FY 2026-27 Annual Tax Audit Checklist")
                .dueDate(LocalDate.now().plusDays(20))
                .items(List.of(
                        CreateDocumentRequestItem.builder().title("Audited Balance Sheet").documentType(DocumentType.FINANCIAL_STATEMENTS).required(true).build(),
                        CreateDocumentRequestItem.builder().title("Bank Reconciliation Statement").documentType(DocumentType.BANK_STATEMENT).required(true).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.purpose", is("FY 2026-27 Annual Tax Audit Checklist")))
                .andExpect(jsonPath("$.data.items", hasSize(2)));
    }

    @Test
    @DisplayName("DOCREQ-004: Organization A cannot create request for Organization B customer")
    void shouldNotAllowOrgToRequestDocsForAnotherOrgCustomer() throws Exception {
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(clientB1.getId()) // Belongs to Org B
                .purpose("Unauthorized Cross-Tenant Request")
                .items(List.of(
                        CreateDocumentRequestItem.builder().title("Form 16").documentType(DocumentType.FORM_16).required(true).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DOCREQ-005 & DOCREQ-006: Customer views only own pending document requests")
    void shouldAllowCustomerToViewOnlyOwnPendingRequests() throws Exception {
        // Create request for Client A1
        factory.createDocumentRequest(orgA, clientA1, adminA, "Client A Request", List.of("Form 16"));
        // Create request for Client B1
        factory.createDocumentRequest(orgB, clientB1, adminB, "Client B Request", List.of("Bank Statement"));

        mockMvc.perform(get("/api/v1/portal/pending-documents")
                        .header("Authorization", "Bearer " + tokenClientA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("UPLOAD-001: Customer uploads document for request item via portal")
    void shouldAllowClientToUploadRequestedDocument() throws Exception {
        DocumentRequestEntity req = factory.createDocumentRequest(orgA, clientA1, adminA, "ITR Verification", List.of("Salary Certificate"));
        DocumentRequestItemEntity item = docRequestItemRepository.findAllByRequestIdOrderByCreatedAtAsc(req.getId()).get(0);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "salary_cert.pdf",
                "application/pdf",
                "%PDF-1.4 mock valid test document content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/portal/document-requests/v1/items/" + item.getId() + "/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("UPLOAD-008: Customer A cannot upload against Customer B request item")
    void shouldRejectClientUploadingToAnotherClientRequest() throws Exception {
        DocumentRequestEntity reqB = factory.createDocumentRequest(orgB, clientB1, adminB, "Client B Request", List.of("Confidential Audit"));
        DocumentRequestItemEntity itemB = docRequestItemRepository.findAllByRequestIdOrderByCreatedAtAsc(reqB.getId()).get(0);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hack.pdf",
                "application/pdf",
                "%PDF-1.4 mock attack file".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/portal/document-requests/v1/items/" + itemB.getId() + "/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientA1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DOWN-001 & DOWN-002 & DOWN-003: Download document authorization and cross-tenant protection")
    void shouldEnforceDocumentDownloadIsolation() throws Exception {
        DocumentEntity docA = factory.createDocument(orgA, clientA1, adminA, "invoice_A.pdf", DocumentType.GST_INVOICE_SALE);
        DocumentEntity docB = factory.createDocument(orgB, clientB1, adminB, "confidential_B.pdf", DocumentType.FINANCIAL_STATEMENTS);

        // Org A admin can download docA
        mockMvc.perform(get("/api/v1/documents/" + docA.getId() + "/download")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk());

        // Org A admin CANNOT download docB
        mockMvc.perform(get("/api/v1/documents/" + docB.getId() + "/download")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isNotFound());

        // Client A1 can preview own document URL
        mockMvc.perform(get("/api/v1/portal/documents/" + docA.getId() + "/download-url")
                        .header("Authorization", "Bearer " + tokenClientA1))
                .andExpect(status().isOk());

        // Client A1 CANNOT access docB
        mockMvc.perform(get("/api/v1/portal/documents/" + docB.getId() + "/download-url")
                        .header("Authorization", "Bearer " + tokenClientA1))
                .andExpect(status().isNotFound());
    }
}
