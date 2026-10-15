package com.taxoryn.module.reminder.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity;
import com.taxoryn.module.docrequest.entity.DocumentRequestEntity.RequestStatus;
import com.taxoryn.module.docrequest.entity.DocumentRequestItemEntity.ItemStatus;
import com.taxoryn.module.docrequest.repository.DocumentRequestRepository;
import com.taxoryn.module.notification.entity.NotificationEntity.Category;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.entity.NotificationEntity.Severity;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.portal.entity.ClientApprovalRequestEntity;
import com.taxoryn.module.portal.entity.ClientApprovalStatus;
import com.taxoryn.module.portal.entity.ClientPortalMessageEntity;
import com.taxoryn.module.portal.entity.PortalMessageSenderType;
import com.taxoryn.module.portal.repository.ClientApprovalRequestRepository;
import com.taxoryn.module.portal.repository.ClientPortalMessageRepository;
import com.taxoryn.module.reminder.dto.FollowUpEvaluationResultDto;
import com.taxoryn.module.reminder.dto.FollowUpFilterRequest;
import com.taxoryn.module.reminder.dto.FollowUpItemDto;
import com.taxoryn.module.reminder.dto.SendFollowUpNudgeRequest;
import com.taxoryn.module.reminder.entity.ReminderEntity;
import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderRecurrenceType;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.entity.ReminderType;
import com.taxoryn.module.reminder.repository.ReminderRepository;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpReminderServiceImpl implements FollowUpReminderService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Duration NUDGE_COOLDOWN = Duration.ofHours(12);

    private final ReminderRepository reminderRepository;
    private final ClientApprovalRequestRepository clientApprovalRequestRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final ClientPortalMessageRepository clientPortalMessageRepository;
    private final ComplianceObligationRepository complianceObligationRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // =========================================================================
    // 1. Deterministic Evaluation Engine
    // =========================================================================

    @Override
    @Transactional
    public FollowUpEvaluationResultDto evaluateFollowUps(UUID organizationId) {
        log.info("Evaluating collaboration follow-ups for organization: {}", organizationId);

        Instant now = Instant.now();
        LocalDate today = LocalDate.now();

        int approvalsCount = evaluateApprovals(organizationId, now, today);
        int docRequestsCount = evaluateDocumentRequests(organizationId, now, today);
        int messagesCount = evaluateCommunications(organizationId, now, today);
        int complianceCount = evaluateComplianceDeadlines(organizationId, today);

        int totalCreated = approvalsCount + docRequestsCount + messagesCount + complianceCount;

        log.info("Follow-up evaluation complete for org {}: approvals={}, docRequests={}, messages={}, compliance={}, totalCreated={}",
                organizationId, approvalsCount, docRequestsCount, messagesCount, complianceCount, totalCreated);

        return FollowUpEvaluationResultDto.builder()
                .organizationId(organizationId)
                .approvalsEvaluated(approvalsCount)
                .documentRequestsEvaluated(docRequestsCount)
                .communicationsEvaluated(messagesCount)
                .complianceObligationsEvaluated(complianceCount)
                .totalRemindersCreated(totalCreated)
                .evaluatedAt(now)
                .build();
    }

    private int evaluateApprovals(UUID orgId, Instant now, LocalDate today) {
        List<ClientApprovalRequestEntity> pendingApprovals = clientApprovalRequestRepository
                .findAllByOrganizationIdAndStatusOrderByCreatedAtDesc(orgId, ClientApprovalStatus.PENDING);

        int created = 0;
        for (ClientApprovalRequestEntity approval : pendingApprovals) {
            // Exclude expired or revoked
            if (approval.getExpiresAt() != null && approval.getExpiresAt().isBefore(now)) {
                continue;
            }

            boolean approachingExpiry = approval.getExpiresAt() != null &&
                    approval.getExpiresAt().isBefore(now.plus(Duration.ofHours(48)));
            boolean pendingLongEnough = approval.getRequestedAt() != null &&
                    approval.getRequestedAt().isBefore(now.minus(Duration.ofHours(48)));

            if (!approachingExpiry && !pendingLongEnough) {
                continue;
            }

            ReminderPriority priority = ReminderPriority.MEDIUM;
            if (approval.getExpiresAt() != null && approval.getExpiresAt().isBefore(now.plus(Duration.ofHours(24)))) {
                priority = ReminderPriority.URGENT;
            } else if (approachingExpiry) {
                priority = ReminderPriority.HIGH;
            }

            String idempotencyKey = "FOLLOWUP::APPROVAL::" + approval.getId() + "::" + today.format(DATE_FMT);
            if (reminderRepository.existsByIdempotencyKey(idempotencyKey)) {
                continue;
            }

            String title = "Pending Client Approval: " + approval.getTitle();
            String desc = "Approval request (" + approval.getApprovalType() + ") is pending client decision.";
            if (approachingExpiry) {
                desc += " Approaching expiration!";
            }

            ReminderEntity reminder = ReminderEntity.builder()
                    .title(title)
                    .description(desc)
                    .reminderType(ReminderType.CLIENT_APPROVAL)
                    .priority(priority)
                    .status(ReminderStatus.PENDING)
                    .clientId(approval.getClientId())
                    .targetUserId(approval.getRequestedByUserId())
                    .referenceType("CLIENT_APPROVAL")
                    .referenceId(approval.getId().toString())
                    .idempotencyKey(idempotencyKey)
                    .scheduledAt(now)
                    .recurrenceType(ReminderRecurrenceType.NONE)
                    .notificationAttempts(0)
                    .build();
            reminder.setOrganizationId(orgId);

            if (safeSaveReminder(reminder)) {
                created++;
            }
        }
        return created;
    }

    private int evaluateDocumentRequests(UUID orgId, Instant now, LocalDate today) {
        List<RequestStatus> eligibleStatuses = List.of(
                RequestStatus.SENT,
                RequestStatus.REQUESTED,
                RequestStatus.PARTIALLY_COMPLETED,
                RequestStatus.OVERDUE
        );

        List<DocumentRequestEntity> requests = documentRequestRepository.findAllByOrganizationIdAndStatusIn(orgId, eligibleStatuses);
        int created = 0;

        for (DocumentRequestEntity req : requests) {
            // Must have items requiring action
            boolean hasActionableItems = req.getItems() != null && req.getItems().stream()
                    .anyMatch(item -> item.getStatus() == ItemStatus.PENDING || item.getStatus() == ItemStatus.REJECTED);

            if (!hasActionableItems) {
                continue;
            }

            boolean isOverdue = req.getDueDate() != null && req.getDueDate().isBefore(today);
            boolean isApproachingDue = req.getDueDate() != null && req.getDueDate().isBefore(today.plusDays(3));
            boolean pendingThreeDays = req.getSentAt() != null && req.getSentAt().isBefore(now.minus(Duration.ofDays(3)));

            if (!isOverdue && !isApproachingDue && !pendingThreeDays) {
                continue;
            }

            ReminderPriority priority = isOverdue ? ReminderPriority.URGENT : (isApproachingDue ? ReminderPriority.HIGH : ReminderPriority.MEDIUM);

            String idempotencyKey = "FOLLOWUP::DOC_REQUEST::" + req.getId() + "::" + today.format(DATE_FMT);
            if (reminderRepository.existsByIdempotencyKey(idempotencyKey)) {
                continue;
            }

            String title = "Outstanding Documents: " + req.getPurpose();
            String desc = "Client has outstanding required documents" + (isOverdue ? " (OVERDUE)" : "");

            ReminderEntity reminder = ReminderEntity.builder()
                    .title(title)
                    .description(desc)
                    .reminderType(ReminderType.DOCUMENT_REQUEST)
                    .priority(priority)
                    .status(ReminderStatus.PENDING)
                    .clientId(req.getClientId())
                    .targetUserId(req.getRequestedByUserId())
                    .referenceType("DOCUMENT_REQUEST")
                    .referenceId(req.getId().toString())
                    .idempotencyKey(idempotencyKey)
                    .scheduledAt(now)
                    .recurrenceType(ReminderRecurrenceType.NONE)
                    .notificationAttempts(0)
                    .build();
            reminder.setOrganizationId(orgId);

            if (safeSaveReminder(reminder)) {
                created++;
            }
        }
        return created;
    }

    private int evaluateCommunications(UUID orgId, Instant now, LocalDate today) {
        List<ClientPortalMessageEntity> unreadMessages = clientPortalMessageRepository
                .findAllByOrganizationIdAndReadByPracticeFalseOrderByCreatedAtAsc(orgId);

        int created = 0;
        for (ClientPortalMessageEntity msg : unreadMessages) {
            // Only remind on genuine client messages waiting for practitioner response
            if (msg.getSenderType() != PortalMessageSenderType.CLIENT) {
                continue;
            }

            // Waiting longer than 12 hours
            if (msg.getCreatedAt() != null && msg.getCreatedAt().isAfter(now.minus(Duration.ofHours(12)))) {
                continue;
            }

            String idempotencyKey = "FOLLOWUP::MESSAGE::" + msg.getId() + "::" + today.format(DATE_FMT);
            if (reminderRepository.existsByIdempotencyKey(idempotencyKey)) {
                continue;
            }

            boolean olderThanDay = msg.getCreatedAt() != null && msg.getCreatedAt().isBefore(now.minus(Duration.ofHours(24)));
            ReminderPriority priority = olderThanDay ? ReminderPriority.HIGH : ReminderPriority.MEDIUM;

            String contextInfo = msg.getContextTitle() != null ? " (" + msg.getContextTitle() + ")" : "";
            String title = "Unanswered Client Inquiry" + contextInfo;
            String desc = "Message from " + msg.getSenderName() + " has been awaiting response for > 12 hours.";

            ReminderEntity reminder = ReminderEntity.builder()
                    .title(title)
                    .description(desc)
                    .reminderType(ReminderType.UNANSWERED_COMMUNICATION)
                    .priority(priority)
                    .status(ReminderStatus.PENDING)
                    .clientId(msg.getClientId())
                    .targetUserId(null) // Assigned practitioner or team
                    .referenceType("PORTAL_MESSAGE")
                    .referenceId(msg.getId().toString())
                    .idempotencyKey(idempotencyKey)
                    .scheduledAt(now)
                    .recurrenceType(ReminderRecurrenceType.NONE)
                    .notificationAttempts(0)
                    .build();
            reminder.setOrganizationId(orgId);

            if (safeSaveReminder(reminder)) {
                created++;
            }
        }
        return created;
    }

    private int evaluateComplianceDeadlines(UUID orgId, LocalDate today) {
        LocalDate horizon = today.plusDays(3);
        List<ComplianceObligationEntity> obligations = complianceObligationRepository
                .findAllByOrganizationIdAndStatutoryDueDateBetween(orgId, today.minusDays(7), horizon);

        int created = 0;
        for (ComplianceObligationEntity ob : obligations) {
            // Exclude completed or cancelled or terminal obligations
            if (ob.getStatus() != null && ob.getStatus().isTerminal()) {
                continue;
            }

            LocalDate dueDate = ob.getStatutoryDueDate();
            if (dueDate == null) {
                continue;
            }

            String idempotencyKey = "FOLLOWUP::COMPLIANCE::" + ob.getId() + "::" + dueDate.format(DATE_FMT);
            if (reminderRepository.existsByIdempotencyKey(idempotencyKey)) {
                continue;
            }

            boolean isOverdue = dueDate.isBefore(today);
            ReminderPriority priority = isOverdue ? ReminderPriority.URGENT :
                    (dueDate.isEqual(today) ? ReminderPriority.HIGH : ReminderPriority.MEDIUM);

            String title = "Compliance Deadline: " + (ob.getTitle() != null ? ob.getTitle() : "Statutory Obligation");
            String desc = "Due on " + dueDate.format(DATE_FMT) + (isOverdue ? " (OVERDUE)" : "");

            ReminderEntity reminder = ReminderEntity.builder()
                    .title(title)
                    .description(desc)
                    .reminderType(ReminderType.COMPLIANCE_DEADLINE)
                    .priority(priority)
                    .status(ReminderStatus.PENDING)
                    .clientId(ob.getClientId())
                    .targetUserId(ob.getAssignedUserId())
                    .referenceType("COMPLIANCE_OBLIGATION")
                    .referenceId(ob.getId().toString())
                    .idempotencyKey(idempotencyKey)
                    .scheduledAt(Instant.now())
                    .recurrenceType(ReminderRecurrenceType.NONE)
                    .notificationAttempts(0)
                    .build();
            reminder.setOrganizationId(orgId);

            if (safeSaveReminder(reminder)) {
                created++;
            }
        }
        return created;
    }

    private boolean safeSaveReminder(ReminderEntity reminder) {
        try {
            reminderRepository.save(reminder);
            return true;
        } catch (DataIntegrityViolationException dive) {
            log.debug("Duplicate idempotency key detected: {}", reminder.getIdempotencyKey());
            return false;
        }
    }

    // =========================================================================
    // 2. Querying Follow-up Items (Practitioner Experience)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<FollowUpItemDto> getFollowUps(FollowUpFilterRequest filter) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        Set<ReminderType> followUpTypes = Set.of(
                ReminderType.CLIENT_APPROVAL,
                ReminderType.DOCUMENT_REQUEST,
                ReminderType.UNANSWERED_COMMUNICATION,
                ReminderType.COMPLIANCE_DEADLINE,
                ReminderType.PRACTITIONER_FOLLOW_UP,
                ReminderType.FOLLOW_UP,
                ReminderType.DOCUMENT_COLLECTION
        );

        Specification<ReminderEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("reminderType"), filter.getCategory()));
            } else {
                predicates.add(root.get("reminderType").in(followUpTypes));
            }

            if (filter.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filter.getClientId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
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
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ReminderEntity> page = reminderRepository.findAll(spec, pageRequest);
        return PagedResponse.of(page, this::toFollowUpItemDto);
    }

    // =========================================================================
    // 3. Manual Follow-up Nudge (On-demand with Cooldown)
    // =========================================================================

    @Override
    @Transactional
    public FollowUpItemDto sendFollowUpNudge(UUID reminderId, SendFollowUpNudgeRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        ReminderEntity reminder = reminderRepository.findByIdAndOrganizationId(reminderId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", "id", reminderId));

        // Reconcile source state: if the underlying action is resolved, fail early
        if (!validateAndReconcileSourceState(reminderId)) {
            throw new BadRequestException("This follow-up action has already been resolved or completed.");
        }

        // Rate limiting: enforce 12h cooldown between manual nudges
        if (reminder.getLastAttemptAt() != null &&
                reminder.getLastAttemptAt().isAfter(Instant.now().minus(NUDGE_COOLDOWN))) {
            long hoursAgo = Duration.between(reminder.getLastAttemptAt(), Instant.now()).toHours();
            throw new BadRequestException("A follow-up nudge was already sent " + hoursAgo +
                    " hour(s) ago. Cooldown period is 12 hours.");
        }

        String customMsg = request != null && StringUtils.hasText(request.getCustomMessage())
                ? request.getCustomMessage().trim()
                : reminder.getDescription();

        // Dispatch notification
        notificationService.notify(
                organizationId,
                null, // target client directly
                reminder.getClientId(),
                NotificationType.FOLLOW_UP_NUDGE,
                Severity.ACTION_REQUIRED,
                Category.REMINDER,
                reminder.getReferenceType(),
                reminder.getReferenceId(),
                "Follow-up: " + reminder.getTitle(),
                customMsg != null ? customMsg : reminder.getTitle(),
                Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
                buildActionUrl(reminder),
                "{\"reminderId\":\"" + reminder.getId() + "\",\"nudgedBy\":\"" + currentUserId + "\"}",
                null
        );

        // Update reminder attempt state
        reminder.setNotificationAttempts(reminder.getNotificationAttempts() + 1);
        reminder.setLastAttemptAt(Instant.now());
        reminder.setLastError(null);
        reminder.setStatus(ReminderStatus.TRIGGERED);
        reminder.setTriggeredAt(Instant.now());

        ReminderEntity saved = reminderRepository.save(reminder);

        // Audit log
        auditService.logEvent(
                organizationId,
                currentUserId,
                "FOLLOW_UP_NUDGE_SENT",
                "REMINDER",
                reminder.getId().toString(),
                null,
                Map.of("clientId", reminder.getClientId(), "referenceType", reminder.getReferenceType(), "referenceId", reminder.getReferenceId())
        );

        log.info("Sent follow-up nudge for reminder {} to client {} by user {}",
                reminderId, reminder.getClientId(), currentUserId);

        return toFollowUpItemDto(saved);
    }

    // =========================================================================
    // 4. Resolve Follow-up
    // =========================================================================

    @Override
    @Transactional
    public FollowUpItemDto resolveFollowUp(UUID reminderId, String resolutionNotes) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        ReminderEntity reminder = reminderRepository.findByIdAndOrganizationId(reminderId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", "id", reminderId));

        reminder.setStatus(ReminderStatus.COMPLETED);
        reminder.setCompletedAt(Instant.now());
        if (StringUtils.hasText(resolutionNotes)) {
            reminder.setNotes(resolutionNotes.trim());
        }

        ReminderEntity saved = reminderRepository.save(reminder);

        auditService.logEvent(
                organizationId,
                currentUserId,
                "FOLLOW_UP_RESOLVED",
                "REMINDER",
                reminder.getId().toString(),
                null,
                Map.of("notes", resolutionNotes != null ? resolutionNotes : "")
        );

        return toFollowUpItemDto(saved);
    }

    // =========================================================================
    // 5. Source State Reconciliation (Prevent Stale Delivery)
    // =========================================================================

    @Override
    @Transactional
    public boolean validateAndReconcileSourceState(UUID reminderId) {
        Optional<ReminderEntity> remOpt = reminderRepository.findById(reminderId);
        if (remOpt.isEmpty()) {
            return false;
        }

        ReminderEntity reminder = remOpt.get();
        if (reminder.getStatus() == ReminderStatus.COMPLETED || reminder.getStatus() == ReminderStatus.CANCELLED) {
            return false;
        }

        String refType = reminder.getReferenceType();
        String refId = reminder.getReferenceId();
        if (!StringUtils.hasText(refType) || !StringUtils.hasText(refId)) {
            return true;
        }

        try {
            UUID sourceId = UUID.fromString(refId);

            if ("CLIENT_APPROVAL".equals(refType)) {
                Optional<ClientApprovalRequestEntity> appOpt = clientApprovalRequestRepository.findById(sourceId);
                if (appOpt.isEmpty() || appOpt.get().getStatus() != ClientApprovalStatus.PENDING) {
                    cancelStaleReminder(reminder, "Source approval is no longer pending");
                    return false;
                }
            } else if ("DOCUMENT_REQUEST".equals(refType)) {
                Optional<DocumentRequestEntity> docOpt = documentRequestRepository.findById(sourceId);
                if (docOpt.isEmpty() ||
                        docOpt.get().getStatus() == RequestStatus.COMPLETED ||
                        docOpt.get().getStatus() == RequestStatus.CANCELLED ||
                        docOpt.get().getStatus() == RequestStatus.DECLINED) {
                    cancelStaleReminder(reminder, "Source document request is completed or cancelled");
                    return false;
                }
            } else if ("PORTAL_MESSAGE".equals(refType)) {
                Optional<ClientPortalMessageEntity> msgOpt = clientPortalMessageRepository.findById(sourceId);
                if (msgOpt.isEmpty() || msgOpt.get().isReadByPractice()) {
                    cancelStaleReminder(reminder, "Source message has been read or answered");
                    return false;
                }
            } else if ("COMPLIANCE_OBLIGATION".equals(refType)) {
                Optional<ComplianceObligationEntity> obOpt = complianceObligationRepository.findById(sourceId);
                if (obOpt.isEmpty() || obOpt.get().getStatus() == null || obOpt.get().getStatus().isTerminal()) {
                    cancelStaleReminder(reminder, "Source compliance obligation is resolved");
                    return false;
                }
            }
        } catch (IllegalArgumentException iae) {
            log.warn("Invalid UUID for referenceId: {}", refId);
        }

        return true;
    }

    private void cancelStaleReminder(ReminderEntity reminder, String reason) {
        log.info("Cancelling stale reminder {} (refType={}, refId={}): {}",
                reminder.getId(), reminder.getReferenceType(), reminder.getReferenceId(), reason);
        reminder.setStatus(ReminderStatus.CANCELLED);
        reminder.setCancelledAt(Instant.now());
        reminder.setNotes(reason);
        reminderRepository.save(reminder);
    }

    // =========================================================================
    // 6. Helpers & Enriched Mapping
    // =========================================================================

    private FollowUpItemDto toFollowUpItemDto(ReminderEntity entity) {
        String clientName = null;
        if (entity.getClientId() != null) {
            clientName = clientRepository.findById(entity.getClientId())
                    .map(ClientEntity::getDisplayName)
                    .orElse(null);
        }

        String targetUserName = null;
        if (entity.getTargetUserId() != null) {
            targetUserName = userRepository.findById(entity.getTargetUserId())
                    .map(u -> u.getFullName())
                    .orElse(null);
        }

        return FollowUpItemDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(clientName)
                .category(entity.getReminderType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .overdue(entity.isOverdue())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .targetUserId(entity.getTargetUserId())
                .targetUserName(targetUserName)
                .actionUrl(buildActionUrl(entity))
                .scheduledAt(entity.getScheduledAt())
                .triggeredAt(entity.getTriggeredAt())
                .lastAttemptAt(entity.getLastAttemptAt())
                .notificationAttempts(entity.getNotificationAttempts())
                .lastError(entity.getLastError())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String buildActionUrl(ReminderEntity entity) {
        String refType = entity.getReferenceType();
        String refId = entity.getReferenceId();
        if (!StringUtils.hasText(refType) || !StringUtils.hasText(refId)) {
            return "/portal/management";
        }

        return switch (refType) {
            case "CLIENT_APPROVAL" -> "/portal/management?tab=approvals&approvalId=" + refId;
            case "DOCUMENT_REQUEST" -> "/portal/management?tab=requests&requestId=" + refId;
            case "PORTAL_MESSAGE" -> "/portal/management?tab=messages&messageId=" + refId;
            case "COMPLIANCE_OBLIGATION" -> "/compliance/workbench?obligationId=" + refId;
            case "TASK" -> "/tasks?taskId=" + refId;
            default -> "/portal/management";
        };
    }
}
