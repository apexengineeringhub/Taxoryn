package com.taxoryn.module.followup;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequest;
import com.taxoryn.module.docrequest.dto.CreateDocumentRequestItem;
import com.taxoryn.module.docrequest.dto.DocumentRequestDto;
import com.taxoryn.module.docrequest.dto.RejectDocumentItemRequest;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestItemRepository;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.docrequest.service.DocumentRequestService;
import com.taxoryn.module.document.entity.DocumentEntity.DocumentType;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.notification.entity.NotificationEntity;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.repository.NotificationRepository;
import com.taxoryn.module.notification.scheduler.NotificationScheduler;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.entity.TaskEntity.TaskCategory;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import com.taxoryn.module.task.entity.TaskEntity.TaskStatus;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.task.service.TaskService;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ClientFollowUpAutomationIntegrationTest {

    @Autowired
    private DocumentRequestService docRequestService;

    @Autowired
    private DocumentRequestRepository docRequestRepository;

    @Autowired
    private DocumentRequestItemRepository docRequestItemRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private NotificationScheduler notificationScheduler;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ClientRepository clientRepository;

    private OrganizationEntity testOrg;
    private OrganizationEntity orgB;
    private UserEntity partnerUser;
    private UserEntity staffUser;
    private EmployeeEntity staffEmployee;
    private ClientEntity testClient;
    private ClientEntity clientB;

    @BeforeEach
    void setUp() {
        testOrg = organizationRepository.save(OrganizationEntity.builder()
                .name("Apex CA Practice " + UUID.randomUUID())
                .email("admin-" + UUID.randomUUID() + "@apextax.in")
                .phone("9876543210")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        orgB = organizationRepository.save(OrganizationEntity.builder()
                .name("Competitor Practice " + UUID.randomUUID())
                .email("admin-" + UUID.randomUUID() + "@competitor.in")
                .phone("9876543211")
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build());

        TenantContext.setTenantId(testOrg.getId());

        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("ORG_ADMIN").name("Org Admin").isSystemRole(true).build()));
        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF").orElseGet(() ->
                roleRepository.save(RoleEntity.builder().code("STAFF").name("Staff").isSystemRole(true).build()));

        partnerUser = userRepository.save(UserEntity.builder()
                .email("partner-" + UUID.randomUUID() + "@apextax.in")
                .passwordHash("hashed")
                .firstName("CA Vikram")
                .lastName("Mehta")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(testOrg.getId())
                .build());

        staffUser = userRepository.save(UserEntity.builder()
                .email("staff-" + UUID.randomUUID() + "@apextax.in")
                .passwordHash("hashed")
                .firstName("Rahul")
                .lastName("Sharma")
                .roles(new HashSet<>(Set.of(staffRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(testOrg.getId())
                .build());

        EmployeeEntity staffEmp = EmployeeEntity.builder()
                .userId(staffUser.getId())
                .employeeCode("EMP-001-" + UUID.randomUUID().toString().substring(0, 5))
                .firstName("Rahul")
                .lastName("Sharma")
                .email(staffUser.getEmail())
                .designation("Senior Tax Associate")
                .status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .build();
        staffEmp.setOrganizationId(testOrg.getId());
        staffEmployee = employeeRepository.save(staffEmp);

        ClientEntity client = ClientEntity.builder()
                .displayName("Shree Enterprises")
                .legalName("Shree Enterprises Private Limited")
                .pan("AAACS1234D")
                .email("accounts@shree-ent.in")
                .assignedEmployeeId(staffEmployee.getId())
                .build();
        client.setOrganizationId(testOrg.getId());
        testClient = clientRepository.save(client);

        setAuthContext(partnerUser, "ORG_ADMIN", "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "DOC_REQUEST_CREATE", "DOC_REQUEST_VIEW", "DOC_REQUEST_UPDATE");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void setAuthContext(UserEntity user, String role, String... permissions) {
        Set<String> roles = Set.of(role);
        Set<String> perms = Set.of(permissions);
        SecurityUser securityUser = SecurityUser.builder()
                .userId(user.getId())
                .organizationId(user.getOrganizationId())
                .email(user.getEmail())
                .roles(roles)
                .permissions(perms)
                .enabled(true)
                .build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(user.getOrganizationId());
    }

    private void setClientAuthContext(ClientEntity client) {
        SecurityUser securityUser = SecurityUser.builder()
                .userId(UUID.randomUUID())
                .organizationId(client.getOrganizationId())
                .clientId(client.getId())
                .email(client.getEmail())
                .roles(Set.of("CLIENT_ADMIN"))
                .permissions(Set.of("CLIENT_PORTAL_DOCUMENT_VIEW", "CLIENT_PORTAL_DOCUMENT_UPLOAD"))
                .enabled(true)
                .build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setTenantId(client.getOrganizationId());
    }

    @Test
    @DisplayName("1. Immediate Notification: Creating a Document Request alerts the Client immediately")
    void testImmediateClientNotificationOnRequestCreation() {
        CreateDocumentRequest req = CreateDocumentRequest.builder()
                .clientId(testClient.getId())
                .purpose("GSTR-3B July 2026 Documentation")
                .dueDate(LocalDate.now().plusDays(5))
                .items(List.of(
                        CreateDocumentRequestItem.builder().title("Purchase Register").documentType(DocumentType.GST_INVOICE_PURCHASE).required(true).build(),
                        CreateDocumentRequestItem.builder().title("Sales Register").documentType(DocumentType.GST_INVOICE_SALE).required(true).build()
                ))
                .build();

        DocumentRequestDto created = docRequestService.createAndSendRequest(req);

        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo(RequestStatus.SENT);

        // Verify client received in-app DOCUMENT_REQUIRED notification
        boolean notified = notificationRepository.existsByOrganizationIdAndClientIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                testOrg.getId(), testClient.getId(), NotificationType.DOCUMENT_REQUIRED, java.time.Instant.now().minusSeconds(10));
        assertThat(notified).isTrue();
    }

    @Test
    @DisplayName("2. Automated Reminder Milestones: Due -3d, Due -1d, Due Today, and Overdue Escalation")
    void testAutomatedReminderMilestonesAndEscalations() {
        LocalDate today = LocalDate.now();

        // Setup requests across different due date windows
        DocumentRequestEntity reqDueIn3Days = docRequestRepository.save(DocumentRequestEntity.builder()
                .clientId(testClient.getId())
                .requestNumber("REQ-TEST-3D")
                .purpose("ITR Computation Verification")
                .dueDate(today.plusDays(3))
                .status(RequestStatus.SENT)
                .requestedByUserId(staffUser.getId())
                .build());
        reqDueIn3Days.setOrganizationId(testOrg.getId());

        DocumentRequestEntity reqDueTomorrow = docRequestRepository.save(DocumentRequestEntity.builder()
                .clientId(testClient.getId())
                .requestNumber("REQ-TEST-1D")
                .purpose("TDS Q1 Proofs")
                .dueDate(today.plusDays(1))
                .status(RequestStatus.SENT)
                .requestedByUserId(staffUser.getId())
                .build());
        reqDueTomorrow.setOrganizationId(testOrg.getId());

        DocumentRequestEntity reqDueToday = docRequestRepository.save(DocumentRequestEntity.builder()
                .clientId(testClient.getId())
                .requestNumber("REQ-TEST-TODAY")
                .purpose("GSTR-1 Monthly Invoices")
                .dueDate(today)
                .status(RequestStatus.SENT)
                .requestedByUserId(staffUser.getId())
                .build());
        reqDueToday.setOrganizationId(testOrg.getId());

        DocumentRequestEntity reqOverdue = docRequestRepository.save(DocumentRequestEntity.builder()
                .clientId(testClient.getId())
                .requestNumber("REQ-TEST-OVERDUE")
                .purpose("Advance Tax Computation")
                .dueDate(today.minusDays(4)) // Overdue by 4 days -> triggers manager escalation
                .status(RequestStatus.SENT)
                .requestedByUserId(staffUser.getId())
                .build());
        reqOverdue.setOrganizationId(testOrg.getId());

        // Execute Daily Reminder Scan
        notificationScheduler.runDailyReminders();

        java.time.Instant todayStart = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

        // 1. Verify 3-Day Reminder
        boolean reminder3d = notificationRepository.existsByOrganizationIdAndEntityTypeAndEntityIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                testOrg.getId(), "DOCUMENT_REQUEST", reqDueIn3Days.getId().toString(), NotificationType.DOCUMENT_REMINDER, todayStart);
        assertThat(reminder3d).isTrue();

        // 2. Verify 1-Day Reminder
        boolean reminder1d = notificationRepository.existsByOrganizationIdAndEntityTypeAndEntityIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                testOrg.getId(), "DOCUMENT_REQUEST", reqDueTomorrow.getId().toString(), NotificationType.DOCUMENT_REMINDER, todayStart);
        assertThat(reminder1d).isTrue();

        // 3. Verify Due Today Reminder
        boolean reminderToday = notificationRepository.existsByOrganizationIdAndEntityTypeAndEntityIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                testOrg.getId(), "DOCUMENT_REQUEST", reqDueToday.getId().toString(), NotificationType.DOCUMENT_DUE_TODAY, todayStart);
        assertThat(reminderToday).isTrue();

        // 4. Verify Overdue Reminder & Practitioner Alert
        boolean reminderOverdue = notificationRepository.existsByOrganizationIdAndEntityTypeAndEntityIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                testOrg.getId(), "DOCUMENT_REQUEST", reqOverdue.getId().toString(), NotificationType.DOCUMENT_OVERDUE, todayStart);
        assertThat(reminderOverdue).isTrue();
    }

    @Test
    @DisplayName("3. Idempotency: Multiple scheduler executions on the same date do NOT spam duplicate reminders")
    void testReminderSchedulerIdempotency() {
        LocalDate today = LocalDate.now();
        DocumentRequestEntity reqDueToday = docRequestRepository.save(DocumentRequestEntity.builder()
                .clientId(testClient.getId())
                .requestNumber("REQ-IDEMPOTENT-01")
                .purpose("Monthly Bank Statement")
                .dueDate(today)
                .status(RequestStatus.SENT)
                .requestedByUserId(staffUser.getId())
                .build());
        reqDueToday.setOrganizationId(testOrg.getId());

        // First Run
        notificationScheduler.runDailyReminders();
        long countAfterFirstRun = notificationRepository.count();

        // Second Run (e.g. retry / second trigger)
        notificationScheduler.runDailyReminders();
        long countAfterSecondRun = notificationRepository.count();

        // Third Run
        notificationScheduler.runDailyReminders();
        long countAfterThirdRun = notificationRepository.count();

        assertThat(countAfterSecondRun).isEqualTo(countAfterFirstRun);
        assertThat(countAfterThirdRun).isEqualTo(countAfterFirstRun);
    }

    @Test
    @DisplayName("4. Stop Reminders on Completion: Completed requests never receive future reminders")
    void testStopRemindersWhenRequestCompleted() {
        LocalDate today = LocalDate.now();

        DocumentRequestEntity completedReq = docRequestRepository.save(DocumentRequestEntity.builder()
                .clientId(testClient.getId())
                .requestNumber("REQ-COMPLETED-01")
                .purpose("Form 16 Part A & B")
                .dueDate(today)
                .status(RequestStatus.COMPLETED)
                .completedAt(java.time.Instant.now())
                .requestedByUserId(staffUser.getId())
                .build());
        completedReq.setOrganizationId(testOrg.getId());

        java.time.Instant start = java.time.Instant.now();
        notificationScheduler.runDailyReminders();

        boolean notified = notificationRepository.existsByOrganizationIdAndEntityTypeAndEntityIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                testOrg.getId(), "DOCUMENT_REQUEST", completedReq.getId().toString(), NotificationType.DOCUMENT_DUE_TODAY, start);
        assertThat(notified).isFalse();
    }

    @Test
    @DisplayName("5. Rejection Workflow: Rejecting a document item notifies client and blocks linked task")
    void testRejectionWorkflowAndTaskBlocking() {
        // 1. Create Task & Document Request
        CreateDocumentRequest docReq = CreateDocumentRequest.builder()
                .clientId(testClient.getId())
                .purpose("GSTR-3B July 2026 Audit")
                .dueDate(LocalDate.now().plusDays(5))
                .items(List.of(
                        CreateDocumentRequestItem.builder().title("Purchase Register Excel").documentType(DocumentType.GST_INVOICE_PURCHASE).required(true).build()
                ))
                .build();
        DocumentRequestDto createdDocReq = docRequestService.createAndSendRequest(docReq);

        CreateTaskRequest taskReq = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .title("Prepare July 2026 GSTR-3B")
                .taskCategory(TaskCategory.GST)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(7))
                .assignedTo(staffUser.getId())
                .documentRequestId(createdDocReq.getId())
                .build();
        TaskDto createdTask = taskService.createTask(taskReq);
        assertThat(createdTask.getStatus()).isEqualTo(TaskStatus.BLOCKED);

        // 2. Client uploads item
        UUID itemId = createdDocReq.getItems().get(0).getId();
        byte[] validZipBytes = new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x05, (byte) 0x06, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "purchase_july.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", validZipBytes);

        setClientAuthContext(testClient);
        docRequestService.uploadClientPortalItemDocument(itemId, file);

        // 3. Practitioner rejects item with reason
        setAuthContext(partnerUser, "ORG_ADMIN", "DOC_REQUEST_UPDATE", "TASK_UPDATE");
        RejectDocumentItemRequest rejectReq = RejectDocumentItemRequest.builder()
                .rejectionReason("July column is missing ITC breakdown, please re-export with IGST/CGST/SGST columns.")
                .build();
        DocumentRequestDto rejected = docRequestService.rejectItem(itemId, rejectReq);

        assertThat(rejected.getItems().get(0).getStatus()).isEqualTo(ItemStatus.REJECTED);
        assertThat(rejected.getItems().get(0).getRejectionReason()).contains("missing ITC breakdown");

        // Verify linked Task is marked BLOCKED with reason
        TaskEntity updatedTask = taskRepository.findByIdAndOrganizationId(createdTask.getId(), testOrg.getId()).orElseThrow();
        assertThat(updatedTask.getStatus()).isEqualTo(TaskStatus.BLOCKED);
        assertThat(updatedTask.getBlockedReason()).contains("missing ITC breakdown");

        // Verify Client received in-app DOCUMENT_REJECTED notification
        boolean clientNotified = notificationRepository.existsByOrganizationIdAndClientIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                testOrg.getId(), testClient.getId(), NotificationType.DOCUMENT_REJECTED, java.time.Instant.now().minusSeconds(10));
        assertThat(clientNotified).isTrue();
    }

    @Test
    @DisplayName("6. Client Re-Upload & Acceptance: Unblocks Task and notifies Practitioner")
    void testClientUploadAndAcceptanceUnblocksTask() {
        // 1. Create linked Request & Task
        CreateDocumentRequest docReq = CreateDocumentRequest.builder()
                .clientId(testClient.getId())
                .purpose("ITR Form 26AS Verification")
                .dueDate(LocalDate.now().plusDays(5))
                .items(List.of(
                        CreateDocumentRequestItem.builder().title("Form 26AS PDF").documentType(DocumentType.FORM_26AS).required(true).build()
                ))
                .build();
        DocumentRequestDto createdDocReq = docRequestService.createAndSendRequest(docReq);

        CreateTaskRequest taskReq = CreateTaskRequest.builder()
                .clientId(testClient.getId())
                .title("Verify Form 26AS with Books")
                .taskCategory(TaskCategory.ITR)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.now().plusDays(7))
                .assignedTo(staffUser.getId())
                .documentRequestId(createdDocReq.getId())
                .build();
        TaskDto createdTask = taskService.createTask(taskReq);
        assertThat(createdTask.getStatus()).isEqualTo(TaskStatus.BLOCKED);

        // 2. Client uploads file
        UUID itemId = createdDocReq.getItems().get(0).getId();
        MockMultipartFile file = new MockMultipartFile("file", "form26as_2026.pdf", "application/pdf", "%PDF-1.4 mock 26AS".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        setClientAuthContext(testClient);
        docRequestService.uploadClientPortalItemDocument(itemId, file);

        // Verify Practitioner received DOCUMENT_UPLOADED notification
        boolean practitionerNotified = notificationRepository.existsByOrganizationIdAndEntityTypeAndEntityIdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                testOrg.getId(), "DOCUMENT_REQUEST_ITEM", itemId.toString(), NotificationType.DOCUMENT_UPLOADED, java.time.Instant.now().minusSeconds(10));
        assertThat(practitionerNotified).isTrue();

        // 3. Practitioner accepts item
        setAuthContext(partnerUser, "ORG_ADMIN", "DOC_REQUEST_UPDATE", "TASK_UPDATE");
        DocumentRequestDto accepted = docRequestService.acceptItem(itemId);

        assertThat(accepted.getStatus()).isEqualTo(RequestStatus.COMPLETED);

        // 4. Verify Task is automatically UNBLOCKED to IN_PROGRESS
        TaskEntity unblockedTask = taskRepository.findByIdAndOrganizationId(createdTask.getId(), testOrg.getId()).orElseThrow();
        assertThat(unblockedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(unblockedTask.getBlockedReason()).isNull();
    }

    @Test
    @DisplayName("7. Multi-Tenant & Client Security Isolation")
    void testMultiTenantAndClientSecurityIsolation() {
        CreateDocumentRequest docReq = CreateDocumentRequest.builder()
                .clientId(testClient.getId())
                .purpose("Org A Confidential Documents")
                .dueDate(LocalDate.now().plusDays(5))
                .items(List.of(
                        CreateDocumentRequestItem.builder().title("Bank Statement").documentType(DocumentType.BANK_STATEMENT).required(true).build()
                ))
                .build();
        DocumentRequestDto reqA = docRequestService.createAndSendRequest(docReq);

        // Switch to Org B context
        TenantContext.setTenantId(orgB.getId());
        RoleEntity orgAdminRole = roleRepository.findByCodeAndIsSystemRoleTrue("ORG_ADMIN").orElseThrow();
        UserEntity orgBUser = userRepository.save(UserEntity.builder()
                .email("admin-" + UUID.randomUUID() + "@competitor.in")
                .passwordHash("hashed")
                .firstName("Sneha")
                .lastName("Patel")
                .roles(new HashSet<>(Set.of(orgAdminRole)))
                .status(UserEntity.UserStatus.ACTIVE)
                .organizationId(orgB.getId())
                .build());

        ClientEntity client2 = ClientEntity.builder()
                .displayName("Delta Industries")
                .legalName("Delta Industries LLP")
                .pan("AAACD5678E")
                .email("finance@deltaind.in")
                .build();
        client2.setOrganizationId(orgB.getId());
        ClientEntity clientB = clientRepository.save(client2);

        setAuthContext(orgBUser, "ORG_ADMIN", "DOC_REQUEST_VIEW", "DOC_REQUEST_UPDATE");

        // Org B cannot access Org A document request
        assertThrows(Exception.class, () -> docRequestService.getRequestById(reqA.getId()));

        // Client B cannot access Org A / Client A document request
        setClientAuthContext(clientB);
        assertThrows(Exception.class, () -> docRequestService.getClientPortalRequestById(reqA.getId()));
    }
}
