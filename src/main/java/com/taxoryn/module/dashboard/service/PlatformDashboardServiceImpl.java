package com.taxoryn.module.dashboard.service;

import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.dashboard.dto.PlatformDashboardSummaryDto;
import com.taxoryn.module.dashboard.dto.PlatformDashboardSummaryDto.*;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackEntity;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackPriority;
import com.taxoryn.module.feedback.entity.ApplicationFeedbackStatus;
import com.taxoryn.module.feedback.repository.ApplicationFeedbackRepository;
import com.taxoryn.module.feedback.repository.FeedbackEngineeringIssueRepository;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import com.taxoryn.module.marketplace.entity.TaxRequirementStatus;
import com.taxoryn.module.marketplace.repository.CustomerTaxRequirementRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceLeadRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformDashboardServiceImpl implements PlatformDashboardService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final MarketplaceProfileRepository marketplaceProfileRepository;
    private final MarketplaceLeadRepository marketplaceLeadRepository;
    private final CustomerTaxRequirementRepository taxRequirementRepository;
    private final ApplicationFeedbackRepository feedbackRepository;
    private final FeedbackEngineeringIssueRepository engineeringIssueRepository;
    private final AuditService auditService;
    private final DataSource dataSource;

    @Override
    public PlatformDashboardSummaryDto getPlatformDashboard() {
        log.debug("Compiling PlatformDashboardSummary for SuperAdmin");

        // 1. Practices Metrics
        long totalPractices = organizationRepository.count();
        long activePractices = organizationRepository.countByStatus(OrganizationEntity.OrganizationStatus.ACTIVE);
        long suspendedPractices = organizationRepository.countByStatus(OrganizationEntity.OrganizationStatus.SUSPENDED);
        long inactivePractices = organizationRepository.countByStatus(OrganizationEntity.OrganizationStatus.INACTIVE);
        long pendingVerificationPractices = marketplaceProfileRepository.countByVerificationStatus(MarketplaceProfileEntity.VerificationStatus.PENDING);

        Instant startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        long newPracticesThisMonth = organizationRepository.countByCreatedAtAfter(startOfMonth);

        PracticeEcosystemDto practiceEcosystem = PracticeEcosystemDto.builder()
                .totalPractices(totalPractices)
                .activePractices(activePractices)
                .pendingVerification(pendingVerificationPractices)
                .inactivePractices(inactivePractices)
                .suspendedPractices(suspendedPractices)
                .newPracticesThisMonth(newPracticesThisMonth)
                .build();

        // 2. User Ecosystem
        List<UserEntity> allUsers = userRepository.findAll();
        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream()
                .filter(u -> u.getStatus() == UserEntity.UserStatus.ACTIVE)
                .count();

        long superAdminCount = 0;
        long practitionerCount = 0;
        long employeeCount = 0;
        long customerCount = 0;

        for (UserEntity user : allUsers) {
            Set<String> roleCodes = (user.getRoles() != null)
                    ? user.getRoles().stream().map(RoleEntity::getCode).collect(Collectors.toSet())
                    : Collections.emptySet();

            if (roleCodes.contains("SUPER_ADMIN") || roleCodes.contains("TAXORYN_SUPERADMIN")) {
                superAdminCount++;
            } else if (roleCodes.contains("CLIENT_USER") || roleCodes.contains("CLIENT_ADMIN") || roleCodes.contains("MARKETPLACE_CUSTOMER") || user.getClientId() != null) {
                customerCount++;
            } else if (roleCodes.contains("STAFF") || roleCodes.contains("ARTICLE_ASSISTANT") || roleCodes.contains("TRAINEE") || roleCodes.contains("PRACTICE_EMPLOYEE")) {
                employeeCount++;
            } else if (roleCodes.contains("ORG_ADMIN") || roleCodes.contains("PRACTITIONER") || roleCodes.contains("PARTNER") || roleCodes.contains("PRACTICE_ADMIN") || roleCodes.contains("PRACTICE_OWNER")) {
                practitionerCount++;
            } else {
                customerCount++;
            }
        }

        UserEcosystemDto userEcosystem = UserEcosystemDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .customers(customerCount)
                .practitioners(practitionerCount)
                .practiceEmployees(employeeCount)
                .taxorynAdminUsers(superAdminCount)
                .build();

        // 3. Marketplace Funnel
        long totalRequirements = taxRequirementRepository.count();
        long activeRequirements = taxRequirementRepository.countByStatus(TaxRequirementStatus.SUBMITTED);
        long matchedRequirements = taxRequirementRepository.countByStatusIn(List.of(TaxRequirementStatus.SUBMITTED, TaxRequirementStatus.CLOSED));

        long totalEnquiries = marketplaceLeadRepository.count();
        long acceptedEnquiries = marketplaceLeadRepository.countByLeadStatusIn(List.of(
                MarketplaceLeadEntity.LeadStatus.ACCEPTED, MarketplaceLeadEntity.LeadStatus.CONVERTED));
        long completedServices = marketplaceLeadRepository.countByLeadStatus(MarketplaceLeadEntity.LeadStatus.CONVERTED);

        double conversionRate = totalEnquiries > 0
                ? ((double) completedServices / totalEnquiries) * 100.0
                : 0.0;

        MarketplaceFunnelDto marketplaceFunnel = MarketplaceFunnelDto.builder()
                .totalRequirements(totalRequirements)
                .activeRequirements(activeRequirements)
                .matchedRequirements(matchedRequirements)
                .totalEnquiries(totalEnquiries)
                .acceptedEnquiries(acceptedEnquiries)
                .completedServices(completedServices)
                .conversionRate(Math.round(conversionRate * 10.0) / 10.0)
                .build();

        // 4. Subscriptions & Platform Revenue
        long starterCount = 0;
        long proCount = 0;
        long businessCount = 0;
        long enterpriseCount = 0;

        List<Object[]> planCounts = organizationRepository.countBySubscriptionPlanGrouped();
        if (planCounts != null) {
            for (Object[] row : planCounts) {
                OrganizationEntity.SubscriptionPlan plan = (OrganizationEntity.SubscriptionPlan) row[0];
                long count = row[1] instanceof Number n ? n.longValue() : 0L;
                if (plan != null) {
                    switch (plan) {
                        case PROFESSIONAL -> proCount += count;
                        case BUSINESS -> businessCount += count;
                        case ENTERPRISE -> enterpriseCount += count;
                        case STARTER -> starterCount += count;
                    }
                } else {
                    starterCount += count;
                }
            }
        }

        BigDecimal mrr = BigDecimal.valueOf(starterCount * 999L
                + proCount * 2999L
                + businessCount * 5999L
                + enterpriseCount * 14999L);
        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12));

        SubscriptionMetricsDto subscriptionMetrics = SubscriptionMetricsDto.builder()
                .totalSubscriptions(totalPractices)
                .starterTiers(starterCount)
                .professionalTiers(proCount)
                .businessTiers(businessCount)
                .enterpriseTiers(enterpriseCount)
                .activeTiers(activePractices)
                .trialOrFreeTiers(inactivePractices)
                .estimatedMrr(mrr)
                .estimatedArr(arr)
                .build();

        // 5. Feedback Operations
        long totalFeedback = feedbackRepository.count();
        long newFeedback = feedbackRepository.countByStatus(ApplicationFeedbackStatus.NEW);
        long underReview = feedbackRepository.countByStatus(ApplicationFeedbackStatus.UNDER_REVIEW);
        long assigned = feedbackRepository.countByStatus(ApplicationFeedbackStatus.ASSIGNED);
        long inProgress = feedbackRepository.countByStatus(ApplicationFeedbackStatus.IN_PROGRESS);
        long resolved = feedbackRepository.countByStatusIn(List.of(ApplicationFeedbackStatus.RESOLVED, ApplicationFeedbackStatus.CLOSED));
        long escalatedToEng = engineeringIssueRepository.count();

        long criticalOpen = feedbackRepository.countByPriorityInAndStatusNotIn(
                List.of(ApplicationFeedbackPriority.CRITICAL, ApplicationFeedbackPriority.HIGH),
                List.of(ApplicationFeedbackStatus.RESOLVED, ApplicationFeedbackStatus.CLOSED, ApplicationFeedbackStatus.REJECTED)
        );

        Map<String, Long> topCategories = new LinkedHashMap<>();
        List<Object[]> catRows = feedbackRepository.countGroupedByCategory();
        if (catRows != null) {
            for (Object[] r : catRows) {
                String catName = r[0] != null ? r[0].toString() : "GENERAL";
                long count = r[1] instanceof Number n ? n.longValue() : 0L;
                topCategories.put(catName, count);
            }
        }

        FeedbackOperationsDto feedbackOperations = FeedbackOperationsDto.builder()
                .totalFeedback(totalFeedback)
                .newFeedback(newFeedback)
                .underReview(underReview)
                .assigned(assigned)
                .inProgress(inProgress)
                .escalatedToEng(escalatedToEng)
                .resolved(resolved)
                .criticalOpen(criticalOpen)
                .topCategories(topCategories)
                .build();

        // 6. Platform Health & Subsystem Status
        String dbStatus = "HEALTHY";
        long activeDbConns = 1;
        long maxDbConns = 15;

        try {
            if (dataSource instanceof HikariDataSource hikari) {
                activeDbConns = hikari.getHikariPoolMXBean() != null ? hikari.getHikariPoolMXBean().getActiveConnections() : 1;
                maxDbConns = hikari.getMaximumPoolSize();
            }
            try (Connection conn = dataSource.getConnection()) {
                if (!conn.isValid(2)) {
                    dbStatus = "DEGRADED";
                }
            }
        } catch (Exception ex) {
            log.warn("Database health check ping error: {}", ex.getMessage());
            dbStatus = "DEGRADED";
        }

        Runtime runtime = Runtime.getRuntime();
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long freeMem = runtime.freeMemory() / (1024 * 1024);
        long maxMem = runtime.maxMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long uptimeSec = runtimeMXBean.getUptime() / 1000;

        PlatformHealthDto platformHealth = PlatformHealthDto.builder()
                .api("HEALTHY")
                .database(dbStatus)
                .backgroundJobs("HEALTHY")
                .marketplace("HEALTHY")
                .notifications("HEALTHY")
                .apiGatewayStatus("HEALTHY")
                .databaseStatus(dbStatus)
                .authServiceStatus("HEALTHY")
                .marketplaceStatus("HEALTHY")
                .feedbackSubsystemStatus("HEALTHY")
                .backgroundJobsStatus("HEALTHY")
                .activeDbConnections(activeDbConns)
                .maxDbConnections(maxDbConns)
                .systemCpuLoad(0.08)
                .usedMemoryMb(usedMem)
                .maxMemoryMb(maxMem)
                .uptimeSeconds(uptimeSec)
                .build();

        // 7. Recent Important Activity & Events (Retrieved from authoritative AuditService)
        List<RecentPlatformActivityDto> recentActivities = auditService.getRecentImportantActivity(6);
        List<RecentAdminActivityDto> legacyActivities = recentActivities.stream()
                .map(r -> RecentAdminActivityDto.builder()
                        .id(r.getId())
                        .action(r.getDisplayTitle())
                        .entityType("PLATFORM")
                        .entityId(r.getDescription())
                        .userEmail("audit@taxoryn.com")
                        .description(r.getDescription())
                        .timestamp(r.getTimestamp())
                        .status(r.getStatus())
                        .build())
                .collect(Collectors.toList());

        // 8. Clean Business-Oriented Overview Model
        PlatformSummaryDto summary = PlatformSummaryDto.builder()
                .activePractices(activePractices)
                .totalPractices(totalPractices)
                .platformUsers(activeUsers)
                .marketplaceCustomers(customerCount)
                .activeSubscriptions(activePractices)
                .build();

        PlatformMarketplaceDto marketplace = PlatformMarketplaceDto.builder()
                .newRequirements(activeRequirements)
                .activeEnquiries(totalEnquiries)
                .matchesCompleted(matchedRequirements)
                .consultationsBooked(acceptedEnquiries)
                .build();

        PlatformAttentionDto attention = PlatformAttentionDto.builder()
                .pendingPracticeVerification(pendingVerificationPractices)
                .openFeedback(newFeedback + underReview + inProgress)
                .securityAlerts(0)
                .paymentIssues(suspendedPractices)
                .marketplaceIssues(0)
                .build();

        PlatformKpisDto kpis = PlatformKpisDto.builder()
                .activePractices(activePractices)
                .totalPractices(totalPractices)
                .activeUsers(activeUsers)
                .totalUsers(totalUsers)
                .activeCustomers(customerCount)
                .totalMarketplaceLeads(totalEnquiries)
                .activeSubscriptions(activePractices)
                .openFeedback(newFeedback + underReview + inProgress)
                .platformStatus("HEALTHY")
                .monthlyRecurringRevenue(mrr)
                .annualRecurringRevenue(arr)
                .build();

        return PlatformDashboardSummaryDto.builder()
                .summary(summary)
                .marketplace(marketplace)
                .attention(attention)
                .health(platformHealth)
                .recentActivity(recentActivities)
                .kpis(kpis)
                .practiceEcosystem(practiceEcosystem)
                .userEcosystem(userEcosystem)
                .marketplaceFunnel(marketplaceFunnel)
                .subscriptionMetrics(subscriptionMetrics)
                .feedbackOperations(feedbackOperations)
                .platformHealth(platformHealth)
                .recentActivities(legacyActivities)
                .build();
    }
}
