package com.taxoryn.module.organization.dto;

import com.taxoryn.module.organization.entity.PracticeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeAdminOverviewDto {
    private UUID organizationId;
    private String organizationName;
    private String legalName;
    private PracticeType practiceType;
    private boolean onboardingCompleted;

    // Subscription & Plan
    private String subscriptionPlan;
    private String subscriptionStatus;
    private LocalDate renewalDate;
    private String billingInterval;

    // Limits & Usage
    private long activeUsers;
    private int maxUsers;
    private long activeClients;
    private int maxClients;
    private long activeLocations;
    private int maxLocations;
    private boolean multiLocationEnabled;
    private long storageUsedBytes;
    private long maxStorageBytes;

    // Modules
    private List<String> enabledModules;
    private int totalEnabledModules;
}
