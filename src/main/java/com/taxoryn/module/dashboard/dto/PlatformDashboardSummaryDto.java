package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Platform-wide aggregated metrics summary for Taxoryn SuperAdmin")
public class PlatformDashboardSummaryDto {

    private PlatformKpisDto kpis;
    private PracticeEcosystemDto practiceEcosystem;
    private UserEcosystemDto userEcosystem;
    private MarketplaceFunnelDto marketplaceFunnel;
    private SubscriptionMetricsDto subscriptionMetrics;
    private FeedbackOperationsDto feedbackOperations;
    private PlatformHealthDto platformHealth;
    private List<RecentAdminActivityDto> recentActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Top-level Platform Executive KPI Summary")
    public static class PlatformKpisDto {
        private long activePractices;
        private long totalPractices;
        private long activeUsers;
        private long totalUsers;
        private long activeCustomers;
        private long totalMarketplaceLeads;
        private long activeSubscriptions;
        private long openFeedback;
        private String platformStatus; // "HEALTHY", "DEGRADED", "INCIDENT"
        private BigDecimal monthlyRecurringRevenue;
        private BigDecimal annualRecurringRevenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Practice ecosystem distribution and lifecycle counts")
    public static class PracticeEcosystemDto {
        private long totalPractices;
        private long activePractices;
        private long pendingVerification;
        private long inactivePractices;
        private long suspendedPractices;
        private long newPracticesThisMonth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User ecosystem and role distribution")
    public static class UserEcosystemDto {
        private long totalUsers;
        private long activeUsers;
        private long customers;
        private long practitioners;
        private long practiceEmployees;
        private long taxorynAdminUsers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Marketplace demand, match, and conversion funnel")
    public static class MarketplaceFunnelDto {
        private long totalRequirements;
        private long activeRequirements;
        private long matchedRequirements;
        private long totalEnquiries;
        private long acceptedEnquiries;
        private long completedServices;
        private double conversionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Platform SaaS subscriptions and recurring revenue")
    public static class SubscriptionMetricsDto {
        private long totalSubscriptions;
        private long starterTiers;
        private long professionalTiers;
        private long businessTiers;
        private long enterpriseTiers;
        private long activeTiers;
        private long trialOrFreeTiers;
        private BigDecimal estimatedMrr;
        private BigDecimal estimatedArr;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Global Application Feedback triage and escalation summary")
    public static class FeedbackOperationsDto {
        private long totalFeedback;
        private long newFeedback;
        private long underReview;
        private long assigned;
        private long inProgress;
        private long escalatedToEng;
        private long resolved;
        private long criticalOpen;
        private Map<String, Long> topCategories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Platform subsystem live health & infrastructure metrics")
    public static class PlatformHealthDto {
        private String apiGatewayStatus;
        private String databaseStatus;
        private String authServiceStatus;
        private String marketplaceStatus;
        private String feedbackSubsystemStatus;
        private String backgroundJobsStatus;
        private long activeDbConnections;
        private long maxDbConnections;
        private double systemCpuLoad;
        private long usedMemoryMb;
        private long maxMemoryMb;
        private long uptimeSeconds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Recent Platform Administrative Events & Security Audit Stream")
    public static class RecentAdminActivityDto {
        private String id;
        private String action;
        private String entityType;
        private String entityId;
        private String userEmail;
        private String description;
        private Instant timestamp;
        private String status;
    }
}
