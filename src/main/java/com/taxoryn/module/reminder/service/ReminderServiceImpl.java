package com.taxoryn.module.reminder.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.repository.EngagementRepository;
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
import com.taxoryn.module.reminder.entity.AutomationTargetType;
import com.taxoryn.module.reminder.entity.ReminderEntity;
import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderRecurrenceType;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.entity.ReminderType;
import com.taxoryn.module.reminder.event.TaxorynBusinessEvent;
import com.taxoryn.module.reminder.repository.ReminderRepository;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.worktemplate.entity.WorkInstanceEntity;
import com.taxoryn.module.worktemplate.repository.WorkInstanceRepository;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hardened core implementation of {@link ReminderService}.
 *
 * <p>Handles:
 * <ul>
 *   <li>Manual reminder CRUD (REST API path) with tenant isolation</li>
 *   <li>Automation-generated reminders with deterministic idempotency keys</li>
 *   <li>Scheduler processing: PENDING → TRIGGERED with notification retry tracking</li>
 *   <li>Recurring reminders: automatic calculation and spawning of next occurrence</li>
 *   <li>Target user resolution for all AutomationTargetType values</li>
 *   <li>Task lifecycle hooks: cancel pending reminders when task is completed/cancelled</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    public static final int MAX_NOTIFICATION_ATTEMPTS = 3;
    private static final DateTimeFormatter IDEM_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final EngagementRepository engagementRepository;
    private final WorkInstanceRepository workInstanceRepository;

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
                .recurrenceType(request.getRecurrenceType() != null ? request.getRecurrenceType() : ReminderRecurrenceType.NONE)
                .notificationAttempts(0)
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
        LocalDate eventDate = event.getDueDate();
        if (eventDate == null) {
            eventDate = LocalDate.now();
        }
        LocalDate scheduledDate = eventDate.plusDays(rule.getDaysOffset());
        Instant scheduledAt = scheduledDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        String referenceType = resolveReferenceType(event);
        String referenceId = resolveReferenceId(event);
        String idempotencyKey = rule.getId() + "::" + referenceType + "::" + referenceId + "::" + scheduledDate.format(IDEM_DATE_FMT);

        if (reminderRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.debug("Skipping duplicate automated reminder: {}", idempotencyKey);
            return null;
        }

        UUID targetUserId = resolveTargetUser(event, rule);
        ReminderType reminderType = mapEventToReminderType(event);
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
                .notificationAttempts(0)
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
        return processAllDueReminders(organizationId, 50);
    }

    @Override
    @Transactional
    public int processAllDueReminders(UUID organizationId, int batchSize) {
        List<ReminderEntity> dueReminders;
        Instant now = Instant.now();
        PageRequest pageRequest = PageRequest.of(0, Math.max(1, batchSize));

        if (organizationId != null) {
            dueReminders = reminderRepository.findDueForOrganization(organizationId, now, MAX_NOTIFICATION_ATTEMPTS, pageRequest);
        } else {
            dueReminders = reminderRepository.findAllDueForProcessing(now, MAX_NOTIFICATION_ATTEMPTS, pageRequest);
        }

        int triggered = 0;
        for (ReminderEntity reminder : dueReminders) {
            try {
                triggerReminder(reminder);
                if (reminder.getStatus() == ReminderStatus.TRIGGERED && reminder.getLastError() == null) {
                    triggered++;
                }
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
    // Recurrence & Trigger Helpers
    // =========================================================================

    /**
     * Triggers a single PENDING reminder:
     * 1. Updates attempt count and timestamp.
     * 2. Attempts in-app notification via NotificationService.
     * 3. If notification succeeds: marks TRIGGERED, records triggeredAt, and spawns next occurrence if recurring.
     * 4. If notification fails: records error. If max attempts reached, marks TRIGGERED with error; otherwise remains PENDING for retry.
     */
    private void triggerReminder(ReminderEntity reminder) {
        int attempts = reminder.getNotificationAttempts() != null ? reminder.getNotificationAttempts() : 0;
        reminder.setNotificationAttempts(attempts + 1);
        reminder.setLastAttemptAt(Instant.now());

        NotificationType notificationType = mapReminderToNotificationType(reminder);
        Severity severity = reminder.isOverdue() ? Severity.ACTION_REQUIRED : Severity.WARNING;

        String entityType = reminder.getReferenceType() != null ? reminder.getReferenceType() : "REMINDER";
        String entityId = reminder.getReferenceId() != null ? reminder.getReferenceId() : reminder.getId().toString();

        String actionUrl = buildActionUrl(reminder);
        boolean notificationSucceeded = false;

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
                    null
            );
            notificationSucceeded = true;
            reminder.setLastError(null);
        } catch (Exception ex) {
            log.error("Notification attempt {} failed for reminder {}: {}",
                    reminder.getNotificationAttempts(), reminder.getId(), ex.getMessage(), ex);
            reminder.setLastError(ex.getMessage());
        }

        if (notificationSucceeded) {
            reminder.setStatus(ReminderStatus.TRIGGERED);
            reminder.setTriggeredAt(Instant.now());
            reminderRepository.save(reminder);

            // Spawn next recurrence if configured
            spawnNextRecurringOccurrence(reminder);
        } else {
            if (reminder.getNotificationAttempts() >= MAX_NOTIFICATION_ATTEMPTS) {
                log.warn("Reminder {} reached max notification attempts ({}), marking as TRIGGERED with last error: {}",
                        reminder.getId(), MAX_NOTIFICATION_ATTEMPTS, reminder.getLastError());
                reminder.setStatus(ReminderStatus.TRIGGERED);
                reminder.setTriggeredAt(Instant.now());
            }
            reminderRepository.save(reminder);
        }
    }

    /**
     * Calculates and creates the next occurrence of a recurring reminder.
     * Guaranteed to be idempotent (at most one pending occurrence per parent lineage and scheduled time).
     */
    private void spawnNextRecurringOccurrence(ReminderEntity reminder) {
        if (reminder.getRecurrenceType() == null || reminder.getRecurrenceType() == ReminderRecurrenceType.NONE) {
            return;
        }

        Instant nextScheduledAt = calculateNextOccurrence(reminder.getScheduledAt(), reminder.getRecurrenceType(), ZoneOffset.UTC);
        if (nextScheduledAt == null) {
            return;
        }

        UUID parentId = reminder.getParentReminderId() != null ? reminder.getParentReminderId() : reminder.getId();

        // Check if an existing PENDING reminder already exists for this parent lineage and next scheduled time
        boolean alreadyExists = reminderRepository.findAllByOrganizationIdAndStatusOrderByScheduledAtAsc(reminder.getOrganizationId(), ReminderStatus.PENDING)
                .stream()
                .anyMatch(r -> (parentId.equals(r.getParentReminderId()) || parentId.equals(r.getId()))
                        && nextScheduledAt.equals(r.getScheduledAt()));

        if (alreadyExists) {
            log.debug("Recurring occurrence for parent {} at {} already exists, skipping spawn", parentId, nextScheduledAt);
            return;
        }

        ReminderEntity nextOccurrence = ReminderEntity.builder()
                .title(reminder.getTitle())
                .description(reminder.getDescription())
                .reminderType(reminder.getReminderType())
                .status(ReminderStatus.PENDING)
                .priority(reminder.getPriority())
                .targetUserId(reminder.getTargetUserId())
                .clientId(reminder.getClientId())
                .engagementId(reminder.getEngagementId())
                .workInstanceId(reminder.getWorkInstanceId())
                .taskId(reminder.getTaskId())
                .automationRuleId(reminder.getAutomationRuleId())
                .referenceType(reminder.getReferenceType())
                .referenceId(reminder.getReferenceId())
                .scheduledAt(nextScheduledAt)
                .recurrenceType(reminder.getRecurrenceType())
                .parentReminderId(parentId)
                .notificationAttempts(0)
                .notes(reminder.getNotes())
                .build();
        nextOccurrence.setOrganizationId(reminder.getOrganizationId());

        reminderRepository.save(nextOccurrence);
        log.info("Spawned next recurring reminder occurrence {} (parent={}, scheduledAt={}) for org {}",
                nextOccurrence.getId(), parentId, nextScheduledAt, reminder.getOrganizationId());
    }

    /**
     * Deterministically calculates the next occurrence instant for a given recurrence pattern.
     * Handles month-end date clamping safely (e.g. Jan 31 -> Feb 28/29).
     */
    public static Instant calculateNextOccurrence(Instant currentScheduledAt, ReminderRecurrenceType recurrenceType, ZoneId zoneId) {
        if (recurrenceType == null || recurrenceType == ReminderRecurrenceType.NONE || currentScheduledAt == null) {
            return null;
        }
        ZoneId zone = zoneId != null ? zoneId : ZoneOffset.UTC;
        ZonedDateTime zdt = currentScheduledAt.atZone(zone);

        ZonedDateTime next = switch (recurrenceType) {
            case DAILY -> zdt.plusDays(1);
            case WEEKLY -> zdt.plusWeeks(1);
            case MONTHLY -> {
                int originalDay = zdt.getDayOfMonth();
                ZonedDateTime plusOneMonth = zdt.plusMonths(1);
                int maxDayInNextMonth = plusOneMonth.toLocalDate().lengthOfMonth();
                int targetDay = Math.min(originalDay, maxDayInNextMonth);
                yield plusOneMonth.withDayOfMonth(targetDay);
            }
            default -> null;
        };

        return next != null ? next.toInstant() : null;
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

    /**
     * Resolves target recipient user based on AutomationTargetType:
     * - TASK_ASSIGNEE: assigned user on the task
     * - ENGAGEMENT_OWNER: assigned user on the engagement
     * - WORK_INSTANCE_ASSIGNEE: assigned user on the work instance
     * - SPECIFIC_USER: explicitly targeted user from event
     */
    private UUID resolveTargetUser(TaxorynBusinessEvent event, AutomationRuleEntity rule) {
        AutomationTargetType targetType = rule.getTargetType() != null ? rule.getTargetType() : AutomationTargetType.TASK_ASSIGNEE;

        switch (targetType) {
            case ENGAGEMENT_OWNER:
                if (event.getEngagementId() != null && engagementRepository != null) {
                    Optional<EngagementEntity> engOpt = engagementRepository.findById(event.getEngagementId());
                    if (engOpt.isPresent() && engOpt.get().getAssignedUserId() != null) {
                        return engOpt.get().getAssignedUserId();
                    }
                }
                break;
            case WORK_INSTANCE_ASSIGNEE:
                if (event.getWorkInstanceId() != null && workInstanceRepository != null) {
                    Optional<WorkInstanceEntity> wiOpt = workInstanceRepository.findById(event.getWorkInstanceId());
                    if (wiOpt.isPresent() && wiOpt.get().getAssignedUserId() != null) {
                        return wiOpt.get().getAssignedUserId();
                    }
                }
                break;
            case TASK_ASSIGNEE:
            case SPECIFIC_USER:
            default:
                break;
        }

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
