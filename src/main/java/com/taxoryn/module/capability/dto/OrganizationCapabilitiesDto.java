package com.taxoryn.module.capability.dto;

import com.taxoryn.module.capability.model.ModuleRecommendationStatus;
import com.taxoryn.module.capability.model.ProductCapability;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * DTO representing the resolved product capabilities and experience configuration
 * for an authenticated tenant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationCapabilitiesDto {

    private UUID organizationId;
    private OrganizationType organizationType;
    private SubscriptionPlan subscriptionPlan;
    private Set<ProductCapability> enabledCapabilities;
    private Map<ProductCapability, ModuleRecommendationStatus> moduleStatuses;
    private List<String> recommendedModules;
    private String defaultDashboardView;
    private DashboardProfileDto dashboardProfile;
    private String onboardingProfile;
    private List<OnboardingStepDto> onboardingChecklist;
    private boolean multiUserPractice;
    private boolean clientPortalSupported;
    private boolean noticeCenterSupported;
    private boolean customInvoicingSupported;
}
