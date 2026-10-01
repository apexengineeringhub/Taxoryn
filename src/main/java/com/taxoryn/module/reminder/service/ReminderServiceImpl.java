package com.taxoryn.module.reminder.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.notification.entity.NotificationEntity.Category;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.entity.NotificationEntity.Severity;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.reminder.dto.CreateReminderRequest;
import com.taxoryn.module.reminder.dto.ReminderDto;
import com.taxoryn.module.reminder.dto.ReminderFilterRequest;
import com.taxoryn.module.reminder.dto.UpdateReminderRequest;
import com.taxoryn.module.reminder.entity.AutomationRuleEntity;
import com.taxoryn.module.reminder.entity.ReminderEntity;
import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderRecurrenceType;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.entity.ReminderType;
import com.taxoryn.module.reminder.event.TaxorynBusinessEvent;
import com.taxoryn.module.reminder.repository.ReminderRepository;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.task.repository.TaskRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core implementation of {@link ReminderService}.
 *
 * <p>Handles:
 * <ul>
 *   <li>Manual reminder CRUD (REST API path)</li>
 *   <li>Automation-generated reminders with idempotency key dedup</li>
 *   <li>Scheduler processing: PENDING → TRIGGERED with notification creation</li>
 *   <li>Task lifecycle hooks: cancel pending reminders when task is completed/cancelled</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    private static final DateTimeFormatter IDEM_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // =========================================================================
    // CRUD — Manual Reminders (REST API)
    // =========================================================================

    @Override
    @Transactional
    public ReminderDto createReminder(CreateReminderRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        UUID targetUserId = request.getTargetUserId() != null ? request.getTargetUserId() : currentUserId;

        ReminderEntity reminder = ReminderEntity.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .reminderType(request.getReminderType())
                .priority(request.getPriority())
                .status(ReminderStatus.PENDING)
                .targetUserId(targetUserId)
                .clientId(request.getClientId())
                .engagementId(request.getEngagementId())
                .workInstanceId(request.getWorkInstanceId())
                .taskId(request.getTaskId())
                .scheduledAt(request.getScheduledAt())
                .recurrenceType(request.getRecurrenceType())
                .notes(request.getNotes())
                .build();
        reminder.setOrganizationId(orgId);

        ReminderEntity saved = reminderRepository.save(reminder);
        log.info("Created manual reminder {} for user {} in org {}", saved.getId(), targetUserId, orgId);
        return enrichDto(saved);
    }

    @Override
    @Transactional
    public ReminderDto updateReminder(UUID reminderId, UpdateReminderRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        ReminderEntity reminder = reminderRepository.findByIdAndOrganizationId(reminderId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", "id", reminderId));

        if (reminder.getStatus() != ReminderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING reminders can be updated. Current status: " + reminder.getStatus());
        }

        if (StringUtils.hasText(request.getTitle()))    reminder.setTitle(request.getTitle().trim());
        if (request.getDescription() != null)           reminder.setDescription(request.getDescription());
        if (request.getReminderType() != null)           reminder.setReminderType(request.getReminderType());
        if (request.getPriority() != null)               reminder.setPriority(request.getPriority());
        if (request.getScheduledAt() != null)            reminder.setScheduledAt(request.getScheduledAt());
        if (request.getRecurrenceType() != null)         reminder.setRecurrenceType(request.getRecurrenceType());
        if (request.getTargetUserId() != null)           reminder.setTargetUserId(request.getTargetUserId());
        if (request.getClientId() != null)               reminder.setClientId(request.getClientId());
        if (request.getEngagementId() != null)           reminder.setEngagementId(request.getEngagementId());
        if (request.getWorkInstanceId() != null)         reminder.setWorkInstanceId(request.getWorkInstanceId());
        if (request.getTaskId() != null)                 reminder.setTaskId(request.getTaskId());
        if (request.getNotes() != null)                  reminder.setNotes(request.getNotes());

        ReminderEntity saved = reminderRepository.save(reminder);
        log.info("Updated reminder {} in org {}", saved.getId(), orgId);
        return enrichDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReminderDto getReminderById(UUID reminderId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        ReminderEntity reminder = reminderRepository.findByIdAndOrganizationId(reminderId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", "id", reminderId));
        return enrichDto(reminder);
    }

    // =========================================================================
    // List queries
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReminderDto> getMyReminders(ReminderFilterRequest filter) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();
        return queryReminders(orgId, userId, filter);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReminderDto> getTeamReminders(ReminderFilterRequest filter) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        UUID targetUserId = filter.getTargetUserId(); // null = all team members
        return queryReminders(orgId, targetUserId, filter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReminderDto> getRemindersForTask(UUID taskId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        return reminderRepository.findAllByOrganizationIdAndTaskIdAndStatusIn(
                        orgId, taskId, Set.of(ReminderStatus.PENDING, ReminderStatus.TRIGGERED))
                .stream()
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    private PagedResponse<ReminderDto> queryReminders(UUID orgId, UUID targetUserId, ReminderFilterRequest filter) {
        Specification<ReminderEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), orgId));

            if (targetUserId != null) {
                predicates.add(cb.equal(root.get("targetUserId"), targetUserId));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getReminderType() != null) {
                predicates.add(cb.equal(root.get("reminderType"), filter.getReminderType()));
            }
            if (filter.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
            }
            if (filter.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filter.getClientId()));
            }
            if (filter.getEngagementId() != null) {
                predicates.add(cb.equal(root.get("engagementId"), filter.getEngagementId()));
            }
            if (filter.getWorkInstanceId() != null) {
                predicates.add(cb.equal(root.get("workInstanceId"), filter.getWorkInstanceId()));
            }
            if (filter.getTaskId() != null) {
                predicates.add(cb.equal(root.get("taskId"), filter.getTaskId()));
            }
            if (filter.getScheduledFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledAt"), filter.getScheduledFrom()));
            }
            if (filter.getScheduledTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("scheduledAt"), filter.getScheduledTo()));
            }
            if (Boolean.TRUE.equals(filter.getOverdueOnly())) {
                predicates.add(cb.equal(root.get("status"), ReminderStatus.PENDING));
                predicates.add(cb.lessThan(root.get("scheduledAt"), Instant.now()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.ASC, "scheduledAt")
        );

        Page<ReminderEntity> page = reminderRepository.findAll(spec, pageRequest);
        return PagedResponse.of(page, this::enrichDto);
    }

    // =========================================================================
    // Lifecycle transitions
    // =========================================================================

    @Override
    @Transactional
    public ReminderDto completeReminder(UUID reminderId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        ReminderEntity reminder = reminderRepository.findByIdAndOrganizationId(reminderId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", "id", reminderId));

        if (reminder.getStatus() != ReminderStatus.PENDING && reminder.getStatus() != ReminderStatus.TRIGGERED) {
            throw new IllegalStateException("Only PENDING or TRIGGERED reminders can be completed. Current: " + reminder.getStatus());
        }

        reminder.setStatus(ReminderStatus.COMPLETED);
        reminder.setCompletedAt(Instant.now());
        ReminderEntity saved = reminderRepository.save(reminder);
        log.info("Completed reminder {} in org {}", reminderId, orgId);
        return enrichDto(saved);
    }

    @Override
    @Transactional
    public ReminderDto cancelReminder(UUID reminderId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        ReminderEntity reminder = reminderRepository.findByIdAndOrganizationId(reminderId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", "id", reminderId));

        if (reminder.getStatus() != ReminderStatus.PENDING && reminder.getStatus() != ReminderStatus.TRIGGERED) {
            throw new IllegalStateException("Only PENDING or TRIGGERED reminders can be cancelled. Current: " + reminder.getStatus());
        }

        reminder.setStatus(ReminderStatus.CANCELLED);
        reminder.setCancelledAt(Instant.now());
        ReminderEntity saved = reminderRepository.save(reminder);
        log.info("Cancelled reminder {} in org {}", reminderId, orgId);
        return enrichDto(saved);
    }

    @Override
    @Transactional
    public int cancelRemindersForTask(UUID organizationId, UUID taskId) {
        int count = reminderRepository.cancelPendingRemindersForTask(organizationId, taskId, Instant.now());
        if (count > 0) {
            log.info("Cancelled {} pending reminders for task {} in org {}", count, taskId, organizationId);
        }
        return count;
    }

    // =========================================================================
    // Automation integration — called by ReminderEventListener
    // =========================================================================

    @Override
    @Transactional
    public ReminderDto createAutomatedReminder(TaxorynBusinessEvent event, AutomationRuleEntity rule) {
        // Compute the scheduled date: due date + offset
        LocalDate eventDate = event.getDueDate();
        if (eventDate == null) {
            eventDate = LocalDate.now();
        }
        LocalDate scheduledDate = eventDate.plusDays(rule.getDaysOffset());
        Instant scheduledAt = scheduledDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        // If the scheduled date is in the past, fire immediately (scheduler will pick it up)
        // but still persist for audit trail

        // Build idempotency key: ruleId::TASK::taskId::2026-10-01
        String referenceType = resolveReferenceType(event);
        String referenceId = resolveReferenceId(event);
        String idempotencyKey = rule.getId() + "::" + referenceType + "::" + referenceId + "::" + scheduledDate.format(IDEM_DATE_FMT);

        // Idempotency check — if this exact reminder was already created, skip
        if (reminderRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.debug("Skipping duplicate automated reminder: {}", idempotencyKey);
            return null;
        }

        // Resolve target user
        UUID targetUserId = resolveTargetUser(event, rule);

        // Map event type to ReminderType
        ReminderType reminderType = mapEventToReminderType(event);

        // Build the reminder title
        String title = buildAutomatedTitle(event, rule);

        ReminderEntity reminder = ReminderEntity.builder()
                .title(title)
                .description("Auto-generated by rule: " + rule.getName())
                .reminderType(reminderType)
                .status(ReminderStatus.PENDING)
                .priority(ReminderPriority.MEDIUM)
                .targetUserId(targetUserId)
                .clientId(event.getClientId())
                .engagementId(event.getEngagementId())
                .workInstanceId(event.getWorkInstanceId())
                .taskId(event.getTaskId())
                .automationRuleId(rule.getId())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .idempotencyKey(idempotencyKey)
                .scheduledAt(scheduledAt)
                .recurrenceType(ReminderRecurrenceType.NONE)
                .build();
        reminder.setOrganizationId(event.getOrganizationId());

        ReminderEntity saved = reminderRepository.save(reminder);
        log.info("Created automated reminder {} via rule '{}' for user {} in org {} [key={}]",
                saved.getId(), rule.getName(), targetUserId, event.getOrganizationId(), idempotencyKey);
        return enrichDto(saved);
    }

    // =========================================================================
    // Scheduler integration — called by ReminderScheduler
    // =========================================================================

    @Override
    @Transactional
    public int processAllDueReminders(UUID organizationId) {
        List<ReminderEntity> dueReminders;
        Instant now = Instant.now();

        if (organizationId != null) {
            dueReminders = reminderRepository.findDueForOrganization(organizationId, now);
        } else {
            dueReminders = reminderRepository.findAllDueForProcessing(now);
        }

        int triggered = 0;
        for (ReminderEntity reminder : dueReminders) {
            try {
                triggerReminder(reminder);
                triggered++;
            } catch (Exception ex) {
                log.error("Failed to trigger reminder {}: {}", reminder.getId(), ex.getMessage(), ex);
            }
        }
        return triggered;
    }

    @Override
    @Transactional(readOnly = true)
    public long countOverdueForCurrentUser() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        UUID userId = SecurityUtils.getCurrentUserId();
        return reminderRepository.countByOrganizationIdAndTargetUserIdAndStatusAndScheduledAtBefore(
                orgId, userId, ReminderStatus.PENDING, Instant.now());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Triggers a single PENDING reminder:
     * 1. Creates an in-app notification via NotificationService
     * 2. Marks the reminder as TRIGGERED
     */
    private void triggerReminder(ReminderEntity reminder) {
        // Determine notification type based on reminder type
        NotificationType notificationType = mapReminderToNotificationType(reminder);
        Severity severity = reminder.isOverdue() ? Severity.ACTION_REQUIRED : Severity.WARNING;

        String entityType = reminder.getReferenceType() != null ? reminder.getReferenceType() : "REMINDER";
        String entityId = reminder.getReferenceId() != null ? reminder.getReferenceId() : reminder.getId().toString();

        String actionUrl = buildActionUrl(reminder);

        try {
            notificationService.notify(
                    reminder.getOrganizationId(),
                    reminder.getTargetUserId(),
                    reminder.getClientId(),
                    notificationType,
                    severity,
                    Category.REMINDER,
                    entityType,
                    entityId,
                    reminder.getTitle(),
                    reminder.getDescription() != null ? reminder.getDescription() : reminder.getTitle(),
                    Set.of(NotificationChannel.IN_APP),
                    actionUrl,
                    "{\"reminderId\":\"" + reminder.getId() + "\"}",
                    null // expiresAt
            );
        } catch (Exception ex) {
            log.error("Failed to send notification for reminder {}: {}", reminder.getId(), ex.getMessage(), ex);
            // Still mark as TRIGGERED — notification failure shouldn't block lifecycle
        }

        reminder.setStatus(ReminderStatus.TRIGGERED);
        reminder.setTriggeredAt(Instant.now());
        reminderRepository.save(reminder);
    }

    private NotificationType mapReminderToNotificationType(ReminderEntity reminder) {
        if (reminder.isOverdue()) {
            return NotificationType.REMINDER_OVERDUE;
        }
        return switch (reminder.getReminderType()) {
            case TASK_DUE -> NotificationType.REMINDER_DUE;
            case TASK_OVERDUE -> NotificationType.REMINDER_OVERDUE;
            default -> NotificationType.REMINDER_TRIGGERED;
        };
    }

    private String buildActionUrl(ReminderEntity reminder) {
        if (reminder.getTaskId() != null) {
            return "/tasks/" + reminder.getTaskId();
        }
        if (reminder.getWorkInstanceId() != null) {
            return "/work-instances/" + reminder.getWorkInstanceId();
        }
        if (reminder.getEngagementId() != null) {
            return "/engagements/" + reminder.getEngagementId();
        }
        return "/reminders";
    }

    private String resolveReferenceType(TaxorynBusinessEvent event) {
        if (event.getTaskId() != null) return "TASK";
        if (event.getWorkInstanceId() != null) return "WORK_INSTANCE";
        return "GENERAL";
    }

    private String resolveReferenceId(TaxorynBusinessEvent event) {
        if (event.getTaskId() != null) return event.getTaskId().toString();
        if (event.getWorkInstanceId() != null) return event.getWorkInstanceId().toString();
        return event.getOrganizationId().toString();
    }

    private UUID resolveTargetUser(TaxorynBusinessEvent event, AutomationRuleEntity rule) {
        // For P0.5 all target types resolve to the assigned user from the event
        // Future: ENGAGEMENT_OWNER, SPECIFIC_USER can be implemented
        return event.getAssignedUserId();
    }

    private ReminderType mapEventToReminderType(TaxorynBusinessEvent event) {
        return switch (event.getEventType()) {
            case TASK_DUE, WORK_INSTANCE_DUE -> ReminderType.TASK_DUE;
            case TASK_OVERDUE -> ReminderType.TASK_OVERDUE;
            case DOCUMENT_UPLOADED -> ReminderType.DOCUMENT_COLLECTION;
            default -> ReminderType.GENERAL;
        };
    }

    private String buildAutomatedTitle(TaxorynBusinessEvent event, AutomationRuleEntity rule) {
        String entityTitle = event.getEntityTitle() != null ? event.getEntityTitle() : "Untitled";
        return switch (event.getEventType()) {
            case TASK_DUE -> "Task Due Soon: " + entityTitle;
            case TASK_OVERDUE -> "Task Overdue: " + entityTitle;
            case TASK_CREATED -> "New Task: " + entityTitle;
            case TASK_ASSIGNED -> "Task Assigned: " + entityTitle;
            case TASK_COMPLETED -> "Task Completed: " + entityTitle;
            case WORK_INSTANCE_CREATED -> "Work Instance Created: " + entityTitle;
            case WORK_INSTANCE_DUE -> "Work Instance Due: " + entityTitle;
            case DOCUMENT_UPLOADED -> "Document Uploaded: " + entityTitle;
        };
    }

    /**
     * Enriches a ReminderDto with display names from related entities.
     * Uses safe optional lookups — missing relations result in null display names.
     */
    private ReminderDto enrichDto(ReminderEntity entity) {
        ReminderDto dto = ReminderDto.fromEntity(entity);

        if (entity.getTargetUserId() != null) {
            userRepository.findById(entity.getTargetUserId())
                    .ifPresent(u -> dto.setTargetUserName(u.getFullName()));
        }

        if (entity.getTaskId() != null) {
            taskRepository.findById(entity.getTaskId())
                    .ifPresent(t -> dto.setTaskTitle(t.getTitle()));
        }

        return dto;
    }
}
