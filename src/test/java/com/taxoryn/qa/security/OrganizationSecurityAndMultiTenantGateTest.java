package com.taxoryn.qa.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.document.entity.DocumentEntity;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.document.repository.DocumentRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.qa.factory.OrganizationTestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationSecurityAndMultiTenantGateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationTestDataFactory factory;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity adminA;
    private UserEntity adminB;
    private UserEntity staffA;
    private ClientEntity clientA;
    private ClientEntity clientB;
    private EmployeeEntity employeeA;
    private EmployeeEntity employeeB;
    private DocumentEntity documentA;
    private DocumentEntity documentB;

    private String tokenAdminA;
    private String tokenAdminB;
    private String tokenStaffA;

    @BeforeEach
    void setUp() {
        orgA = factory.createOrganization("Secure Org A " + UUID.randomUUID().toString().substring(0, 5), "admin.secA." + UUID.randomUUID().toString().substring(0, 5) + "@seca.in");
        orgB = factory.createOrganization("Secure Org B " + UUID.randomUUID().toString().substring(0, 5), "admin.secB." + UUID.randomUUID().toString().substring(0, 5) + "@secb.in");

        adminA = factory.createAdminUser(orgA, "adminA." + UUID.randomUUID().toString().substring(0, 5) + "@seca.in", "AdminPass123!");
        adminB = factory.createAdminUser(orgB, "adminB." + UUID.randomUUID().toString().substring(0, 5) + "@secb.in", "AdminPass123!");
        staffA = factory.createEmployeeUser(orgA, "staffA." + UUID.randomUUID().toString().substring(0, 5) + "@seca.in", "STAFF", "StaffPass123!");

        clientA = factory.createClient(orgA, "Client Alpha", "contact.clientA." + UUID.randomUUID().toString().substring(0, 5) + "@alpha.com");
        clientB = factory.createClient(orgB, "Client Beta", "contact.clientB." + UUID.randomUUID().toString().substring(0, 5) + "@beta.com");

        employeeA = factory.createEmployee(orgA, staffA, "EMP-SEC-A", "Audit", "Auditor");
        UserEntity staffB = factory.createEmployeeUser(orgB, "staffB." + UUID.randomUUID().toString().substring(0, 5) + "@secb.in", "STAFF", "StaffPass123!");
        employeeB = factory.createEmployee(orgB, staffB, "EMP-SEC-B", "Audit", "Auditor");

        documentA = factory.createDocument(orgA, clientA, adminA, "alpha_financials.pdf", DocumentType.FINANCIAL_STATEMENTS);
        documentB = factory.createDocument(orgB, clientB, adminB, "beta_financials.pdf", DocumentType.FINANCIAL_STATEMENTS);

        tokenAdminA = factory.generateBearerToken(adminA);
        tokenAdminB = factory.generateBearerToken(adminB);
        tokenStaffA = factory.generateBearerToken(staffA);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // =========================================================================
    // 1. MULTI-TENANT ISOLATION & IDOR TESTS (HIGHEST PRIORITY)
    // =========================================================================

    @Test
    @DisplayName("SEC-001: Strict Cross-Tenant Client Isolation - Org A cannot view Org B client")
    void shouldRejectCrossTenantClientAccess() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + clientB.getId())
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SEC-002: Strict Cross-Tenant Employee Isolation - Org A cannot view Org B employee")
    void shouldRejectCrossTenantEmployeeAccess() throws Exception {
        mockMvc.perform(get("/api/v1/employees/" + employeeB.getId())
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SEC-003: Strict Cross-Tenant Document Vault Isolation - Org A cannot view Org B document")
    void shouldRejectCrossTenantDocumentAccess() throws Exception {
        mockMvc.perform(get("/api/v1/documents/" + documentB.getId() + "/download")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 2. MASS ASSIGNMENT PROTECTION
    // =========================================================================

    @Test
    @DisplayName("SEC-004: Mass Assignment Protection - Server ignores injected organizationId & elevated roles")
    void shouldIgnoreInjectedOrganizationIdAndRole() throws Exception {
        Map<String, Object> maliciousPayload = Map.of(
                "clientType", "INDIVIDUAL",
                "displayName", "Attacker Injected Client",
                "email", "hacker." + UUID.randomUUID().toString().substring(0, 5) + "@hacker.com",
                "phone", "+919876543299",
                "organizationId", orgB.getId().toString(), // Attempting to assign client to Org B!
                "isAdmin", true,
                "roles", List.of("SUPER_ADMIN")
        );

        String responseJson = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousPayload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String createdId = objectMapper.readTree(responseJson).get("data").get("id").asText();
        ClientEntity createdClient = clientRepository.findById(UUID.fromString(createdId)).orElseThrow();

        // Must belong to Org A (caller's security context), NOT Org B!
        assertEquals(orgA.getId(), createdClient.getOrganizationId());
        assertNotEquals(orgB.getId(), createdClient.getOrganizationId());
    }

    // =========================================================================
    // 3. JWT TOKEN AUTHENTICATION & TAMPERING TESTS
    // =========================================================================

    @Test
    @DisplayName("SEC-005: Missing Token returns 401 Unauthorized")
    void shouldRejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SEC-006: Tampered / Modified JWT Token returns 401 Unauthorized")
    void shouldRejectTamperedToken() throws Exception {
        String tamperedToken = tokenAdminA.substring(0, tokenAdminA.length() - 5) + "abcde";
        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 4. AUDIT LOGGING & SENSITIVE DATA LEAK PREVENTION
    // =========================================================================

    @Test
    @DisplayName("SEC-007: Audit Logs must NOT leak raw passwords, hashes, or secrets")
    void shouldEnsureAuditLogsDoNotContainSensitiveData() {
        List<AuditLogEntity> recentLogs = auditLogRepository.findAll(PageRequest.of(0, 50)).getContent();
        for (AuditLogEntity logEntry : recentLogs) {
            String val = (logEntry.getNewValue() != null ? logEntry.getNewValue().toLowerCase() : "") +
                    (logEntry.getOldValue() != null ? logEntry.getOldValue().toLowerCase() : "");
            assertFalse(val.contains("passwordhash"), "Audit log leaked passwordHash: " + logEntry.getId());
            assertFalse(val.contains("validstrongpass"), "Audit log leaked plain password: " + logEntry.getId());
            assertFalse(val.contains("secret-key"), "Audit log leaked secret key: " + logEntry.getId());
        }
    }

    // =========================================================================
    // 5. PLATFORM VS PRACTICE BOUNDARY RBAC TESTS
    // =========================================================================

    @Test
    @DisplayName("SEC-008: Practice Admin cannot access Feedback Ops API (/api/v1/admin/feedback) -> 403 Forbidden")
    void practiceAdminCannotAccessFeedbackOpsApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/feedback")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/feedback/stats")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-009: Practice Staff/Employee cannot modify subscription -> 403 Forbidden")
    void employeeCannotModifySubscription() throws Exception {
        Map<String, Object> changePlanReq = Map.of(
                "plan", "ENTERPRISE",
                "billingInterval", "YEARLY"
        );

        mockMvc.perform(post("/api/v1/subscriptions/change-plan")
                        .header("Authorization", "Bearer " + tokenStaffA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePlanReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-010: Practice Staff/Employee cannot modify organization settings or branding -> 403 Forbidden")
    void employeeCannotModifyOrganizationProfileOrSettings() throws Exception {
        Map<String, Object> updateOrgReq = Map.of(
                "name", "Hacked Org Name",
                "phone", "+919876543210"
        );

        mockMvc.perform(put("/api/v1/organizations/current")
                        .header("Authorization", "Bearer " + tokenStaffA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateOrgReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-011: Strict Audit Trail Isolation - Org A admin cannot see Org B audit events")
    void strictAuditTrailTenantIsolation() throws Exception {
        // Create an audit log entry for Org B
        AuditLogEntity logB = AuditLogEntity.builder()
                .organizationId(orgB.getId())
                .userId(adminB.getId())
                .action("ORG_B_CONFIDENTIAL_ACTION")
                .entityType("CONFIDENTIAL")
                .entityId(UUID.randomUUID().toString())
                .createdAt(java.time.Instant.now())
                .build();
        auditLogRepository.save(logB);

        // Org A admin fetches audit logs
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].action", not(hasItem("ORG_B_CONFIDENTIAL_ACTION"))));
    }

    @Test
    @DisplayName("SEC-012: IDOR Protection - Org A Admin attempting to update Org B is denied")
    void shouldRejectCrossTenantOrganizationUpdate() throws Exception {
        Map<String, Object> updateOrgReq = Map.of(
                "name", "Malicious Name Override",
                "phone", "+919876543210"
        );

        mockMvc.perform(put("/api/v1/organizations/" + orgB.getId())
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateOrgReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-013: Organization User can access standard 'Give Feedback' endpoint (/api/v1/feedback)")
    void organizationUserCanSubmitFeedback() throws Exception {
        Map<String, Object> feedbackReq = Map.of(
                "type", "SUGGESTION",
                "category", "BILLING",
                "rating", 5,
                "title", "Add automated UPI QR code to invoices",
                "description", "Clients request instant UPI QR code on generated PDF invoices."
        );

        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedbackReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title", is("Add automated UPI QR code to invoices")));
    }
}
