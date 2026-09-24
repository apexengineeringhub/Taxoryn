package com.taxoryn.module.compliance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practitioner Compliance Workbench Summary Metrics")
public class ComplianceWorkbenchSummaryDto {

    @Schema(description = "Total active compliance workflows in portfolio", example = "42")
    private long totalActive;

    @Schema(description = "Workflows due today (by internal target date)", example = "3")
    private long dueToday;

    @Schema(description = "Workflows due this week", example = "12")
    private long dueThisWeek;

    @Schema(description = "Workflows overdue target date", example = "2")
    private long overdue;

    @Schema(description = "Workflows blocked waiting for client", example = "8")
    private long waitingForClient;

    @Schema(description = "Workflows submitted under review", example = "5")
    private long underReview;

    @Schema(description = "Workflows approved and ready for government portal filing", example = "7")
    private long readyForFiling;

    @Schema(description = "Workflows completed in current calendar month", example = "19")
    private long completedThisMonth;

    @Schema(description = "Workflows assigned to current logged-in employee", example = "14")
    private long myAssigned;

    @Schema(description = "Workflows where current logged-in employee is reviewer", example = "6")
    private long myReviews;

    public long getMyAssignedCount() {
        return myAssigned;
    }

    public long getMyReviewCount() {
        return myReviews;
    }
}
