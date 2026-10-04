package com.taxoryn.module.reminder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.notification.repository.NotificationRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.reminder.dto.CreateReminderRequest;
import com.taxoryn.module.reminder.dto.SaveAutomationRuleRequest;
import com.taxoryn.module.reminder.dto.UpdateReminderRequest;
import com.taxoryn.module.reminder.entity.AutomationEventType;
import com.taxoryn.module.reminder.entity.AutomationRuleEntity;
import com.taxoryn.module.reminder.entity.AutomationTargetType;
import com.taxoryn.module.reminder.entity.ReminderEntity;
import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderRecurrenceType;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.entity.ReminderType;
import com.taxoryn.module.reminder.event.ReminderEventListener;
import com.taxoryn.module.reminder.event.TaxorynBusinessEvent;
import com.taxoryn.module.reminder.repository.AutomationRuleRepository;
import com.taxoryn.module.reminder.repository.ReminderRepository;
import com.taxoryn.module.reminder.scheduler.ReminderScheduler;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReminderIntegrationTest {

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
    private ReminderRepository reminderRepository;

    @Autowired
    private AutomationRuleRepository automationRuleRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ReminderEventListener reminderEventListener;

    @Autowired
    private ReminderScheduler reminderScheduler;

    private OrganizationEntity tenantA;
    private OrganizationEntity tenantB;
    private UserEntity adminUserA;
    private UserEntity staffUserA;
    private UserEntity adminUserB;
    private String tokenAdminA;
    private String tokenStaffA;
    private String tokenAdminB;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        reminderRepository.deleteAll();
        automationRuleRepository.deleteAll();
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

        RoleEntity staffRole = roleRepository.findByCodeAndIsSystemRoleTrue("STAFF").orElseGet(() -> {
            RoleEntity r = RoleEntity.builder()
                    .code("STAFF")
                    .name("Staff")
                    .isSystemRole(true)
                    .build();
            return roleRepository.save(r);
        });

        // 1. Setup Tenant A
        tenantA = organizationRepository.save(OrganizationEntity.builder()
                .name("CA Practice Alpha " + UUID.randomUUID())
                .legalName("Alpha Partners LLP")
                .email("admin." + UUID.randomUUID() + "@alpha.com")
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

        staffUserA = userRepository.save(UserEntity.builder()
                .organizationId(tenantA.getId())
                .email("staff.a." + UUID.randomUUID() + "@alpha.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Staff")
                .lastName("Alpha")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(List.of(staffRole)))
                .build());

        tokenAdminA = jwtTokenProvider.generateAccessToken(
                adminUserA.getId(), tenantA.getId(), null, adminUserA.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("TASK_CREATE", "TASK_UPDATE", "TASK_VIEW", "TASK_DELETE")
        );

        tokenStaffA = jwtTokenProvider.generateAccessToken(
                staffUserA.getId(), tenantA.getId(), null, staffUserA.getEmail(),
                Set.of("STAFF"),
                Set.of("TASK_CREATE", "TASK_UPDATE", "TASK_VIEW")
        );

        // 2. Setup Tenant B
        tenantB = organizationRepository.save(OrganizationEntity.builder()
                .name("CA Practice Beta " + UUID.randomUUID())
                .legalName("Beta Advisors LLP")
                .email("admin." + UUID.randomUUID() + "@beta.com")
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

        tokenAdminB = jwtTokenProvider.generateAccessToken(
                adminUserB.getId(), tenantB.getId(), null, adminUserB.getEmail(),
                Set.of("ORG_ADMIN"),
                Set.of("TASK_CREATE", "TASK_UPDATE", "TASK_VIEW", "TASK_DELETE")
        );

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // =========================================================================
    // 1. Manual Reminder CRUD Integration Tests
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/reminders should create a new manual reminder with 201 Created")
    void testCreateManualReminder() throws Exception {
        CreateReminderRequest request = CreateReminderRequest.builder()
                .title("Submit GSTR-3B for Client X")
                .description("Ensure input tax credit is reconciled")
                .reminderType(ReminderType.TASK_DUE)
                .priority(ReminderPriority.HIGH)
                .scheduledAt(Instant.now().plus(3, ChronoUnit.DAYS))
                .recurrenceType(ReminderRecurrenceType.MONTHLY)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Submit GSTR-3B for Client X"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.recurrenceType").value("MONTHLY"))
                .andExpect(jsonPath("$.data.organizationId").value(tenantA.getId().toString()))
                .andExpect(jsonPath("$.data.targetUserId").value(adminUserA.getId().toString()))
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
        UUID reminderId = UUID.fromString(idStr);

        ReminderEntity persisted = reminderRepository.findById(reminderId).orElseThrow();
        assertEquals(tenantA.getId(), persisted.getOrganizationId());
        assertEquals("Submit GSTR-3B for Client X", persisted.getTitle());
    }

    @Test
    @DisplayName("GET /api/v1/reminders/my and GET /api/v1/reminders/{id} should retrieve reminder")
    void testGetReminderEndpoints() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        ReminderEntity reminder = ReminderEntity.builder()
                .title("Review Draft Audit Report")
                .reminderType(ReminderType.GENERAL)
                .status(ReminderStatus.PENDING)
                .priority(ReminderPriority.MEDIUM)
                .targetUserId(adminUserA.getId())
                .scheduledAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .recurrenceType(ReminderRecurrenceType.NONE)
                .notificationAttempts(0)
                .build();
        reminder.setOrganizationId(tenantA.getId());
        reminder = reminderRepository.save(reminder);
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/reminders/" + reminder.getId())
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Review Draft Audit Report"))
                .andExpect(jsonPath("$.data.targetUserId").value(adminUserA.getId().toString()));

        mockMvc.perform(get("/api/v1/reminders/my")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].title").value("Review Draft Audit Report"));
    }

    @Test
    @DisplayName("PUT /api/v1/reminders/{id} should update a PENDING reminder")
    void testUpdateReminderEndpoint() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        ReminderEntity reminder = ReminderEntity.builder()
                .title("Initial Title")
                .reminderType(ReminderType.GENERAL)
                .status(ReminderStatus.PENDING)
                .priority(ReminderPriority.LOW)
                .targetUserId(adminUserA.getId())
                .scheduledAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .recurrenceType(ReminderRecurrenceType.NONE)
                .notificationAttempts(0)
                .build();
        reminder.setOrganizationId(tenantA.getId());
        reminder = reminderRepository.save(reminder);
        TenantContext.clear();

        UpdateReminderRequest updateRequest = UpdateReminderRequest.builder()
                .title("Updated Title")
                .priority(ReminderPriority.URGENT)
                .build();

        mockMvc.perform(put("/api/v1/reminders/" + reminder.getId())
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.priority").value("URGENT"));
    }

    @Test
    @DisplayName("POST /api/v1/reminders/{id}/complete and /cancel should transition reminder lifecycle")
    void testCompleteAndCancelEndpoints() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        ReminderEntity reminder1 = ReminderEntity.builder()
                .title("Reminder to complete")
                .status(ReminderStatus.PENDING)
                .targetUserId(adminUserA.getId())
                .scheduledAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        reminder1.setOrganizationId(tenantA.getId());
        reminder1 = reminderRepository.save(reminder1);

        ReminderEntity reminder2 = ReminderEntity.builder()
                .title("Reminder to cancel")
                .status(ReminderStatus.PENDING)
                .targetUserId(adminUserA.getId())
                .scheduledAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        reminder2.setOrganizationId(tenantA.getId());
        reminder2 = reminderRepository.save(reminder2);
        TenantContext.clear();

        mockMvc.perform(post("/api/v1/reminders/" + reminder1.getId() + "/complete")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        mockMvc.perform(post("/api/v1/reminders/" + reminder2.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAt").isNotEmpty());
    }

    // =========================================================================
    // 2. Strict Tenant Isolation Tests
    // =========================================================================

    @Test
    @DisplayName("Reminders must strictly enforce tenant isolation across organizations")
    void testStrictTenantIsolationOnReminders() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        // Create reminder for Tenant A
        ReminderEntity reminderA = ReminderEntity.builder()
                .title("Tenant A Confidential Reminder")
                .status(ReminderStatus.PENDING)
                .targetUserId(adminUserA.getId())
                .scheduledAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        reminderA.setOrganizationId(tenantA.getId());
        reminderA = reminderRepository.save(reminderA);
        TenantContext.clear();

        // Tenant B tries to access Tenant A's reminder -> 404 Not Found
        mockMvc.perform(get("/api/v1/reminders/" + reminderA.getId())
                        .header("Authorization", "Bearer " + tokenAdminB))
                .andExpect(status().isNotFound());

        // Tenant B tries to update Tenant A's reminder -> 404 Not Found
        UpdateReminderRequest updateRequest = UpdateReminderRequest.builder()
                .title("Hacked Title")
                .build();
        mockMvc.perform(put("/api/v1/reminders/" + reminderA.getId())
                        .header("Authorization", "Bearer " + tokenAdminB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        // Tenant B tries to complete Tenant A's reminder -> 404 Not Found
        mockMvc.perform(post("/api/v1/reminders/" + reminderA.getId() + "/complete")
                        .header("Authorization", "Bearer " + tokenAdminB))
                .andExpect(status().isNotFound());

        // Tenant B list my reminders -> does not see Tenant A's reminder
        mockMvc.perform(get("/api/v1/reminders/my")
                        .header("Authorization", "Bearer " + tokenAdminB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    // =========================================================================
    // 3. Automation Rule Management & Cross-Tenant Security Tests
    // =========================================================================

    @Test
    @DisplayName("Automation Rules CRUD with org-scoping and system-default inclusion")
    void testAutomationRulesEndpoints() throws Exception {
        // Create system default rule (org = null)
        AutomationRuleEntity systemDefault = automationRuleRepository.save(AutomationRuleEntity.builder()
                .organizationId(null)
                .name("Global 2 Days Before Task Due")
                .description("System default rule")
                .eventType(AutomationEventType.TASK_DUE)
                .daysOffset(-2)
                .targetType(AutomationTargetType.TASK_ASSIGNEE)
                .enabled(true)
                .build());

        // List rules for Tenant A -> sees system default rule
        mockMvc.perform(get("/api/v1/automation-rules")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].systemDefault").value(true));

        // Create Tenant A specific rule
        SaveAutomationRuleRequest createReq = SaveAutomationRuleRequest.builder()
                .name("Alpha Custom 1 Day Before")
                .description("Custom rule for practice Alpha")
                .eventType(AutomationEventType.TASK_DUE)
                .daysOffset(-1)
                .targetType(AutomationTargetType.TASK_ASSIGNEE)
                .enabled(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/automation-rules")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Alpha Custom 1 Day Before"))
                .andExpect(jsonPath("$.data.systemDefault").value(false))
                .andReturn();

        String ruleIdStr = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();
        UUID ruleId = UUID.fromString(ruleIdStr);

        // Tenant B cannot modify Tenant A's rule -> 403 Forbidden
        SaveAutomationRuleRequest hackReq = SaveAutomationRuleRequest.builder()
                .name("Beta Hack")
                .eventType(AutomationEventType.TASK_DUE)
                .daysOffset(-5)
                .targetType(AutomationTargetType.TASK_ASSIGNEE)
                .enabled(true)
                .build();

        mockMvc.perform(put("/api/v1/automation-rules/" + ruleId)
                        .header("Authorization", "Bearer " + tokenAdminB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackReq)))
                .andExpect(status().isForbidden());

        // Tenant A can disable and delete its own rule
        mockMvc.perform(post("/api/v1/automation-rules/" + ruleId + "/disable")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(delete("/api/v1/automation-rules/" + ruleId)
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk());

        assertFalse(automationRuleRepository.existsById(ruleId));
    }

    // =========================================================================
    // 4. Event-Driven Reminder Creation & Scheduler Processing Integration
    // =========================================================================

    @Test
    @DisplayName("Business event listener should trigger automated reminder creation based on active rules")
    void testEventListenerCreatesAutomatedReminder() throws Exception {
        TenantContext.setTenantId(tenantA.getId());
        // Create active automation rule for Tenant A
        AutomationRuleEntity rule = automationRuleRepository.save(AutomationRuleEntity.builder()
                .organizationId(tenantA.getId())
                .name("Notify on Task Created")
                .eventType(AutomationEventType.TASK_CREATED)
                .daysOffset(0)
                .targetType(AutomationTargetType.TASK_ASSIGNEE)
                .enabled(true)
                .build());
        TenantContext.clear();

        UUID taskId = UUID.randomUUID();
        TaxorynBusinessEvent event = TaxorynBusinessEvent.builder()
                .eventType(AutomationEventType.TASK_CREATED)
                .organizationId(tenantA.getId())
                .taskId(taskId)
                .assignedUserId(staffUserA.getId())
                .entityTitle("File Annual Return MGT-7")
                .dueDate(LocalDate.now())
                .build();

        // Publish event to listener
        reminderEventListener.onBusinessEvent(event);

        // Wait for async processing if needed
        List<ReminderEntity> reminders = Collections.emptyList();
        for (int i = 0; i < 20; i++) {
            reminders = reminderRepository.findAll();
            if (!reminders.isEmpty()) {
                break;
            }
            Thread.sleep(100);
        }

        assertFalse(reminders.isEmpty());
        ReminderEntity created = reminders.stream()
                .filter(r -> taskId.equals(r.getTaskId()))
                .findFirst()
                .orElseThrow();

        assertEquals(tenantA.getId(), created.getOrganizationId());
        assertEquals(staffUserA.getId(), created.getTargetUserId());
        assertEquals(ReminderStatus.PENDING, created.getStatus());
        assertTrue(created.getTitle().contains("File Annual Return MGT-7"));
    }

    @Test
    @DisplayName("ReminderScheduler should process due reminders, create notifications, and spawn recurring instances")
    void testReminderSchedulerExecution() {
        TenantContext.setTenantId(tenantA.getId());
        Instant pastDue = Instant.now().minus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);

        ReminderEntity dueRecurring = ReminderEntity.builder()
                .title("Recurring Tax Audit Verification")
                .description("Review test invoices")
                .reminderType(ReminderType.TASK_DUE)
                .status(ReminderStatus.PENDING)
                .priority(ReminderPriority.HIGH)
                .targetUserId(adminUserA.getId())
                .scheduledAt(pastDue)
                .recurrenceType(ReminderRecurrenceType.DAILY)
                .notificationAttempts(0)
                .build();
        dueRecurring.setOrganizationId(tenantA.getId());
        final ReminderEntity savedDueRecurring = reminderRepository.save(dueRecurring);
        TenantContext.clear();

        // Run scheduler (iterates over active orgs, sets TenantContext internally)
        reminderScheduler.processDueReminders();

        // Reload original reminder
        ReminderEntity original = reminderRepository.findById(savedDueRecurring.getId()).orElseThrow();
        assertEquals(ReminderStatus.TRIGGERED, original.getStatus());
        assertNotNull(original.getTriggeredAt());
        assertEquals(1, original.getNotificationAttempts());

        // Check spawned next recurrence
        List<ReminderEntity> allReminders = reminderRepository.findAll();
        ReminderEntity nextOccurrence = allReminders.stream()
                .filter(r -> savedDueRecurring.getId().equals(r.getParentReminderId()))
                .findFirst()
                .orElseThrow();

        assertEquals(ReminderStatus.PENDING, nextOccurrence.getStatus());
        assertEquals(tenantA.getId(), nextOccurrence.getOrganizationId());
        assertEquals(adminUserA.getId(), nextOccurrence.getTargetUserId());
        assertEquals(ReminderRecurrenceType.DAILY, nextOccurrence.getRecurrenceType());
        assertEquals(pastDue.plus(1, ChronoUnit.DAYS), nextOccurrence.getScheduledAt());

        // Check in-app notification record was created
        var notifications = notificationRepository.findAll();
        assertFalse(notifications.isEmpty());
        var notif = notifications.get(0);
        assertEquals(tenantA.getId(), notif.getOrganizationId());
        assertEquals(adminUserA.getId(), notif.getUserId());
        assertEquals("Recurring Tax Audit Verification", notif.getTitle());
    }
}
