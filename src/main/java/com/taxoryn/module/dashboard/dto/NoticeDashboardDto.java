package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tax Notice Management Dashboard Metrics")
public class NoticeDashboardDto {

    @Schema(description = "Total open active notices", example = "8")
    @Builder.Default
    private long totalOpenNotices = 0;

    @Schema(description = "Notices requiring practice or client response drafting", example = "4")
    @Builder.Default
    private long noticesRequiringResponse = 0;

    @Schema(description = "Notices where response has been submitted, awaiting department order", example = "2")
    @Builder.Default
    private long noticesAwaitingResponse = 0;

    @Schema(description = "Notices requiring personal or virtual hearing appearance", example = "2")
    @Builder.Default
    private long noticesRequiringHearing = 0;

    @Schema(description = "Upcoming hearing dates scheduled in next 30 days", example = "2")
    @Builder.Default
    private long upcomingHearings = 0;

    @Schema(description = "Overdue response deadlines past statutory date", example = "1")
    @Builder.Default
    private long overdueResponseDeadlines = 0;

    @Schema(description = "Total disputed tax demand amount across active notices (INR)", example = "2500000.00")
    @Builder.Default
    private BigDecimal totalDemandAmount = BigDecimal.ZERO;

    @Schema(description = "Timestamp when this summary was generated")
    @Builder.Default
    private Instant generatedAt = Instant.now();
}
