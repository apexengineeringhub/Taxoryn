package com.taxoryn.module.review.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.compliance.dto.ApproveWorkflowRequest;
import com.taxoryn.module.compliance.dto.RequestWorkflowChangesRequest;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.compliance.service.ComplianceWorkflowService;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import com.taxoryn.module.notification.entity.NotificationEntity;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.review.dto.ReviewActionDto;
import com.taxoryn.module.review.dto.ReviewRequestDto;
import com.taxoryn.module.review.dto.SubmitReviewRequest;
import com.taxoryn.module.review.entity.ReviewActionEntity;
import com.taxoryn.module.review.entity.ReviewRequestEntity;
import com.taxoryn.module.review.entity.ReviewStatus;
import com.taxoryn.module.review.repository.ReviewActionRepository;
import com.taxoryn.module.review.repository.ReviewRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private static final String RESOURCE_COMPLIANCE_WORK = "COMPLIANCE_WORK";
    private final ReviewRequestRepository reviewRepository;
    private final ReviewActionRepository actionRepository;
    private final ComplianceWorkflowRepository workflowRepository;
    private final ComplianceWorkflowService workflowService;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PracticeSecurityScopeEvaluator scopeEvaluator;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional
    public ReviewRequestDto submit(SubmitReviewRequest request) {
        requirePermission("REVIEW_SUBMIT");
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        ComplianceWorkflowEntity workflow = resolveWorkflow(request.getResourceType(), request.getResourceId(), orgId);
        requireClientScope(workflow.getClientId());
        if (workflow.getReviewerEmployeeId() == null) {
            throw new BusinessValidationException("A reviewer must be assigned to the compliance workflow before submission");
        }
        EmployeeEntity reviewer = employeeRepository.findByIdAndOrganizationId(workflow.getReviewerEmployeeId(), orgId)
                .filter(e -> e.getStatus() == EmployeeEntity.EmployeeStatus.ACTIVE && e.getUserId() != null)
                .orElseThrow(() -> new BusinessValidationException("Assigned reviewer must be an active user in this organization"));
        UUID reviewerUserId = reviewer.getUserId();
        UserEntity reviewerAccount = userRepository.findByIdAndOrganizationId(reviewerUserId, orgId)
                .filter(u -> u.getStatus() == UserEntity.UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessValidationException("Assigned reviewer account must be active in this organization"));
        boolean canReview = reviewerAccount.getRoles().stream().flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> "REVIEW_APPROVE".equals(p.getCode()) && p.getModule().equals("REVIEW"));
        if (!canReview) throw new BusinessValidationException("Assigned reviewer lacks review approval permission");
        if (!scopeEvaluator.userHasClientAccess(orgId, reviewerAccount, workflow.getClientId())) {
            throw new BusinessValidationException("Assigned reviewer does not have access to this client portfolio");
        }
        if (actorId.equals(reviewerUserId)) {
            throw new ForbiddenException("The preparer cannot approve their own review");
        }
        if (workflow.getWorkflowStatus() != ComplianceWorkflowStatus.IN_PROGRESS
                && workflow.getWorkflowStatus() != ComplianceWorkflowStatus.CHANGES_REQUIRED) {
            throw new BusinessValidationException("Only in-progress or returned compliance work can be submitted for review");
        }
        var existing = reviewRepository.findFirstByOrganizationIdAndResourceTypeAndResourceIdAndStatusInOrderByRequestedAtDesc(
                orgId, RESOURCE_COMPLIANCE_WORK, workflow.getId(), List.of(ReviewStatus.SUBMITTED, ReviewStatus.UNDER_REVIEW));
        if (existing.isPresent()) throw new BusinessValidationException("This compliance work already has an active review");

        workflowService.submitReview(workflow.getId());
        ReviewRequestEntity review = new ReviewRequestEntity();
        review.setOrganizationId(orgId);
        review.setResourceType(RESOURCE_COMPLIANCE_WORK);
        review.setResourceId(workflow.getId());
        review.setReviewType("COMPLIANCE_WORK");
        review.setStatus(ReviewStatus.UNDER_REVIEW);
        review.setRequestedBy(actorId);
        review.setAssignedReviewerId(reviewerUserId);
        review.setRequestedAt(Instant.now());
        review = reviewRepository.save(review);
        appendAction(review, "SUBMITTED", actorId, null, orgId);
        appendAction(review, "REVIEW_ASSIGNED", actorId, null, orgId);
        appendAction(review, "UNDER_REVIEW", actorId, null, orgId);
        audit("REVIEW_SUBMITTED", review, null, ReviewStatus.UNDER_REVIEW);
        audit("REVIEW_ASSIGNED", review, null, reviewerUserId);
        audit("REVIEW_STARTED", review, ReviewStatus.SUBMITTED, ReviewStatus.UNDER_REVIEW);
        notificationService.notify(orgId, reviewerUserId, null,
                NotificationEntity.NotificationType.TASK_ASSIGNED,
                NotificationEntity.Severity.ACTION_REQUIRED, NotificationEntity.Category.COMPLIANCE,
                "REVIEW_REQUEST", review.getId().toString(), "Compliance work needs review",
                "A compliance work item has been submitted for your review.", Set.of(),
                "/compliance/workflows/" + workflow.getId(), null, null);
        return map(review);
    }

    @Transactional(readOnly = true)
    public ReviewRequestDto get(UUID id) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        ReviewRequestEntity review = reviewRepository.findById(id).filter(r -> orgId.equals(r.getOrganizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Review request not found"));
        authorizeView(review, orgId);
        return map(review);
    }

    @Transactional(readOnly = true)
    public ReviewRequestDto getForResource(String resourceType, UUID resourceId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        ComplianceWorkflowEntity workflow = resolveWorkflow(resourceType, resourceId, orgId);
        requireClientScope(workflow.getClientId());
        ReviewRequestEntity review = reviewRepository.findFirstByOrganizationIdAndResourceTypeAndResourceIdOrderByRequestedAtDesc(
                orgId, RESOURCE_COMPLIANCE_WORK, resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Review request not found"));
        authorizeView(review, orgId);
        return map(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewRequestDto> list() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        return reviewRepository.findTop100ByOrganizationIdOrderByRequestedAtDesc(orgId).stream()
                .filter(r -> canView(r, orgId)).map(this::map).toList();
    }

    @Transactional
    public ReviewRequestDto approve(UUID id, String comment) {
        ReviewRequestEntity review = lockAndAuthorize(id, "REVIEW_APPROVE");
        UUID orgId = review.getOrganizationId();
        requireActiveState(review);
        review.setStatus(ReviewStatus.APPROVED);
        review.setReviewedAt(Instant.now());
        review.setReviewedBy(SecurityUtils.getCurrentUserId());
        review.setReviewComment(comment);
        workflowService.approveWorkflow(review.getResourceId(), ApproveWorkflowRequest.builder().approvalNotes(comment).build());
        ComplianceWorkflowEntity approvedWorkflow = resolveWorkflow(review.getResourceType(), review.getResourceId(), orgId);
        UUID nextOwnerId = approvedWorkflow.getAssignedUserId() != null ? approvedWorkflow.getAssignedUserId() : review.getRequestedBy();
        if (nextOwnerId != null && !nextOwnerId.equals(review.getReviewedBy())) {
            notificationService.notify(orgId, nextOwnerId, null,
                    NotificationEntity.NotificationType.TASK_ASSIGNED, NotificationEntity.Severity.SUCCESS,
                    NotificationEntity.Category.COMPLIANCE, "REVIEW_REQUEST", review.getId().toString(),
                    "Compliance work approved", "Your compliance work was approved and is ready for the next step.",
                    Set.of(), "/compliance/workflows/" + review.getResourceId(), null, null);
        }
        review = reviewRepository.save(review);
        appendAction(review, "APPROVED", SecurityUtils.getCurrentUserId(), comment, orgId);
        audit("REVIEW_APPROVED", review, ReviewStatus.UNDER_REVIEW, ReviewStatus.APPROVED);
        return map(review);
    }

    @Transactional
    public ReviewRequestDto reject(UUID id, String reason) {
        if (!StringUtils.hasText(reason) || reason.trim().length() < 5) throw new BusinessValidationException("A meaningful rejection reason is required");
        ReviewRequestEntity review = lockAndAuthorize(id, "REVIEW_REJECT");
        UUID orgId = review.getOrganizationId();
        requireActiveState(review);
        review.setStatus(ReviewStatus.REJECTED);
        review.setReviewedAt(Instant.now());
        review.setReviewedBy(SecurityUtils.getCurrentUserId());
        review.setReviewComment(reason.trim());
        review.setRejectionReason(reason.trim());
        workflowService.requestChanges(review.getResourceId(), RequestWorkflowChangesRequest.builder().reason(reason.trim()).build());
        review = reviewRepository.save(review);
        appendAction(review, "REJECTED", SecurityUtils.getCurrentUserId(), reason.trim(), orgId);
        audit("REVIEW_REJECTED", review, ReviewStatus.UNDER_REVIEW, ReviewStatus.REJECTED);
        notificationService.notify(orgId, review.getRequestedBy(), null,
                NotificationEntity.NotificationType.TASK_ASSIGNED,
                NotificationEntity.Severity.WARNING, NotificationEntity.Category.COMPLIANCE,
                "REVIEW_REQUEST", review.getId().toString(), "Compliance work returned for changes",
                reason.trim(), Set.of(), "/compliance/workflows/" + review.getResourceId(), null, null);
        return map(review);
    }

    @Transactional
    public ReviewRequestDto resubmit(UUID id) {
        requirePermission("REVIEW_SUBMIT");
        UUID actor = SecurityUtils.getCurrentUserId();
        ReviewRequestEntity review = reviewRepository.findForUpdate(id, SecurityUtils.getCurrentOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Review request not found"));
        requireClientScope(resolveWorkflow(review.getResourceType(), review.getResourceId(), review.getOrganizationId()).getClientId());
        if (review.getStatus() != ReviewStatus.REJECTED || !actor.equals(review.getRequestedBy())) {
            throw new ForbiddenException("Only the original preparer can resubmit a rejected review");
        }
        EmployeeEntity reviewer = employeeRepository.findByOrganizationIdAndUserId(review.getOrganizationId(), review.getAssignedReviewerId())
                .filter(e -> e.getStatus() == EmployeeEntity.EmployeeStatus.ACTIVE)
                .orElseThrow(() -> new BusinessValidationException("Assigned reviewer is no longer active"));
        if (actor.equals(reviewer.getUserId())) throw new ForbiddenException("The preparer cannot approve their own review");
        workflowService.submitReview(review.getResourceId());
        ReviewStatus old = review.getStatus();
        review.setStatus(ReviewStatus.UNDER_REVIEW);
        review.setRequestedAt(Instant.now());
        review.setReviewedAt(null);
        review.setReviewedBy(null);
        review.setReviewComment(null);
        review.setRejectionReason(null);
        review = reviewRepository.save(review);
        appendAction(review, "RESUBMITTED", actor, null, review.getOrganizationId());
        appendAction(review, "UNDER_REVIEW", actor, null, review.getOrganizationId());
        audit("REVIEW_RESUBMITTED", review, old, ReviewStatus.UNDER_REVIEW);
        audit("REVIEW_STARTED", review, ReviewStatus.REJECTED, ReviewStatus.UNDER_REVIEW);
        notificationService.notify(review.getOrganizationId(), review.getAssignedReviewerId(), null,
                NotificationEntity.NotificationType.TASK_ASSIGNED, NotificationEntity.Severity.ACTION_REQUIRED,
                NotificationEntity.Category.COMPLIANCE, "REVIEW_REQUEST", review.getId().toString(),
                "Compliance work resubmitted for review", "A compliance work item is ready for review again.",
                Set.of(), "/compliance/workflows/" + review.getResourceId(), null, null);
        return map(review);
    }

    @Transactional
    public ReviewRequestDto cancel(UUID id) {
        requirePermission("REVIEW_SUBMIT");
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        ReviewRequestEntity review = reviewRepository.findForUpdate(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Review request not found"));
        requireClientScope(resolveWorkflow(review.getResourceType(), review.getResourceId(), orgId).getClientId());
        if (!SecurityUtils.getCurrentUserId().equals(review.getRequestedBy())) throw new ForbiddenException("Only the preparer can cancel this review");
        if (review.getStatus() != ReviewStatus.SUBMITTED && review.getStatus() != ReviewStatus.UNDER_REVIEW) throw new BusinessValidationException("Review is not cancellable");
        ReviewStatus old = review.getStatus();
        workflowService.withdrawReview(review.getResourceId());
        review.setStatus(ReviewStatus.CANCELLED);
        review = reviewRepository.save(review);
        appendAction(review, "CANCELLED", SecurityUtils.getCurrentUserId(), null, orgId);
        audit("REVIEW_CANCELLED", review, old, ReviewStatus.CANCELLED);
        return map(review);
    }

    private ReviewRequestEntity lockAndAuthorize(UUID id, String permission) {
        requirePermission(permission);
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        ReviewRequestEntity review = reviewRepository.findForUpdate(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Review request not found"));
        ComplianceWorkflowEntity workflow = resolveWorkflow(review.getResourceType(), review.getResourceId(), orgId);
        requireClientScope(workflow.getClientId());
        if (!SecurityUtils.getCurrentUserId().equals(review.getAssignedReviewerId())) throw new ForbiddenException("Only the assigned reviewer may decide this review");
        return review;
    }

    private void requireActiveState(ReviewRequestEntity review) {
        if (review.getStatus() != ReviewStatus.UNDER_REVIEW) {
            throw new AppException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT, "Review is no longer awaiting a decision");
        }
        if (review.getRequestedBy().equals(SecurityUtils.getCurrentUserId())) throw new ForbiddenException("Maker-checker rule prevents self-approval");
    }

    private ComplianceWorkflowEntity resolveWorkflow(String type, UUID resourceId, UUID orgId) {
        if (!RESOURCE_COMPLIANCE_WORK.equalsIgnoreCase(type)) throw new BusinessValidationException("Only COMPLIANCE_WORK reviews are enabled in this release");
        return workflowRepository.findByIdAndOrganizationId(resourceId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance work not found"));
    }

    private void requireClientScope(UUID clientId) {
        PracticeSecurityScope scope = scopeEvaluator.evaluateCurrentScope();
        Set<UUID> accessible = scopeEvaluator.getAccessibleClientIdsForScope(scope);
        if (accessible != null && !accessible.contains(clientId)) throw new ForbiddenException("You do not have access to this client");
    }

    private void requirePermission(String permission) {
        if (!SecurityUtils.hasAuthority(permission)) throw new ForbiddenException("Missing permission: " + permission);
    }

    private boolean canView(ReviewRequestEntity review, UUID orgId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId.equals(review.getRequestedBy()) || userId.equals(review.getAssignedReviewerId())) {
            try { requireClientScope(resolveWorkflow(review.getResourceType(), review.getResourceId(), orgId).getClientId()); return true; }
            catch (ForbiddenException ex) { return false; }
        }
        if (!SecurityUtils.hasAuthority("REVIEW_VIEW")) return false;
        try { requireClientScope(resolveWorkflow(review.getResourceType(), review.getResourceId(), orgId).getClientId()); return true; }
        catch (ForbiddenException ex) { return false; }
    }

    private void authorizeView(ReviewRequestEntity review, UUID orgId) {
        if (!canView(review, orgId)) throw new ForbiddenException("You do not have access to this review");
    }

    private void appendAction(ReviewRequestEntity review, String action, UUID actor, String comment, UUID orgId) {
        ReviewActionEntity history = new ReviewActionEntity();
        history.setOrganizationId(orgId);
        history.setReviewRequestId(review.getId());
        history.setAction(action);
        history.setActorId(actor);
        history.setComment(comment);
        history.setOccurredAt(Instant.now());
        actionRepository.save(history);
    }

    private void audit(String action, ReviewRequestEntity review, Object oldValue, Object newValue) {
        auditService.logEvent(review.getOrganizationId(), SecurityUtils.getCurrentUserId(), action,
                "REVIEW_REQUEST", review.getId().toString(), oldValue, newValue);
    }

    private ReviewRequestDto map(ReviewRequestEntity review) {
        List<ReviewActionDto> history = actionRepository.findByOrganizationIdAndReviewRequestIdOrderByOccurredAtAsc(
                review.getOrganizationId(), review.getId()).stream().map(a -> ReviewActionDto.builder()
                .action(a.getAction()).actorId(a.getActorId()).occurredAt(a.getOccurredAt()).comment(a.getComment()).build()).toList();
        return ReviewRequestDto.builder().id(review.getId()).resourceType(review.getResourceType()).resourceId(review.getResourceId())
                .reviewType(review.getReviewType()).status(review.getStatus()).requestedBy(review.getRequestedBy())
                .assignedReviewerId(review.getAssignedReviewerId()).requestedAt(review.getRequestedAt()).reviewedAt(review.getReviewedAt())
                .reviewedBy(review.getReviewedBy()).reviewComment(review.getReviewComment()).rejectionReason(review.getRejectionReason())
                .version(review.getVersion()).history(history).build();
    }
}
