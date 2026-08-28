package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tailored Support Overview and Operational Metrics for Taxoryn Support Admins")
public class SupportDashboardDto {

    @Schema(description = "Core support triage and ticket KPIs")
    private SupportKpisDto kpis;

    @Schema(description = "High-priority support items requiring immediate attention")
    private List<SupportAttentionItemDto> supportAttention;

    @Schema(description = "Recent support activities and feedback updates")
    private List<RecentSupportActivityDto> recentActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Support KPI Metrics")
    public static class SupportKpisDto {
        @Schema(description = "Total active open support cases and feedback", example = "12")
        private long openCases;

        @Schema(description = "Cases currently awaiting customer response", example = "4")
        private long waitingForCustomer;

        @Schema(description = "Critical and high priority open issues", example = "2")
        private long highPriority;

        @Schema(description = "Unresolved application feedback tickets", example = "5")
        private long unresolvedFeedback;

        @Schema(description = "Successfully resolved tickets this month", example = "28")
        private long resolvedThisMonth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Actionable Support Item")
    public static class SupportAttentionItemDto {
        private String id;
        private String title;
        private String description;
        private String priority;
        private String status;
        private String actionTarget;
        private String actionLabel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Recent Support Event")
    public static class RecentSupportActivityDto {
        private String id;
        private String title;
        private String description;
        private String actor;
        private String target;
        private Instant timestamp;
        private String status;
        private String severity;
        private String navigationTarget;
    }
}
