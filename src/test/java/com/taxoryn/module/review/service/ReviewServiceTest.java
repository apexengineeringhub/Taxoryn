package com.taxoryn.module.review.service;

import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.compliance.entity.ComplianceWorkflowEntity;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.compliance.repository.ComplianceWorkflowRepository;
import com.taxoryn.module.compliance.service.ComplianceWorkflowService;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.notification.service.NotificationService;
import com.taxoryn.module.review.dto.ReviewRequestDto;
import com.taxoryn.module.review.entity.ReviewRequestEntity;
import com.taxoryn.module.review.entity.ReviewStatus;
import com.taxoryn.module.review.repository.ReviewActionRepository;
import com.taxoryn.module.review.repository.ReviewRequestRepository;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock private ReviewRequestRepository reviewRepository;
    @Mock private ReviewActionRepository actionRepository;
    @Mock private ComplianceWorkflowRepository workflowRepository;
    @Mock private ComplianceWorkflowService workflowService;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private PracticeSecurityScopeEvaluator scopeEvaluator;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @InjectMocks private ReviewService service;

    private MockedStatic<SecurityUtils> security;
    private final UUID orgId = UUID.randomUUID();
    private final UUID reviewerId = UUID.randomUUID();
    private final UUID reviewId = UUID.randomUUID();
    private final UUID workflowId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::getCurrentOrganizationId).thenReturn(orgId);
        security.when(SecurityUtils::getCurrentUserId).thenReturn(reviewerId);
        security.when(() -> SecurityUtils.hasAuthority("REVIEW_APPROVE")).thenReturn(true);
        PracticeSecurityScope scope = mock(PracticeSecurityScope.class);
        lenient().when(scope.isFirmAdmin()).thenReturn(true);
        lenient().when(scopeEvaluator.evaluateCurrentScope()).thenReturn(scope);
        lenient().when(scopeEvaluator.getAccessibleClientIdsForScope(scope)).thenReturn(null);
    }

    @AfterEach
    void tearDown() { security.close(); }

    @Test
    void unauthorizedUserCannotSubmitReview() {
        security.when(() -> SecurityUtils.hasAuthority("REVIEW_SUBMIT")).thenReturn(false);
        assertThatThrownBy(() -> service.submit(new com.taxoryn.module.review.dto.SubmitReviewRequest()))
                .isInstanceOf(ForbiddenException.class);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void assignedReviewerCanApproveAndHistoryIsReturned() {
        ReviewRequestEntity review = new ReviewRequestEntity();
        review.setId(reviewId);
        review.setOrganizationId(orgId);
        review.setResourceType("COMPLIANCE_WORK");
        review.setResourceId(workflowId);
        review.setRequestedBy(UUID.randomUUID());
        review.setAssignedReviewerId(reviewerId);
        review.setStatus(ReviewStatus.UNDER_REVIEW);
        ComplianceWorkflowEntity workflow = new ComplianceWorkflowEntity();
        workflow.setId(workflowId);
        workflow.setOrganizationId(orgId);
        workflow.setClientId(clientId);
        workflow.setWorkflowStatus(ComplianceWorkflowStatus.UNDER_REVIEW);
        when(reviewRepository.findForUpdate(reviewId, orgId)).thenReturn(Optional.of(review));
        when(workflowRepository.findByIdAndOrganizationId(workflowId, orgId)).thenReturn(Optional.of(workflow));
        when(reviewRepository.save(any(ReviewRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRepository.findByOrganizationIdAndReviewRequestIdOrderByOccurredAtAsc(orgId, reviewId)).thenReturn(List.of());

        ReviewRequestDto result = service.approve(reviewId, "Checked supporting documents");

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(result.getReviewedBy()).isEqualTo(reviewerId);
        verify(workflowService).approveWorkflow(org.mockito.ArgumentMatchers.eq(workflowId), any());
        verify(actionRepository).save(any());
    }

    @Test
    void makerCannotApproveOwnReview() {
        ReviewRequestEntity review = new ReviewRequestEntity();
        review.setId(reviewId);
        review.setOrganizationId(orgId);
        review.setResourceType("COMPLIANCE_WORK");
        review.setResourceId(workflowId);
        review.setRequestedBy(reviewerId);
        review.setAssignedReviewerId(reviewerId);
        review.setStatus(ReviewStatus.UNDER_REVIEW);
        ComplianceWorkflowEntity workflow = new ComplianceWorkflowEntity();
        workflow.setId(workflowId);
        workflow.setOrganizationId(orgId);
        workflow.setClientId(clientId);
        when(reviewRepository.findForUpdate(reviewId, orgId)).thenReturn(Optional.of(review));
        when(workflowRepository.findByIdAndOrganizationId(workflowId, orgId)).thenReturn(Optional.of(workflow));

        assertThatThrownBy(() -> service.approve(reviewId, null)).isInstanceOf(ForbiddenException.class);
        verify(workflowService, never()).approveWorkflow(any(), any());
    }

    @Test
    void reviewerWithoutPermissionCannotApprove() {
        security.when(() -> SecurityUtils.hasAuthority("REVIEW_APPROVE")).thenReturn(false);
        assertThatThrownBy(() -> service.approve(reviewId, null)).isInstanceOf(ForbiddenException.class);
        verify(reviewRepository, never()).findForUpdate(any(), any());
    }

    @Test
    void reviewerWithoutClientScopeCannotApprove() {
        ReviewRequestEntity review = new ReviewRequestEntity();
        review.setId(reviewId);
        review.setOrganizationId(orgId);
        review.setResourceType("COMPLIANCE_WORK");
        review.setResourceId(workflowId);
        review.setRequestedBy(UUID.randomUUID());
        review.setAssignedReviewerId(reviewerId);
        review.setStatus(ReviewStatus.UNDER_REVIEW);
        ComplianceWorkflowEntity workflow = new ComplianceWorkflowEntity();
        workflow.setId(workflowId);
        workflow.setOrganizationId(orgId);
        workflow.setClientId(clientId);
        when(reviewRepository.findForUpdate(reviewId, orgId)).thenReturn(Optional.of(review));
        when(workflowRepository.findByIdAndOrganizationId(workflowId, orgId)).thenReturn(Optional.of(workflow));
        PracticeSecurityScope scope = mock(PracticeSecurityScope.class);
        when(scopeEvaluator.evaluateCurrentScope()).thenReturn(scope);
        when(scopeEvaluator.getAccessibleClientIdsForScope(scope)).thenReturn(Set.of());

        assertThatThrownBy(() -> service.approve(reviewId, null)).isInstanceOf(ForbiddenException.class);
        verify(workflowService, never()).approveWorkflow(any(), any());
    }

    @Test
    void rejectedReviewCanBeResubmittedByOriginalPreparer() {
        UUID preparerId = UUID.randomUUID();
        ReviewRequestEntity review = new ReviewRequestEntity();
        review.setId(reviewId);
        review.setOrganizationId(orgId);
        review.setResourceType("COMPLIANCE_WORK");
        review.setResourceId(workflowId);
        review.setRequestedBy(preparerId);
        review.setAssignedReviewerId(reviewerId);
        review.setStatus(ReviewStatus.REJECTED);
        security.when(SecurityUtils::getCurrentUserId).thenReturn(preparerId);
        security.when(() -> SecurityUtils.hasAuthority("REVIEW_SUBMIT")).thenReturn(true);
        EmployeeEntity reviewer = EmployeeEntity.builder().userId(reviewerId).status(EmployeeEntity.EmployeeStatus.ACTIVE)
                .employeeCode("REV-1").firstName("Reviewer").build();
        when(reviewRepository.findForUpdate(reviewId, orgId)).thenReturn(Optional.of(review));
        when(workflowRepository.findByIdAndOrganizationId(workflowId, orgId)).thenReturn(Optional.of(
                ComplianceWorkflowEntity.builder().clientId(clientId).build()));
        when(employeeRepository.findByOrganizationIdAndUserId(orgId, reviewerId)).thenReturn(Optional.of(reviewer));
        when(reviewRepository.save(any(ReviewRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRepository.findByOrganizationIdAndReviewRequestIdOrderByOccurredAtAsc(orgId, reviewId)).thenReturn(List.of());

        ReviewRequestDto result = service.resubmit(reviewId);

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.UNDER_REVIEW);
        verify(workflowService).submitReview(workflowId);
    }

    @Test
    void rejectionRequiresMeaningfulReason() {
        assertThatThrownBy(() -> service.reject(reviewId, " ")).isInstanceOf(com.taxoryn.core.exception.BusinessValidationException.class);
        verify(reviewRepository, never()).findForUpdate(any(), any());
    }

    @Test
    void approvedReviewCannotBeApprovedAgainAndReturnsConflict() {
        ReviewRequestEntity review = new ReviewRequestEntity();
        review.setId(reviewId);
        review.setOrganizationId(orgId);
        review.setResourceType("COMPLIANCE_WORK");
        review.setResourceId(workflowId);
        review.setRequestedBy(UUID.randomUUID());
        review.setAssignedReviewerId(reviewerId);
        review.setStatus(ReviewStatus.APPROVED);
        ComplianceWorkflowEntity workflow = new ComplianceWorkflowEntity();
        workflow.setId(workflowId);
        workflow.setOrganizationId(orgId);
        workflow.setClientId(clientId);
        when(reviewRepository.findForUpdate(reviewId, orgId)).thenReturn(Optional.of(review));
        when(workflowRepository.findByIdAndOrganizationId(workflowId, orgId)).thenReturn(Optional.of(workflow));

        assertThatThrownBy(() -> service.approve(reviewId, null))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        verify(workflowService, never()).approveWorkflow(any(), any());
    }
}
