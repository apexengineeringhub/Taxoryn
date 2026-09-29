package com.taxoryn.module.organization.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.TenantAccessDeniedException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.organization.dto.PlanRecommendationDto;
import com.taxoryn.module.organization.dto.PracticeProfileDto;
import com.taxoryn.module.organization.dto.PracticeSetupOnboardingRequest;
import com.taxoryn.module.organization.dto.UpdatePracticeProfileRequest;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.entity.PracticeProfileEntity;
import com.taxoryn.module.organization.entity.PracticeType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.organization.repository.PracticeProfileRepository;
import com.taxoryn.module.subscription.dto.ChangePlanRequest;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeProfileServiceImpl implements PracticeProfileService {

    private final PracticeProfileRepository practiceProfileRepository;
    private final OrganizationRepository organizationRepository;
    private final PlanRecommendationService planRecommendationService;
    private final SubscriptionService subscriptionService;
    private final com.taxoryn.module.subscription.repository.SubscriptionRepository subscriptionRepository;
    private final com.taxoryn.module.subscription.service.SubscriptionPlanEntitlementService subscriptionPlanEntitlementService;
    private final com.taxoryn.module.user.repository.UserRepository userRepository;
    private final com.taxoryn.module.client.repository.ClientRepository clientRepository;
    private final com.taxoryn.module.organization.repository.LocationRepository locationRepository;
    private final com.taxoryn.module.document.repository.DocumentRepository documentRepository;
    private final com.taxoryn.module.moduleconfig.repository.OrganizationModuleRepository organizationModuleRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public PracticeProfileDto getPracticeProfile(UUID organizationId) {
        validateTenantAccess(organizationId);

        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        PracticeProfileEntity profile = practiceProfileRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createDefaultProfile(organization));

        return mapToDto(profile, organization.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public com.taxoryn.module.organization.dto.PracticeAdminOverviewDto getPracticeAdminOverview(UUID organizationId) {
        validateTenantAccess(organizationId);

        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        PracticeProfileEntity profile = practiceProfileRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createDefaultProfile(organization));

        com.taxoryn.module.subscription.entity.SubscriptionEntity sub = subscriptionRepository.findByOrganizationId(organizationId)
                .orElse(null);

        SubscriptionPlan plan = sub != null ? sub.getPlan() : SubscriptionPlan.STARTER;
        String status = sub != null ? sub.getStatus().name() : "ACTIVE";
        java.time.LocalDate renewalDate = sub != null ? sub.getRenewalDate() : null;
        String billingInterval = sub != null && sub.getBillingInterval() != null ? sub.getBillingInterval().name() : "MONTHLY";

        long activeUsers = userRepository.countByOrganizationIdAndClientIdIsNull(organizationId);
        int maxUsers = sub != null ? sub.getMaxUsers() : com.taxoryn.module.subscription.entity.SubscriptionPlanDefaults.getDefaultMaxUsers(plan);

        long activeClients = clientRepository.countByOrganizationId(organizationId);
        int maxClients = sub != null ? sub.getMaxClients() : com.taxoryn.module.subscription.entity.SubscriptionPlanDefaults.getDefaultMaxClients(plan);

        long activeLocations = locationRepository.countByOrganizationIdAndIsActiveTrue(organizationId);
        int maxLocations = subscriptionPlanEntitlementService.getMaxLocations(plan);
        boolean multiLocationEnabled = subscriptionPlanEntitlementService.isMultiLocationEnabled(plan);

        long storageBytes = documentRepository.getTotalStorageBytesByOrganizationId(organizationId);
        long maxStorageBytes = sub != null ? sub.getMaxStorageBytes() : com.taxoryn.module.subscription.entity.SubscriptionPlanDefaults.getDefaultMaxStorageBytes(plan);

        List<String> enabledModules = organizationModuleRepository.findByOrganizationId(organizationId).stream()
                .filter(com.taxoryn.module.moduleconfig.entity.OrganizationModuleEntity::isEnabled)
                .map(m -> m.getModuleCode().name())
                .collect(Collectors.toList());

        return com.taxoryn.module.organization.dto.PracticeAdminOverviewDto.builder()
                .organizationId(organization.getId())
                .organizationName(organization.getName())
                .legalName(organization.getLegalName())
                .practiceType(profile.getPracticeType())
                .onboardingCompleted(profile.isOnboardingCompleted())
                .subscriptionPlan(plan.name())
                .subscriptionStatus(status)
                .renewalDate(renewalDate)
                .billingInterval(billingInterval)
                .activeUsers(activeUsers)
                .maxUsers(maxUsers)
                .activeClients(activeClients)
                .maxClients(maxClients)
                .activeLocations(activeLocations)
                .maxLocations(maxLocations)
                .multiLocationEnabled(multiLocationEnabled)
                .storageUsedBytes(storageBytes)
                .maxStorageBytes(maxStorageBytes)
                .enabledModules(enabledModules)
                .totalEnabledModules(enabledModules.size())
                .build();
    }

    @Override
    @Transactional
    public PracticeProfileDto initializeDefaultProfile(UUID organizationId, OrganizationType organizationType) {
        PracticeProfileEntity existing = practiceProfileRepository.findByOrganizationId(organizationId).orElse(null);
        if (existing != null) {
            String orgName = organizationRepository.findById(organizationId).map(OrganizationEntity::getName).orElse("Practice");
            return mapToDto(existing, orgName);
        }

        PracticeType derivedType = PracticeType.fromOrganizationType(organizationType);
        PracticeProfileEntity profile = PracticeProfileEntity.builder()
                .organizationId(organizationId)
                .practiceType(derivedType)
                .yearsInPractice(0)
                .approximateClientCount(0)
                .practitionerCount(1)
                .employeeCount(1)
                .locationCount(1)
                .onboardingCompleted(false)
                .recommendedPlan(SubscriptionPlan.STARTER.name())
                .confirmedPlan(SubscriptionPlan.STARTER.name())
                .build();

        PracticeProfileEntity saved = practiceProfileRepository.save(profile);
        String orgName = organizationRepository.findById(organizationId).map(OrganizationEntity::getName).orElse("Practice");
        log.info("Initialized default PracticeProfile for orgId={}: practiceType={}, recommendedPlan={}", organizationId, derivedType, saved.getRecommendedPlan());
        return mapToDto(saved, orgName);
    }

    @Override
    @Transactional
    public PracticeProfileDto updatePracticeProfile(UUID organizationId, UpdatePracticeProfileRequest request) {
        validateTenantAccess(organizationId);

        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        PracticeProfileEntity profile = practiceProfileRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createDefaultProfile(organization));

        if (request.getPracticeType() != null) {
            profile.setPracticeType(request.getPracticeType());
        }
        if (request.getYearsInPractice() != null) {
            profile.setYearsInPractice(request.getYearsInPractice());
        }
        if (request.getApproximateClientCount() != null) {
            profile.setApproximateClientCount(request.getApproximateClientCount());
        }
        if (request.getServicesOffered() != null) {
            profile.setServicesOffered(String.join(",", request.getServicesOffered()));
        }
        if (request.getPractitionerCount() != null) {
            profile.setPractitionerCount(request.getPractitionerCount());
        }
        if (request.getEmployeeCount() != null) {
            profile.setEmployeeCount(request.getEmployeeCount());
        }
        if (request.getLocationCount() != null) {
            profile.setLocationCount(request.getLocationCount());
        }
        if (request.getPrimaryTaxServices() != null) {
            profile.setPrimaryTaxServices(String.join(",", request.getPrimaryTaxServices()));
        }

        // Recompute recommendation
        PlanRecommendationDto recommendation = planRecommendationService.recommendPlan(profile);
        profile.setRecommendedPlan(recommendation.getRecommendedPlan());

        PracticeProfileEntity saved = practiceProfileRepository.save(profile);

        auditService.logEvent(
                organizationId,
                SecurityUtils.getCurrentUserId(),
                "PRACTICE_PROFILE_UPDATED",
                "PRACTICE_PROFILE",
                saved.getId().toString(),
                null,
                Map.of("practiceType", saved.getPracticeType().name(), "recommendedPlan", saved.getRecommendedPlan())
        );

        return mapToDto(saved, organization.getName());
    }

    @Override
    @Transactional
    public PracticeProfileDto completeOnboardingSetup(UUID organizationId, PracticeSetupOnboardingRequest request) {
        validateTenantAccess(organizationId);

        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        PracticeProfileEntity profile = practiceProfileRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createDefaultProfile(organization));

        profile.setPracticeType(request.getPracticeType() != null ? request.getPracticeType() : PracticeType.UNKNOWN);
        profile.setYearsInPractice(request.getYearsInPractice() != null ? request.getYearsInPractice() : 0);
        profile.setApproximateClientCount(request.getApproximateClientCount() != null ? request.getApproximateClientCount() : 0);
        profile.setServicesOffered(request.getServicesOffered() != null ? String.join(",", request.getServicesOffered()) : "");
        profile.setPractitionerCount(request.getPractitionerCount() != null ? request.getPractitionerCount() : 1);
        profile.setEmployeeCount(request.getEmployeeCount() != null ? request.getEmployeeCount() : 1);
        profile.setLocationCount(request.getLocationCount() != null ? request.getLocationCount() : 1);
        profile.setPrimaryTaxServices(request.getPrimaryTaxServices() != null ? String.join(",", request.getPrimaryTaxServices()) : "");
        profile.setOnboardingCompleted(true);

        PlanRecommendationDto recommendation = planRecommendationService.recommendPlan(profile);
        profile.setRecommendedPlan(recommendation.getRecommendedPlan());

        PracticeProfileEntity saved = practiceProfileRepository.save(profile);

        auditService.logEvent(
                organizationId,
                SecurityUtils.getCurrentUserId(),
                "PRACTICE_ONBOARDING_COMPLETED",
                "PRACTICE_PROFILE",
                saved.getId().toString(),
                null,
                Map.of("practiceType", saved.getPracticeType().name(), "recommendedPlan", saved.getRecommendedPlan())
        );

        log.info("Practice onboarding completed for orgId={}, recommendedPlan={}", organizationId, saved.getRecommendedPlan());
        return mapToDto(saved, organization.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public PlanRecommendationDto getPlanRecommendation(UUID organizationId) {
        validateTenantAccess(organizationId);

        PracticeProfileEntity profile = practiceProfileRepository.findByOrganizationId(organizationId)
                .orElse(null);

        return planRecommendationService.recommendPlan(profile);
    }

    @Override
    @Transactional
    public PracticeProfileDto confirmPlan(UUID organizationId, String planCode) {
        validateTenantAccess(organizationId);

        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        PracticeProfileEntity profile = practiceProfileRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createDefaultProfile(organization));

        SubscriptionPlan plan;
        try {
            plan = SubscriptionPlan.valueOf(planCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid subscription plan code: " + planCode);
        }

        profile.setConfirmedPlan(plan.name());
        PracticeProfileEntity saved = practiceProfileRepository.save(profile);

        // Update organization subscription to the confirmed plan
        ChangePlanRequest changePlanRequest = new ChangePlanRequest();
        changePlanRequest.setPlan(plan);
        subscriptionService.changePlan(organizationId, changePlanRequest);

        auditService.logEvent(
                organizationId,
                SecurityUtils.getCurrentUserId(),
                "PRACTICE_PLAN_CONFIRMED",
                "PRACTICE_PROFILE",
                saved.getId().toString(),
                null,
                Map.of("confirmedPlan", plan.name())
        );

        return mapToDto(saved, organization.getName());
    }

    private PracticeProfileEntity createDefaultProfile(OrganizationEntity organization) {
        PracticeType derivedType = PracticeType.fromOrganizationType(organization.getOrganizationType());
        PracticeProfileEntity profile = PracticeProfileEntity.builder()
                .organizationId(organization.getId())
                .practiceType(derivedType)
                .yearsInPractice(0)
                .approximateClientCount(0)
                .practitionerCount(1)
                .employeeCount(1)
                .locationCount(1)
                .onboardingCompleted(false)
                .build();

        PlanRecommendationDto recommendation = planRecommendationService.recommendPlan(profile);
        profile.setRecommendedPlan(recommendation.getRecommendedPlan());

        return practiceProfileRepository.save(profile);
    }

    private PracticeProfileDto mapToDto(PracticeProfileEntity entity, String organizationName) {
        List<String> services = entity.getServicesOffered() != null && !entity.getServicesOffered().isBlank()
                ? Arrays.stream(entity.getServicesOffered().split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList())
                : Collections.emptyList();

        List<String> taxServices = entity.getPrimaryTaxServices() != null && !entity.getPrimaryTaxServices().isBlank()
                ? Arrays.stream(entity.getPrimaryTaxServices().split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList())
                : Collections.emptyList();

        return PracticeProfileDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .organizationName(organizationName)
                .practiceType(entity.getPracticeType())
                .yearsInPractice(entity.getYearsInPractice())
                .approximateClientCount(entity.getApproximateClientCount())
                .servicesOffered(services)
                .practitionerCount(entity.getPractitionerCount())
                .employeeCount(entity.getEmployeeCount())
                .locationCount(entity.getLocationCount())
                .primaryTaxServices(taxServices)
                .onboardingCompleted(entity.isOnboardingCompleted())
                .recommendedPlan(entity.getRecommendedPlan())
                .confirmedPlan(entity.getConfirmedPlan())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void validateTenantAccess(UUID organizationId) {
        if (organizationId == null) {
            throw new UnauthorizedException("Organization ID is required");
        }
        UUID currentTenant = SecurityUtils.getCurrentOrganizationId();
        if (currentTenant == null) {
            throw new UnauthorizedException("Authenticated tenant context is required");
        }
        if (!SecurityUtils.isTaxorynSuperAdmin() && !currentTenant.equals(organizationId)) {
            throw new TenantAccessDeniedException("Cross-tenant access violation: Target organization " +
                    organizationId + " does not match authenticated tenant " + currentTenant);
        }
    }
}
