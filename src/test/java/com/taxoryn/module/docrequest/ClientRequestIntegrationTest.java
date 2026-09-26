package com.taxoryn.module.docrequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequestItem;
import com.taxoryn.module.docrequest.dto.RejectDocumentItemRequest;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
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
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClientRequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRequestRepository docRequestRepository;

    @Autowired
    private DocumentRequestItemRepository docRequestItemRepository;

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

    private OrganizationEntity organization;
    private UserEntity practitioner;
    private ClientEntity client;
    private String token;

    @BeforeEach
    void setUp() {
        RoleEntity adminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("ORG_ADMIN")
                        .name("Organization Administrator")
                        .isSystemRole(true)
                        .permissions(new HashSet<>())
                        .build()));

        organization = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA Firm - " + UUID.randomUUID())
                .legalName("Apex CA Firm LLP")
                .email("apex.ca." + UUID.randomUUID() + "@firm.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        practitioner = userRepository.save(UserEntity.builder()
                .organizationId(organization.getId())
                .email("ca.practitioner." + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Rajesh")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build());

        token = jwtTokenProvider.generateAccessToken(
                practitioner.getId(),
                organization.getId(),
                practitioner.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("ROLE_ORG_ADMIN", "CLIENT_VIEW", "CLIENT_UPDATE", "CLIENT_CREATE", "DOCUMENT_VIEW", "DOCUMENT_READ", "DOCUMENT_WRITE", "DOCUMENT_UPLOAD")
        );

        ClientEntity c = ClientEntity.builder()
                .displayName("Shree Ganesh Traders")
                .legalName("Shree Ganesh Traders LLP")
                .clientType(ClientType.LLP)
                .pan("AABCS1234D")
                .email("shreeganesh@trade.com")
                .status(ClientStatus.ACTIVE)
                .build();
        c.setOrganizationId(organization.getId());
        client = clientRepository.save(c);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create & send multi-item document request to client")
    void testCreateAndSendDocumentRequest() throws Exception {
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(client.getId())
                .purpose("ITR FY 2025-26 Compliance Audit")
                .dueDate(LocalDate.now().plusDays(15))
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .message("Please provide audited financial statements and bank statements.")
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .title("Bank Statement (Apr-Mar)")
                                .documentType(DocumentType.BANK_STATEMENT)
                                .required(true)
                                .build(),
                        CreateDocumentRequestItem.builder()
                                .title("Form 26AS Tax Credit Statement")
                                .documentType(DocumentType.FORM_26AS)
                                .required(true)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.pendingItems").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    @DisplayName("Upload document against request item, accept item, and verify completion")
    void testUploadAndAcceptDocumentItemCompletesRequest() throws Exception {
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(client.getId())
                .purpose("Bank statement verification")
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .title("HDFC Bank Statement")
                                .documentType(DocumentType.BANK_STATEMENT)
                                .required(true)
                                .build()
                ))
                .build();

        String createResponse = mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID itemId = UUID.fromString(objectMapper.readTree(createResponse).path("data").path("items").get(0).path("id").asText());

        // Upload file for the item
        byte[] fileContent = "%PDF-1.4 HDFC bank statement data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "HDFC_Statement.pdf", "application/pdf", fileContent);

        mockMvc.perform(multipart("/api/v1/document-requests/items/" + itemId + "/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadedItems").value(1))
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_COMPLETED"));

        // Accept the item
        mockMvc.perform(post("/api/v1/document-requests/items/" + itemId + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acceptedItems").value(1))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Reject document item with mandatory reason and verify state update")
    void testRejectDocumentItem() throws Exception {
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(client.getId())
                .purpose("PAN verification")
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .title("PAN Card Copy")
                                .documentType(DocumentType.PAN_CARD)
                                .required(true)
                                .build()
                ))
                .build();

        String createResponse = mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID itemId = UUID.fromString(objectMapper.readTree(createResponse).path("data").path("items").get(0).path("id").asText());

        // Upload file for the item
        byte[] fileContent = "%PDF-1.4 PAN Card blurred image".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "PAN_blur.pdf", "application/pdf", fileContent);

        mockMvc.perform(multipart("/api/v1/document-requests/items/" + itemId + "/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Reject item
        RejectDocumentItemRequest rejectReq = RejectDocumentItemRequest.builder()
                .rejectionReason("Document is blurry. Please upload a clear scanned copy showing full PAN number.")
                .build();

        mockMvc.perform(post("/api/v1/document-requests/items/" + itemId + "/reject")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rejectedItems").value(1))
                .andExpect(jsonPath("$.data.items[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.data.items[0].rejectionReason").value("Document is blurry. Please upload a clear scanned copy showing full PAN number."));
    }

    @Test
    @DisplayName("Cancel document request updates status to CANCELLED")
    void testCancelDocumentRequest() throws Exception {
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(client.getId())
                .purpose("Obsolete request")
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .title("Obsolete document")
                                .documentType(DocumentType.OTHER)
                                .build()
                ))
                .build();

        String createResponse = mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID reqId = UUID.fromString(objectMapper.readTree(createResponse).path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/document-requests/" + reqId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Retrieve client document requests by client ID")
    void testGetClientRequests() throws Exception {
        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .clientId(client.getId())
                .purpose("FY 2025-26 GST Invoices")
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .title("Purchase Invoices")
                                .documentType(DocumentType.GST_INVOICE_PURCHASE)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/document-requests/clients/" + client.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].purpose").value("FY 2025-26 GST Invoices"));
    }
}
