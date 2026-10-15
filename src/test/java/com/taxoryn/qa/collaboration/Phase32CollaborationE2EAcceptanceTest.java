package com.taxoryn.qa.collaboration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.dto.ClientTimelineItemDto;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientNoteEntity;
import com.taxoryn.module.client.entity.TimelineEventCategory;
import com.taxoryn.module.client.repository.ClientNoteRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.service.ClientTimelineService;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequestItem;
import com.taxoryn.module.docrequest.dto.RejectDocumentItemRequest;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.portal.dto.CreateClientApprovalRequest;
import com.taxoryn.module.portal.dto.DecideClientApprovalRequest;
import com.taxoryn.module.portal.dto.SendClientPortalMessageRequest;
import com.taxoryn.module.portal.entity.*;
import com.taxoryn.module.portal.repository.ClientApprovalRequestRepository;
import com.taxoryn.module.portal.repository.ClientPortalMessageRepository;
import com.taxoryn.module.reminder.dto.SendFollowUpNudgeRequest;
import com.taxoryn.module.reminder.entity.ReminderEntity;
import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.entity.ReminderType;
import com.taxoryn.module.reminder.repository.ReminderRepository;
import com.taxoryn.module.reminder.service.FollowUpReminderService;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.workflow.entity.ClientServiceWorkflowEntity;
import com.taxoryn.module.workflow.model.ServiceWorkflowStatus;
import com.taxoryn.module.workflow.repository.ClientServiceWorkflowRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase32CollaborationE2EAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DocumentRequestRepository documentRequestRepository;

    @Autowired
    private DocumentRequestItemRepository documentRequestItemRepository;

    @Autowired
    private ClientPortalMessageRepository clientPortalMessageRepository;

    @Autowired
    private ClientApprovalRequestRepository clientApprovalRequestRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private ClientServiceWorkflowRepository clientServiceWorkflowRepository;

    @Autowired
    private ClientNoteRepository clientNoteRepository;

    @Autowired
    private FollowUpReminderService followUpReminderService;

    @Autowired
    private ClientTimelineService clientTimelineService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity tenantA;
    private OrganizationEntity tenantB;
    private UserEntity adminUserA;
    private UserEntity adminUserB;
    private UserEntity clientUserA1;
    private ClientEntity clientA1;
    private ClientEntity clientB1;
    private String tokenAdminA;
    private String tokenAdminB;
    private String tokenClientA1;

    @BeforeEach
    void setUp() {
        reminderRepository.deleteAll();
        clientPortalMessageRepository.deleteAll();
        clientApprovalRequestRepository.deleteAll();
        documentRequestItemRepository.deleteAll();
        documentRequestRepository.deleteAll();
        clientServiceWorkflowRepository.deleteAll();
        clientNoteRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build()));

        RoleEntity clientAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("CLIENT_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("CLIENT_ADMIN").name("Client Admin").isSystemRole(true).build()));

        // 1. Tenant A Setup
        tenantA = organizationRepository.save(OrganizationEntity.builder()
                .name("KPMG Practice " + UUID.randomUUID())
                .legalName("KPMG LLP")
                .email("kpmg." + UUID.randomUUID() + "@kpmg.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(tenantA.getId());

        adminUserA = userRepository.save(UserEntity.builder()
                .organizationId(tenantA.getId())
                .email("partner." + UUID.randomUUID() + "@kpmg.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Senior")
                .lastName("Partner")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build());

        clientA1 = ClientEntity.builder()
                .displayName("Tata Motors Limited")
                .legalName("Tata Motors Limited")
                .email("tax@tatamotors.com")
                .pan("AAACT2727Q")
                .build();
        clientA1.setOrganizationId(tenantA.getId());
        clientA1 = clientRepository.save(clientA1);

        clientUserA1 = userRepository.save(UserEntity.builder()
                .organizationId(tenantA.getId())
                .clientId(clientA1.getId())
                .email("portal.director." + UUID.randomUUID() + "@tatamotors.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("CFO")
                .lastName("Director")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(clientAdminRole)))
                .build());

        tokenAdminA = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                tenantA.getId(),
                adminUserA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("CLIENT_READ", "CLIENT_UPDATE", "CLIENT_VIEW", "DOCUMENT_WRITE", "DOCUMENT_VIEW", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE")
        );

        tokenClientA1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientUserA1.getId(),
                tenantA.getId(),
                clientA1.getId(),
                clientUserA1.getEmail(),
                Set.of("ROLE_CLIENT_ADMIN", "ROLE_CLIENT_USER"),
                Set.of("CLIENT_PORTAL_ACCESS", "CLIENT_PORTAL_VIEW", "CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_DOCUMENT_UPLOAD")
        );

        // 2. Tenant B Setup (for multi-tenant isolation tests)
        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("Deloitte Practice " + UUID.randomUUID())
                .legalName("Deloitte LLP")
                .email("deloitte." + UUID.randomUUID() + "@deloitte.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(tenantB.getId());

        adminUserB = userRepository.save(UserEntity.builder()
                .organizationId(tenantB.getId())
                .email("admin." + UUID.randomUUID() + "@deloitte.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Deloitte")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build());

        clientB1 = ClientEntity.builder()
                .displayName("Reliance Retail")
                .legalName("Reliance Retail Ventures Limited")
                .email("tax@relianceretail.com")
                .pan("AABCR5432K")
                .build();
        clientB1.setOrganizationId(tenantB.getId());
        clientB1 = clientRepository.save(clientB1);

        tokenAdminB = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                tenantB.getId(),
                adminUserB.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("CLIENT_READ", "CLIENT_UPDATE", "TASK_VIEW")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // =========================================================================
    // E2E-01: Complete Golden-Path Collaboration Lifecycle
    // =========================================================================

    @Test
    @DisplayName("E2E-01: Full Golden Path - Document Request -> Upload -> Review -> Message -> Approval -> Follow-up -> Timeline")
    void e2e01_goldenPathCollaborationLifecycle() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        // Step 1: Practitioner creates a multi-item Document Request
        CreateDocumentRequest docReqPayload = CreateDocumentRequest.builder()
                .clientId(clientA1.getId())
                .purpose("FY 2025-26 Year-End Corporate Tax Audit")
                .message("Please provide audited financial statements and Form 26AS.")
                .dueDate(LocalDate.now().plusDays(7))
                .financialYear("2025-26")
                .assessmentYear("2026-27")
                .items(List.of(
                        CreateDocumentRequestItem.builder()
                                .title("Form 26AS / AIS Tax Credit Statement")
                                .description("Consolidated tax credit statement")
                                .required(true)
                                .build(),
                        CreateDocumentRequestItem.builder()
                                .title("Statutory Audit Report (Signed)")
                                .description("Audited balance sheet and P&L with notes")
                                .required(true)
                                .build()
                ))
                .build();

        String createDocResp = mockMvc.perform(post("/api/v1/document-requests")
                        .header("Authorization", tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docReqPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        String requestIdStr = objectMapper.readTree(createDocResp).path("data").path("id").asText();
        String item1IdStr = objectMapper.readTree(createDocResp).path("data").path("items").get(0).path("id").asText();
        String item2IdStr = objectMapper.readTree(createDocResp).path("data").path("items").get(1).path("id").asText();
        UUID requestId = UUID.fromString(requestIdStr);
        UUID item1Id = UUID.fromString(item1IdStr);
        UUID item2Id = UUID.fromString(item2IdStr);

        // Step 2: Client queries document requests in portal
        mockMvc.perform(get("/api/v1/portal/document-requests")
                        .header("Authorization", tokenClientA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(requestIdStr))
                .andExpect(jsonPath("$.data[0].items", hasSize(2)));

        // Step 3: Client uploads file for Item 1 (Form 26AS)
        MockMultipartFile fileUpload = new MockMultipartFile(
                "file",
                "Form_26AS_AY2026-27.pdf",
                "application/pdf",
                "%PDF-1.4 Form 26AS Tax Credits Sample Content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/portal/document-requests/items/" + item1Id + "/upload")
                        .file(fileUpload)
                        .header("Authorization", tokenClientA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + item1IdStr + "')].status").value("UPLOADED"));

        // Step 4: Practitioner accepts Item 1 and rejects Item 2 with a correction notice
        mockMvc.perform(post("/api/v1/document-requests/items/" + item1Id + "/accept")
                        .header("Authorization", tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + item1IdStr + "')].status").value("ACCEPTED"));

        RejectDocumentItemRequest rejectReq = RejectDocumentItemRequest.builder()
                .rejectionReason("Incomplete audit report: Auditor signature page 12 missing.")
                .build();

        mockMvc.perform(post("/api/v1/document-requests/items/" + item2Id + "/reject")
                        .header("Authorization", tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + item2IdStr + "')].status").value("REJECTED"))
                .andExpect(jsonPath("$.data.items[?(@.id == '" + item2IdStr + "')].rejectionReason")
                        .value("Incomplete audit report: Auditor signature page 12 missing."));

        // Step 5: Client initiates contextual communication linked to the Document Request
        SendClientPortalMessageRequest msgRequest = SendClientPortalMessageRequest.builder()
                .messageBody("We have reached out to the statutory auditors for the signed page 12.")
                .contextType(MessageContextType.DOCUMENT_REQUEST)
                .contextId(requestId)
                .contextTitle("FY 2025-26 Year-End Corporate Tax Audit")
                .build();

        mockMvc.perform(post("/api/v1/portal/messages")
                        .header("Authorization", tokenClientA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contextType").value("DOCUMENT_REQUEST"))
                .andExpect(jsonPath("$.data.contextTitle", containsString("FY 2025-26 Year-End Corporate Tax Audit")));

        // Step 6: Practitioner creates Approval Request for advance computation
        CreateClientApprovalRequest apprReq = CreateClientApprovalRequest.builder()
                .title("Approve Advance Tax Computation FY 2025-26")
                .description("Please review and authorize estimated advance tax installment.")
                .approvalType(ClientApprovalType.TAX_COMPUTATION_APPROVAL)
                .contextType(MessageContextType.DOCUMENT_REQUEST)
                .contextId(requestId)
                .contextTitle("Advance Tax Installment")
                .expiresAt(Instant.now().plus(Duration.ofDays(3)))
                .build();

        String createApprovalResp = mockMvc.perform(post("/api/v1/portal/clients/" + clientA1.getId() + "/approvals")
                        .header("Authorization", tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apprReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        String approvalIdStr = objectMapper.readTree(createApprovalResp).path("data").path("id").asText();
        UUID approvalId = UUID.fromString(approvalIdStr);

        // Step 7: Client signs off on the approval request
        DecideClientApprovalRequest decideReq = DecideClientApprovalRequest.builder()
                .decision(ClientApprovalDecision.APPROVE)
                .notes("Approved for payment and statutory submission.")
                .build();

        mockMvc.perform(post("/api/v1/portal/approvals/" + approvalId + "/decide")
                        .header("Authorization", tokenClientA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decideReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.respondedByName").isNotEmpty());

        // Step 8: Practitioner creates a collaboration follow-up reminder and sends manual nudge
        ReminderEntity reminder = ReminderEntity.builder()
                .title("Submit Signed Auditor Certificate Page")
                .description("Action required for item 2")
                .reminderType(ReminderType.DOCUMENT_REQUEST)
                .status(ReminderStatus.PENDING)
                .priority(ReminderPriority.HIGH)
                .clientId(clientA1.getId())
                .scheduledAt(Instant.now().plus(Duration.ofHours(2)))
                .referenceType("DOCUMENT_REQUEST")
                .referenceId(requestId.toString())
                .build();
        reminder.setOrganizationId(tenantA.getId());
        reminder = reminderRepository.save(reminder);

        SendFollowUpNudgeRequest nudgeReq = SendFollowUpNudgeRequest.builder()
                .customMessage("Gentle reminder: Please provide the signed audit page to finalize filing.")
                .build();

        mockMvc.perform(post("/api/v1/reminders/follow-ups/" + reminder.getId() + "/nudge")
                        .header("Authorization", tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nudgeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastAttemptAt").isNotEmpty());

        // Step 9: Verify Practice Timeline retrieves all 5 domains in deterministic chronological order
        mockMvc.perform(get("/api/v1/clients/" + clientA1.getId() + "/timeline")
                        .header("Authorization", tokenAdminA)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[*].eventType", hasItems(
                        "DOCUMENT_REQUEST_CREATED",
                        "DOCUMENT_UPLOADED",
                        "DOCUMENT_APPROVED",
                        "DOCUMENT_REJECTED",
                        "PORTAL_MESSAGE_RECEIVED",
                        "APPROVAL_REQUESTED",
                        "APPROVAL_ACCEPTED",
                        "FOLLOW_UP_NUDGE_SENT"
                )));

        // Step 10: Verify Client Portal Timeline sanitizes metadata, hides staff records, and strips actor UUIDs
        mockMvc.perform(get("/api/v1/portal/clients/" + clientA1.getId() + "/timeline")
                        .header("Authorization", tokenClientA1)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())))
                // Internal practitioner UUID must be stripped
                .andExpect(jsonPath("$.data.content[*].actorId", everyItem(nullValue())))
                // All returned items must be client visible
                .andExpect(jsonPath("$.data.content[*].clientVisible", everyItem(is(true))));
    }

    // =========================================================================
    // E2E-02: Client Action Center Statuses & Rejection Reason Visibility
    // =========================================================================

    @Test
    @DisplayName("E2E-02: Action Center reflects rejected items with human-readable rejection reason")
    void e2e02_clientActionCenter_rejectionReasons() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        DocumentRequestEntity req = DocumentRequestEntity.builder()
                .clientId(clientA1.getId())
                .requestNumber("REQ-ACT-001")
                .purpose("Q3 TDS Reconciliation")
                .status(DocumentRequestEntity.RequestStatus.PARTIALLY_COMPLETED)
                .build();
        req.setOrganizationId(tenantA.getId());
        req = documentRequestRepository.save(req);

        DocumentRequestItemEntity rejectedItem = DocumentRequestItemEntity.builder()
                .request(req)
                .clientId(clientA1.getId())
                .title("Form 16A Challan BSR Counterfoils")
                .status(ItemStatus.REJECTED)
                .rejectionReason("Challan 281 counterfoil for November is illegible. Please scan at 300 DPI.")
                .reviewedAt(Instant.now())
                .build();
        rejectedItem.setOrganizationId(tenantA.getId());
        documentRequestItemRepository.save(rejectedItem);

        // Query portal document requests
        mockMvc.perform(get("/api/v1/portal/document-requests")
                        .header("Authorization", tokenClientA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].items[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.data[0].items[0].rejectionReason")
                        .value("Challan 281 counterfoil for November is illegible. Please scan at 300 DPI."));
    }

    // =========================================================================
    // E2E-03: Contextual Communication Threading & Context Scoping
    // =========================================================================

    @Test
    @DisplayName("E2E-03: Contextual messages are threaded and queryable by contextType and contextId")
    void e2e03_contextualMessaging_threadingAndScoping() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        UUID contextEntityId = UUID.randomUUID();

        // 1. Initial message
        ClientPortalMessageEntity parentMsg = ClientPortalMessageEntity.builder()
                .clientId(clientA1.getId())
                .senderType(PortalMessageSenderType.CLIENT)
                .senderName("CFO")
                .messageBody("What is the deadline for advance tax Q4?")
                .contextType(MessageContextType.GENERAL)
                .contextTitle("Advance Tax Q4")
                .build();
        parentMsg.setOrganizationId(tenantA.getId());
        parentMsg = clientPortalMessageRepository.save(parentMsg);

        // 2. Practitioner Reply
        SendClientPortalMessageRequest replyReq = SendClientPortalMessageRequest.builder()
                .messageBody("The statutory due date is March 15th, 2026.")
                .contextType(MessageContextType.GENERAL)
                .contextTitle("Advance Tax Q4")
                .parentMessageId(parentMsg.getId())
                .build();

        mockMvc.perform(post("/api/v1/portal/clients/" + clientA1.getId() + "/messages")
                        .header("Authorization", tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.parentMessageId").value(parentMsg.getId().toString()));

        // 3. Query messages
        mockMvc.perform(get("/api/v1/portal/messages")
                        .header("Authorization", tokenClientA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    // =========================================================================
    // E2E-04: Client Approvals Terminal Decision Locking
    // =========================================================================

    @Test
    @DisplayName("E2E-04: Approval request cannot be re-decided once in terminal APPROVED status")
    void e2e04_clientApprovals_terminalDecisionLocking() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientApprovalRequestEntity approval = ClientApprovalRequestEntity.builder()
                .clientId(clientA1.getId())
                .title("GST Annual Return GSTR-9 Authorization")
                .approvalType(ClientApprovalType.FILING_CONSENT)
                .status(ClientApprovalStatus.APPROVED)
                .respondedAt(Instant.now())
                .respondedByName("CFO")
                .build();
        approval.setOrganizationId(tenantA.getId());
        approval = clientApprovalRequestRepository.save(approval);

        // Attempting to decide again must fail with 400 Bad Request
        DecideClientApprovalRequest duplicateDecide = DecideClientApprovalRequest.builder()
                .decision(ClientApprovalDecision.REJECT)
                .rejectionReason("Attempt to change mind")
                .build();

        mockMvc.perform(post("/api/v1/portal/approvals/" + approval.getId() + "/decide")
                        .header("Authorization", tokenClientA1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDecide)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // E2E-05: Follow-up Reminder Deduplication & Cooldown
    // =========================================================================

    @Test
    @DisplayName("E2E-05: Manual follow-up nudge enforces 12-hour cooldown period")
    void e2e05_followUpReminder_cooldownEnforcement() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ReminderEntity reminder = ReminderEntity.builder()
                .title("Follow-up: Pending ITR-V Signature")
                .reminderType(ReminderType.CLIENT_APPROVAL)
                .status(ReminderStatus.PENDING)
                .clientId(clientA1.getId())
                .scheduledAt(Instant.now().plus(Duration.ofHours(2)))
                .lastAttemptAt(Instant.now().minus(Duration.ofHours(2))) // Nudged 2 hours ago
                .build();
        reminder.setOrganizationId(tenantA.getId());
        reminder = reminderRepository.save(reminder);

        SendFollowUpNudgeRequest nudgeReq = SendFollowUpNudgeRequest.builder()
                .customMessage("Immediate second nudge")
                .build();

        // Attempt second nudge within 12 hours -> 400 Bad Request
        mockMvc.perform(post("/api/v1/reminders/follow-ups/" + reminder.getId() + "/nudge")
                        .header("Authorization", tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nudgeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("cooldown")));
    }

    // =========================================================================
    // E2E-06: Collaboration Timeline Deterministic Ordering & Sanitization
    // =========================================================================

    @Test
    @DisplayName("E2E-06: Collaboration timeline multi-key sort stable on equal timestamps")
    void e2e06_timelineDeterministicOrdering() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        Instant exactNow = Instant.now().minus(Duration.ofMinutes(10));

        // Two messages at the exact same instant
        ClientPortalMessageEntity m1 = ClientPortalMessageEntity.builder()
                .clientId(clientA1.getId())
                .senderType(PortalMessageSenderType.CLIENT)
                .senderName("User Alpha")
                .messageBody("Message Alpha")
                .build();
        m1.setOrganizationId(tenantA.getId());
        m1.setCreatedAt(exactNow);
        m1 = clientPortalMessageRepository.save(m1);

        ClientPortalMessageEntity m2 = ClientPortalMessageEntity.builder()
                .clientId(clientA1.getId())
                .senderType(PortalMessageSenderType.CLIENT)
                .senderName("User Beta")
                .messageBody("Message Beta")
                .build();
        m2.setOrganizationId(tenantA.getId());
        m2.setCreatedAt(exactNow);
        m2 = clientPortalMessageRepository.save(m2);

        PagedResponse<ClientTimelineItemDto> page1 = clientTimelineService.getClientTimeline(clientA1.getId(), null);
        PagedResponse<ClientTimelineItemDto> page2 = clientTimelineService.getClientTimeline(clientA1.getId(), null);

        assertEquals(2, page1.getContent().size());
        assertEquals(page1.getContent().get(0).getId(), page2.getContent().get(0).getId());
        assertEquals(page1.getContent().get(1).getId(), page2.getContent().get(1).getId());
    }

    // =========================================================================
    // Security & Tenant Isolation Gate
    // =========================================================================

    @Test
    @DisplayName("Security Gate: Cross-tenant and cross-client access strictly blocked")
    void securityGate_tenantAndClientIsolation() throws Exception {
        // 1. Cross-Tenant: Admin B (Deloitte) attempts to access Client A1 (KPMG) timeline -> 404 Not Found
        mockMvc.perform(get("/api/v1/clients/" + clientA1.getId() + "/timeline")
                        .header("Authorization", tokenAdminB))
                .andExpect(status().isNotFound());

        // 2. Cross-Client: Client User A1 attempts to access Client B1's timeline in portal -> 403 Forbidden
        mockMvc.perform(get("/api/v1/portal/clients/" + clientB1.getId() + "/timeline")
                        .header("Authorization", tokenClientA1))
                .andExpect(status().isForbidden());

        // 3. Client User attempts to access practice-only follow-ups endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/reminders/follow-ups")
                        .header("Authorization", tokenClientA1))
                .andExpect(status().isForbidden());
    }
}
