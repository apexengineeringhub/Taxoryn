package com.taxoryn.module.capability.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.capability.dto.DashboardProfileDto;
import com.taxoryn.module.capability.dto.OnboardingStepDto;
import com.taxoryn.module.capability.dto.OrganizationCapabilitiesDto;
import com.taxoryn.module.capability.model.ModuleRecommendationStatus;
import com.taxoryn.module.capability.model.ProductCapability;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import com.taxoryn.module.subscription.entity.SubscriptionEntity.SubscriptionPlan;
import com.taxoryn.module.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCapabilityServiceImpl implements ProductCapabilityService {

    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public OrganizationCapabilitiesDto getCurrentOrganizationCapabilities() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required to resolve capabilities");
        }
        return getCapabilitiesByOrganizationId(organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationCapabilitiesDto getCapabilitiesByOrganizationId(UUID organizationId) {
        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        OrganizationType orgType = org.getOrganizationType() != null ? org.getOrganizationType() : OrganizationType.UNKNOWN;

        SubscriptionPlan plan = subscriptionRepository.findByOrganizationId(organizationId)
                .map(SubscriptionEntity::getPlan)
                .orElse(SubscriptionPlan.STARTER);

        Set<ProductCapability> capabilities = resolveCapabilities(orgType, plan);
        Map<ProductCapability, ModuleRecommendationStatus> moduleStatuses = resolveModuleStatuses(orgType, plan);
        List<String> recommendedModules = resolveRecommendedModules(orgType);
        String defaultDashboardView = resolveDefaultDashboardView(orgType);
        DashboardProfileDto dashboardProfile = resolveDashboardProfile(orgType);
        String onboardingProfile = resolveOnboardingProfile(orgType);
        List<OnboardingStepDto> onboardingChecklist = resolveOnboardingChecklist(orgType);

        boolean multiUser = orgType != OrganizationType.SOLO_PRACTITIONER;
        boolean clientPortal = capabilities.contains(ProductCapability.CLIENT_PORTAL);
        boolean noticeCenter = capabilities.contains(ProductCapability.TAX_NOTICE_MANAGEMENT);
        boolean customInvoicing = capabilities.contains(ProductCapability.BILLING_INVOICING);

        return OrganizationCapabilitiesDto.builder()
                .organizationId(organizationId)
                .organizationType(orgType)
                .subscriptionPlan(plan)
                .enabledCapabilities(capabilities)
                .moduleStatuses(moduleStatuses)
                .recommendedModules(recommendedModules)
                .defaultDashboardView(defaultDashboardView)
                .dashboardProfile(dashboardProfile)
                .onboardingProfile(onboardingProfile)
                .onboardingChecklist(onboardingChecklist)
                .multiUserPractice(multiUser)
                .clientPortalSupported(clientPortal)
                .noticeCenterSupported(noticeCenter)
                .customInvoicingSupported(customInvoicing)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCapabilityEnabled(ProductCapability capability) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            return false;
        }
        return isCapabilityEnabled(organizationId, capability);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCapabilityEnabled(UUID organizationId, ProductCapability capability) {
        if (organizationId == null || capability == null) {
            return false;
        }
        OrganizationCapabilitiesDto dto = getCapabilitiesByOrganizationId(organizationId);
        return dto.getEnabledCapabilities() != null && dto.getEnabledCapabilities().contains(capability);
    }

    private Set<ProductCapability> resolveCapabilities(OrganizationType orgType, SubscriptionPlan plan) {
        Set<ProductCapability> set = EnumSet.noneOf(ProductCapability.class);

        switch (orgType) {
            case SOLO_PRACTITIONER -> {
                set.add(ProductCapability.CLIENT_MANAGEMENT);
                set.add(ProductCapability.GST_COMPLIANCE);
                set.add(ProductCapability.ITR_COMPLIANCE);
                set.add(ProductCapability.TDS_COMPLIANCE);
                set.add(ProductCapability.COMPLIANCE_CALENDAR);
                set.add(ProductCapability.TASK_MANAGEMENT);
                set.add(ProductCapability.DOCUMENT_MANAGEMENT);
                set.add(ProductCapability.DOCUMENT_REQUESTS);
                set.add(ProductCapability.TAX_NOTICE_MANAGEMENT);
                set.add(ProductCapability.BILLING_INVOICING);
                set.add(ProductCapability.CLIENT_PORTAL);
            }
            case SMALL_TAX_FIRM -> {
                set.add(ProductCapability.CLIENT_MANAGEMENT);
                set.add(ProductCapability.GST_COMPLIANCE);
                set.add(ProductCapability.ITR_COMPLIANCE);
                set.add(ProductCapability.TDS_COMPLIANCE);
                set.add(ProductCapability.COMPLIANCE_CALENDAR);
                set.add(ProductCapability.TASK_MANAGEMENT);
                set.add(ProductCapability.DOCUMENT_MANAGEMENT);
                set.add(ProductCapability.DOCUMENT_REQUESTS);
                set.add(ProductCapability.TAX_NOTICE_MANAGEMENT);
                set.add(ProductCapability.BILLING_INVOICING);
                set.add(ProductCapability.CENTRAL_REPORTING);
                set.add(ProductCapability.TEAM_MANAGEMENT);
                set.add(ProductCapability.CLIENT_PORTAL);
            }
            case GROWING_PRACTICE -> {
                set.add(ProductCapability.CLIENT_MANAGEMENT);
                set.add(ProductCapability.GST_COMPLIANCE);
                set.add(ProductCapability.ITR_COMPLIANCE);
                set.add(ProductCapability.TDS_COMPLIANCE);
                set.add(ProductCapability.COMPLIANCE_CALENDAR);
                set.add(ProductCapability.TASK_MANAGEMENT);
                set.add(ProductCapability.DOCUMENT_MANAGEMENT);
                set.add(ProductCapability.DOCUMENT_REQUESTS);
                set.add(ProductCapability.TAX_NOTICE_MANAGEMENT);
                set.add(ProductCapability.BILLING_INVOICING);
                set.add(ProductCapability.CENTRAL_REPORTING);
                set.add(ProductCapability.TEAM_MANAGEMENT);
                set.add(ProductCapability.CLIENT_PORTAL);
                if (plan != SubscriptionPlan.STARTER) {
                    set.add(ProductCapability.ADVANCED_ANALYTICS);
                }
            }
            case BUSINESS -> {
                set.add(ProductCapability.GST_COMPLIANCE);
                set.add(ProductCapability.ITR_COMPLIANCE);
                set.add(ProductCapability.TDS_COMPLIANCE);
                set.add(ProductCapability.COMPLIANCE_CALENDAR);
                set.add(ProductCapability.TASK_MANAGEMENT);
                set.add(ProductCapability.DOCUMENT_MANAGEMENT);
                set.add(ProductCapability.TAX_NOTICE_MANAGEMENT);
                set.add(ProductCapability.TEAM_MANAGEMENT);
            }
            case UNKNOWN -> {
                set.add(ProductCapability.CLIENT_MANAGEMENT);
                set.add(ProductCapability.GST_COMPLIANCE);
                set.add(ProductCapability.ITR_COMPLIANCE);
                set.add(ProductCapability.TDS_COMPLIANCE);
                set.add(ProductCapability.COMPLIANCE_CALENDAR);
                set.add(ProductCapability.TASK_MANAGEMENT);
                set.add(ProductCapability.DOCUMENT_MANAGEMENT);
                set.add(ProductCapability.DOCUMENT_REQUESTS);
                set.add(ProductCapability.TAX_NOTICE_MANAGEMENT);
                set.add(ProductCapability.BILLING_INVOICING);
                set.add(ProductCapability.CENTRAL_REPORTING);
                set.add(ProductCapability.TEAM_MANAGEMENT);
                set.add(ProductCapability.CLIENT_PORTAL);
                if (plan == SubscriptionPlan.BUSINESS || plan == SubscriptionPlan.ENTERPRISE) {
                    set.add(ProductCapability.ADVANCED_ANALYTICS);
                }
            }
        }

        return set;
    }

    private Map<ProductCapability, ModuleRecommendationStatus> resolveModuleStatuses(
            OrganizationType orgType, SubscriptionPlan plan) {
        Map<ProductCapability, ModuleRecommendationStatus> map = new EnumMap<>(ProductCapability.class);

        switch (orgType) {
            case SOLO_PRACTITIONER -> {
                map.put(ProductCapability.CLIENT_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.GST_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.ITR_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TDS_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.COMPLIANCE_CALENDAR, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TASK_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.DOCUMENT_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.DOCUMENT_REQUESTS, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.BILLING_INVOICING, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.CLIENT_PORTAL, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.CENTRAL_REPORTING, ModuleRecommendationStatus.RECOMMENDED);
                map.put(ProductCapability.TAX_NOTICE_MANAGEMENT, ModuleRecommendationStatus.RECOMMENDED);
                map.put(ProductCapability.TEAM_MANAGEMENT, ModuleRecommendationStatus.NOT_RECOMMENDED);
                map.put(ProductCapability.ADVANCED_ANALYTICS,
                        plan == SubscriptionPlan.STARTER ? ModuleRecommendationStatus.UPGRADE_REQUIRED : ModuleRecommendationStatus.RECOMMENDED);
            }
            case SMALL_TAX_FIRM -> {
                map.put(ProductCapability.CLIENT_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.GST_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.ITR_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TDS_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.COMPLIANCE_CALENDAR, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TASK_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.DOCUMENT_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.DOCUMENT_REQUESTS, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.BILLING_INVOICING, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.CENTRAL_REPORTING, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TEAM_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.CLIENT_PORTAL, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TAX_NOTICE_MANAGEMENT, ModuleRecommendationStatus.RECOMMENDED);
                map.put(ProductCapability.ADVANCED_ANALYTICS,
                        plan == SubscriptionPlan.STARTER ? ModuleRecommendationStatus.UPGRADE_REQUIRED : ModuleRecommendationStatus.ACTIVE);
            }
            case GROWING_PRACTICE -> {
                map.put(ProductCapability.CLIENT_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.GST_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.ITR_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TDS_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.COMPLIANCE_CALENDAR, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TASK_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.DOCUMENT_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.DOCUMENT_REQUESTS, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TAX_NOTICE_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.BILLING_INVOICING, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.CENTRAL_REPORTING, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TEAM_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.CLIENT_PORTAL, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.ADVANCED_ANALYTICS,
                        plan == SubscriptionPlan.STARTER ? ModuleRecommendationStatus.UPGRADE_REQUIRED : ModuleRecommendationStatus.ACTIVE);
            }
            case BUSINESS -> {
                map.put(ProductCapability.GST_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.ITR_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TDS_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.COMPLIANCE_CALENDAR, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TASK_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.DOCUMENT_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TEAM_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TAX_NOTICE_MANAGEMENT, ModuleRecommendationStatus.RECOMMENDED);
                map.put(ProductCapability.CENTRAL_REPORTING, ModuleRecommendationStatus.RECOMMENDED);
                map.put(ProductCapability.CLIENT_MANAGEMENT, ModuleRecommendationStatus.NOT_RECOMMENDED);
                map.put(ProductCapability.CLIENT_PORTAL, ModuleRecommendationStatus.NOT_RECOMMENDED);
                map.put(ProductCapability.DOCUMENT_REQUESTS, ModuleRecommendationStatus.NOT_RECOMMENDED);
                map.put(ProductCapability.BILLING_INVOICING, ModuleRecommendationStatus.NOT_RECOMMENDED);
                map.put(ProductCapability.ADVANCED_ANALYTICS,
                        plan == SubscriptionPlan.STARTER ? ModuleRecommendationStatus.UPGRADE_REQUIRED : ModuleRecommendationStatus.ACTIVE);
            }
            case UNKNOWN -> {
                map.put(ProductCapability.CLIENT_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.GST_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.ITR_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TDS_COMPLIANCE, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.COMPLIANCE_CALENDAR, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TASK_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.DOCUMENT_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.DOCUMENT_REQUESTS, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TAX_NOTICE_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.BILLING_INVOICING, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.CENTRAL_REPORTING, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.TEAM_MANAGEMENT, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.CLIENT_PORTAL, ModuleRecommendationStatus.ACTIVE);
                map.put(ProductCapability.ADVANCED_ANALYTICS,
                        (plan == SubscriptionPlan.BUSINESS || plan == SubscriptionPlan.ENTERPRISE)
                                ? ModuleRecommendationStatus.ACTIVE
                                : ModuleRecommendationStatus.UPGRADE_REQUIRED);
            }
        }

        return map;
    }

    private List<OnboardingStepDto> resolveOnboardingChecklist(OrganizationType orgType) {
        return switch (orgType) {
            case SOLO_PRACTITIONER -> List.of(
                    OnboardingStepDto.builder()
                            .stepKey("PRACTICE_PROFILE")
                            .title("Complete Practice Profile")
                            .description("Setup your practice contact details, letterhead, and tax registrations.")
                            .targetRoute("/settings/profile")
                            .sortOrder(1)
                            .mandatory(true)
                            .targetCapability(null)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("FIRST_CLIENT")
                            .title("Add First Client")
                            .description("Create your first client profile with PAN, GSTIN, and contact details.")
                            .targetRoute("/clients/new")
                            .sortOrder(2)
                            .mandatory(true)
                            .targetCapability(ProductCapability.CLIENT_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("COMPLIANCE_SETUP")
                            .title("Configure Tax Compliance")
                            .description("Enable GST, ITR, and TDS return tracking for your clients.")
                            .targetRoute("/compliance")
                            .sortOrder(3)
                            .mandatory(true)
                            .targetCapability(ProductCapability.GST_COMPLIANCE)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("WORKLIST_SETUP")
                            .title("Organize Daily Tasks")
                            .description("Create and schedule your compliance tasks and deadlines.")
                            .targetRoute("/tasks")
                            .sortOrder(4)
                            .mandatory(false)
                            .targetCapability(ProductCapability.TASK_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("DOCUMENT_VAULT")
                            .title("Upload Client Documents")
                            .description("Store client tax records and organize statutory folders.")
                            .targetRoute("/documents")
                            .sortOrder(5)
                            .mandatory(false)
                            .targetCapability(ProductCapability.DOCUMENT_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("FIRST_INVOICE")
                            .title("Create First Professional Invoice")
                            .description("Bill your clients and track professional fee receipts.")
                            .targetRoute("/billing")
                            .sortOrder(6)
                            .mandatory(false)
                            .targetCapability(ProductCapability.BILLING_INVOICING)
                            .build()
            );
            case SMALL_TAX_FIRM -> List.of(
                    OnboardingStepDto.builder()
                            .stepKey("FIRM_PROFILE")
                            .title("Firm Profile & Letterhead")
                            .description("Configure firm credentials, partners, and office details.")
                            .targetRoute("/settings/profile")
                            .sortOrder(1)
                            .mandatory(true)
                            .targetCapability(null)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("INVITE_STAFF")
                            .title("Invite Team Members")
                            .description("Add staff accountants, assistants, and assign system roles.")
                            .targetRoute("/team")
                            .sortOrder(2)
                            .mandatory(true)
                            .targetCapability(ProductCapability.TEAM_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("IMPORT_CLIENTS")
                            .title("Import Client Database")
                            .description("Add or bulk import practice clients and statutory numbers.")
                            .targetRoute("/clients")
                            .sortOrder(3)
                            .mandatory(true)
                            .targetCapability(ProductCapability.CLIENT_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("ASSIGN_PORTFOLIOS")
                            .title("Assign Client Portfolios")
                            .description("Distribute client responsibility across your team members.")
                            .targetRoute("/clients/assign")
                            .sortOrder(4)
                            .mandatory(true)
                            .targetCapability(ProductCapability.CLIENT_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("COMPLIANCE_CALENDAR")
                            .title("Setup Statutory Calendars")
                            .description("Schedule upcoming GST, TDS, and ITR filing deadlines.")
                            .targetRoute("/compliance")
                            .sortOrder(5)
                            .mandatory(true)
                            .targetCapability(ProductCapability.COMPLIANCE_CALENDAR)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("CLIENT_PORTAL")
                            .title("Enable Client Portal Access")
                            .description("Grant taxpayers direct access to their acknowledgements and receipts.")
                            .targetRoute("/portal/settings")
                            .sortOrder(6)
                            .mandatory(false)
                            .targetCapability(ProductCapability.CLIENT_PORTAL)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("PROFESSIONAL_BILLING")
                            .title("Configure Professional Billing")
                            .description("Setup billing rates, fee schedules, and payment reminders.")
                            .targetRoute("/billing")
                            .sortOrder(7)
                            .mandatory(false)
                            .targetCapability(ProductCapability.BILLING_INVOICING)
                            .build()
            );
            case GROWING_PRACTICE -> List.of(
                    OnboardingStepDto.builder()
                            .stepKey("PRACTICE_CONFIG")
                            .title("Enterprise Practice Configuration")
                            .description("Setup multi-department structure, branches, and compliance protocols.")
                            .targetRoute("/settings/profile")
                            .sortOrder(1)
                            .mandatory(true)
                            .targetCapability(null)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("ONBOARD_HIERARCHY")
                            .title("Onboard Employees & Managers")
                            .description("Invite staff, managers, partners, and configure access levels.")
                            .targetRoute("/team")
                            .sortOrder(2)
                            .mandatory(true)
                            .targetCapability(ProductCapability.TEAM_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("BULK_CLIENT_IMPORT")
                            .title("Bulk Import Client Base")
                            .description("Import large client portfolios with full compliance histories.")
                            .targetRoute("/clients/import")
                            .sortOrder(3)
                            .mandatory(true)
                            .targetCapability(ProductCapability.CLIENT_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("MANAGER_PORTFOLIOS")
                            .title("Configure Manager Portfolios")
                            .description("Establish team reporting lines and portfolio oversight scopes.")
                            .targetRoute("/team/hierarchy")
                            .sortOrder(4)
                            .mandatory(true)
                            .targetCapability(ProductCapability.TEAM_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("COMPLIANCE_PIPELINE")
                            .title("Configure Compliance Pipeline")
                            .description("Deploy automated filing tracking across GST, TDS, and ITR.")
                            .targetRoute("/compliance")
                            .sortOrder(5)
                            .mandatory(true)
                            .targetCapability(ProductCapability.GST_COMPLIANCE)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("NOTICE_CENTER")
                            .title("Setup Notice Tracking Center")
                            .description("Centralize tax assessment notices, hearings, and reply drafting.")
                            .targetRoute("/notices")
                            .sortOrder(6)
                            .mandatory(false)
                            .targetCapability(ProductCapability.TAX_NOTICE_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("CENTRAL_REPORTING")
                            .title("Configure Central Reporting")
                            .description("Setup practice realization, employee utilization, and audit logs.")
                            .targetRoute("/reports")
                            .sortOrder(7)
                            .mandatory(false)
                            .targetCapability(ProductCapability.CENTRAL_REPORTING)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("ADVANCED_ANALYTICS")
                            .title("Enable Practice Analytics")
                            .description("Unlock forecasting, realization metrics, and team capacity insights.")
                            .targetRoute("/analytics")
                            .sortOrder(8)
                            .mandatory(false)
                            .targetCapability(ProductCapability.ADVANCED_ANALYTICS)
                            .build()
            );
            case BUSINESS -> List.of(
                    OnboardingStepDto.builder()
                            .stepKey("CORPORATE_PROFILE")
                            .title("Corporate Profile & Registrations")
                            .description("Configure company legal name, PAN, CIN, TAN, and GSTIN registrations.")
                            .targetRoute("/settings/profile")
                            .sortOrder(1)
                            .mandatory(true)
                            .targetCapability(null)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("INVITE_TAX_TEAM")
                            .title("Invite In-House Tax Team")
                            .description("Add finance managers, accounts officers, and internal auditors.")
                            .targetRoute("/team")
                            .sortOrder(2)
                            .mandatory(true)
                            .targetCapability(ProductCapability.TEAM_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("CORPORATE_COMPLIANCE")
                            .title("Configure Corporate Tax Obligations")
                            .description("Setup GST 3B/1, TDS 24Q/26Q, and Corporate ITR-6 filing trackers.")
                            .targetRoute("/compliance")
                            .sortOrder(3)
                            .mandatory(true)
                            .targetCapability(ProductCapability.GST_COMPLIANCE)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("STATUTORY_CALENDAR")
                            .title("Setup Statutory Filing Calendar")
                            .description("Configure tax payment due dates and advance reminder alerts.")
                            .targetRoute("/calendar")
                            .sortOrder(4)
                            .mandatory(true)
                            .targetCapability(ProductCapability.COMPLIANCE_CALENDAR)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("DOCUMENT_VAULT")
                            .title("Organize Corporate Tax Vault")
                            .description("Centralize challans, return acknowledgements, and audit schedules.")
                            .targetRoute("/documents")
                            .sortOrder(5)
                            .mandatory(false)
                            .targetCapability(ProductCapability.DOCUMENT_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("TAX_NOTICES")
                            .title("Track Assessment Notices")
                            .description("Monitor IT department communications, demand notices, and appeals.")
                            .targetRoute("/notices")
                            .sortOrder(6)
                            .mandatory(false)
                            .targetCapability(ProductCapability.TAX_NOTICE_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("COMPLIANCE_REPORTING")
                            .title("Setup Management Tax Reporting")
                            .description("Configure tax audit summaries and executive board reports.")
                            .targetRoute("/reports")
                            .sortOrder(7)
                            .mandatory(false)
                            .targetCapability(ProductCapability.CENTRAL_REPORTING)
                            .build()
            );
            case UNKNOWN -> List.of(
                    OnboardingStepDto.builder()
                            .stepKey("CLASSIFY_PERSONA")
                            .title("Select Organization Model")
                            .description("Choose your practice type to customize your navigation and dashboard.")
                            .targetRoute("/settings/organization")
                            .sortOrder(1)
                            .mandatory(true)
                            .targetCapability(null)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("ORGANIZATION_PROFILE")
                            .title("Setup Organization Profile")
                            .description("Configure legal entity details, contact information, and preferences.")
                            .targetRoute("/settings/profile")
                            .sortOrder(2)
                            .mandatory(true)
                            .targetCapability(null)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("INITIAL_CLIENTS")
                            .title("Add First Client / Portfolio")
                            .description("Create initial client profiles or internal corporate records.")
                            .targetRoute("/clients")
                            .sortOrder(3)
                            .mandatory(true)
                            .targetCapability(ProductCapability.CLIENT_MANAGEMENT)
                            .build(),
                    OnboardingStepDto.builder()
                            .stepKey("COMPLIANCE_SETUP")
                            .title("Configure Tax Compliance Obligations")
                            .description("Setup GST, TDS, and ITR filing trackers.")
                            .targetRoute("/compliance")
                            .sortOrder(4)
                            .mandatory(true)
                            .targetCapability(ProductCapability.GST_COMPLIANCE)
                            .build()
            );
        };
    }

    private DashboardProfileDto resolveDashboardProfile(OrganizationType orgType) {
        return switch (orgType) {
            case SOLO_PRACTITIONER -> DashboardProfileDto.builder()
                    .profileKey("SOLO_WORKLIST")
                    .title("Solo Practitioner Worklist")
                    .description("Single-user task, client compliance, and invoicing command center.")
                    .defaultRoute("/tasks?tab=WORKLIST&scope=MY_WORK")
                    .primaryMetrics(List.of("ACTIVE_CLIENTS", "MY_PENDING_TASKS", "UPCOMING_DEADLINES", "UNPAID_INVOICES"))
                    .quickActions(List.of("ADD_CLIENT", "NEW_TASK", "NEW_INVOICE", "FILE_RETURN"))
                    .recommendedWidgets(List.of("MY_WORKLIST", "UPCOMING_COMPLIANCE_CALENDAR", "RECENT_CLIENT_VAULT", "INVOICING_SUMMARY"))
                    .build();
            case SMALL_TAX_FIRM -> DashboardProfileDto.builder()
                    .profileKey("TEAM_PRACTICE")
                    .title("Small Firm Practice Dashboard")
                    .description("Multi-staff workload distribution, client portfolio tracking, and firm compliance overview.")
                    .defaultRoute("/dashboard")
                    .primaryMetrics(List.of("TOTAL_CLIENTS", "TEAM_PENDING_TASKS", "OVERDUE_COMPLIANCES", "COLLECTION_REALIZATION"))
                    .quickActions(List.of("INVITE_STAFF", "BULK_ASSIGN_CLIENTS", "CREATE_TASK", "GENERATE_REPORT"))
                    .recommendedWidgets(List.of("FIRM_OVERVIEW_METRICS", "STAFF_WORKLOAD_DISTRIBUTION", "PORTFOLIO_COMPLIANCE_HEALTH", "CLIENT_PORTAL_ACTIVITY", "FEE_COLLECTIONS"))
                    .build();
            case GROWING_PRACTICE -> DashboardProfileDto.builder()
                    .profileKey("GROWING_PRACTICE")
                    .title("Growing Practice Executive Cockpit")
                    .description("Department-level performance, manager portfolio oversight, and assessment notice control.")
                    .defaultRoute("/dashboard")
                    .primaryMetrics(List.of("ACTIVE_PORTFOLIOS", "DEPARTMENT_UTILIZATION", "CRITICAL_NOTICES", "PRACTICE_REVENUE_RUN_RATE"))
                    .quickActions(List.of("DISPATCH_TASK_BATCH", "ALLOCATE_PORTFOLIO", "LOG_ASSESSMENT_NOTICE", "EXPORT_PRACTICE_AUDIT"))
                    .recommendedWidgets(List.of("EXECUTIVE_PRACTICE_KPI", "MANAGER_PORTFOLIO_HEALTH", "NOTICE_DISPUTE_TRACKER", "REALIZATION_ANALYTICS", "TEAM_CAPACITY_HEATMAP"))
                    .build();
            case BUSINESS -> DashboardProfileDto.builder()
                    .profileKey("IN_HOUSE_COMPLIANCE")
                    .title("Corporate In-House Tax Dashboard")
                    .description("Internal statutory filings, tax notice dispute tracking, and corporate document archive.")
                    .defaultRoute("/compliance")
                    .primaryMetrics(List.of("UPCOMING_STATUTORY_DEADLINES", "FILING_COMPLIANCE_RATE", "ACTIVE_NOTICES_HEARINGS", "PENDING_INTERNAL_TASKS"))
                    .quickActions(List.of("LOG_TAX_CHALLAN", "FILE_GST_3B", "LOG_INCOME_TAX_NOTICE", "UPLOAD_ANNUAL_REPORT"))
                    .recommendedWidgets(List.of("CORPORATE_COMPLIANCE_CALENDAR", "GST_TDS_FILING_PIPELINE", "TAX_ASSESSMENT_NOTICE_BOARD", "INTERNAL_AUDIT_VAULT"))
                    .build();
            case UNKNOWN -> DashboardProfileDto.builder()
                    .profileKey("STANDARD_PRACTICE")
                    .title("Standard Practice Dashboard")
                    .description("Standard general tax practice command center.")
                    .defaultRoute("/dashboard")
                    .primaryMetrics(List.of("TOTAL_CLIENTS", "ACTIVE_TASKS", "UPCOMING_DEADLINES", "COMPLIANCE_HEALTH"))
                    .quickActions(List.of("ADD_CLIENT", "NEW_TASK", "FILE_RETURN", "CREATE_INVOICE"))
                    .recommendedWidgets(List.of("PRACTICE_SUMMARY", "UPCOMING_COMPLIANCE", "TASK_WORKLIST", "DOCUMENT_VAULT"))
                    .build();
        };
    }

    private List<String> resolveRecommendedModules(OrganizationType orgType) {
        return switch (orgType) {
            case SOLO_PRACTITIONER -> List.of("CLIENTS", "GST", "ITR", "TDS", "NOTICES", "TASKS", "BILLING");
            case SMALL_TAX_FIRM -> List.of("CLIENTS", "GST", "ITR", "TDS", "NOTICES", "TASKS", "TEAM", "BILLING", "PORTAL", "REPORTS");
            case GROWING_PRACTICE -> List.of("CLIENTS", "GST", "ITR", "TDS", "NOTICES", "TASKS", "TEAM", "BILLING", "PORTAL", "REPORTS");
            case BUSINESS -> List.of("GST", "ITR", "TDS", "NOTICES", "COMPLIANCE_CALENDAR", "DOCUMENTS", "TASKS", "TEAM");
            case UNKNOWN -> List.of("CLIENTS", "GST", "ITR", "TDS", "NOTICES", "TASKS", "DOCUMENTS", "BILLING", "REPORTS");
        };
    }

    private String resolveDefaultDashboardView(OrganizationType orgType) {
        return switch (orgType) {
            case SOLO_PRACTITIONER -> "SOLO_WORKLIST";
            case SMALL_TAX_FIRM -> "TEAM_PRACTICE";
            case GROWING_PRACTICE -> "GROWING_PRACTICE";
            case BUSINESS -> "IN_HOUSE_COMPLIANCE";
            case UNKNOWN -> "STANDARD_PRACTICE";
        };
    }

    private String resolveOnboardingProfile(OrganizationType orgType) {
        return switch (orgType) {
            case SOLO_PRACTITIONER -> "SOLO_CONSULTANT_PROFILE";
            case SMALL_TAX_FIRM -> "SMALL_FIRM_PROFILE";
            case GROWING_PRACTICE -> "MULTI_DISCIPLINARY_PRACTICE";
            case BUSINESS -> "IN_HOUSE_TAX_TEAM";
            case UNKNOWN -> "GENERAL_PRACTICE";
        };
    }
}
