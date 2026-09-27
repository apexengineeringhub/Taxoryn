package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice Compliance Workspace Dashboard Metrics")
public class ComplianceDashboardDto {

    @Schema(description = "Total compliance obligations in scope", example = "120")
    @Builder.Default
    private long totalObligations = 0;

    @Schema(description = "Pending obligations not yet started", example = "45")
    @Builder.Default
    private long pending = 0;

    @Schema(description = "Obligations currently in progress", example = "35")
    @Builder.Default
    private long inProgress = 0;

    @Schema(description = "Completed/Filed obligations", example = "36")
    @Builder.Default
    private long completed = 0;

    @Schema(description = "Overdue obligations past statutory deadline", example = "4")
    @Builder.Default
    private long overdue = 0;

    @Schema(description = "Upcoming deadlines within next 15 days", example = "18")
    @Builder.Default
    private long upcomingDeadlines = 0;

    @Schema(description = "GST Compliance Overview")
    @Builder.Default
    private CategoryComplianceSummary gst = CategoryComplianceSummary.builder().category("GST").build();

    @Schema(description = "ITR Compliance Overview")
    @Builder.Default
    private CategoryComplianceSummary itr = CategoryComplianceSummary.builder().category("ITR").build();

    @Schema(description = "TDS Compliance Overview")
    @Builder.Default
    private CategoryComplianceSummary tds = CategoryComplianceSummary.builder().category("TDS").build();

    @Schema(description = "Tax Notice Compliance Overview")
    @Builder.Default
    private CategoryComplianceSummary notices = CategoryComplianceSummary.builder().category("NOTICES").build();

    @Schema(description = "Timestamp when this summary was generated")
    @Builder.Default
    private Instant generatedAt = Instant.now();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Category-specific compliance summary")
    public static class CategoryComplianceSummary {
        private String category;
        @Builder.Default
        private long total = 0;
        @Builder.Default
        private long pending = 0;
        @Builder.Default
        private long inProgress = 0;
        @Builder.Default
        private long completed = 0;
        @Builder.Default
        private long overdue = 0;
    }
}
