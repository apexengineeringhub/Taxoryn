package com.taxoryn.module.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientType;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.portal.dto.CreateClientApprovalRequest;
import com.taxoryn.module.portal.dto.DecideClientApprovalRequest;
import com.taxoryn.module.portal.dto.RevokeClientApprovalRequest;
import com.taxoryn.module.portal.entity.ClientApprovalDecision;
import com.taxoryn.module.portal.entity.ClientApprovalRequestEntity;
import com.taxoryn.module.portal.entity.ClientApprovalStatus;
import com.taxoryn.module.portal.entity.ClientApprovalType;
import com.taxoryn.module.portal.entity.MessageContextType;
import com.taxoryn.module.portal.repository.ClientApprovalRequestRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class ClientApprovalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ClientApprovalRequestRepository clientApprovalRequestRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID tenantId;
    private OrganizationEntity organization;
    private UserEntity practitionerUser;
    private String practitionerToken;

    private ClientEntity clientA;
    private UserEntity clientAUser;
    private String clientAToken;

    private UUID foreignTenantId;
    private OrganizationEntity foreignOrg;
    private ClientEntity clientForeign;
    private UserEntity clientForeignUser;
    private String clientForeignToken;

    @BeforeEach
    void setUp() {
        cleanData();

        organization = organizationRepository.save(OrganizationEntity.builder()
                .name("Alpha Advisory LLP")
                .legalName("Alpha Advisory LLP")
                .email("admin@alphaadvisory.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        tenantId = organization.getId();
        TenantContext.setTenantId(tenantId);

        RoleEntity orgAdminRole = roleRepository.findByCodeAndOrganizationId("ORG_ADMIN", tenantId)
                .orElseGet(() -> {
                    RoleEntity r = RoleEntity.builder()
                            .code("ORG_ADMIN")
                            .name("Org Admin")
                            .description("Org Admin")
                            .permissions(new HashSet<>())
                            .build();
                    r.setOrganizationId(tenantId);
                    return roleRepository.save(r);
                });

        RoleEntity clientUserRole = roleRepository.findByCodeAndOrganizationId("CLIENT_USER", tenantId)
                .orElseGet(() -> {
                    RoleEntity r = RoleEntity.builder()
                            .code("CLIENT_USER")
                            .name("Client User")
                            .description("Client User")
                            .permissions(new HashSet<>())
                            .build();
                    r.setOrganizationId(tenantId);
                    return roleRepository.save(r);
                });

        practitionerUser = UserEntity.builder()
                .email("ca.kapoor@alphaadvisory.com")
                .firstName("Kapoor")
                .lastName("CA")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(orgAdminRole))
                .build();
        practitionerUser.setOrganizationId(tenantId);
        practitionerUser = userRepository.save(practitionerUser);

        practitionerToken = jwtTokenProvider.generateAccessToken(
                practitionerUser.getId(),
                tenantId,
                practitionerUser.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("CLIENT_VIEW", "CLIENT_UPDATE", "DOCUMENT_READ", "DOCUMENT_VIEW")
        );

        clientA = ClientEntity.builder()
                .displayName("Omega Infotech Private Limited")
                .legalName("Omega Infotech Private Limited")
                .clientType(ClientType.PRIVATE_LIMITED)
                .pan("AABCO1234F")
                .email("finance@omegainfotech.com")
                .build();
        clientA.setOrganizationId(tenantId);
        clientA = clientRepository.save(clientA);

        clientAUser = UserEntity.builder()
                .email("client.omega@omegainfotech.com")
                .firstName("Rohan")
                .lastName("Verma")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .clientId(clientA.getId())
                .roles(Set.of(clientUserRole))
                .build();
        clientAUser.setOrganizationId(tenantId);
        clientAUser = userRepository.save(clientAUser);

        clientAToken = jwtTokenProvider.generateAccessToken(
                clientAUser.getId(),
                tenantId,
                clientA.getId(),
                clientAUser.getEmail(),
                Set.of("ROLE_CLIENT_USER"),
                Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_DOCUMENT_UPLOAD")
        );

        // Foreign Tenant & Client
        foreignOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Foreign Tax Services")
                .legalName("Foreign Tax Services")
                .email("admin@foreigntax.com")
                .status(OrganizationStatus.ACTIVE)
                .build());
        foreignTenantId = foreignOrg.getId();
        TenantContext.setTenantId(foreignTenantId);

        clientForeign = ClientEntity.builder()
                .displayName("Gamma Industries")
                .legalName("Gamma Industries")
                .clientType(ClientType.PROPRIETORSHIP)
                .pan("GBCPG5678G")
                .email("contact@gammaind.com")
                .build();
        clientForeign.setOrganizationId(foreignTenantId);
        clientForeign = clientRepository.save(clientForeign);

        clientForeignUser = UserEntity.builder()
                .email("gamma.user@gammaind.com")
                .firstName("Gamma")
                .lastName("Owner")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .status(UserStatus.ACTIVE)
                .clientId(clientForeign.getId())
                .roles(new HashSet<>())
                .build();
        clientForeignUser.setOrganizationId(foreignTenantId);
        clientForeignUser = userRepository.save(clientForeignUser);

        clientForeignToken = jwtTokenProvider.generateAccessToken(
                clientForeignUser.getId(),
                foreignTenantId,
                clientForeign.getId(),
                clientForeignUser.getEmail(),
                Set.of("ROLE_CLIENT_USER"),
                Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_DOCUMENT_VIEW")
        );

        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        cleanData();
        TenantContext.clear();
    }

    private void cleanData() {
        TenantContext.clear();
        clientApprovalRequestRepository.deleteAll();
        documentRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        clientRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    @DisplayName("Practice user creates approval request with context and document reference")
    void testCreateApprovalRequest_Success() throws Exception {
        TenantContext.setTenantId(tenantId);

        DocumentEntity doc = DocumentEntity.builder()
                .clientId(clientA.getId())
                .fileName("AY2024-25_Tax_Computation_Summary.pdf")
                .contentType("application/pdf")
                .fileSize(204800L)
                .storageKey("org/" + tenantId + "/client/" + clientA.getId() + "/doc.pdf")
                .build();
        doc.setOrganizationId(tenantId);
        doc = documentRepository.save(doc);

        CreateClientApprovalRequest request = CreateClientApprovalRequest.builder()
                .title("Approve AY 2024-25 Income Tax Computation")
                .description("Please review the final computation before we proceed with ITR-6 generation.")
                .approvalType(ClientApprovalType.TAX_COMPUTATION_APPROVAL)
                .contextType(MessageContextType.GENERAL)
                .documentId(doc.getId())
                .contentVersion("v1.0")
                .contentHash("SHA256:abcd1234efgh5678")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        mockMvc.perform(post("/api/v1/portal/clients/{clientId}/approvals", clientA.getId())
                        .header("Authorization", "Bearer " + practitionerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("Approve AY 2024-25 Income Tax Computation"))
                .andExpect(jsonPath("$.data.approvalType").value("TAX_COMPUTATION_APPROVAL"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.documentId").value(doc.getId().toString()))
                .andExpect(jsonPath("$.data.documentName").value("AY2024-25_Tax_Computation_Summary.pdf"))
                .andExpect(jsonPath("$.data.contentVersion").value("v1.0"))
                .andExpect(jsonPath("$.data.requestedByName").value("Kapoor CA"));

        // Verify audit log entry
        boolean hasAudit = auditLogRepository.findAll().stream()
                .anyMatch(a -> "CLIENT_APPROVAL_REQUEST_CREATED".equals(a.getAction()));
        assertTrue(hasAudit, "CLIENT_APPROVAL_REQUEST_CREATED audit log event should be emitted");
    }

    @Test
    @DisplayName("Practice user cannot attach document belonging to another client or organization")
    void testCreateApprovalRequest_DocumentBelongsToForeign_Forbidden() throws Exception {
        TenantContext.setTenantId(foreignTenantId);

        DocumentEntity foreignDoc = DocumentEntity.builder()
                .clientId(clientForeign.getId())
                .fileName("Confidential_Foreign.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .storageKey("foreign/key.pdf")
                .build();
        foreignDoc.setOrganizationId(foreignTenantId);
        foreignDoc = documentRepository.save(foreignDoc);

        TenantContext.setTenantId(tenantId);

        CreateClientApprovalRequest request = CreateClientApprovalRequest.builder()
                .title("Unauthorized Document Reference")
                .approvalType(ClientApprovalType.GENERAL_APPROVAL)
                .documentId(foreignDoc.getId())
                .build();

        mockMvc.perform(post("/api/v1/portal/clients/{clientId}/approvals", clientA.getId())
                        .header("Authorization", "Bearer " + practitionerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Duplicate active pending approval request for same context is rejected")
    void testCreateApprovalRequest_DuplicateContext_Conflict() throws Exception {
        TenantContext.setTenantId(tenantId);
        UUID sharedContextId = UUID.randomUUID();

        ClientApprovalRequestEntity existing = ClientApprovalRequestEntity.builder()
                .clientId(clientA.getId())
                .title("First Request")
                .approvalType(ClientApprovalType.FILING_CONSENT)
                .status(ClientApprovalStatus.PENDING)
                .contextType(MessageContextType.GENERAL)
                .contextId(sharedContextId)
                .contentVersion("v1.0")
                .requestedByUserId(practitionerUser.getId())
                .requestedByName("Kapoor CA")
                .requestedAt(Instant.now())
                .build();
        existing.setOrganizationId(tenantId);
        clientApprovalRequestRepository.save(existing);

        CreateClientApprovalRequest duplicateRequest = CreateClientApprovalRequest.builder()
                .title("Second Request on Same Context")
                .approvalType(ClientApprovalType.FILING_CONSENT)
                .contextType(MessageContextType.GENERAL)
                .contextId(sharedContextId)
                .build();

        mockMvc.perform(post("/api/v1/portal/clients/{clientId}/approvals", clientA.getId())
                        .header("Authorization", "Bearer " + practitionerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Client user can list only their own approval requests")
    void testGetApprovalRequests_ClientScopedIsolation() throws Exception {
        TenantContext.setTenantId(tenantId);

        // Approval for client A
        ClientApprovalRequestEntity reqA = ClientApprovalRequestEntity.builder()
                .clientId(clientA.getId())
                .title("Omega Computation Approval")
                .approvalType(ClientApprovalType.TAX_COMPUTATION_APPROVAL)
                .status(ClientApprovalStatus.PENDING)
                .contextType(MessageContextType.GENERAL)
                .contentVersion("v1.0")
                .requestedByUserId(practitionerUser.getId())
                .requestedByName("Kapoor CA")
                .requestedAt(Instant.now())
                .build();
        reqA.setOrganizationId(tenantId);
        clientApprovalRequestRepository.save(reqA);

        // Foreign approval for client Foreign
        TenantContext.setTenantId(foreignTenantId);
        ClientApprovalRequestEntity reqForeign = ClientApprovalRequestEntity.builder()
                .clientId(clientForeign.getId())
                .title("Gamma Terms Approval")
                .approvalType(ClientApprovalType.ENGAGEMENT_TERMS)
                .status(ClientApprovalStatus.PENDING)
                .contextType(MessageContextType.GENERAL)
                .contentVersion("v1.0")
                .requestedByUserId(practitionerUser.getId())
                .requestedByName("Foreign Practitioner")
                .requestedAt(Instant.now())
                .build();
        reqForeign.setOrganizationId(foreignTenantId);
        clientApprovalRequestRepository.save(reqForeign);

        // Client A calls GET /api/v1/portal/approvals
        mockMvc.perform(get("/api/v1/portal/approvals")
                        .header("Authorization", "Bearer " + clientAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Omega Computation Approval"))
                .andExpect(jsonPath("$.data[0].clientId").value(clientA.getId().toString()));

        // Client Foreign calls GET /api/v1/portal/approvals
        mockMvc.perform(get("/api/v1/portal/approvals")
                        .header("Authorization", "Bearer " + clientForeignToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Gamma Terms Approval"))
                .andExpect(jsonPath("$.data[0].clientId").value(clientForeign.getId().toString()));
    }

    @Test
    @DisplayName("Client approves request with server-verified identity and notes")
    void testDecideApprovalRequest_Approve_Success() throws Exception {
        TenantContext.setTenantId(tenantId);

        ClientApprovalRequestEntity req = ClientApprovalRequestEntity.builder()
                .clientId(clientA.getId())
                .title("Form 16 Sign-off")
                .approvalType(ClientApprovalType.DOCUMENT_SIGN_OFF)
                .status(ClientApprovalStatus.PENDING)
                .contextType(MessageContextType.GENERAL)
                .contentVersion("v1.0")
                .requestedByUserId(practitionerUser.getId())
                .requestedByName("Kapoor CA")
                .requestedAt(Instant.now())
                .build();
        req.setOrganizationId(tenantId);
        req = clientApprovalRequestRepository.save(req);

        DecideClientApprovalRequest decideRequest = DecideClientApprovalRequest.builder()
                .decision(ClientApprovalDecision.APPROVE)
                .expectedContentVersion("v1.0")
                .notes("Reviewed and confirmed matching internal payroll records.")
                .build();

        mockMvc.perform(post("/api/v1/portal/approvals/{approvalId}/decide", req.getId())
                        .header("Authorization", "Bearer " + clientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decideRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(req.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.respondedByName").value("Rohan Verma"))
                .andExpect(jsonPath("$.data.respondedByEmail").value("client.omega@omegainfotech.com"))
                .andExpect(jsonPath("$.data.respondedByUserId").value(clientAUser.getId().toString()))
                .andExpect(jsonPath("$.data.respondedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.decisionNotes").value("Reviewed and confirmed matching internal payroll records."));

        // Verify DB update
        ClientApprovalRequestEntity updated = clientApprovalRequestRepository.findById(req.getId()).orElseThrow();
        assertEquals(ClientApprovalStatus.APPROVED, updated.getStatus());
        assertEquals(clientAUser.getId(), updated.getRespondedByUserId());

        // Verify audit log
        boolean hasAudit = auditLogRepository.findAll().stream()
                .anyMatch(a -> "CLIENT_APPROVAL_APPROVED".equals(a.getAction()));
        assertTrue(hasAudit, "CLIENT_APPROVAL_APPROVED audit log event should be recorded");
    }

    @Test
    @DisplayName("Client rejects request with mandatory rejection reason")
    void testDecideApprovalRequest_Reject_RequiresReason() throws Exception {
        TenantContext.setTenantId(tenantId);

        ClientApprovalRequestEntity req = ClientApprovalRequestEntity.builder()
                .clientId(clientA.getId())
                .title("ITR-6 Filing Consent")
                .approvalType(ClientApprovalType.FILING_CONSENT)
                .status(ClientApprovalStatus.PENDING)
                .contextType(MessageContextType.GENERAL)
                .contentVersion("v1.0")
                .requestedByUserId(practitionerUser.getId())
                .requestedByName("Kapoor CA")
                .requestedAt(Instant.now())
                .build();
        req.setOrganizationId(tenantId);
        req = clientApprovalRequestRepository.save(req);

        // Rejection without reason -> 400 Bad Request
        DecideClientApprovalRequest emptyReasonRequest = DecideClientApprovalRequest.builder()
                .decision(ClientApprovalDecision.REJECT)
                .rejectionReason("")
                .build();

        mockMvc.perform(post("/api/v1/portal/approvals/{approvalId}/decide", req.getId())
                        .header("Authorization", "Bearer " + clientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyReasonRequest)))
                .andExpect(status().isBadRequest());

        // Rejection with proper reason -> 200 OK
        DecideClientApprovalRequest validRejectRequest = DecideClientApprovalRequest.builder()
                .decision(ClientApprovalDecision.REJECT)
                .rejectionReason("TDS on contract payments in 26AS does not match Schedule 115.")
                .build();

        mockMvc.perform(post("/api/v1/portal/approvals/{approvalId}/decide", req.getId())
                        .header("Authorization", "Bearer " + clientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("TDS on contract payments in 26AS does not match Schedule 115."))
                .andExpect(jsonPath("$.data.respondedByName").value("Rohan Verma"));

        // Verify audit log
        boolean hasAudit = auditLogRepository.findAll().stream()
                .anyMatch(a -> "CLIENT_APPROVAL_REJECTED".equals(a.getAction()));
        assertTrue(hasAudit, "CLIENT_APPROVAL_REJECTED audit log event should be recorded");
    }

    @Test
    @DisplayName("Cannot decide already approved or rejected approval request")
    void testDecideApprovalRequest_AlreadyDecided_BadRequest() throws Exception {
        TenantContext.setTenantId(tenantId);

        ClientApprovalRequestEntity req = ClientApprovalRequestEntity.builder()
                .clientId(clientA.getId())
                .title("Already Approved Request")
                .approvalType(ClientApprovalType.INFORMATION_CONFIRMATION)
                .status(ClientApprovalStatus.APPROVED)
                .contextType(MessageContextType.GENERAL)
                .contentVersion("v1.0")
                .requestedByUserId(practitionerUser.getId())
                .requestedByName("Kapoor CA")
                .requestedAt(Instant.now())
                .respondedByUserId(clientAUser.getId())
                .respondedByName("Rohan Verma")
                .respondedAt(Instant.now())
                .build();
        req.setOrganizationId(tenantId);
        req = clientApprovalRequestRepository.save(req);

        DecideClientApprovalRequest decideRequest = DecideClientApprovalRequest.builder()
                .decision(ClientApprovalDecision.APPROVE)
                .build();

        mockMvc.perform(post("/api/v1/portal/approvals/{approvalId}/decide", req.getId())
                        .header("Authorization", "Bearer " + clientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decideRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Content version mismatch prevents client from approving stale terms")
    void testDecideApprovalRequest_VersionMismatch_BadRequest() throws Exception {
        TenantContext.setTenantId(tenantId);

        ClientApprovalRequestEntity req = ClientApprovalRequestEntity.builder()
                .clientId(clientA.getId())
                .title("Engagement Letter Terms")
                .approvalType(ClientApprovalType.ENGAGEMENT_TERMS)
                .status(ClientApprovalStatus.PENDING)
                .contextType(MessageContextType.GENERAL)
                .contentVersion("v2.0")
                .requestedByUserId(practitionerUser.getId())
                .requestedByName("Kapoor CA")
                .requestedAt(Instant.now())
                .build();
        req.setOrganizationId(tenantId);
        req = clientApprovalRequestRepository.save(req);

        DecideClientApprovalRequest decideRequest = DecideClientApprovalRequest.builder()
                .decision(ClientApprovalDecision.APPROVE)
                .expectedContentVersion("v1.0")
                .build();

        mockMvc.perform(post("/api/v1/portal/approvals/{approvalId}/decide", req.getId())
                        .header("Authorization", "Bearer " + clientAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decideRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Foreign client cannot decide or view another client's approval request")
    void testDecideApprovalRequest_CrossClient_Denied() throws Exception {
        TenantContext.setTenantId(tenantId);

        ClientApprovalRequestEntity req = ClientApprovalRequestEntity.builder()
                .clientId(clientA.getId())
                .title("Client A Confidential Approval")
                .approvalType(ClientApprovalType.FILING_CONSENT)
                .status(ClientApprovalStatus.PENDING)
                .contextType(MessageContextType.GENERAL)
                .contentVersion("v1.0")
                .requestedByUserId(practitionerUser.getId())
                .requestedByName("Kapoor CA")
                .requestedAt(Instant.now())
                .build();
        req.setOrganizationId(tenantId);
        req = clientApprovalRequestRepository.save(req);

        DecideClientApprovalRequest decideRequest = DecideClientApprovalRequest.builder()
                .decision(ClientApprovalDecision.APPROVE)
                .build();

        // Foreign client attempts to decide Client A's approval -> 404 Not Found (or 403 Forbidden)
        mockMvc.perform(post("/api/v1/portal/approvals/{approvalId}/decide", req.getId())
                        .header("Authorization", "Bearer " + clientForeignToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decideRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Practice user can revoke pending request with audit entry")
    void testRevokeApprovalRequest_Success() throws Exception {
        TenantContext.setTenantId(tenantId);

        ClientApprovalRequestEntity req = ClientApprovalRequestEntity.builder()
                .clientId(clientA.getId())
                .title("Mistaken Approval Request")
                .approvalType(ClientApprovalType.GENERAL_APPROVAL)
                .status(ClientApprovalStatus.PENDING)
                .contextType(MessageContextType.GENERAL)
                .contentVersion("v1.0")
                .requestedByUserId(practitionerUser.getId())
                .requestedByName("Kapoor CA")
                .requestedAt(Instant.now())
                .build();
        req.setOrganizationId(tenantId);
        req = clientApprovalRequestRepository.save(req);

        RevokeClientApprovalRequest revokeRequest = RevokeClientApprovalRequest.builder()
                .reason("Generated with outdated draft computation. Revised version being prepared.")
                .build();

        mockMvc.perform(post("/api/v1/portal/clients/{clientId}/approvals/{approvalId}/revoke", clientA.getId(), req.getId())
                        .header("Authorization", "Bearer " + practitionerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"))
                .andExpect(jsonPath("$.data.revocationReason").value("Generated with outdated draft computation. Revised version being prepared."))
                .andExpect(jsonPath("$.data.revokedByName").value("Kapoor CA"));

        // Verify DB
        ClientApprovalRequestEntity revoked = clientApprovalRequestRepository.findById(req.getId()).orElseThrow();
        assertEquals(ClientApprovalStatus.REVOKED, revoked.getStatus());

        // Verify audit log
        boolean hasAudit = auditLogRepository.findAll().stream()
                .anyMatch(a -> "CLIENT_APPROVAL_REVOKED".equals(a.getAction()));
        assertTrue(hasAudit, "CLIENT_APPROVAL_REVOKED audit log event should be recorded");
    }

    @Test
    @DisplayName("Client portal dashboard includes pending approvals count and list")
    void testDashboard_ReflectsPendingApprovals() throws Exception {
        TenantContext.setTenantId(tenantId);

        ClientApprovalRequestEntity pendingReq = ClientApprovalRequestEntity.builder()
                .clientId(clientA.getId())
                .title("Urgent Consent Request")
                .approvalType(ClientApprovalType.FILING_CONSENT)
                .status(ClientApprovalStatus.PENDING)
                .contextType(MessageContextType.GENERAL)
                .contentVersion("v1.0")
                .requestedByUserId(practitionerUser.getId())
                .requestedByName("Kapoor CA")
                .requestedAt(Instant.now())
                .build();
        pendingReq.setOrganizationId(tenantId);
        clientApprovalRequestRepository.save(pendingReq);

        mockMvc.perform(get("/api/v1/portal/dashboard")
                        .header("Authorization", "Bearer " + clientAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingApprovalsCount").value(1))
                .andExpect(jsonPath("$.data.pendingApprovals", hasSize(1)))
                .andExpect(jsonPath("$.data.pendingApprovals[0].title").value("Urgent Consent Request"));
    }
}
