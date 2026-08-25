package com.taxoryn.module.feedback.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.feedback.dto.*;
import com.taxoryn.module.feedback.entity.*;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AdminApplicationFeedbackService {

    PagedResponse<AdminApplicationFeedbackSummaryDto> getFeedbackList(
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
    );

    AdminApplicationFeedbackDetailDto getFeedbackDetail(UUID id);

    AdminApplicationFeedbackDetailDto startReview(UUID id);

    AdminApplicationFeedbackDetailDto assignFeedback(UUID id, AssignFeedbackRequest request);

    FeedbackNoteDto addNote(UUID id, CreateFeedbackNoteRequest request);

    AdminApplicationFeedbackDetailDto updatePriority(UUID id, UpdateFeedbackPriorityRequest request);

    AdminApplicationFeedbackDetailDto resolveFeedback(UUID id, ResolveFeedbackRequest request);

    AdminApplicationFeedbackDetailDto closeFeedback(UUID id, CloseFeedbackRequest request);

    AdminApplicationFeedbackDetailDto rejectFeedback(UUID id, RejectFeedbackRequest request);

    AdminApplicationFeedbackDetailDto markDuplicate(UUID id, MarkDuplicateFeedbackRequest request);

    EngineeringIssueDto escalateToEngineering(UUID id, EscalateToEngineeringRequest request);

    AdminFeedbackStatsDto getStats();

    List<AdminAssigneeDto> getAssignees();

    List<FeedbackTeam> getTeams();
}
