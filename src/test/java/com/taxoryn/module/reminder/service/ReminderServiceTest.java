package com.taxoryn.module.reminder.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.notification.entity.NotificationEntity;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.reminder.dto.CreateReminderRequest;
import com.taxoryn.module.reminder.dto.ReminderDto;
import com.taxoryn.module.reminder.dto.ReminderFilterRequest;
import com.taxoryn.module.reminder.dto.UpdateReminderRequest;
import com.taxoryn.module.reminder.entity.AutomationEventType;
import com.taxoryn.module.reminder.entity.AutomationRuleEntity;
import com.taxoryn.module.reminder.entity.AutomationTargetType;
import com.taxoryn.module.reminder.entity.ReminderEntity;
import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderRecurrenceType;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.entity.ReminderType;
import com.taxoryn.module.reminder.event.TaxorynBusinessEvent;
import com.taxoryn.module.reminder.repository.ReminderRepository;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.worktemplate.entity.WorkInstanceEntity;
import com.taxoryn.module.worktemplate.repository.WorkInstanceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EngagementRepository engagementRepository;

    @Mock
    private WorkInstanceRepository workInstanceRepository;

    @InjectMocks
    private ReminderServiceImpl reminderService;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        SecurityUser principal = SecurityUser.builder()
                .userId(userId)
                .organizationId(tenantId)
                .email("practitioner@taxoryn.com")
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("TASK_CREATE", "TASK_UPDATE", "TASK_VIEW"))
                .enabled(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // 1. Manual Reminder CRUD Tests
    // =========================================================================

    @Test
    @DisplayName("Should create manual reminder with current user default target")
    void testCreateReminderDefaultTarget() {
        Instant scheduledAt = Instant.now().plus(2, ChronoUnit.DAYS);
        CreateReminderRequest request = CreateReminderRequest.builder()
                .title("Follow up with client for ITR docs")
                .description("Client needs to provide Form 16A")
                .reminderType(ReminderType.GENERAL)
                .priority(ReminderPriority.HIGH)
                .scheduledAt(scheduledAt)
                .recurrenceType(ReminderRecurrenceType.NONE)
                .build();

        when(reminderRepository.save(any(ReminderEntity.class))).thenAnswer(inv -> {
            ReminderEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ReminderDto result = reminderService.createReminder(request);

        assertNotNull(result);
        assertEquals("Follow up with client for ITR docs", result.getTitle());
        assertEquals(ReminderStatus.PENDING, result.getStatus());
        assertEquals(ReminderPriority.HIGH, result.getPriority());
        assertEquals(userId, result.getTargetUserId());
        assertEquals(tenantId, result.getOrganizationId());
        assertEquals(0, result.getNotificationAttempts());

        ArgumentCaptor<ReminderEntity> captor = ArgumentCaptor.forClass(ReminderEntity.class);
        verify(reminderRepository).save(captor.capture());
        ReminderEntity saved = captor.getValue();
        assertEquals(tenantId, saved.getOrganizationId());
        assertEquals(userId, saved.getTargetUserId());
        assertEquals(ReminderRecurrenceType.NONE, saved.getRecurrenceType());
    }

    @Test
    @DisplayName("Should create manual reminder with explicit target user")
    void testCreateReminderExplicitTarget() {
        UUID otherUser = UUID.randomUUID();
        CreateReminderRequest request = CreateReminderRequest.builder()
                .title("Review GST draft")
                .reminderType(ReminderType.TASK_DUE)
                .priority(ReminderPriority.MEDIUM)
                .targetUserId(otherUser)
                .scheduledAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        when(reminderRepository.save(any(ReminderEntity.class))).thenAnswer(inv -> {
            ReminderEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ReminderDto result = reminderService.createReminder(request);

        assertNotNull(result);
        assertEquals(otherUser, result.getTargetUserId());
    }

    @Test
    @DisplayName("Should update PENDING reminder successfully")
    void testUpdateReminderSuccess() {
        UUID reminderId = UUID.randomUUID();
        ReminderEntity existing = ReminderEntity.builder()
                .title("Initial title")
                .status(ReminderStatus.PENDING)
                .reminderType(ReminderType.GENERAL)
                .priority(ReminderPriority.LOW)
                .scheduledAt(Instant.now().plus(3, ChronoUnit.DAYS))
                .recurrenceType(ReminderRecurrenceType.NONE)
                .build();
        existing.setId(reminderId);
        existing.setOrganizationId(tenantId);

        when(reminderRepository.findByIdAndOrganizationId(reminderId, tenantId)).thenReturn(Optional.of(existing));
        when(reminderRepository.save(any(ReminderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateReminderRequest updateRequest = UpdateReminderRequest.builder()
                .title("Updated Title")
                .priority(ReminderPriority.URGENT)
                .recurrenceType(ReminderRecurrenceType.WEEKLY)
                .build();

        ReminderDto result = reminderService.updateReminder(reminderId, updateRequest);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals(ReminderPriority.URGENT, result.getPriority());
        assertEquals(ReminderRecurrenceType.WEEKLY, result.getRecurrenceType());
    }

    @Test
    @DisplayName("Should reject update on TRIGGERED or COMPLETED reminder")
    void testUpdateReminderNonPendingThrowsException() {
        UUID reminderId = UUID.randomUUID();
        ReminderEntity triggered = ReminderEntity.builder()
                .title("Triggered reminder")
                .status(ReminderStatus.TRIGGERED)
                .build();
        triggered.setId(reminderId);
        triggered.setOrganizationId(tenantId);

        when(reminderRepository.findByIdAndOrganizationId(reminderId, tenantId)).thenReturn(Optional.of(triggered));

        UpdateReminderRequest updateRequest = UpdateReminderRequest.builder()
                .title("New Title")
                .build();

        assertThrows(IllegalStateException.class, () -> reminderService.updateReminder(reminderId, updateRequest));
    }

    @Test
    @DisplayName("Should retrieve reminder by ID with organization verification")
    void testGetReminderById() {
        UUID reminderId = UUID.randomUUID();
        ReminderEntity entity = ReminderEntity.builder()
                .title("Sample Reminder")
                .status(ReminderStatus.PENDING)
                .targetUserId(userId)
                .build();
        entity.setId(reminderId);
        entity.setOrganizationId(tenantId);

        UserEntity mockUser = UserEntity.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@taxoryn.com")
                .build();
        mockUser.setId(userId);

        when(reminderRepository.findByIdAndOrganizationId(reminderId, tenantId)).thenReturn(Optional.of(entity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        ReminderDto result = reminderService.getReminderById(reminderId);

        assertNotNull(result);
        assertEquals(reminderId, result.getId());
        assertEquals("John Doe", result.getTargetUserName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when reminder not found in tenant")
    void testGetReminderByIdNotFound() {
        UUID reminderId = UUID.randomUUID();
        when(reminderRepository.findByIdAndOrganizationId(reminderId, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reminderService.getReminderById(reminderId));
    }

    // =========================================================================
    // 2. Lifecycle Transitions: Complete & Cancel
    // =========================================================================

    @Test
    @DisplayName("Should complete PENDING or TRIGGERED reminder")
    void testCompleteReminder() {
        UUID reminderId = UUID.randomUUID();
        ReminderEntity entity = ReminderEntity.builder()
                .title("Task reminder")
                .status(ReminderStatus.TRIGGERED)
                .build();
        entity.setId(reminderId);
        entity.setOrganizationId(tenantId);

        when(reminderRepository.findByIdAndOrganizationId(reminderId, tenantId)).thenReturn(Optional.of(entity));
        when(reminderRepository.save(any(ReminderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ReminderDto result = reminderService.completeReminder(reminderId);

        assertNotNull(result);
        assertEquals(ReminderStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    @DisplayName("Should cancel PENDING or TRIGGERED reminder")
    void testCancelReminder() {
        UUID reminderId = UUID.randomUUID();
        ReminderEntity entity = ReminderEntity.builder()
                .title("Task reminder")
                .status(ReminderStatus.PENDING)
                .build();
        entity.setId(reminderId);
        entity.setOrganizationId(tenantId);

        when(reminderRepository.findByIdAndOrganizationId(reminderId, tenantId)).thenReturn(Optional.of(entity));
        when(reminderRepository.save(any(ReminderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ReminderDto result = reminderService.cancelReminder(reminderId);

        assertNotNull(result);
        assertEquals(ReminderStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCancelledAt());
    }

    @Test
    @DisplayName("Should cancel pending reminders for a completed task")
    void testCancelRemindersForTask() {
        UUID taskId = UUID.randomUUID();
        when(reminderRepository.cancelPendingRemindersForTask(eq(tenantId), eq(taskId), any(Instant.class)))
                .thenReturn(2);

        int cancelled = reminderService.cancelRemindersForTask(tenantId, taskId);

        assertEquals(2, cancelled);
        verify(reminderRepository).cancelPendingRemindersForTask(eq(tenantId), eq(taskId), any(Instant.class));
    }

    // =========================================================================
    // 3. Automated Reminder Creation & Idempotency
    // =========================================================================

    @Test
    @DisplayName("Should create automated reminder with deterministic idempotency key")
    void testCreateAutomatedReminderSuccess() {
        UUID taskId = UUID.randomUUID();
        LocalDate dueDate = LocalDate.of(2026, 10, 15);

        TaxorynBusinessEvent event = TaxorynBusinessEvent.builder()
                .eventType(AutomationEventType.TASK_DUE)
                .organizationId(tenantId)
                .taskId(taskId)
                .assignedUserId(userId)
                .entityTitle("File GSTR-1 for Oct")
                .dueDate(dueDate)
                .build();

        UUID ruleId = UUID.randomUUID();
        AutomationRuleEntity rule = AutomationRuleEntity.builder()
                .name("Task Due Warning")
                .eventType(AutomationEventType.TASK_DUE)
                .daysOffset(-2) // 2 days before due date
                .targetType(AutomationTargetType.TASK_ASSIGNEE)
                .enabled(true)
                .build();
        rule.setId(ruleId);

        String expectedScheduledDate = "2026-10-13";
        String expectedKey = ruleId + "::TASK::" + taskId + "::" + expectedScheduledDate;

        when(reminderRepository.existsByIdempotencyKey(expectedKey)).thenReturn(false);
        when(reminderRepository.save(any(ReminderEntity.class))).thenAnswer(inv -> {
            ReminderEntity r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ReminderDto result = reminderService.createAutomatedReminder(event, rule);

        assertNotNull(result);
        assertEquals(expectedKey, result.getIdempotencyKey());
        assertEquals(ReminderStatus.PENDING, result.getStatus());
        assertEquals("Task Due Soon: File GSTR-1 for Oct", result.getTitle());
        assertEquals(userId, result.getTargetUserId());

        ArgumentCaptor<ReminderEntity> captor = ArgumentCaptor.forClass(ReminderEntity.class);
        verify(reminderRepository).save(captor.capture());
        ReminderEntity saved = captor.getValue();
        assertEquals(expectedKey, saved.getIdempotencyKey());
        assertEquals(ruleId, saved.getAutomationRuleId());
        assertEquals(dueDate.minusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant(), saved.getScheduledAt());
    }

    @Test
    @DisplayName("Should skip duplicate automated reminder when idempotency key exists")
    void testCreateAutomatedReminderDuplicateSkipped() {
        UUID taskId = UUID.randomUUID();
        LocalDate dueDate = LocalDate.of(2026, 10, 15);

        TaxorynBusinessEvent event = TaxorynBusinessEvent.builder()
                .eventType(AutomationEventType.TASK_DUE)
                .organizationId(tenantId)
                .taskId(taskId)
                .assignedUserId(userId)
                .dueDate(dueDate)
                .build();

        UUID ruleId = UUID.randomUUID();
        AutomationRuleEntity rule = AutomationRuleEntity.builder()
                .name("Task Due Warning")
                .eventType(AutomationEventType.TASK_DUE)
                .daysOffset(-1)
                .targetType(AutomationTargetType.TASK_ASSIGNEE)
                .build();
        rule.setId(ruleId);

        when(reminderRepository.existsByIdempotencyKey(anyString())).thenReturn(true);

        ReminderDto result = reminderService.createAutomatedReminder(event, rule);

        assertNull(result);
        verify(reminderRepository, never()).save(any(ReminderEntity.class));
    }

    // =========================================================================
    // 4. Target User Resolution Tests
    // =========================================================================

    @Test
    @DisplayName("Should resolve ENGAGEMENT_OWNER target user accurately")
    void testResolveTargetUserEngagementOwner() {
        UUID engagementId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();

        EngagementEntity mockEng = EngagementEntity.builder()
                .assignedUserId(ownerUserId)
                .name("GST Annual Retainer")
                .build();
        mockEng.setId(engagementId);

        when(engagementRepository.findById(engagementId)).thenReturn(Optional.of(mockEng));
        when(reminderRepository.existsByIdempotencyKey(anyString())).thenReturn(false);
        when(reminderRepository.save(any(ReminderEntity.class))).thenAnswer(inv -> {
            ReminderEntity r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        TaxorynBusinessEvent event = TaxorynBusinessEvent.builder()
                .eventType(AutomationEventType.TASK_DUE)
                .organizationId(tenantId)
                .engagementId(engagementId)
                .assignedUserId(userId) // task assignee
                .dueDate(LocalDate.now())
                .build();

        AutomationRuleEntity rule = AutomationRuleEntity.builder()
                .name("Engagement Owner Alert")
                .eventType(AutomationEventType.TASK_DUE)
                .daysOffset(0)
                .targetType(AutomationTargetType.ENGAGEMENT_OWNER)
                .build();
        rule.setId(UUID.randomUUID());

        ReminderDto result = reminderService.createAutomatedReminder(event, rule);

        assertNotNull(result);
        assertEquals(ownerUserId, result.getTargetUserId());
    }

    @Test
    @DisplayName("Should resolve WORK_INSTANCE_ASSIGNEE target user accurately")
    void testResolveTargetUserWorkInstanceAssignee() {
        UUID workInstanceId = UUID.randomUUID();
        UUID workAssigneeId = UUID.randomUUID();

        WorkInstanceEntity mockWork = WorkInstanceEntity.builder()
                .assignedUserId(workAssigneeId)
                .title("ITR Filing Instance")
                .build();
        mockWork.setId(workInstanceId);

        when(workInstanceRepository.findById(workInstanceId)).thenReturn(Optional.of(mockWork));
        when(reminderRepository.existsByIdempotencyKey(anyString())).thenReturn(false);
        when(reminderRepository.save(any(ReminderEntity.class))).thenAnswer(inv -> {
            ReminderEntity r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        TaxorynBusinessEvent event = TaxorynBusinessEvent.builder()
                .eventType(AutomationEventType.WORK_INSTANCE_DUE)
                .organizationId(tenantId)
                .workInstanceId(workInstanceId)
                .assignedUserId(userId)
                .dueDate(LocalDate.now())
                .build();

        AutomationRuleEntity rule = AutomationRuleEntity.builder()
                .name("Work Instance Assignee Alert")
                .eventType(AutomationEventType.WORK_INSTANCE_DUE)
                .daysOffset(0)
                .targetType(AutomationTargetType.WORK_INSTANCE_ASSIGNEE)
                .build();
        rule.setId(UUID.randomUUID());

        ReminderDto result = reminderService.createAutomatedReminder(event, rule);

        assertNotNull(result);
        assertEquals(workAssigneeId, result.getTargetUserId());
    }

    // =========================================================================
    // 5. Recurrence Calculation & Edge Cases
    // =========================================================================

    @Test
    @DisplayName("Should calculate daily recurrence next occurrence")
    void testCalculateNextOccurrenceDaily() {
        Instant initial = ZonedDateTime.of(2026, 10, 15, 9, 0, 0, 0, ZoneOffset.UTC).toInstant();
        Instant next = ReminderServiceImpl.calculateNextOccurrence(initial, ReminderRecurrenceType.DAILY, ZoneOffset.UTC);

        Instant expected = ZonedDateTime.of(2026, 10, 16, 9, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertEquals(expected, next);
    }

    @Test
    @DisplayName("Should calculate weekly recurrence next occurrence")
    void testCalculateNextOccurrenceWeekly() {
        Instant initial = ZonedDateTime.of(2026, 10, 15, 9, 0, 0, 0, ZoneOffset.UTC).toInstant();
        Instant next = ReminderServiceImpl.calculateNextOccurrence(initial, ReminderRecurrenceType.WEEKLY, ZoneOffset.UTC);

        Instant expected = ZonedDateTime.of(2026, 10, 22, 9, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertEquals(expected, next);
    }

    @Test
    @DisplayName("Should calculate monthly recurrence with safe month-end date clamping (Jan 31 -> Feb 28/29)")
    void testCalculateNextOccurrenceMonthlyMonthEnd() {
        // Non leap year test: 2025 Jan 31 -> Feb 28
        Instant jan31 = ZonedDateTime.of(2025, 1, 31, 10, 0, 0, 0, ZoneOffset.UTC).toInstant();
        Instant nextFeb = ReminderServiceImpl.calculateNextOccurrence(jan31, ReminderRecurrenceType.MONTHLY, ZoneOffset.UTC);
        Instant expectedFeb = ZonedDateTime.of(2025, 2, 28, 10, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertEquals(expectedFeb, nextFeb);

        // Leap year test: 2024 Jan 31 -> Feb 29
        Instant jan31Leap = ZonedDateTime.of(2024, 1, 31, 10, 0, 0, 0, ZoneOffset.UTC).toInstant();
        Instant nextFebLeap = ReminderServiceImpl.calculateNextOccurrence(jan31Leap, ReminderRecurrenceType.MONTHLY, ZoneOffset.UTC);
        Instant expectedFebLeap = ZonedDateTime.of(2024, 2, 29, 10, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertEquals(expectedFebLeap, nextFebLeap);

        // 31-day month to 30-day month test: Aug 31 -> Sep 30
        Instant aug31 = ZonedDateTime.of(2026, 8, 31, 10, 0, 0, 0, ZoneOffset.UTC).toInstant();
        Instant nextSep = ReminderServiceImpl.calculateNextOccurrence(aug31, ReminderRecurrenceType.MONTHLY, ZoneOffset.UTC);
        Instant expectedSep = ZonedDateTime.of(2026, 9, 30, 10, 0, 0, 0, ZoneOffset.UTC).toInstant();
        assertEquals(expectedSep, nextSep);
    }

    // =========================================================================
    // 6. Scheduler Triggering, Notification Retries, and Recurring Spawn
    // =========================================================================

    @Test
    @DisplayName("Should trigger due reminder, dispatch notification, and spawn next occurrence when recurring")
    void testProcessDueRemindersSuccessfulTriggerAndRecurrence() {
        UUID reminderId = UUID.randomUUID();
        Instant scheduledAt = Instant.now().minus(1, ChronoUnit.HOURS);

        ReminderEntity reminder = ReminderEntity.builder()
                .title("Weekly TDS check")
                .status(ReminderStatus.PENDING)
                .reminderType(ReminderType.TASK_DUE)
                .priority(ReminderPriority.HIGH)
                .targetUserId(userId)
                .scheduledAt(scheduledAt)
                .recurrenceType(ReminderRecurrenceType.WEEKLY)
                .notificationAttempts(0)
                .build();
        reminder.setId(reminderId);
        reminder.setOrganizationId(tenantId);

        when(reminderRepository.findDueForOrganization(eq(tenantId), any(Instant.class), eq(3), any(Pageable.class)))
                .thenReturn(List.of(reminder));
        when(reminderRepository.findAllByOrganizationIdAndStatusOrderByScheduledAtAsc(eq(tenantId), eq(ReminderStatus.PENDING)))
                .thenReturn(Collections.emptyList());

        when(reminderRepository.save(any(ReminderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = reminderService.processAllDueReminders(tenantId, 50);

        assertEquals(1, count);
        assertEquals(ReminderStatus.TRIGGERED, reminder.getStatus());
        assertNotNull(reminder.getTriggeredAt());
        assertEquals(1, reminder.getNotificationAttempts());
        assertNull(reminder.getLastError());

        // Verify notification was sent
        verify(notificationService).notify(
                eq(tenantId), eq(userId), isNull(), any(), any(), any(), any(), any(),
                eq("Weekly TDS check"), anyString(), anySet(), anyString(), anyString(), isNull()
        );

        // Verify next occurrence was saved with parentReminderId
        ArgumentCaptor<ReminderEntity> captor = ArgumentCaptor.forClass(ReminderEntity.class);
        verify(reminderRepository, atLeast(2)).save(captor.capture());

        List<ReminderEntity> savedEntities = captor.getAllValues();
        ReminderEntity spawned = savedEntities.get(savedEntities.size() - 1);
        assertEquals(ReminderStatus.PENDING, spawned.getStatus());
        assertEquals(reminderId, spawned.getParentReminderId());
        assertEquals(ReminderRecurrenceType.WEEKLY, spawned.getRecurrenceType());
        assertEquals(scheduledAt.plus(7, ChronoUnit.DAYS), spawned.getScheduledAt());
    }

    @Test
    @DisplayName("Should track notification retry attempt on failure and remain PENDING when attempts < 3")
    void testProcessDueRemindersNotificationFailureRetry() {
        UUID reminderId = UUID.randomUUID();
        ReminderEntity reminder = ReminderEntity.builder()
                .title("Failing notification reminder")
                .status(ReminderStatus.PENDING)
                .targetUserId(userId)
                .scheduledAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .recurrenceType(ReminderRecurrenceType.NONE)
                .notificationAttempts(1)
                .build();
        reminder.setId(reminderId);
        reminder.setOrganizationId(tenantId);

        when(reminderRepository.findDueForOrganization(eq(tenantId), any(Instant.class), eq(3), any(Pageable.class)))
                .thenReturn(List.of(reminder));
        when(reminderRepository.save(any(ReminderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        doThrow(new RuntimeException("Notification service timeout"))
                .when(notificationService).notify(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        int count = reminderService.processAllDueReminders(tenantId, 50);

        assertEquals(0, count);
        assertEquals(ReminderStatus.PENDING, reminder.getStatus()); // Stays PENDING for next attempt
        assertEquals(2, reminder.getNotificationAttempts());
        assertNotNull(reminder.getLastAttemptAt());
        assertEquals("Notification service timeout", reminder.getLastError());
        assertNull(reminder.getTriggeredAt());
    }

    @Test
    @DisplayName("Should mark reminder as TRIGGERED with lastError when max attempts (3) reached")
    void testProcessDueRemindersMaxAttemptsReached() {
        UUID reminderId = UUID.randomUUID();
        ReminderEntity reminder = ReminderEntity.builder()
                .title("Max attempt reminder")
                .status(ReminderStatus.PENDING)
                .targetUserId(userId)
                .scheduledAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .recurrenceType(ReminderRecurrenceType.NONE)
                .notificationAttempts(2) // 3rd attempt upcoming
                .build();
        reminder.setId(reminderId);
        reminder.setOrganizationId(tenantId);

        when(reminderRepository.findDueForOrganization(eq(tenantId), any(Instant.class), eq(3), any(Pageable.class)))
                .thenReturn(List.of(reminder));
        when(reminderRepository.save(any(ReminderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        doThrow(new RuntimeException("Permanent delivery failure"))
                .when(notificationService).notify(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        int count = reminderService.processAllDueReminders(tenantId, 50);

        assertEquals(0, count);
        assertEquals(ReminderStatus.TRIGGERED, reminder.getStatus()); // Marked TRIGGERED on 3rd attempt
        assertEquals(3, reminder.getNotificationAttempts());
        assertNotNull(reminder.getLastAttemptAt());
        assertNotNull(reminder.getTriggeredAt());
        assertEquals("Permanent delivery failure", reminder.getLastError());
    }
}
