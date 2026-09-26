package com.taxoryn.module.organization.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.organization.dto.PlanRecommendationDto;
import com.taxoryn.module.organization.dto.PracticeProfileDto;
import com.taxoryn.module.organization.dto.PracticeSetupOnboardingRequest;
import com.taxoryn.module.organization.dto.UpdatePracticeProfileRequest;
import com.taxoryn.module.organization.service.PracticeProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/practice-profile")
@RequiredArgsConstructor
@Tag(name = "Practice Profile & Onboarding", description = "Endpoints for managing practice profile, setup onboarding, and plan recommendations")
@SecurityRequirement(name = "BearerAuth")
public class PracticeProfileController {

    private final PracticeProfileService practiceProfileService;

    @GetMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Get current practice profile", description = "Retrieves profile and operational parameters of current practice.")
    public ResponseEntity<ApiResponse<PracticeProfileDto>> getCurrentPracticeProfile() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeProfileDto profile = practiceProfileService.getPracticeProfile(orgId);
        return ResponseEntity.ok(ApiResponse.success("Practice profile retrieved successfully", profile));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Get practice admin overview hub", description = "Retrieves consolidated practice profile, subscription, limits, location, and module metrics for admin hub.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.organization.dto.PracticeAdminOverviewDto>> getPracticeAdminOverview() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        com.taxoryn.module.organization.dto.PracticeAdminOverviewDto overview = practiceProfileService.getPracticeAdminOverview(orgId);
        return ResponseEntity.ok(ApiResponse.success("Practice admin overview retrieved successfully", overview));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update current practice profile", description = "Updates profile information and re-evaluates plan recommendations.")
    public ResponseEntity<ApiResponse<PracticeProfileDto>> updateCurrentPracticeProfile(@Valid @RequestBody UpdatePracticeProfileRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeProfileDto updated = practiceProfileService.updatePracticeProfile(orgId, request);
        return ResponseEntity.ok(ApiResponse.success("Practice profile updated successfully", updated));
    }

    @PostMapping("/onboarding")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Complete practice onboarding setup", description = "Submits initial practice setup questions and generates recommended subscription plan.")
    public ResponseEntity<ApiResponse<PracticeProfileDto>> completeOnboarding(@Valid @RequestBody PracticeSetupOnboardingRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeProfileDto updated = practiceProfileService.completeOnboardingSetup(orgId, request);
        return ResponseEntity.ok(ApiResponse.success("Practice setup completed successfully", updated));
    }

    @GetMapping("/recommendation")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Get recommended subscription plan", description = "Generates recommended plan based on current practice profile criteria.")
    public ResponseEntity<ApiResponse<PlanRecommendationDto>> getPlanRecommendation() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PlanRecommendationDto recommendation = practiceProfileService.getPlanRecommendation(orgId);
        return ResponseEntity.ok(ApiResponse.success("Plan recommendation generated successfully", recommendation));
    }

    @PostMapping("/confirm-plan")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Confirm and activate recommended or selected plan", description = "Confirms chosen subscription plan and updates tenant subscription.")
    public ResponseEntity<ApiResponse<PracticeProfileDto>> confirmPlan(@RequestBody ConfirmPlanPayload payload) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        PracticeProfileDto profile = practiceProfileService.confirmPlan(orgId, payload.getPlan());
        return ResponseEntity.ok(ApiResponse.success("Subscription plan confirmed successfully", profile));
    }

    @Data
    public static class ConfirmPlanPayload {
        private String plan;
    }
}
