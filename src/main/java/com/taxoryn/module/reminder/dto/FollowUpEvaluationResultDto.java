package com.taxoryn.module.reminder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Summary result of evaluating collaboration follow-up reminders")
public class FollowUpEvaluationResultDto {

    private UUID organizationId;
    private int approvalsEvaluated;
    private int documentRequestsEvaluated;
    private int communicationsEvaluated;
    private int complianceObligationsEvaluated;
    private int totalRemindersCreated;
    private Instant evaluatedAt;
}
