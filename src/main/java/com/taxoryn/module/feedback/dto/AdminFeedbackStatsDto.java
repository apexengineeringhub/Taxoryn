package com.taxoryn.module.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFeedbackStatsDto {
    private long totalCount;
    private long newCount;
    private long underReviewCount;
    private long assignedCount;
    private long inProgressCount;
    private long escalatedCount;
    private long resolvedCount;
    private long closedCount;
    private long rejectedCount;
    private long duplicateCount;
    private long criticalCount;
    private long highCount;
}
