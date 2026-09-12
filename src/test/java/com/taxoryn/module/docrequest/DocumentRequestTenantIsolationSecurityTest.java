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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentRequestTenantIsolationSecurityTest {

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

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;

    private UserEntity practitionerA;
    private String tokenA;

    private UserEntity practitionerB;
    private String tokenB;

    private ClientEntity clientA1;
    private ClientEntity clientA2;

    private ClientEntity clientB1;
    private ClientEntity clientB2;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        docRequestItemRepository.deleteAll();
        docRequestRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. Create Organization A & Organization B
        orgA = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex Chartered Accountants (Org A)")
                .email("contact@apextax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Beacon Tax Partners (Org B)")
                .email("contact@beacontax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        RoleEntity orgAdminRole = roleRepository.save(RoleEntity.builder()
                .code("ORG_ADMIN")
                .name("Organization Admin")
                .isSystemRole(true)
                .permissions(new HashSet<>())
                .build());

        // 2. Create Practitioners in Org A and Org B
        TenantContext.setTenantId(orgA.getId());
        practitionerA = userRepository.save(UserEntity.builder()
                .email("practitioner.a@apextax.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Arun")
                .lastName("Kumar")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        tokenA = "Bearer " + jwtTokenProvider.generateAccessToken(
                practitionerA.getId(),
                orgA.getId(),
                practitionerA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "DOCUMENT_VIEW", "DOCUMENT_WRITE")
        );

        // Create Clients in Org A (A1, A2)
        clientA1 = clientRepository.save(ClientEntity.builder()
                .displayName("Alpha Traders Pvt Ltd (Client A1)")
                .legalName("Alpha Traders Private Limited")
                .pan("AAAAP1111A")
                .clientType(ClientType.PRIVATE_LIMITED)
                .email("accounts@alphatraders.com")
                .status(ClientStatus.ACTIVE)
                .build());

        clientA2 = clientRepository.save(ClientEntity.builder()
                .displayName("Acrobat Retailers (Client A2)")
                .legalName("Acrobat Retailers LLP")
                .pan("BBBBQ2222B")
                .clientType(ClientType.LLP)
                .email("info@acrobat.com")
                .status(ClientStatus.ACTIVE)
                .build());

        TenantContext.clear();

        // Org B setup
        TenantContext.setTenantId(orgB.getId());
        practitionerB = userRepository.save(UserEntity.builder()
                .email("practitioner.b@beacontax.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .firstName("Bhavna")
                .lastName("Shah")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .build());

        tokenB = "Bearer " + jwtTokenProvider.generateAccessToken(
                practitionerB.getId(),
                orgB.getId(),
                practitionerB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_CREATE", "CLIENT_UPDATE", "DOCUMENT_VIEW", "DOCUMENT_WRITE")
        );

        // Create Clients in Org B (B1, B2)
        clientB1 = clientRepository.save(ClientEntity.builder()
                .displayName("Bravo Technologies (Client B1)")
                .legalName("Bravo Technologies Private Limited")
                .pan("CCCCR3333C")
                .clientType(ClientType.PRIVATE_LIMITED)
                .email("contact@bravotech.com")
                .status(ClientStatus.ACTIVE)
                .build());

        clientB2 = clientRepository.save(ClientEntity.builder()
                .displayName("Bluefin Logistics (Client B2)")
                .legalName("Bluefin Logistics LLP")
                .pan("DDDDD4444D")
                .clientType(ClientType.LLP)
                .email("operations@bluefin.com")
                .status(ClientStatus.ACTIVE)
                .build());

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Security Isolation: Organization A client list returns ONLY Org A clients (A1, A2) and zero Org B clients")
    void testOrganizationA_ClientQueryIsStrictlyScoped() throws Exception {
        mockMvc.perform(get("/api/v1/clients?size=100")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[*].id", containsInAnyOrder(clientA1.getId().toString(), clientA2.getId().toString())))
                .andExpect(jsonPath("$.data.content[*].id", not(hasItem(clientB1.getId().toString()))))
                .andExpect(jsonPath("$.data.content[*].id", not(hasItem(clientB2.getId().toString()))))
                .andExpect(jsonPath("$.data.content[*].displayName", not(hasItem(containsString("Client B")))));
    }

    @Test
    @DisplayName("Security Isolation: Organization B client list returns ONLY Org B clients (B1, B2) and zero Org A clients")
    void testOrganizationB_ClientQueryIsStrictlyScoped() throws Exception {
        mockMvc.perform(get("/api/v1/clients?size=100")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[*].id", containsInAnyOrder(clientB1.getId().toString(), clientB2.getId().toString())))
                .andExpect(jsonPath("$.data.content[*].id", not(hasItem(clientA1.getId().toString()))))
                .andExpect(jsonPath("$.data.content[*].id", not(hasItem(clientA2.getId().toString()))))
                .andExpect(jsonPath("$.data.content[*].displayName", not(hasItem(containsString("Client A")))));
    }

    @Test
    @DisplayName("IDOR Defense: Organization A user cannot create Document Request for Organization B client")
    void testCreateDocumentRequest_RejectsForeignClient() throws Exception {
        CreateDocumentRequest maliciousRequest = CreateDocumentRequest.builder()
                .clientId(clientB1.getId()) // Client belonging to Org B
                .purpose("Attacker Cross-Tenant Tax Audit")
                .dueDate(LocalDate.now().plusDays(7))
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.BANK_STATEMENT)
                                .title("Confidential Financial Records")
                                .required(true)
                                .build()
                ))
                .build();

        // Practitioner A (Org A) attempts to create document request for Client B1 (Org B) -> 404 (Resource Not Found)
        mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

        // Verify zero document requests were created in database for Client B1 by Org A
        List<com.taxoryn.module.docrequest.entity.DocumentRequestEntity> allRequests = docRequestRepository.findAll();
        org.junit.jupiter.api.Assertions.assertTrue(allRequests.isEmpty());
    }

    @Test
    @DisplayName("Security Isolation: Organization A user cannot query document requests for Organization B client")
    void testGetClientRequests_RejectsForeignClient() throws Exception {
        // First create a legitimate request in Org B
        CreateDocumentRequest validOrgBRequest = CreateDocumentRequest.builder()
                .clientId(clientB1.getId())
                .purpose("Org B GST Compliance")
                .dueDate(LocalDate.now().plusDays(10))
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.GST_INVOICE_PURCHASE)
                                .title("Purchase Invoices")
                                .required(true)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrgBRequest)))
                .andExpect(status().isCreated());

        // Practitioner A (Org A) attempts to list document requests for Client B1 -> 404
        mockMvc.perform(get("/api/v1/document-requests/clients/" + clientB1.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Security Isolation: Organization A user cannot access or review Org B document request items")
    void testReviewItem_RejectsCrossTenantAccess() throws Exception {
        // Create request in Org B
        CreateDocumentRequest validOrgBRequest = CreateDocumentRequest.builder()
                .clientId(clientB1.getId())
                .purpose("Org B Tax Audit")
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.FORM_26AS)
                                .title("Form 26AS")
                                .required(true)
                                .build()
                ))
                .build();

        String orgBResponseStr = mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrgBRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID reqId = UUID.fromString(objectMapper.readTree(orgBResponseStr).path("data").path("id").asText());
        UUID itemId = UUID.fromString(objectMapper.readTree(orgBResponseStr).path("data").path("items").get(0).path("id").asText());

        // Practitioner A (Org A) attempts to get Org B request by ID -> 404
        mockMvc.perform(get("/api/v1/document-requests/" + reqId)
                        .header("Authorization", tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

        // Practitioner A (Org A) attempts to accept Org B item -> 404
        mockMvc.perform(post("/api/v1/document-requests/items/" + itemId + "/accept")
                        .header("Authorization", tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

        // Practitioner A (Org A) attempts to reject Org B item -> 404
        RejectDocumentItemRequest rejectReq = new RejectDocumentItemRequest("Invalid format");
        mockMvc.perform(post("/api/v1/document-requests/items/" + itemId + "/reject")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("End-to-End: Same organization client selection and document request creation succeeds")
    void testSameOrganizationDocumentRequest_Succeeds() throws Exception {
        CreateDocumentRequest validOrgARequest = CreateDocumentRequest.builder()
                .clientId(clientA1.getId())
                .purpose("ITR FY 2026-27 Filing")
                .dueDate(LocalDate.now().plusDays(14))
                .financialYear("2026-27")
                .assessmentYear("2027-28")
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.FORM_16)
                                .title("Form 16 Part A & B")
                                .required(true)
                                .build(),
                        CreateDocumentRequestItem.builder()
                                .documentType(DocumentType.BANK_STATEMENT)
                                .title("Savings Account Statement")
                                .required(false)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrgARequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.clientId").value(clientA1.getId().toString()))
                .andExpect(jsonPath("$.data.purpose").value("ITR FY 2026-27 Filing"))
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.totalItems").value(2));

        // Practitioner A queries list of document requests -> Contains 1 request
        mockMvc.perform(get("/api/v1/document-requests")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].clientId").value(clientA1.getId().toString()));

        // Practitioner B queries list of document requests -> Contains 0 requests
        mockMvc.perform(get("/api/v1/document-requests")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }
}
