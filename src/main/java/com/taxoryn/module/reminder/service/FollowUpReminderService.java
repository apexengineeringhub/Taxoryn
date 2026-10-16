package com.taxoryn.module.reminder.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.reminder.dto.FollowUpEvaluationResultDto;
import com.taxoryn.module.reminder.dto.FollowUpFilterRequest;
import com.taxoryn.module.reminder.dto.FollowUpItemDto;
import com.taxoryn.module.reminder.dto.SendFollowUpNudgeRequest;

import java.util.UUID;

/**
 * Service orchestrating deterministic collaboration follow-ups and reminders
 * across pending approvals, document requests, messages, and compliance deadlines.
 */
public interface FollowUpReminderService {

    /**
     * Evaluates all 5 reminder categories for an organization,
     * deterministically generating reminder candidates with unique idempotency keys.
     *
     * @param organizationId target organization/tenant
     * @return summary evaluation result
     */
    FollowUpEvaluationResultDto evaluateFollowUps(UUID organizationId);

    /**
     * Retrieves paginated follow-up items for the practitioner view.
     *
     * @param filter query filters
     * @return paginated follow-ups
     */
    PagedResponse<FollowUpItemDto> getFollowUps(FollowUpFilterRequest filter);

    /**
     * Sends an on-demand follow-up nudge for a specific reminder item,
     * subject to cooldown/rate limits and live state verification.
     *
     * @param reminderId target reminder ID
     * @param request optional custom nudge message
     * @return updated follow-up item
     */
    FollowUpItemDto sendFollowUpNudge(UUID reminderId, SendFollowUpNudgeRequest request);

    /**
     * Marks a follow-up reminder as completed or dismissed.
     *
     * @param reminderId target reminder ID
     * @param resolutionNotes notes explaining resolution
     * @return updated follow-up item
     */
    FollowUpItemDto resolveFollowUp(UUID reminderId, String resolutionNotes);

    /**
     * Validates live source state before triggering or delivery:
     * If the underlying action is resolved, cancels the reminder.
     *
     * @param reminderId reminder ID to validate
     * @return true if still eligible, false if cancelled
     */
    boolean validateAndReconcileSourceState(UUID reminderId);
}
