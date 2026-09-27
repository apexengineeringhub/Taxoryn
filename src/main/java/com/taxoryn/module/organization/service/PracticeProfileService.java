package com.taxoryn.module.organization.service;

import com.taxoryn.module.organization.dto.PlanRecommendationDto;
import com.taxoryn.module.organization.dto.PracticeAdminOverviewDto;
import com.taxoryn.module.organization.dto.PracticeProfileDto;
import com.taxoryn.module.organization.dto.PracticeSetupOnboardingRequest;
import com.taxoryn.module.organization.dto.UpdatePracticeProfileRequest;
import com.taxoryn.module.organization.entity.OrganizationType;

import java.util.UUID;

public interface PracticeProfileService {

    PracticeProfileDto getPracticeProfile(UUID organizationId);

    PracticeAdminOverviewDto getPracticeAdminOverview(UUID organizationId);

    PracticeProfileDto initializeDefaultProfile(UUID organizationId, OrganizationType organizationType);

    PracticeProfileDto updatePracticeProfile(UUID organizationId, UpdatePracticeProfileRequest request);

    PracticeProfileDto completeOnboardingSetup(UUID organizationId, PracticeSetupOnboardingRequest request);

    PlanRecommendationDto getPlanRecommendation(UUID organizationId);

    PracticeProfileDto confirmPlan(UUID organizationId, String planCode);
}
