package com.taxoryn.module.feedback.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.feedback.dto.*;
import com.taxoryn.module.feedback.entity.*;
import com.taxoryn.module.feedback.repository.*;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminApplicationFeedbackServiceTest {

    @Mock
    private ApplicationFeedbackRepository feedbackRepository;

    @Mock
    private FeedbackAssignmentRepository assignmentRepository;

    @Mock
    private FeedbackNoteRepository noteRepository;

    @Mock
    private FeedbackStatusHistoryRepository statusHistoryRepository;

    @Mock
    private FeedbackEngineeringIssueRepository engineeringIssueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminApplicationFeedbackServiceImpl adminFeedbackService;

    private UUID adminUserId;
    private UUID feedbackId;
    private UUID reporterUserId;
    private UUID practiceId;

    private UserEntity adminUser;
    private UserEntity reporterUser;
    private OrganizationEntity practice;
    private ApplicationFeedbackEntity feedbackEntity;

    @BeforeEach
    void setUp() {
        adminUserId = UUID.randomUUID();
        feedbackId = UUID.randomUUID();
        reporterUserId = UUID.randomUUID();
        practiceId = UUID.randomUUID();

        adminUser = UserEntity.builder()
                .email("superadmin@taxoryn.com")
                .firstName("Super")
                .lastName("Admin")
                .status(UserEntity.UserStatus.ACTIVE)
                .build();
        adminUser.setId(adminUserId);

        reporterUser = UserEntity.builder()
                .email("user@taxoryn.com")
                .firstName("Raj")
                .lastName("Kumar")
                .status(UserEntity.UserStatus.ACTIVE)
                .build();
        reporterUser.setId(reporterUserId);

        practice = OrganizationEntity.builder()
                .name("Kumar & Co")
                .email("contact@kumarco.com")
                .subscriptionPlan(OrganizationEntity.SubscriptionPlan.PROFESSIONAL)
                .build();
        practice.setId(practiceId);

        feedbackEntity = ApplicationFeedbackEntity.builder()
                .userId(reporterUserId)
                .practiceId(practiceId)
                .actorType(ApplicationFeedbackActorType.PRACTITIONER)
                .contextType(ApplicationFeedbackContextType.PRACTICE_PORTAL)
                .type(ApplicationFeedbackType.PROBLEM)
                .category(ApplicationFeedbackCategory.TAX_SERVICES)
                .rating(2)
                .title("GSTR-3B Auto population error")
                .description("Auto calculation fails on Table 4 eligible ITC.")
                .page("/gst/returns/gstr3b")
                .feature("GST_AUTO_CALC")
                .source("WEB")
                .status(ApplicationFeedbackStatus.NEW)
                .priority(ApplicationFeedbackPriority.HIGH)
                .build();
        feedbackEntity.setId(feedbackId);

        SecurityUser securityUser = SecurityUser.builder()
                .userId(adminUserId)
                .email(adminUser.getEmail())
                .password("password")
                .enabled(true)
                .roles(Set.of("ROLE_SUPER_ADMIN"))
                .permissions(Set.of("FEEDBACK_MANAGE"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getFeedbackList: returns paged summaries enriched with reporter and practice names")
    void testGetFeedbackList() {
        Pageable pageable = PageRequest.of(0, 10);
        when(feedbackRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(feedbackEntity), pageable, 1));
        when(userRepository.findAllById(any())).thenReturn(List.of(reporterUser));
        when(organizationRepository.findAllById(any())).thenReturn(List.of(practice));

        PagedResponse<AdminApplicationFeedbackSummaryDto> response = adminFeedbackService.getFeedbackList(
                null, null, null, null, null, null, null, null, null, null, null, pageable
        );

        assertThat(response.getContent()).hasSize(1);
        AdminApplicationFeedbackSummaryDto summary = response.getContent().get(0);
        assertThat(summary.getId()).isEqualTo(feedbackId);
        assertThat(summary.getTitle()).isEqualTo("GSTR-3B Auto population error");
        assertThat(summary.getReporterName()).isEqualTo("Raj Kumar");
        assertThat(summary.getPracticeName()).isEqualTo("Kumar & Co");
        assertThat(summary.getStatus()).isEqualTo(ApplicationFeedbackStatus.NEW);
    }

    @Test
    @DisplayName("getFeedbackDetail: returns detailed feedback DTO with reporter, practice, notes, and timeline")
    void testGetFeedbackDetail() {
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));
        when(userRepository.findById(reporterUserId)).thenReturn(Optional.of(reporterUser));
        when(organizationRepository.findById(practiceId)).thenReturn(Optional.of(practice));
        when(assignmentRepository.findByFeedbackIdOrderByAssignedAtDesc(feedbackId)).thenReturn(Collections.emptyList());
        when(noteRepository.findByFeedbackIdOrderByCreatedAtAsc(feedbackId)).thenReturn(Collections.emptyList());
        when(statusHistoryRepository.findByFeedbackIdOrderByCreatedAtAsc(feedbackId)).thenReturn(Collections.emptyList());
        when(engineeringIssueRepository.findByFeedbackId(feedbackId)).thenReturn(Optional.empty());

        AdminApplicationFeedbackDetailDto detail = adminFeedbackService.getFeedbackDetail(feedbackId);

        assertThat(detail.getId()).isEqualTo(feedbackId);
        assertThat(detail.getReporterName()).isEqualTo("Raj Kumar");
        assertThat(detail.getPracticeName()).isEqualTo("Kumar & Co");
        assertThat(detail.getStatus()).isEqualTo(ApplicationFeedbackStatus.NEW);
    }

    @Test
    @DisplayName("startReview: transitions NEW to UNDER_REVIEW and logs timeline and audit")
    void testStartReview() {
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));
        when(feedbackRepository.save(any(ApplicationFeedbackEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(reporterUserId)).thenReturn(Optional.of(reporterUser));
        when(organizationRepository.findById(practiceId)).thenReturn(Optional.of(practice));

        AdminApplicationFeedbackDetailDto result = adminFeedbackService.startReview(feedbackId);

        assertThat(feedbackEntity.getStatus()).isEqualTo(ApplicationFeedbackStatus.UNDER_REVIEW);
        verify(statusHistoryRepository).save(any(FeedbackStatusHistoryEntity.class));
        verify(auditService).logEvent(eq("ADMIN_FEEDBACK_REVIEW_STARTED"), eq("APPLICATION_FEEDBACK"), eq(feedbackId.toString()), any(), any());
    }

    @Test
    @DisplayName("startReview: throws exception if feedback is already in terminal status CLOSED")
    void testStartReviewOnClosedFeedback() {
        feedbackEntity.setStatus(ApplicationFeedbackStatus.CLOSED);
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));

        assertThatThrownBy(() -> adminFeedbackService.startReview(feedbackId))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Cannot start review on feedback in terminal status");
    }

    @Test
    @DisplayName("assignFeedback: sets team, assignee, transitions NEW/UNDER_REVIEW to ASSIGNED, and creates assignment record")
    void testAssignFeedback() {
        UUID assignedStaffId = UUID.randomUUID();
        UserEntity assignedStaff = UserEntity.builder().firstName("Pooja").lastName("Mehta").email("pooja@taxoryn.com").build();
        assignedStaff.setId(assignedStaffId);

        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));
        when(userRepository.findById(assignedStaffId)).thenReturn(Optional.of(assignedStaff));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(reporterUserId)).thenReturn(Optional.of(reporterUser));
        when(organizationRepository.findById(practiceId)).thenReturn(Optional.of(practice));
        when(assignmentRepository.findByFeedbackIdAndActiveTrue(feedbackId)).thenReturn(Collections.emptyList());
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AssignFeedbackRequest request = AssignFeedbackRequest.builder()
                .team(FeedbackTeam.ENGINEERING)
                .assignedUserId(assignedStaffId)
                .reason("Critical calculation bug")
                .build();

        AdminApplicationFeedbackDetailDto result = adminFeedbackService.assignFeedback(feedbackId, request);

        assertThat(feedbackEntity.getStatus()).isEqualTo(ApplicationFeedbackStatus.ASSIGNED);
        assertThat(feedbackEntity.getAssignedTeam()).isEqualTo(FeedbackTeam.ENGINEERING);
        assertThat(feedbackEntity.getAssignedUserId()).isEqualTo(assignedStaffId);
        verify(assignmentRepository).save(any(FeedbackAssignmentEntity.class));
        verify(statusHistoryRepository).save(any(FeedbackStatusHistoryEntity.class));
    }

    @Test
    @DisplayName("addNote: creates internal note with author name and internal visibility")
    void testAddNote() {
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(noteRepository.save(any(FeedbackNoteEntity.class))).thenAnswer(i -> {
            FeedbackNoteEntity e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        CreateFeedbackNoteRequest req = CreateFeedbackNoteRequest.builder()
                .note("Investigated logs - appears to be an issue in RoundingUtil.")
                .visibility(FeedbackNoteVisibility.INTERNAL)
                .build();

        FeedbackNoteDto noteDto = adminFeedbackService.addNote(feedbackId, req);

        assertThat(noteDto.getNote()).isEqualTo("Investigated logs - appears to be an issue in RoundingUtil.");
        assertThat(noteDto.getAuthorName()).isEqualTo("Super Admin");
        assertThat(noteDto.getVisibility()).isEqualTo(FeedbackNoteVisibility.INTERNAL);
        verify(auditService).logEvent(eq("ADMIN_FEEDBACK_NOTE_ADDED"), eq("APPLICATION_FEEDBACK"), eq(feedbackId.toString()), any(), any());
    }

    @Test
    @DisplayName("updatePriority: updates priority and records history")
    void testUpdatePriority() {
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(reporterUserId)).thenReturn(Optional.of(reporterUser));
        when(organizationRepository.findById(practiceId)).thenReturn(Optional.of(practice));
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateFeedbackPriorityRequest req = UpdateFeedbackPriorityRequest.builder()
                .priority(ApplicationFeedbackPriority.CRITICAL)
                .reason("Impacts all filing clients during quarterly deadline")
                .build();

        AdminApplicationFeedbackDetailDto result = adminFeedbackService.updatePriority(feedbackId, req);

        assertThat(feedbackEntity.getPriority()).isEqualTo(ApplicationFeedbackPriority.CRITICAL);
        verify(statusHistoryRepository).save(any(FeedbackStatusHistoryEntity.class));
    }

    @Test
    @DisplayName("resolveFeedback: requires resolution notes, transitions to RESOLVED, and sets resolvedBy/resolvedAt")
    void testResolveFeedback() {
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(reporterUserId)).thenReturn(Optional.of(reporterUser));
        when(organizationRepository.findById(practiceId)).thenReturn(Optional.of(practice));
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ResolveFeedbackRequest req = ResolveFeedbackRequest.builder()
                .resolutionNote("Fixed in v1.2.4 patch release - verified calculation formulas.")
                .build();

        AdminApplicationFeedbackDetailDto result = adminFeedbackService.resolveFeedback(feedbackId, req);

        assertThat(feedbackEntity.getStatus()).isEqualTo(ApplicationFeedbackStatus.RESOLVED);
        assertThat(feedbackEntity.getResolutionNote()).isEqualTo("Fixed in v1.2.4 patch release - verified calculation formulas.");
        assertThat(feedbackEntity.getResolvedBy()).isEqualTo(adminUserId);
        assertThat(feedbackEntity.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("resolveFeedback: throws exception when resolution note is blank")
    void testResolveFeedbackBlankNote() {
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));

        ResolveFeedbackRequest req = ResolveFeedbackRequest.builder()
                .resolutionNote("   ")
                .build();

        assertThatThrownBy(() -> adminFeedbackService.resolveFeedback(feedbackId, req))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Resolution note is required");
    }

    @Test
    @DisplayName("closeFeedback: closes resolved feedback and sets closedBy/closedAt")
    void testCloseFeedback() {
        feedbackEntity.setStatus(ApplicationFeedbackStatus.RESOLVED);
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(reporterUserId)).thenReturn(Optional.of(reporterUser));
        when(organizationRepository.findById(practiceId)).thenReturn(Optional.of(practice));
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CloseFeedbackRequest req = CloseFeedbackRequest.builder().reason("Customer confirmed resolution in production").build();

        AdminApplicationFeedbackDetailDto result = adminFeedbackService.closeFeedback(feedbackId, req);

        assertThat(feedbackEntity.getStatus()).isEqualTo(ApplicationFeedbackStatus.CLOSED);
        assertThat(feedbackEntity.getClosedBy()).isEqualTo(adminUserId);
        assertThat(feedbackEntity.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("closeFeedback: throws exception if feedback is not in resolved, rejected, or duplicate state")
    void testCloseFeedbackInvalidState() {
        feedbackEntity.setStatus(ApplicationFeedbackStatus.IN_PROGRESS);
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));

        assertThatThrownBy(() -> adminFeedbackService.closeFeedback(feedbackId, new CloseFeedbackRequest()))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Only resolved, rejected, or duplicate feedback can be closed");
    }

    @Test
    @DisplayName("rejectFeedback: sets status to REJECTED and records reason")
    void testRejectFeedback() {
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(reporterUserId)).thenReturn(Optional.of(reporterUser));
        when(organizationRepository.findById(practiceId)).thenReturn(Optional.of(practice));
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RejectFeedbackRequest req = RejectFeedbackRequest.builder().reason("Intended system behavior per GST rules").build();

        AdminApplicationFeedbackDetailDto result = adminFeedbackService.rejectFeedback(feedbackId, req);

        assertThat(feedbackEntity.getStatus()).isEqualTo(ApplicationFeedbackStatus.REJECTED);
        verify(statusHistoryRepository).save(any(FeedbackStatusHistoryEntity.class));
    }

    @Test
    @DisplayName("markDuplicate: sets status to DUPLICATE and links duplicateOfId")
    void testMarkDuplicate() {
        UUID originalFeedbackId = UUID.randomUUID();
        ApplicationFeedbackEntity original = ApplicationFeedbackEntity.builder().title("Original report").build();
        original.setId(originalFeedbackId);

        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));
        when(feedbackRepository.findById(originalFeedbackId)).thenReturn(Optional.of(original));
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(reporterUserId)).thenReturn(Optional.of(reporterUser));
        when(organizationRepository.findById(practiceId)).thenReturn(Optional.of(practice));
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MarkDuplicateFeedbackRequest req = MarkDuplicateFeedbackRequest.builder()
                .duplicateOfId(originalFeedbackId)
                .reason("Duplicate of existing ticket")
                .build();

        AdminApplicationFeedbackDetailDto result = adminFeedbackService.markDuplicate(feedbackId, req);

        assertThat(feedbackEntity.getStatus()).isEqualTo(ApplicationFeedbackStatus.DUPLICATE);
        assertThat(feedbackEntity.getDuplicateOfId()).isEqualTo(originalFeedbackId);
    }

    @Test
    @DisplayName("markDuplicate: prevents self-duplicate reference")
    void testMarkDuplicateSelf() {
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));

        MarkDuplicateFeedbackRequest req = MarkDuplicateFeedbackRequest.builder()
                .duplicateOfId(feedbackId)
                .build();

        assertThatThrownBy(() -> adminFeedbackService.markDuplicate(feedbackId, req))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Feedback cannot be marked as a duplicate of itself");
    }

    @Test
    @DisplayName("escalateToEngineering: creates internal engineering issue, sets status to ESCALATED, and saves note")
    void testEscalateToEngineering() {
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));
        when(engineeringIssueRepository.findByFeedbackId(feedbackId)).thenReturn(Optional.empty());
        when(engineeringIssueRepository.save(any(FeedbackEngineeringIssueEntity.class))).thenAnswer(i -> {
            FeedbackEngineeringIssueEntity e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(noteRepository.save(any(FeedbackNoteEntity.class))).thenAnswer(i -> {
            FeedbackNoteEntity e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
        when(feedbackRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EscalateToEngineeringRequest req = EscalateToEngineeringRequest.builder()
                .title("Calculation discrepancy in ITC Table 4")
                .description("Formula precision error causes 1 rupee discrepancy in summary.")
                .priority(ApplicationFeedbackPriority.HIGH)
                .internalNotes("High priority for sprint release.")
                .build();

        EngineeringIssueDto issueDto = adminFeedbackService.escalateToEngineering(feedbackId, req);

        assertThat(issueDto.getIssueCode()).startsWith("ENG-");
        assertThat(issueDto.getTitle()).isEqualTo("Calculation discrepancy in ITC Table 4");
        assertThat(issueDto.getStatus()).isEqualTo(EngineeringIssueStatus.OPEN);
        assertThat(issueDto.getAssignedTeam()).isEqualTo("ENGINEERING");
        assertThat(feedbackEntity.getStatus()).isEqualTo(ApplicationFeedbackStatus.ESCALATED);
        verify(noteRepository).save(any(FeedbackNoteEntity.class));
        verify(statusHistoryRepository).save(any(FeedbackStatusHistoryEntity.class));
    }

    @Test
    @DisplayName("escalateToEngineering: prevents duplicate engineering issue creation on same feedback")
    void testEscalateToEngineeringDuplicatePrevention() {
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedbackEntity));
        when(engineeringIssueRepository.findByFeedbackId(feedbackId)).thenReturn(Optional.of(FeedbackEngineeringIssueEntity.builder().build()));

        EscalateToEngineeringRequest req = EscalateToEngineeringRequest.builder()
                .title("Bug")
                .description("Description")
                .build();

        assertThatThrownBy(() -> adminFeedbackService.escalateToEngineering(feedbackId, req))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("An engineering issue already exists for this feedback");
    }

    @Test
    @DisplayName("getStats: returns aggregate metrics")
    void testGetStats() {
        when(feedbackRepository.count()).thenReturn(10L);
        when(feedbackRepository.countByStatus(ApplicationFeedbackStatus.NEW)).thenReturn(3L);
        when(feedbackRepository.countByStatus(ApplicationFeedbackStatus.UNDER_REVIEW)).thenReturn(2L);
        when(feedbackRepository.countByStatus(ApplicationFeedbackStatus.ASSIGNED)).thenReturn(1L);
        when(feedbackRepository.countByStatus(ApplicationFeedbackStatus.IN_PROGRESS)).thenReturn(1L);
        when(feedbackRepository.countByStatus(ApplicationFeedbackStatus.ESCALATED)).thenReturn(1L);
        when(feedbackRepository.countByStatus(ApplicationFeedbackStatus.RESOLVED)).thenReturn(1L);
        when(feedbackRepository.countByStatus(ApplicationFeedbackStatus.CLOSED)).thenReturn(1L);
        when(feedbackRepository.countByStatus(ApplicationFeedbackStatus.REJECTED)).thenReturn(0L);
        when(feedbackRepository.countByStatus(ApplicationFeedbackStatus.DUPLICATE)).thenReturn(0L);
        when(feedbackRepository.count(any(Specification.class))).thenReturn(2L);

        AdminFeedbackStatsDto stats = adminFeedbackService.getStats();

        assertThat(stats.getTotalCount()).isEqualTo(10L);
        assertThat(stats.getNewCount()).isEqualTo(3L);
        assertThat(stats.getUnderReviewCount()).isEqualTo(2L);
    }
}
