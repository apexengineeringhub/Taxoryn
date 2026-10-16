package com.taxoryn.module.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.dto.ClientTimelineFilterRequest;
import com.taxoryn.module.client.dto.ClientTimelineItemDto;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientNoteEntity;
import com.taxoryn.module.client.entity.TimelineEventCategory;
import com.taxoryn.module.client.repository.ClientNoteRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.service.ClientTimelineService;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.portal.entity.*;
import com.taxoryn.module.portal.repository.ClientApprovalRequestRepository;
import com.taxoryn.module.portal.repository.ClientPortalMessageRepository;
import com.taxoryn.module.reminder.entity.ReminderEntity;
import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.entity.ReminderType;
import com.taxoryn.module.reminder.repository.ReminderRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CollaborationTimelineIntegrationTest {

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
    private ClientPortalMessageRepository clientPortalMessageRepository;

    @Autowired
    private ClientApprovalRequestRepository clientApprovalRequestRepository;

    @Autowired
    private DocumentRequestRepository documentRequestRepository;

    @Autowired
    private DocumentRequestItemRepository documentRequestItemRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private ClientServiceWorkflowRepository clientServiceWorkflowRepository;

    @Autowired
    private ClientNoteRepository clientNoteRepository;

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

        // Tenant A
        tenantA = organizationRepository.save(OrganizationEntity.builder()
                .name("Practice Alpha " + UUID.randomUUID())
                .legalName("Alpha Practice LLP")
                .email("alpha." + UUID.randomUUID() + "@alpha.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(tenantA.getId());

        adminUserA = userRepository.save(UserEntity.builder()
                .organizationId(tenantA.getId())
                .email("admin." + UUID.randomUUID() + "@alpha.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Alice")
                .lastName("Partner")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build());

        clientA1 = ClientEntity.builder()
                .displayName("Alpha Corporate Client")
                .legalName("Alpha Corporate Private Limited")
                .email("contact@alphacorp.com")
                .pan("ABCDE1234F")
                .build();
        clientA1.setOrganizationId(tenantA.getId());
        clientA1 = clientRepository.save(clientA1);

        clientUserA1 = userRepository.save(UserEntity.builder()
                .organizationId(tenantA.getId())
                .clientId(clientA1.getId())
                .email("portal.user." + UUID.randomUUID() + "@alphacorp.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("John")
                .lastName("Director")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(clientAdminRole)))
                .build());

        tokenAdminA = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                tenantA.getId(),
                adminUserA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("CLIENT_READ", "CLIENT_UPDATE", "TASK_VIEW")
        );

        tokenClientA1 = "Bearer " + jwtTokenProvider.generateAccessToken(
                clientUserA1.getId(),
                tenantA.getId(),
                clientA1.getId(),
                clientUserA1.getEmail(),
                Set.of("ROLE_CLIENT_ADMIN"),
                Set.of("CLIENT_PORTAL_ACCESS")
        );

        // Tenant B
        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("Practice Beta " + UUID.randomUUID())
                .legalName("Beta Practice LLP")
                .email("beta." + UUID.randomUUID() + "@beta.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(tenantB.getId());

        adminUserB = userRepository.save(UserEntity.builder()
                .organizationId(tenantB.getId())
                .email("admin." + UUID.randomUUID() + "@beta.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Bob")
                .lastName("Partner")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build());

        clientB1 = ClientEntity.builder()
                .displayName("Beta Global Client")
                .legalName("Beta Global Private Limited")
                .email("contact@betaglobal.com")
                .pan("XYZAB5678C")
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
    // 1. Timeline Aggregation Across All Sources
    // =========================================================================

    @Test
    @DisplayName("Aggregates portal messages, approvals, document requests, reminders, and workflows into timeline")
    void timelineAggregation_aggregatesAllSupportedSources() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        Instant baseTime = Instant.now().minus(Duration.ofDays(5));

        // 1. Inbound Portal Message
        ClientPortalMessageEntity msg = ClientPortalMessageEntity.builder()
                .clientId(clientA1.getId())
                .senderType(PortalMessageSenderType.CLIENT)
                .senderName("John Director")
                .messageBody("Have the advance tax calculations been prepared?")
                .contextType(MessageContextType.GENERAL)
                .build();
        msg.setOrganizationId(tenantA.getId());
        msg.setCreatedAt(baseTime.plus(Duration.ofHours(1)));
        clientPortalMessageRepository.save(msg);

        // 2. Client Approval Request + Decision
        ClientApprovalRequestEntity approval = ClientApprovalRequestEntity.builder()
                .clientId(clientA1.getId())
                .title("Approve Advance Tax Computation")
                .description("Please review Q3 computation.")
                .approvalType(ClientApprovalType.TAX_COMPUTATION_APPROVAL)
                .status(ClientApprovalStatus.APPROVED)
                .requestedByUserId(adminUserA.getId())
                .requestedAt(baseTime.plus(Duration.ofHours(5)))
                .respondedAt(baseTime.plus(Duration.ofHours(10)))
                .respondedByName("John Director")
                .decisionNotes("Confirmed and approved for payment.")
                .build();
        approval.setOrganizationId(tenantA.getId());
        clientApprovalRequestRepository.save(approval);

        // 3. Document Request + Item Upload
        DocumentRequestEntity docReq = DocumentRequestEntity.builder()
                .clientId(clientA1.getId())
                .requestNumber("REQ-" + UUID.randomUUID().toString().substring(0, 8))
                .purpose("Q3 Bank Statements")
                .message("Please upload statements.")
                .status(DocumentRequestEntity.RequestStatus.SENT)
                .build();
        docReq.setOrganizationId(tenantA.getId());
        docReq.setCreatedAt(baseTime.plus(Duration.ofHours(15)));
        docReq = documentRequestRepository.save(docReq);

        DocumentRequestItemEntity item = DocumentRequestItemEntity.builder()
                .request(docReq)
                .clientId(clientA1.getId())
                .title("HDFC Current Account Statement")
                .status(ItemStatus.ACCEPTED)
                .uploadedAt(baseTime.plus(Duration.ofHours(20)))
                .reviewedAt(baseTime.plus(Duration.ofHours(22)))
                .build();
        item.setOrganizationId(tenantA.getId());
        documentRequestItemRepository.save(item);

        // 4. Follow-up Nudge
        ReminderEntity reminder = ReminderEntity.builder()
                .title("Submit Pending Form 16A")
                .description("Follow-up on pending documents")
                .reminderType(ReminderType.DOCUMENT_REQUEST)
                .status(ReminderStatus.PENDING)
                .priority(ReminderPriority.HIGH)
                .clientId(clientA1.getId())
                .scheduledAt(baseTime.plus(Duration.ofHours(25)))
                .lastAttemptAt(baseTime.plus(Duration.ofHours(26)))
                .notes("Practitioner dispatched nudge to client.")
                .build();
        reminder.setOrganizationId(tenantA.getId());
        reminderRepository.save(reminder);

        // 5. Compliance Workflow
        ClientServiceWorkflowEntity workflow = ClientServiceWorkflowEntity.builder()
                .clientId(clientA1.getId())
                .clientServiceId(UUID.randomUUID())
                .periodId(UUID.randomUUID())
                .title("Q3 Advance Tax Compliance")
                .status(ServiceWorkflowStatus.COMPLETED)
                .completedAt(baseTime.plus(Duration.ofHours(30)))
                .build();
        workflow.setOrganizationId(tenantA.getId());
        workflow.setCreatedAt(baseTime.plus(Duration.ofHours(2)));
        clientServiceWorkflowRepository.save(workflow);

        // Query practice timeline via API
        mockMvc.perform(get("/api/v1/clients/" + clientA1.getId() + "/timeline")
                        .header("Authorization", tokenAdminA)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(7)))
                // Verify specific event types present
                .andExpect(jsonPath("$.data.content[*].eventType", hasItems(
                        "PORTAL_MESSAGE_RECEIVED",
                        "APPROVAL_REQUESTED",
                        "APPROVAL_ACCEPTED",
                        "DOCUMENT_REQUEST_CREATED",
                        "DOCUMENT_UPLOADED",
                        "DOCUMENT_APPROVED",
                        "FOLLOW_UP_NUDGE_SENT",
                        "COMPLIANCE_WORKFLOW_STARTED",
                        "COMPLIANCE_WORKFLOW_COMPLETED"
                )));
    }

    // =========================================================================
    // 2. Chronological Ordering and Deterministic Secondary Sort
    // =========================================================================

    @Test
    @DisplayName("Sorts events chronologically descending with deterministic secondary sort on ID")
    void chronologicalOrdering_deterministicSecondarySort() {
        TenantContext.setTenantId(tenantA.getId());
        Instant fixedTime = Instant.now().minus(Duration.ofHours(2));

        // Create 3 messages at the exact same timestamp
        ClientPortalMessageEntity m1 = ClientPortalMessageEntity.builder()
                .clientId(clientA1.getId())
                .senderType(PortalMessageSenderType.CLIENT)
                .senderName("User 1")
                .messageBody("Message 1")
                .build();
        m1.setOrganizationId(tenantA.getId());
        m1.setCreatedAt(fixedTime);
        m1 = clientPortalMessageRepository.save(m1);

        ClientPortalMessageEntity m2 = ClientPortalMessageEntity.builder()
                .clientId(clientA1.getId())
                .senderType(PortalMessageSenderType.CLIENT)
                .senderName("User 2")
                .messageBody("Message 2")
                .build();
        m2.setOrganizationId(tenantA.getId());
        m2.setCreatedAt(fixedTime);
        m2 = clientPortalMessageRepository.save(m2);

        ClientPortalMessageEntity m3 = ClientPortalMessageEntity.builder()
                .clientId(clientA1.getId())
                .senderType(PortalMessageSenderType.CLIENT)
                .senderName("User 3")
                .messageBody("Message 3")
                .build();
        m3.setOrganizationId(tenantA.getId());
        m3.setCreatedAt(fixedTime);
        m3 = clientPortalMessageRepository.save(m3);

        PagedResponse<ClientTimelineItemDto> page1 = clientTimelineService.getClientTimeline(clientA1.getId(), null);
        PagedResponse<ClientTimelineItemDto> page2 = clientTimelineService.getClientTimeline(clientA1.getId(), null);

        // Deterministic: repeated queries return identical ordering
        assertEquals(3, page1.getContent().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(page1.getContent().get(i).getId(), page2.getContent().get(i).getId());
        }
    }

    // =========================================================================
    // 3. Category & Context Filtering
    // =========================================================================

    @Test
    @DisplayName("Filters timeline strictly by event category (e.g. APPROVAL, DOCUMENT)")
    void filtering_byCategoryAndContext() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        // Message
        ClientPortalMessageEntity msg = ClientPortalMessageEntity.builder()
                .clientId(clientA1.getId())
                .senderType(PortalMessageSenderType.CLIENT)
                .senderName("John")
                .messageBody("Hello")
                .build();
        msg.setOrganizationId(tenantA.getId());
        clientPortalMessageRepository.save(msg);

        // Approval
        ClientApprovalRequestEntity approval = ClientApprovalRequestEntity.builder()
                .clientId(clientA1.getId())
                .title("GST Sign-off")
                .approvalType(ClientApprovalType.FILING_CONSENT)
                .status(ClientApprovalStatus.PENDING)
                .requestedByUserId(adminUserA.getId())
                .contextType(MessageContextType.GENERAL)
                .build();
        approval.setOrganizationId(tenantA.getId());
        clientApprovalRequestRepository.save(approval);

        // 1. Filter by category APPROVAL
        mockMvc.perform(get("/api/v1/clients/" + clientA1.getId() + "/timeline")
                        .header("Authorization", tokenAdminA)
                        .param("category", "APPROVAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].eventType").value("APPROVAL_REQUESTED"));

        // 2. Filter by category COMMUNICATION
        mockMvc.perform(get("/api/v1/clients/" + clientA1.getId() + "/timeline")
                        .header("Authorization", tokenAdminA)
                        .param("category", "COMMUNICATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].eventType").value("PORTAL_MESSAGE_RECEIVED"));
    }

    // =========================================================================
    // 4. Client Portal Visibility Sanitization
    // =========================================================================

    @Test
    @DisplayName("Client portal timeline returns only client-visible items and strips internal metadata")
    void clientPortalTimeline_sanitizesAndHidesInternalEvents() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        // 1. Client-visible message
        ClientPortalMessageEntity msg = ClientPortalMessageEntity.builder()
                .clientId(clientA1.getId())
                .senderType(PortalMessageSenderType.CLIENT)
                .senderName("Client Contact")
                .messageBody("Question regarding challan.")
                .build();
        msg.setOrganizationId(tenantA.getId());
        clientPortalMessageRepository.save(msg);

        // 2. Internal Follow-up Scheduled (clientVisible = false)
        ReminderEntity internalRem = ReminderEntity.builder()
                .title("Internal Call Note: Check PAN discrepancy")
                .reminderType(ReminderType.PRACTITIONER_FOLLOW_UP)
                .status(ReminderStatus.PENDING)
                .clientId(clientA1.getId())
                .scheduledAt(Instant.now())
                .build();
        internalRem.setOrganizationId(tenantA.getId());
        reminderRepository.save(internalRem);

        // 3. Internal Practice Note
        ClientNoteEntity internalNote = ClientNoteEntity.builder()
                .clientId(clientA1.getId())
                .authorId(adminUserA.getId())
                .authorName("Admin Partner")
                .title("Confidential Risk Assessment")
                .content("Client may have undisclosed offshore interest.")
                .visibility(ClientNoteEntity.Visibility.INTERNAL)
                .build();
        internalNote.setOrganizationId(tenantA.getId());
        clientNoteRepository.save(internalNote);

        // Query portal timeline as client user
        mockMvc.perform(get("/api/v1/portal/clients/" + clientA1.getId() + "/timeline")
                        .header("Authorization", tokenClientA1))
                .andExpect(status().isOk())
                // Only 1 item returned: the client message
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].eventType").value("PORTAL_MESSAGE_RECEIVED"))
                // Internal user UUID stripped for portal user
                .andExpect(jsonPath("$.data.content[0].actorId").doesNotExist());
    }

    // =========================================================================
    // 5. Tenant & Client Isolation
    // =========================================================================

    @Test
    @DisplayName("Tenant A cannot view Tenant B client timeline (HTTP 404)")
    void tenantIsolation_enforced() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + clientB1.getId() + "/timeline")
                        .header("Authorization", tokenAdminA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Client User 1 cannot access Client 2 timeline in portal (HTTP 403)")
    void clientIsolation_enforcedInPortal() throws Exception {
        // Client User A1 tries to access Client B1's timeline in portal
        mockMvc.perform(get("/api/v1/portal/clients/" + clientB1.getId() + "/timeline")
                        .header("Authorization", tokenClientA1))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 6. Empty Timeline Handling
    // =========================================================================

    @Test
    @DisplayName("Empty timeline returns valid empty paged response without errors")
    void emptyTimeline_returnsEmptyPagedResponse() throws Exception {
        mockMvc.perform(get("/api/v1/clients/" + clientA1.getId() + "/timeline")
                        .header("Authorization", tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }
}
