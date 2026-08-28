package com.taxoryn.module.feedback.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.feedback.dto.*;
import com.taxoryn.module.feedback.entity.*;
import com.taxoryn.module.feedback.repository.*;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminApplicationFeedbackServiceImpl implements AdminApplicationFeedbackService {

    private final ApplicationFeedbackRepository feedbackRepository;
    private final FeedbackAssignmentRepository assignmentRepository;
    private final FeedbackNoteRepository noteRepository;
    private final FeedbackStatusHistoryRepository statusHistoryRepository;
    private final FeedbackEngineeringIssueRepository engineeringIssueRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AdminApplicationFeedbackSummaryDto> getFeedbackList(
            String search,
            ApplicationFeedbackActorType actorType,
            ApplicationFeedbackType type,
            ApplicationFeedbackCategory category,
            ApplicationFeedbackStatus status,
            ApplicationFeedbackPriority priority,
            UUID practiceId,
            FeedbackTeam assignedTeam,
            UUID assignedUserId,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    ) {
        Specification<ApplicationFeedbackEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("page")), pattern),
                        cb.like(cb.lower(root.get("feature")), pattern)
                ));
            }

            if (actorType != null) {
                predicates.add(cb.equal(root.get("actorType"), actorType));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (practiceId != null) {
                predicates.add(cb.equal(root.get("practiceId"), practiceId));
            }
            if (assignedTeam != null) {
                predicates.add(cb.equal(root.get("assignedTeam"), assignedTeam));
            }
            if (assignedUserId != null) {
                predicates.add(cb.equal(root.get("assignedUserId"), assignedUserId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ApplicationFeedbackEntity> page = feedbackRepository.findAll(spec, pageable);
        if (page.isEmpty()) {
            return PagedResponse.of(page, f -> null);
        }

        // Batch load users and organizations for summary enrichment
        Set<UUID> userIds = page.getContent().stream().map(ApplicationFeedbackEntity::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> assignedUserIds = page.getContent().stream().map(ApplicationFeedbackEntity::getAssignedUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        userIds.addAll(assignedUserIds);

        Map<UUID, UserEntity> userMap = userIds.isEmpty() ? Map.of() :
                userRepository.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, u -> u));

        Set<UUID> orgIds = page.getContent().stream().map(ApplicationFeedbackEntity::getPracticeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, OrganizationEntity> orgMap = orgIds.isEmpty() ? Map.of() :
                organizationRepository.findAllById(orgIds).stream().collect(Collectors.toMap(OrganizationEntity::getId, o -> o));

        return PagedResponse.of(page, feedback -> toSummaryDto(feedback, userMap, orgMap));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminApplicationFeedbackDetailDto getFeedbackDetail(UUID id) {
        ApplicationFeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application Feedback not found with id: " + id));

        UserEntity reporter = feedback.getUserId() != null ? userRepository.findById(feedback.getUserId()).orElse(null) : null;
        OrganizationEntity practice = feedback.getPracticeId() != null ? organizationRepository.findById(feedback.getPracticeId()).orElse(null) : null;
        UserEntity assignedUser = feedback.getAssignedUserId() != null ? userRepository.findById(feedback.getAssignedUserId()).orElse(null) : null;
        UserEntity resolvedByUser = feedback.getResolvedBy() != null ? userRepository.findById(feedback.getResolvedBy()).orElse(null) : null;
        UserEntity closedByUser = feedback.getClosedBy() != null ? userRepository.findById(feedback.getClosedBy()).orElse(null) : null;

        String duplicateOfTitle = null;
        if (feedback.getDuplicateOfId() != null) {
            duplicateOfTitle = feedbackRepository.findById(feedback.getDuplicateOfId())
                    .map(ApplicationFeedbackEntity::getTitle).orElse(null);
        }

        List<FeedbackAssignmentEntity> assignments = assignmentRepository.findByFeedbackIdOrderByAssignedAtDesc(id);
        FeedbackAssignmentDto activeAssignmentDto = assignments.stream()
                .filter(FeedbackAssignmentEntity::isActive)
                .findFirst()
                .map(this::toAssignmentDto)
                .orElse(null);
        List<FeedbackAssignmentDto> assignmentHistory = assignments.stream().map(this::toAssignmentDto).toList();

        List<FeedbackNoteDto> notes = noteRepository.findByFeedbackIdOrderByCreatedAtAsc(id)
                .stream().map(this::toNoteDto).toList();

        List<FeedbackStatusHistoryDto> timeline = statusHistoryRepository.findByFeedbackIdOrderByCreatedAtAsc(id)
                .stream().map(this::toStatusHistoryDto).toList();

        EngineeringIssueDto engineeringIssueDto = engineeringIssueRepository.findByFeedbackId(id)
                .map(this::toEngineeringIssueDto).orElse(null);

        return AdminApplicationFeedbackDetailDto.builder()
                .id(feedback.getId())
                .feedbackCode(formatFeedbackCode(feedback.getId()))
                .userId(feedback.getUserId())
                .reporterName(reporter != null ? reporter.getFullName() : "Anonymous / System")
                .reporterEmail(reporter != null ? reporter.getEmail() : null)
                .reporterPhone(reporter != null ? reporter.getPhone() : null)
                .actorType(feedback.getActorType())
                .practiceId(feedback.getPracticeId())
                .practiceName(practice != null ? practice.getName() : null)
                .practiceEmail(practice != null ? practice.getEmail() : null)
                .practiceSubscriptionPlan(practice != null ? practice.getSubscriptionPlan().name() : null)
                .contextType(feedback.getContextType())
                .type(feedback.getType())
                .category(feedback.getCategory())
                .rating(feedback.getRating())
                .title(feedback.getTitle())
                .description(feedback.getDescription())
                .page(feedback.getPage())
                .feature(feedback.getFeature())
                .source(feedback.getSource())
                .status(feedback.getStatus())
                .priority(feedback.getPriority())
                .assignedTeam(feedback.getAssignedTeam())
                .assignedUserId(feedback.getAssignedUserId())
                .assignedUserName(assignedUser != null ? assignedUser.getFullName() : null)
                .activeAssignment(activeAssignmentDto)
                .assignmentHistory(assignmentHistory)
                .duplicateOfId(feedback.getDuplicateOfId())
                .duplicateOfTitle(duplicateOfTitle)
                .resolutionNote(feedback.getResolutionNote())
                .resolvedBy(feedback.getResolvedBy())
                .resolvedByName(resolvedByUser != null ? resolvedByUser.getFullName() : null)
                .resolvedAt(feedback.getResolvedAt())
                .closedBy(feedback.getClosedBy())
                .closedByName(closedByUser != null ? closedByUser.getFullName() : null)
                .closedAt(feedback.getClosedAt())
                .engineeringIssue(engineeringIssueDto)
                .notes(notes)
                .timeline(timeline)
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public AdminApplicationFeedbackDetailDto startReview(UUID id) {
        ApplicationFeedbackEntity feedback = requireFeedback(id);
        ApplicationFeedbackStatus oldStatus = feedback.getStatus();

        if (oldStatus == ApplicationFeedbackStatus.CLOSED || oldStatus == ApplicationFeedbackStatus.REJECTED || oldStatus == ApplicationFeedbackStatus.DUPLICATE) {
            throw new BusinessValidationException("Cannot start review on feedback in terminal status: " + oldStatus);
        }

        if (oldStatus == ApplicationFeedbackStatus.NEW) {
            feedback.setStatus(ApplicationFeedbackStatus.UNDER_REVIEW);
            feedbackRepository.save(feedback);

            recordStatusHistory(id, oldStatus, ApplicationFeedbackStatus.UNDER_REVIEW, "Admin review started");
            auditService.logEvent("ADMIN_FEEDBACK_REVIEW_STARTED", "APPLICATION_FEEDBACK", id.toString(), null,
                    "Admin review started for feedback " + formatFeedbackCode(id));
        }

        return getFeedbackDetail(id);
    }

    @Override
    @Transactional
    public AdminApplicationFeedbackDetailDto assignFeedback(UUID id, AssignFeedbackRequest request) {
        ApplicationFeedbackEntity feedback = requireFeedback(id);
        ApplicationFeedbackStatus oldStatus = feedback.getStatus();

        if (request.getTeam() == null) {
            throw new BusinessValidationException("Assigned team is required");
        }

        UserEntity assignedUser = null;
        if (request.getAssignedUserId() != null) {
            assignedUser = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new BusinessValidationException("Assigned user not found with id: " + request.getAssignedUserId()));
        }

        UUID adminId = SecurityUtils.getCurrentUserId();
        SecurityUser adminUser = SecurityUtils.getCurrentUser().orElse(null);

        // Deactivate existing active assignments
        List<FeedbackAssignmentEntity> activeAssignments = assignmentRepository.findByFeedbackIdAndActiveTrue(id);
        for (FeedbackAssignmentEntity activeAssignment : activeAssignments) {
            activeAssignment.setActive(false);
            activeAssignment.setUnassignedAt(Instant.now());
            assignmentRepository.save(activeAssignment);
        }

        // Create new assignment
        FeedbackAssignmentEntity newAssignment = FeedbackAssignmentEntity.builder()
                .feedbackId(id)
                .team(request.getTeam())
                .assignedUserId(request.getAssignedUserId())
                .assignedBy(adminId)
                .reason(request.getReason())
                .assignedAt(Instant.now())
                .active(true)
                .build();
        assignmentRepository.save(newAssignment);

        feedback.setAssignedTeam(request.getTeam());
        feedback.setAssignedUserId(request.getAssignedUserId());

        // Status transition: if NEW or UNDER_REVIEW -> ASSIGNED
        if (oldStatus == ApplicationFeedbackStatus.NEW || oldStatus == ApplicationFeedbackStatus.UNDER_REVIEW) {
            feedback.setStatus(ApplicationFeedbackStatus.ASSIGNED);
            recordStatusHistory(id, oldStatus, ApplicationFeedbackStatus.ASSIGNED,
                    "Assigned to " + request.getTeam() + (assignedUser != null ? " (" + assignedUser.getFullName() + ")" : ""));
        } else {
            recordStatusHistory(id, oldStatus, oldStatus,
                    "Reassigned to " + request.getTeam() + (assignedUser != null ? " (" + assignedUser.getFullName() + ")" : ""));
        }

        feedbackRepository.save(feedback);
        auditService.logEvent("ADMIN_FEEDBACK_ASSIGNED", "APPLICATION_FEEDBACK", id.toString(), null,
                "Assigned feedback to team=" + request.getTeam() + ", user=" + request.getAssignedUserId());

        return getFeedbackDetail(id);
    }

    @Override
    @Transactional
    public FeedbackNoteDto addNote(UUID id, CreateFeedbackNoteRequest request) {
        requireFeedback(id);
        if (!StringUtils.hasText(request.getNote())) {
            throw new BusinessValidationException("Note text cannot be blank");
        }

        UUID authorId = SecurityUtils.getCurrentUserId();
        String authorName = userRepository.findById(authorId)
                .map(UserEntity::getFullName).orElse("Taxoryn Admin");

        FeedbackNoteEntity noteEntity = FeedbackNoteEntity.builder()
                .feedbackId(id)
                .authorId(authorId)
                .authorName(authorName)
                .note(request.getNote().trim())
                .visibility(request.getVisibility() != null ? request.getVisibility() : FeedbackNoteVisibility.INTERNAL)
                .build();

        FeedbackNoteEntity saved = noteRepository.save(noteEntity);
        auditService.logEvent("ADMIN_FEEDBACK_NOTE_ADDED", "APPLICATION_FEEDBACK", id.toString(), null,
                "Internal note added by " + authorName);

        return toNoteDto(saved);
    }

    @Override
    @Transactional
    public AdminApplicationFeedbackDetailDto updatePriority(UUID id, UpdateFeedbackPriorityRequest request) {
        ApplicationFeedbackEntity feedback = requireFeedback(id);
        ApplicationFeedbackPriority oldPriority = feedback.getPriority();

        if (request.getPriority() == null) {
            throw new BusinessValidationException("Priority is required");
        }

        feedback.setPriority(request.getPriority());
        feedbackRepository.save(feedback);

        recordStatusHistory(id, feedback.getStatus(), feedback.getStatus(),
                "Priority changed from " + oldPriority + " to " + request.getPriority() +
                        (StringUtils.hasText(request.getReason()) ? ": " + request.getReason().trim() : ""));

        auditService.logEvent("ADMIN_FEEDBACK_PRIORITY_UPDATED", "APPLICATION_FEEDBACK", id.toString(), null,
                "Priority updated from " + oldPriority + " to " + request.getPriority());

        return getFeedbackDetail(id);
    }

    @Override
    @Transactional
    public AdminApplicationFeedbackDetailDto resolveFeedback(UUID id, ResolveFeedbackRequest request) {
        ApplicationFeedbackEntity feedback = requireFeedback(id);
        ApplicationFeedbackStatus oldStatus = feedback.getStatus();

        if (!StringUtils.hasText(request.getResolutionNote())) {
            throw new BusinessValidationException("Resolution note is required to resolve feedback");
        }

        UUID adminId = SecurityUtils.getCurrentUserId();

        feedback.setStatus(ApplicationFeedbackStatus.RESOLVED);
        feedback.setResolutionNote(request.getResolutionNote().trim());
        feedback.setResolvedBy(adminId);
        feedback.setResolvedAt(Instant.now());
        feedbackRepository.save(feedback);

        recordStatusHistory(id, oldStatus, ApplicationFeedbackStatus.RESOLVED,
                "Resolved: " + request.getResolutionNote().trim());

        auditService.logEvent("ADMIN_FEEDBACK_RESOLVED", "APPLICATION_FEEDBACK", id.toString(), null,
                "Feedback resolved by admin");

        return getFeedbackDetail(id);
    }

    @Override
    @Transactional
    public AdminApplicationFeedbackDetailDto closeFeedback(UUID id, CloseFeedbackRequest request) {
        ApplicationFeedbackEntity feedback = requireFeedback(id);
        ApplicationFeedbackStatus oldStatus = feedback.getStatus();

        if (oldStatus != ApplicationFeedbackStatus.RESOLVED && oldStatus != ApplicationFeedbackStatus.REJECTED && oldStatus != ApplicationFeedbackStatus.DUPLICATE) {
            throw new BusinessValidationException("Only resolved, rejected, or duplicate feedback can be closed. Current status: " + oldStatus);
        }

        UUID adminId = SecurityUtils.getCurrentUserId();

        feedback.setStatus(ApplicationFeedbackStatus.CLOSED);
        feedback.setClosedBy(adminId);
        feedback.setClosedAt(Instant.now());
        feedbackRepository.save(feedback);

        String reason = request != null && StringUtils.hasText(request.getReason()) ? request.getReason().trim() : "Closed by admin";
        recordStatusHistory(id, oldStatus, ApplicationFeedbackStatus.CLOSED, reason);

        auditService.logEvent("ADMIN_FEEDBACK_CLOSED", "APPLICATION_FEEDBACK", id.toString(), null,
                "Feedback closed by admin");

        return getFeedbackDetail(id);
    }

    @Override
    @Transactional
    public AdminApplicationFeedbackDetailDto rejectFeedback(UUID id, RejectFeedbackRequest request) {
        ApplicationFeedbackEntity feedback = requireFeedback(id);
        ApplicationFeedbackStatus oldStatus = feedback.getStatus();

        if (!StringUtils.hasText(request.getReason())) {
            throw new BusinessValidationException("Rejection reason is required");
        }

        feedback.setStatus(ApplicationFeedbackStatus.REJECTED);
        feedbackRepository.save(feedback);

        recordStatusHistory(id, oldStatus, ApplicationFeedbackStatus.REJECTED,
                "Rejected: " + request.getReason().trim());

        auditService.logEvent("ADMIN_FEEDBACK_REJECTED", "APPLICATION_FEEDBACK", id.toString(), null,
                "Feedback rejected by admin");

        return getFeedbackDetail(id);
    }

    @Override
    @Transactional
    public AdminApplicationFeedbackDetailDto markDuplicate(UUID id, MarkDuplicateFeedbackRequest request) {
        ApplicationFeedbackEntity feedback = requireFeedback(id);
        ApplicationFeedbackStatus oldStatus = feedback.getStatus();

        if (request.getDuplicateOfId() == null) {
            throw new BusinessValidationException("Original feedback ID is required");
        }
        if (request.getDuplicateOfId().equals(id)) {
            throw new BusinessValidationException("Feedback cannot be marked as a duplicate of itself");
        }

        ApplicationFeedbackEntity original = feedbackRepository.findById(request.getDuplicateOfId())
                .orElseThrow(() -> new BusinessValidationException("Original feedback not found with id: " + request.getDuplicateOfId()));

        feedback.setStatus(ApplicationFeedbackStatus.DUPLICATE);
        feedback.setDuplicateOfId(original.getId());
        feedbackRepository.save(feedback);

        String reason = "Marked as duplicate of " + formatFeedbackCode(original.getId()) +
                (StringUtils.hasText(request.getReason()) ? " (" + request.getReason().trim() + ")" : "");
        recordStatusHistory(id, oldStatus, ApplicationFeedbackStatus.DUPLICATE, reason);

        auditService.logEvent("ADMIN_FEEDBACK_DUPLICATE_MARKED", "APPLICATION_FEEDBACK", id.toString(), null,
                "Feedback marked as duplicate of " + original.getId());

        return getFeedbackDetail(id);
    }

    @Override
    @Transactional
    public EngineeringIssueDto escalateToEngineering(UUID id, EscalateToEngineeringRequest request) {
        ApplicationFeedbackEntity feedback = requireFeedback(id);
        ApplicationFeedbackStatus oldStatus = feedback.getStatus();

        if (feedback.getStatus() == ApplicationFeedbackStatus.CLOSED || feedback.getStatus() == ApplicationFeedbackStatus.REJECTED) {
            throw new BusinessValidationException("Cannot escalate closed or rejected feedback");
        }

        if (engineeringIssueRepository.findByFeedbackId(id).isPresent()) {
            throw new BusinessValidationException("An engineering issue already exists for this feedback");
        }

        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getDescription())) {
            throw new BusinessValidationException("Title and problem description are required for engineering escalation");
        }

        UUID adminId = SecurityUtils.getCurrentUserId();
        String issueCode = "ENG-" + String.format("%05d", Math.abs(id.hashCode() % 100000));

        FeedbackEngineeringIssueEntity engineeringIssue = FeedbackEngineeringIssueEntity.builder()
                .feedbackId(id)
                .issueCode(issueCode)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .priority(request.getPriority() != null ? request.getPriority() : ApplicationFeedbackPriority.HIGH)
                .status(EngineeringIssueStatus.OPEN)
                .assignedTeam("ENGINEERING")
                .creatorUserId(adminId)
                .build();

        FeedbackEngineeringIssueEntity savedIssue = engineeringIssueRepository.save(engineeringIssue);

        // Update feedback status to ESCALATED
        feedback.setStatus(ApplicationFeedbackStatus.ESCALATED);
        feedback.setAssignedTeam(FeedbackTeam.ENGINEERING);
        feedbackRepository.save(feedback);

        // Optional internal notes
        if (StringUtils.hasText(request.getInternalNotes())) {
            addNote(id, CreateFeedbackNoteRequest.builder()
                    .note("[Escalation Note] " + request.getInternalNotes().trim())
                    .visibility(FeedbackNoteVisibility.INTERNAL)
                    .build());
        }

        recordStatusHistory(id, oldStatus, ApplicationFeedbackStatus.ESCALATED, "Escalated to Engineering: " + issueCode);
        auditService.logEvent("ADMIN_FEEDBACK_ESCALATED", "APPLICATION_FEEDBACK", id.toString(), null,
                "Escalated feedback to engineering issue " + issueCode);

        return toEngineeringIssueDto(savedIssue);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminFeedbackStatsDto getStats() {
        long total = feedbackRepository.count();
        long newCount = feedbackRepository.countByStatus(ApplicationFeedbackStatus.NEW);
        long underReview = feedbackRepository.countByStatus(ApplicationFeedbackStatus.UNDER_REVIEW);
        long assigned = feedbackRepository.countByStatus(ApplicationFeedbackStatus.ASSIGNED);
        long inProgress = feedbackRepository.countByStatus(ApplicationFeedbackStatus.IN_PROGRESS);
        long escalated = feedbackRepository.countByStatus(ApplicationFeedbackStatus.ESCALATED);
        long resolved = feedbackRepository.countByStatus(ApplicationFeedbackStatus.RESOLVED);
        long closed = feedbackRepository.countByStatus(ApplicationFeedbackStatus.CLOSED);
        long rejected = feedbackRepository.countByStatus(ApplicationFeedbackStatus.REJECTED);
        long duplicate = feedbackRepository.countByStatus(ApplicationFeedbackStatus.DUPLICATE);

        // Count priorities
        long critical = feedbackRepository.count((root, q, cb) -> cb.equal(root.get("priority"), ApplicationFeedbackPriority.CRITICAL));
        long high = feedbackRepository.count((root, q, cb) -> cb.equal(root.get("priority"), ApplicationFeedbackPriority.HIGH));

        return AdminFeedbackStatsDto.builder()
                .totalCount(total)
                .newCount(newCount)
                .underReviewCount(underReview)
                .assignedCount(assigned)
                .inProgressCount(inProgress)
                .escalatedCount(escalated)
                .resolvedCount(resolved)
                .closedCount(closed)
                .rejectedCount(rejected)
                .duplicateCount(duplicate)
                .criticalCount(critical)
                .highCount(high)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAssigneeDto> getAssignees() {
        return userRepository.findAll().stream()
                .filter(u -> u.getStatus() == UserEntity.UserStatus.ACTIVE)
                .map(u -> AdminAssigneeDto.builder()
                        .userId(u.getId())
                        .name(u.getFullName())
                        .email(u.getEmail())
                        .role(u.getRoles().isEmpty() ? "Staff" : u.getRoles().iterator().next().getName())
                        .build())
                .sorted(Comparator.comparing(AdminAssigneeDto::getName))
                .toList();
    }

    @Override
    public List<FeedbackTeam> getTeams() {
        return List.of(FeedbackTeam.values());
    }

    // ==========================================
    // Helper Methods & Mappers
    // ==========================================

    private ApplicationFeedbackEntity requireFeedback(UUID id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application Feedback not found with id: " + id));
    }

    private void recordStatusHistory(UUID feedbackId, ApplicationFeedbackStatus oldStatus, ApplicationFeedbackStatus newStatus, String reason) {
        UUID changedBy = SecurityUtils.getCurrentUserId();
        String changedByName = userRepository.findById(changedBy)
                .map(UserEntity::getFullName).orElse("Taxoryn Admin");

        FeedbackStatusHistoryEntity history = FeedbackStatusHistoryEntity.builder()
                .feedbackId(feedbackId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .changedByName(changedByName)
                .reason(reason)
                .build();
        statusHistoryRepository.save(history);
    }

    private String formatFeedbackCode(UUID id) {
        return "FB-" + id.toString().substring(0, 8).toUpperCase();
    }

    private AdminApplicationFeedbackSummaryDto toSummaryDto(
            ApplicationFeedbackEntity f,
            Map<UUID, UserEntity> userMap,
            Map<UUID, OrganizationEntity> orgMap
    ) {
        UserEntity reporter = f.getUserId() != null ? userMap.get(f.getUserId()) : null;
        UserEntity assignedUser = f.getAssignedUserId() != null ? userMap.get(f.getAssignedUserId()) : null;
        OrganizationEntity practice = f.getPracticeId() != null ? orgMap.get(f.getPracticeId()) : null;

        String excerpt = f.getDescription();
        if (excerpt != null && excerpt.length() > 140) {
            excerpt = excerpt.substring(0, 137) + "...";
        }

        return AdminApplicationFeedbackSummaryDto.builder()
                .id(f.getId())
                .feedbackCode(formatFeedbackCode(f.getId()))
                .actorType(f.getActorType())
                .contextType(f.getContextType())
                .type(f.getType())
                .category(f.getCategory())
                .rating(f.getRating())
                .title(f.getTitle())
                .descriptionExcerpt(excerpt)
                .page(f.getPage())
                .feature(f.getFeature())
                .status(f.getStatus())
                .priority(f.getPriority())
                .assignedTeam(f.getAssignedTeam())
                .assignedUserId(f.getAssignedUserId())
                .assignedUserName(assignedUser != null ? assignedUser.getFullName() : null)
                .practiceId(f.getPracticeId())
                .practiceName(practice != null ? practice.getName() : null)
                .reporterName(reporter != null ? reporter.getFullName() : "Anonymous / System")
                .reporterEmail(reporter != null ? reporter.getEmail() : null)
                .hasEngineeringIssue(f.getStatus() == ApplicationFeedbackStatus.ESCALATED)
                .hasDuplicateOf(f.getDuplicateOfId() != null)
                .duplicateOfId(f.getDuplicateOfId())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }

    private FeedbackAssignmentDto toAssignmentDto(FeedbackAssignmentEntity entity) {
        UserEntity assignedUser = entity.getAssignedUserId() != null ? userRepository.findById(entity.getAssignedUserId()).orElse(null) : null;
        UserEntity assignedBy = entity.getAssignedBy() != null ? userRepository.findById(entity.getAssignedBy()).orElse(null) : null;

        return FeedbackAssignmentDto.builder()
                .id(entity.getId())
                .feedbackId(entity.getFeedbackId())
                .team(entity.getTeam())
                .assignedUserId(entity.getAssignedUserId())
                .assignedUserName(assignedUser != null ? assignedUser.getFullName() : null)
                .assignedUserEmail(assignedUser != null ? assignedUser.getEmail() : null)
                .assignedBy(entity.getAssignedBy())
                .assignedByName(assignedBy != null ? assignedBy.getFullName() : null)
                .reason(entity.getReason())
                .assignedAt(entity.getAssignedAt())
                .unassignedAt(entity.getUnassignedAt())
                .active(entity.isActive())
                .build();
    }

    private FeedbackNoteDto toNoteDto(FeedbackNoteEntity entity) {
        return FeedbackNoteDto.builder()
                .id(entity.getId())
                .feedbackId(entity.getFeedbackId())
                .authorId(entity.getAuthorId())
                .authorName(entity.getAuthorName())
                .note(entity.getNote())
                .visibility(entity.getVisibility())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private FeedbackStatusHistoryDto toStatusHistoryDto(FeedbackStatusHistoryEntity entity) {
        return FeedbackStatusHistoryDto.builder()
                .id(entity.getId())
                .feedbackId(entity.getFeedbackId())
                .oldStatus(entity.getOldStatus())
                .newStatus(entity.getNewStatus())
                .changedBy(entity.getChangedBy())
                .changedByName(entity.getChangedByName())
                .reason(entity.getReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private EngineeringIssueDto toEngineeringIssueDto(FeedbackEngineeringIssueEntity entity) {
        UserEntity creator = entity.getCreatorUserId() != null ? userRepository.findById(entity.getCreatorUserId()).orElse(null) : null;

        return EngineeringIssueDto.builder()
                .id(entity.getId())
                .feedbackId(entity.getFeedbackId())
                .issueCode(entity.getIssueCode())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .assignedTeam(entity.getAssignedTeam())
                .createdBy(entity.getCreatorUserId())
                .createdByName(creator != null ? creator.getFullName() : null)
                .externalSystem(entity.getExternalSystem())
                .externalIssueId(entity.getExternalIssueId())
                .externalIssueUrl(entity.getExternalIssueUrl())
                .externalStatus(entity.getExternalStatus())
                .lastSyncedAt(entity.getLastSyncedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
