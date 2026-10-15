package com.taxoryn.module.reminder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.notification.entity.NotificationEntity;
import com.taxoryn.module.notification.repository.NotificationRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.portal.entity.*;
import com.taxoryn.module.portal.repository.ClientApprovalRequestRepository;
import com.taxoryn.module.portal.repository.ClientPortalMessageRepository;
import com.taxoryn.module.reminder.dto.FollowUpEvaluationResultDto;
import com.taxoryn.module.reminder.dto.SendFollowUpNudgeRequest;
import com.taxoryn.module.reminder.entity.ReminderEntity;
import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.entity.ReminderType;
import com.taxoryn.module.reminder.repository.ReminderRepository;
import com.taxoryn.module.reminder.scheduler.ReminderScheduler;
import com.taxoryn.module.reminder.service.FollowUpReminderService;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FollowUpReminderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ClientApprovalRequestRepository clientApprovalRequestRepository;

    @Autowired
    private DocumentRequestRepository documentRequestRepository;

    @Autowired
    private DocumentRequestItemRepository documentRequestItemRepository;

    @Autowired
    private ClientPortalMessageRepository clientPortalMessageRepository;

    @Autowired
    private ComplianceObligationRepository complianceObligationRepository;

    @Autowired
    private FollowUpReminderService followUpReminderService;

    @Autowired
    private ReminderScheduler reminderScheduler;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private OrganizationEntity tenantA;
    private OrganizationEntity tenantB;
    private UserEntity adminUserA;
    private UserEntity adminUserB;
    private ClientEntity clientA1;
    private ClientEntity clientB1;
    private String tokenAdminA;
    private String tokenAdminB;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        reminderRepository.deleteAll();
        clientPortalMessageRepository.deleteAll();
        clientApprovalRequestRepository.deleteAll();
        documentRequestItemRepository.deleteAll();
        documentRequestRepository.deleteAll();
        complianceObligationRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() -> {
            RoleEntity r = RoleEntity.builder()
                    .code("ORG_ADMIN")
                    .name("Organization Administrator")
                    .isSystemRole(true)
                    .build();
            return roleRepository.save(r);
        });

        // Setup Tenant A
        tenantA = organizationRepository.save(OrganizationEntity.builder()
                .name("CA Practice Alpha " + UUID.randomUUID())
                .legalName("Alpha Partners LLP")
                .email("admin.a." + UUID.randomUUID() + "@alpha.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(tenantA.getId());

        adminUserA = userRepository.save(UserEntity.builder()
                .organizationId(tenantA.getId())
                .email("admin.a." + UUID.randomUUID() + "@alpha.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("Alpha")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build());

        clientA1 = ClientEntity.builder()
                .displayName("Alpha Corp Ltd")
                .legalName("Alpha Corp Limited")
                .email("contact@alphacorp.com")
                .pan("ABCDE1234F")
                .build();
        clientA1.setOrganizationId(tenantA.getId());
        clientA1 = clientRepository.save(clientA1);

        tokenAdminA = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserA.getId(),
                tenantA.getId(),
                adminUserA.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("TASK_VIEW", "TASK_UPDATE", "TASK_CREATE", "CLIENT_VIEW")
        );

        // Setup Tenant B
        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("CA Practice Beta " + UUID.randomUUID())
                .legalName("Beta Partners LLP")
                .email("admin.b." + UUID.randomUUID() + "@beta.com")
                .status(OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(tenantB.getId());

        adminUserB = userRepository.save(UserEntity.builder()
                .organizationId(tenantB.getId())
                .email("admin.b." + UUID.randomUUID() + "@beta.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Admin")
                .lastName("Beta")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(orgAdminRole)))
                .build());

        clientB1 = ClientEntity.builder()
                .displayName("Beta Industries")
                .legalName("Beta Industries Private Limited")
                .email("contact@betaind.com")
                .pan("XYZAB5678C")
                .build();
        clientB1.setOrganizationId(tenantB.getId());
        clientB1 = clientRepository.save(clientB1);

        tokenAdminB = "Bearer " + jwtTokenProvider.generateAccessToken(
                adminUserB.getId(),
                tenantB.getId(),
                adminUserB.getEmail(),
                Set.of("ROLE_ORG_ADMIN"),
                Set.of("TASK_VIEW", "TASK_UPDATE", "TASK_CREATE", "CLIENT_VIEW")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // =========================================================================
    // Category 1: Pending Client Approvals
    // =========================================================================

    @Test
    @DisplayName("Eligible pending approval approaching expiration creates reminder candidate")
    void pendingApproval_producesReminderCandidate() {
        TenantContext.setTenantId(tenantA.getId());

        ClientApprovalRequestEntity approval = ClientApprovalRequestEntity.builder()
                .clientId(clientA1.getId())
                .title("Approve ITR-6 Computation")
                .description("Please review and approve FY 2024-25 computation.")
                .approvalType(ClientApprovalType.TAX_COMPUTATION_APPROVAL)
                .status(ClientApprovalStatus.PENDING)
                .contextType(MessageContextType.GENERAL)
                .requestedByUserId(adminUserA.getId())
                .requestedAt(Instant.now().minus(Duration.ofHours(50)))
                .expiresAt(Instant.now().plus(Duration.ofHours(20))) // approaching expiry (< 24h)
                .build();
        approval.setOrganizationId(tenantA.getId());
        clientApprovalRequestRepository.save(approval);

        FollowUpEvaluationResultDto result = followUpReminderService.evaluateFollowUps(tenantA.getId());
        assertEquals(1, result.getApprovalsEvaluated());
        assertEquals(1, result.getTotalRemindersCreated());

        List<ReminderEntity> reminders = reminderRepository.findAll();
        assertEquals(1, reminders.size());
        ReminderEntity r = reminders.get(0);
        assertEquals(ReminderType.CLIENT_APPROVAL, r.getReminderType());
        assertEquals(ReminderPriority.URGENT, r.getPriority());
        assertEquals(approval.getId().toString(), r.getReferenceId());
        assertTrue(r.getIdempotencyKey().contains("FOLLOWUP::APPROVAL::" + approval.getId()));
    }

    @Test
    @DisplayName("Approved, rejected, revoked, or expired approvals are excluded from reminders")
    void resolvedApproval_excludedFromReminder() {
        TenantContext.setTenantId(tenantA.getId());

        // Approved
        ClientApprovalRequestEntity approved = ClientApprovalRequestEntity.builder()
                .clientId(clientA1.getId())
                .title("Approved Request")
                .approvalType(ClientApprovalType.FILING_CONSENT)
                .status(ClientApprovalStatus.APPROVED)
                .requestedByUserId(adminUserA.getId())
                .build();
        approved.setOrganizationId(tenantA.getId());
        clientApprovalRequestRepository.save(approved);

        // Expired
        ClientApprovalRequestEntity expired = ClientApprovalRequestEntity.builder()
                .clientId(clientA1.getId())
                .title("Expired Request")
                .approvalType(ClientApprovalType.DOCUMENT_SIGN_OFF)
                .status(ClientApprovalStatus.PENDING)
                .expiresAt(Instant.now().minus(Duration.ofHours(5)))
                .requestedByUserId(adminUserA.getId())
                .build();
        expired.setOrganizationId(tenantA.getId());
        clientApprovalRequestRepository.save(expired);

        FollowUpEvaluationResultDto result = followUpReminderService.evaluateFollowUps(tenantA.getId());
        assertEquals(0, result.getApprovalsEvaluated());
        assertEquals(0, result.getTotalRemindersCreated());
    }

    // =========================================================================
    // Category 2: Outstanding Document Requests
    // =========================================================================

    @Test
    @DisplayName("Eligible outstanding document request produces reminder")
    void outstandingDocumentRequest_producesReminder() {
        TenantContext.setTenantId(tenantA.getId());

        DocumentRequestEntity req = DocumentRequestEntity.builder()
                .clientId(clientA1.getId())
                .requestNumber("REQ-2026-001")
                .purpose("Q3 GST Audit Documents")
                .status(RequestStatus.SENT)
                .dueDate(LocalDate.now().plusDays(1)) // due tomorrow
                .sentAt(Instant.now().minus(Duration.ofDays(2)))
                .requestedByUserId(adminUserA.getId())
                .build();
        req.setOrganizationId(tenantA.getId());
        documentRequestRepository.save(req);

        DocumentRequestItemEntity item = DocumentRequestItemEntity.builder()
                .request(req)
                .clientId(clientA1.getId())
                .title("GSTR-2B Statement")
                .status(ItemStatus.PENDING)
                .required(true)
                .build();
        item.setOrganizationId(tenantA.getId());
        documentRequestItemRepository.save(item);
        req.setItems(List.of(item));

        FollowUpEvaluationResultDto result = followUpReminderService.evaluateFollowUps(tenantA.getId());
        assertEquals(1, result.getDocumentRequestsEvaluated());
        assertEquals(1, result.getTotalRemindersCreated());

        List<ReminderEntity> reminders = reminderRepository.findAll();
        assertEquals(1, reminders.size());
        assertEquals(ReminderType.DOCUMENT_REQUEST, reminders.get(0).getReminderType());
        assertEquals(ReminderPriority.HIGH, reminders.get(0).getPriority());
    }

    @Test
    @DisplayName("Completed or cancelled document requests are excluded from reminders")
    void completedOrCancelledDocumentRequest_excluded() {
        TenantContext.setTenantId(tenantA.getId());

        DocumentRequestEntity completedReq = DocumentRequestEntity.builder()
                .clientId(clientA1.getId())
                .requestNumber("REQ-COMPLETED-01")
                .purpose("Completed Request")
                .status(RequestStatus.COMPLETED)
                .dueDate(LocalDate.now().minusDays(1))
                .requestedByUserId(adminUserA.getId())
                .build();
        completedReq.setOrganizationId(tenantA.getId());
        documentRequestRepository.save(completedReq);

        FollowUpEvaluationResultDto result = followUpReminderService.evaluateFollowUps(tenantA.getId());
        assertEquals(0, result.getDocumentRequestsEvaluated());
        assertEquals(0, result.getTotalRemindersCreated());
    }

    // =========================================================================
    // Category 3: Unanswered Client Communication
    // =========================================================================

    @Test
    @DisplayName("Unanswered client message waiting > 12h generates reminder candidate")
    void unansweredClientCommunication_producesReminder() {
        TenantContext.setTenantId(tenantA.getId());

        ClientPortalMessageEntity msg = ClientPortalMessageEntity.builder()
                .clientId(clientA1.getId())
                .senderType(PortalMessageSenderType.CLIENT)
                .senderName("John Client")
                .messageBody("When will our advance tax calculation be ready?")
                .readByPractice(false)
                .readByClient(true)
                .contextType(MessageContextType.GENERAL)
                .build();
        msg.setOrganizationId(tenantA.getId());
        msg = clientPortalMessageRepository.save(msg);

        // Backdate created_at in database to simulate a message waiting 18 hours
        jdbcTemplate.update("UPDATE client_portal_messages SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(Duration.ofHours(18))), msg.getId());
        entityManager.clear();

        FollowUpEvaluationResultDto result = followUpReminderService.evaluateFollowUps(tenantA.getId());
        assertEquals(1, result.getCommunicationsEvaluated());
        assertEquals(1, result.getTotalRemindersCreated());

        ReminderEntity r = reminderRepository.findAll().get(0);
        assertEquals(ReminderType.UNANSWERED_COMMUNICATION, r.getReminderType());
    }

    @Test
    @DisplayName("Read client messages or practice messages do not generate client-waiting reminder")
    void readOrSystemMessages_excluded() {
        TenantContext.setTenantId(tenantA.getId());

        ClientPortalMessageEntity readMsg = ClientPortalMessageEntity.builder()
                .clientId(clientA1.getId())
                .senderType(PortalMessageSenderType.CLIENT)
                .senderName("John Client")
                .messageBody("Already answered message")
                .readByPractice(true)
                .build();
        readMsg.setOrganizationId(tenantA.getId());
        readMsg.setCreatedAt(Instant.now().minus(Duration.ofHours(20)));
        clientPortalMessageRepository.save(readMsg);

        FollowUpEvaluationResultDto result = followUpReminderService.evaluateFollowUps(tenantA.getId());
        assertEquals(0, result.getCommunicationsEvaluated());
    }

    // =========================================================================
    // Category 4: Upcoming Compliance Deadlines
    // =========================================================================

    @Test
    @DisplayName("Compliance obligation approaching statutory due date generates reminder")
    void upcomingComplianceDeadline_producesReminder() {
        TenantContext.setTenantId(tenantA.getId());

        LocalDate statutoryDate = LocalDate.now().plusDays(2);
        ComplianceObligationEntity ob = ComplianceObligationEntity.builder()
                .clientId(clientA1.getId())
                .title("GSTR-3B Monthly Return")
                .obligationType(ComplianceObligationType.GST_RETURN)
                .statutoryDueDate(statutoryDate)
                .status(ComplianceObligationStatus.UPCOMING)
                .assignedUserId(adminUserA.getId())
                .build();
        ob.setOrganizationId(tenantA.getId());
        complianceObligationRepository.save(ob);

        FollowUpEvaluationResultDto result = followUpReminderService.evaluateFollowUps(tenantA.getId());
        assertEquals(1, result.getComplianceObligationsEvaluated());
        assertEquals(1, result.getTotalRemindersCreated());

        ReminderEntity r = reminderRepository.findAll().get(0);
        assertEquals(ReminderType.COMPLIANCE_DEADLINE, r.getReminderType());
        assertEquals(ob.getId().toString(), r.getReferenceId());
    }

    // =========================================================================
    // Deduplication & Reconciliation Tests
    // =========================================================================

    @Test
    @DisplayName("Duplicate evaluation runs do not create duplicate reminders")
    void deduplication_preventsDuplicateReminders() {
        TenantContext.setTenantId(tenantA.getId());

        ClientApprovalRequestEntity approval = ClientApprovalRequestEntity.builder()
                .clientId(clientA1.getId())
                .title("Annual Sign-off")
                .approvalType(ClientApprovalType.GENERAL_APPROVAL)
                .status(ClientApprovalStatus.PENDING)
                .requestedByUserId(adminUserA.getId())
                .requestedAt(Instant.now().minus(Duration.ofHours(60)))
                .build();
        approval.setOrganizationId(tenantA.getId());
        clientApprovalRequestRepository.save(approval);

        // Run 1
        FollowUpEvaluationResultDto run1 = followUpReminderService.evaluateFollowUps(tenantA.getId());
        assertEquals(1, run1.getTotalRemindersCreated());
        assertEquals(1, reminderRepository.count());

        // Run 2 (same day)
        FollowUpEvaluationResultDto run2 = followUpReminderService.evaluateFollowUps(tenantA.getId());
        assertEquals(0, run2.getTotalRemindersCreated());
        assertEquals(1, reminderRepository.count());
    }

    @Test
    @DisplayName("Source state reconciliation cancels reminder when source action is resolved")
    void reconciliation_cancelsStaleReminder() {
        TenantContext.setTenantId(tenantA.getId());

        ClientApprovalRequestEntity approval = ClientApprovalRequestEntity.builder()
                .clientId(clientA1.getId())
                .title("Audit Terms")
                .approvalType(ClientApprovalType.ENGAGEMENT_TERMS)
                .status(ClientApprovalStatus.PENDING)
                .requestedByUserId(adminUserA.getId())
                .requestedAt(Instant.now().minus(Duration.ofHours(60)))
                .build();
        approval.setOrganizationId(tenantA.getId());
        clientApprovalRequestRepository.save(approval);

        followUpReminderService.evaluateFollowUps(tenantA.getId());
        ReminderEntity r = reminderRepository.findAll().get(0);
        assertEquals(ReminderStatus.PENDING, r.getStatus());

        // Client approves it
        approval.setStatus(ClientApprovalStatus.APPROVED);
        clientApprovalRequestRepository.save(approval);

        // Reconciliation
        boolean stillEligible = followUpReminderService.validateAndReconcileSourceState(r.getId());
        assertFalse(stillEligible);

        ReminderEntity reconciled = reminderRepository.findById(r.getId()).orElseThrow();
        assertEquals(ReminderStatus.CANCELLED, reconciled.getStatus());
        assertTrue(reconciled.getNotes().contains("no longer pending"));
    }

    // =========================================================================
    // Manual Nudge & REST API Tests
    // =========================================================================

    @Test
    @DisplayName("Practitioner sends manual nudge: notification dispatched, attempt count incremented")
    void manualNudge_succeedsAndDispatchesNotification() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientApprovalRequestEntity approval = ClientApprovalRequestEntity.builder()
                .clientId(clientA1.getId())
                .title("Urgent Filing Consent")
                .approvalType(ClientApprovalType.FILING_CONSENT)
                .status(ClientApprovalStatus.PENDING)
                .requestedByUserId(adminUserA.getId())
                .requestedAt(Instant.now().minus(Duration.ofHours(50)))
                .build();
        approval.setOrganizationId(tenantA.getId());
        clientApprovalRequestRepository.save(approval);

        followUpReminderService.evaluateFollowUps(tenantA.getId());
        ReminderEntity r = reminderRepository.findAll().get(0);

        SendFollowUpNudgeRequest nudgeReq = new SendFollowUpNudgeRequest("Please sign off before midnight.");

        mockMvc.perform(post("/api/v1/reminders/follow-ups/" + r.getId() + "/nudge")
                        .header("Authorization", tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nudgeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TRIGGERED"))
                .andExpect(jsonPath("$.data.notificationAttempts").value(1));

        // Verify in-app notification recorded
        List<NotificationEntity> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        assertEquals("Follow-up: " + r.getTitle(), notifications.get(0).getTitle());
    }

    @Test
    @DisplayName("Manual nudge enforces 12-hour cooldown period")
    void manualNudge_enforcesCooldown() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ClientApprovalRequestEntity approval = ClientApprovalRequestEntity.builder()
                .clientId(clientA1.getId())
                .title("Terms Acknowledgment")
                .approvalType(ClientApprovalType.ENGAGEMENT_TERMS)
                .status(ClientApprovalStatus.PENDING)
                .requestedByUserId(adminUserA.getId())
                .requestedAt(Instant.now().minus(Duration.ofHours(50)))
                .build();
        approval.setOrganizationId(tenantA.getId());
        clientApprovalRequestRepository.save(approval);

        followUpReminderService.evaluateFollowUps(tenantA.getId());
        ReminderEntity r = reminderRepository.findAll().get(0);

        // Nudge 1 succeeds via MockMvc
        mockMvc.perform(post("/api/v1/reminders/follow-ups/" + r.getId() + "/nudge")
                        .header("Authorization", tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendFollowUpNudgeRequest())))
                .andExpect(status().isOk());

        // Nudge 2 within cooldown fails
        mockMvc.perform(post("/api/v1/reminders/follow-ups/" + r.getId() + "/nudge")
                        .header("Authorization", tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendFollowUpNudgeRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cooldown period is 12 hours")));
    }

    @Test
    @DisplayName("Practitioner resolves follow-up: status transitions to COMPLETED")
    void resolveFollowUp_marksCompleted() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ReminderEntity reminder = ReminderEntity.builder()
                .title("Call client regarding PAN")
                .reminderType(ReminderType.PRACTITIONER_FOLLOW_UP)
                .status(ReminderStatus.PENDING)
                .clientId(clientA1.getId())
                .scheduledAt(Instant.now())
                .build();
        reminder.setOrganizationId(tenantA.getId());
        reminderRepository.save(reminder);

        mockMvc.perform(post("/api/v1/reminders/follow-ups/" + reminder.getId() + "/resolve")
                        .header("Authorization", tokenAdminA)
                        .param("notes", "Client phoned in and confirmed PAN details."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        ReminderEntity updated = reminderRepository.findById(reminder.getId()).orElseThrow();
        assertEquals(ReminderStatus.COMPLETED, updated.getStatus());
        assertEquals("Client phoned in and confirmed PAN details.", updated.getNotes());
    }

    @Test
    @DisplayName("Tenant isolation is strictly enforced across follow-up queries and actions")
    void tenantIsolation_enforcedInFollowUps() throws Exception {
        TenantContext.setTenantId(tenantA.getId());

        ReminderEntity reminderA = ReminderEntity.builder()
                .title("Tenant A Confidential Reminder")
                .reminderType(ReminderType.CLIENT_APPROVAL)
                .status(ReminderStatus.PENDING)
                .clientId(clientA1.getId())
                .scheduledAt(Instant.now())
                .build();
        reminderA.setOrganizationId(tenantA.getId());
        reminderRepository.save(reminderA);

        // Tenant B practitioner attempts to nudge Tenant A's reminder
        mockMvc.perform(post("/api/v1/reminders/follow-ups/" + reminderA.getId() + "/nudge")
                        .header("Authorization", tokenAdminB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        // Tenant B lists follow-ups: should see 0 items
        mockMvc.perform(get("/api/v1/reminders/follow-ups")
                        .header("Authorization", tokenAdminB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("ReminderScheduler processDueReminders executes evaluation and delivery across active tenants")
    void scheduledProcessing_evaluatesAndTriggers() {
        TenantContext.setTenantId(tenantA.getId());

        ClientApprovalRequestEntity approval = ClientApprovalRequestEntity.builder()
                .clientId(clientA1.getId())
                .title("Scheduled Scan Approval")
                .approvalType(ClientApprovalType.TAX_COMPUTATION_APPROVAL)
                .status(ClientApprovalStatus.PENDING)
                .requestedByUserId(adminUserA.getId())
                .requestedAt(Instant.now().minus(Duration.ofHours(50)))
                .build();
        approval.setOrganizationId(tenantA.getId());
        clientApprovalRequestRepository.save(approval);

        TenantContext.clear();

        // Run full scheduler
        assertDoesNotThrow(() -> reminderScheduler.processDueReminders());

        // Reminder was evaluated and triggered
        List<ReminderEntity> reminders = reminderRepository.findAll();
        assertEquals(1, reminders.size());
        assertEquals(ReminderStatus.TRIGGERED, reminders.get(0).getStatus());
        assertEquals(1, reminders.get(0).getNotificationAttempts());
    }
}
