package com.taxoryn.module.compliance.scheduler;

import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.compliance.dto.GenerateComplianceRequest;
import com.taxoryn.module.compliance.service.ComplianceService;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity.OrganizationStatus;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceScheduler {

    private final ComplianceService complianceService;
    private final OrganizationRepository organizationRepository;

    /**
     * Daily background job to detect and mark overdue compliance obligations.
     * Runs every day at 01:00 AM.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void markOverdueObligationsDaily() {
        log.info("Running daily scheduled job: Mark overdue compliance obligations");
        try {
            int overdueCount = complianceService.processOverdueObligations();
            log.info("Daily overdue compliance job completed. Updated {} obligations to OVERDUE.", overdueCount);
        } catch (Exception ex) {
            log.error("Error running daily overdue compliance job: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Monthly background job to auto-generate statutory compliance obligations
     * for all active tenant organizations. Runs on the 1st of every month at 02:00 AM.
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void generateMonthlyComplianceObligations() {
        LocalDate now = LocalDate.now();
        String currentMonthPeriod = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("Running monthly scheduled job: Generate compliance obligations for period {}", currentMonthPeriod);

        List<OrganizationEntity> activeOrgs = organizationRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrganizationStatus.ACTIVE)
                .toList();

        for (OrganizationEntity org : activeOrgs) {
            try {
                TenantContext.setTenantId(org.getId());
                GenerateComplianceRequest request = GenerateComplianceRequest.builder()
                        .period(currentMonthPeriod)
                        .build();
                var generated = complianceService.generateComplianceObligations(request);
                log.info("Generated {} compliance obligations for organization: {}", generated.size(), org.getName());
            } catch (Exception ex) {
                log.error("Failed to generate compliance obligations for organization {}: {}", org.getId(), ex.getMessage(), ex);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
