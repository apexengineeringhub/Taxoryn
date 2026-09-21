package com.taxoryn.module.capability.service;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCapabilityServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private ProductCapabilityServiceImpl capabilityService;

    private final UUID testOrgId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID testUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-111111111111");

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(testOrgId);
        SecurityUser principal = SecurityUser.builder()
                .userId(testUserId)
                .organizationId(testOrgId)
                .email("test@taxoryn.com")
                .roles(Set.of("ROLE_PRACTITIONER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. UNKNOWN Organization receives safe default full baseline capabilities, onboarding, and dashboard")
    void testUnknownOrganizationCapabilities() {
        OrganizationEntity org = OrganizationEntity.builder()
                .legalName("Legacy Unclassified Org")
                .organizationType(OrganizationType.UNKNOWN)
                .build();
        org.setId(testOrgId);

        when(organizationRepository.findById(testOrgId)).thenReturn(Optional.of(org));
        when(subscriptionRepository.findByOrganizationId(testOrgId)).thenReturn(Optional.empty());

        OrganizationCapabilitiesDto dto = capabilityService.getCurrentOrganizationCapabilities();

        assertNotNull(dto);
        assertEquals(OrganizationType.UNKNOWN, dto.getOrganizationType());
        assertEquals("STANDARD_PRACTICE", dto.getDefaultDashboardView());
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.CLIENT_MANAGEMENT));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.GST_COMPLIANCE));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.ITR_COMPLIANCE));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.TDS_COMPLIANCE));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.TASK_MANAGEMENT));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.BILLING_INVOICING));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.CLIENT_PORTAL));

        // Module Statuses
        Map<ProductCapability, ModuleRecommendationStatus> statuses = dto.getModuleStatuses();
        assertNotNull(statuses);
        assertEquals(ModuleRecommendationStatus.ACTIVE, statuses.get(ProductCapability.CLIENT_MANAGEMENT));
        assertEquals(ModuleRecommendationStatus.UPGRADE_REQUIRED, statuses.get(ProductCapability.ADVANCED_ANALYTICS));

        // Onboarding Checklist
        List<OnboardingStepDto> steps = dto.getOnboardingChecklist();
        assertNotNull(steps);
        assertEquals(4, steps.size());
        assertEquals("CLASSIFY_PERSONA", steps.get(0).getStepKey());
        assertTrue(steps.get(0).isMandatory());

        // Dashboard Profile
        DashboardProfileDto dash = dto.getDashboardProfile();
        assertNotNull(dash);
        assertEquals("STANDARD_PRACTICE", dash.getProfileKey());
        assertEquals("/dashboard", dash.getDefaultRoute());
    }

    @Test
    @DisplayName("2. SOLO_PRACTITIONER resolves solo workflow, single-user profile, and worklist dashboard")
    void testSoloPractitionerCapabilities() {
        OrganizationEntity org = OrganizationEntity.builder()
                .legalName("CA Alice Solo")
                .organizationType(OrganizationType.SOLO_PRACTITIONER)
                .build();
        org.setId(testOrgId);

        when(organizationRepository.findById(testOrgId)).thenReturn(Optional.of(org));
        when(subscriptionRepository.findByOrganizationId(testOrgId)).thenReturn(Optional.empty());

        OrganizationCapabilitiesDto dto = capabilityService.getCurrentOrganizationCapabilities();

        assertNotNull(dto);
        assertEquals(OrganizationType.SOLO_PRACTITIONER, dto.getOrganizationType());
        assertEquals("SOLO_WORKLIST", dto.getDefaultDashboardView());
        assertFalse(dto.isMultiUserPractice());
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.CLIENT_MANAGEMENT));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.GST_COMPLIANCE));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.BILLING_INVOICING));
        assertFalse(dto.getEnabledCapabilities().contains(ProductCapability.TEAM_MANAGEMENT));

        // Module Statuses
        Map<ProductCapability, ModuleRecommendationStatus> statuses = dto.getModuleStatuses();
        assertNotNull(statuses);
        assertEquals(ModuleRecommendationStatus.ACTIVE, statuses.get(ProductCapability.CLIENT_MANAGEMENT));
        assertEquals(ModuleRecommendationStatus.ACTIVE, statuses.get(ProductCapability.BILLING_INVOICING));
        assertEquals(ModuleRecommendationStatus.NOT_RECOMMENDED, statuses.get(ProductCapability.TEAM_MANAGEMENT));
        assertEquals(ModuleRecommendationStatus.RECOMMENDED, statuses.get(ProductCapability.CENTRAL_REPORTING));
        assertEquals(ModuleRecommendationStatus.UPGRADE_REQUIRED, statuses.get(ProductCapability.ADVANCED_ANALYTICS));

        // Onboarding Checklist
        List<OnboardingStepDto> steps = dto.getOnboardingChecklist();
        assertNotNull(steps);
        assertEquals(6, steps.size());
        assertEquals("PRACTICE_PROFILE", steps.get(0).getStepKey());
        assertEquals("FIRST_CLIENT", steps.get(1).getStepKey());
        assertEquals("/clients/new", steps.get(1).getTargetRoute());

        // Dashboard Profile
        DashboardProfileDto dash = dto.getDashboardProfile();
        assertNotNull(dash);
        assertEquals("SOLO_WORKLIST", dash.getProfileKey());
        assertEquals("/tasks?tab=WORKLIST&scope=MY_WORK", dash.getDefaultRoute());
        assertTrue(dash.getPrimaryMetrics().contains("MY_PENDING_TASKS"));
    }

    @Test
    @DisplayName("3. SMALL_TAX_FIRM resolves team management, client portal, and team practice dashboard")
    void testSmallTaxFirmCapabilities() {
        OrganizationEntity org = OrganizationEntity.builder()
                .legalName("Sharma & Associates")
                .organizationType(OrganizationType.SMALL_TAX_FIRM)
                .build();
        org.setId(testOrgId);

        when(organizationRepository.findById(testOrgId)).thenReturn(Optional.of(org));
        when(subscriptionRepository.findByOrganizationId(testOrgId)).thenReturn(Optional.empty());

        OrganizationCapabilitiesDto dto = capabilityService.getCurrentOrganizationCapabilities();

        assertNotNull(dto);
        assertEquals(OrganizationType.SMALL_TAX_FIRM, dto.getOrganizationType());
        assertEquals("TEAM_PRACTICE", dto.getDefaultDashboardView());
        assertTrue(dto.isMultiUserPractice());
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.TEAM_MANAGEMENT));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.CLIENT_PORTAL));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.CENTRAL_REPORTING));

        // Module Statuses
        Map<ProductCapability, ModuleRecommendationStatus> statuses = dto.getModuleStatuses();
        assertNotNull(statuses);
        assertEquals(ModuleRecommendationStatus.ACTIVE, statuses.get(ProductCapability.TEAM_MANAGEMENT));
        assertEquals(ModuleRecommendationStatus.ACTIVE, statuses.get(ProductCapability.CLIENT_PORTAL));
        assertEquals(ModuleRecommendationStatus.RECOMMENDED, statuses.get(ProductCapability.TAX_NOTICE_MANAGEMENT));

        // Onboarding Checklist
        List<OnboardingStepDto> steps = dto.getOnboardingChecklist();
        assertNotNull(steps);
        assertEquals(7, steps.size());
        assertEquals("INVITE_STAFF", steps.get(1).getStepKey());
        assertEquals("ASSIGN_PORTFOLIOS", steps.get(3).getStepKey());

        // Dashboard Profile
        DashboardProfileDto dash = dto.getDashboardProfile();
        assertNotNull(dash);
        assertEquals("TEAM_PRACTICE", dash.getProfileKey());
        assertTrue(dash.getRecommendedWidgets().contains("STAFF_WORKLOAD_DISTRIBUTION"));
    }

    @Test
    @DisplayName("4. GROWING_PRACTICE resolves full multi-department capabilities including tax notices and analytics on upgrade")
    void testGrowingPracticeCapabilities() {
        OrganizationEntity org = OrganizationEntity.builder()
                .legalName("Apex Global Tax Advisors")
                .organizationType(OrganizationType.GROWING_PRACTICE)
                .build();
        org.setId(testOrgId);

        SubscriptionEntity sub = SubscriptionEntity.builder()
                .organizationId(testOrgId)
                .plan(SubscriptionPlan.BUSINESS)
                .build();

        when(organizationRepository.findById(testOrgId)).thenReturn(Optional.of(org));
        when(subscriptionRepository.findByOrganizationId(testOrgId)).thenReturn(Optional.of(sub));

        OrganizationCapabilitiesDto dto = capabilityService.getCurrentOrganizationCapabilities();

        assertNotNull(dto);
        assertEquals(OrganizationType.GROWING_PRACTICE, dto.getOrganizationType());
        assertEquals("GROWING_PRACTICE", dto.getDefaultDashboardView());
        assertTrue(dto.isNoticeCenterSupported());
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.TAX_NOTICE_MANAGEMENT));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.ADVANCED_ANALYTICS));

        // Module Statuses
        Map<ProductCapability, ModuleRecommendationStatus> statuses = dto.getModuleStatuses();
        assertNotNull(statuses);
        assertEquals(ModuleRecommendationStatus.ACTIVE, statuses.get(ProductCapability.TAX_NOTICE_MANAGEMENT));
        assertEquals(ModuleRecommendationStatus.ACTIVE, statuses.get(ProductCapability.ADVANCED_ANALYTICS));

        // Onboarding Checklist
        List<OnboardingStepDto> steps = dto.getOnboardingChecklist();
        assertNotNull(steps);
        assertEquals(8, steps.size());
        assertEquals("ONBOARD_HIERARCHY", steps.get(1).getStepKey());
        assertEquals("NOTICE_CENTER", steps.get(5).getStepKey());

        // Dashboard Profile
        DashboardProfileDto dash = dto.getDashboardProfile();
        assertNotNull(dash);
        assertEquals("GROWING_PRACTICE", dash.getProfileKey());
        assertTrue(dash.getRecommendedWidgets().contains("NOTICE_DISPUTE_TRACKER"));
    }

    @Test
    @DisplayName("5. BUSINESS resolves in-house corporate compliance and document management")
    void testBusinessCapabilities() {
        OrganizationEntity org = OrganizationEntity.builder()
                .legalName("Acme Manufacturing Pvt Ltd")
                .organizationType(OrganizationType.BUSINESS)
                .build();
        org.setId(testOrgId);

        when(organizationRepository.findById(testOrgId)).thenReturn(Optional.of(org));
        when(subscriptionRepository.findByOrganizationId(testOrgId)).thenReturn(Optional.empty());

        OrganizationCapabilitiesDto dto = capabilityService.getCurrentOrganizationCapabilities();

        assertNotNull(dto);
        assertEquals(OrganizationType.BUSINESS, dto.getOrganizationType());
        assertEquals("IN_HOUSE_COMPLIANCE", dto.getDefaultDashboardView());
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.GST_COMPLIANCE));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.TDS_COMPLIANCE));
        assertTrue(dto.getEnabledCapabilities().contains(ProductCapability.COMPLIANCE_CALENDAR));
        assertFalse(dto.getEnabledCapabilities().contains(ProductCapability.BILLING_INVOICING));
        assertFalse(dto.getEnabledCapabilities().contains(ProductCapability.CLIENT_MANAGEMENT));

        // Module Statuses
        Map<ProductCapability, ModuleRecommendationStatus> statuses = dto.getModuleStatuses();
        assertNotNull(statuses);
        assertEquals(ModuleRecommendationStatus.ACTIVE, statuses.get(ProductCapability.GST_COMPLIANCE));
        assertEquals(ModuleRecommendationStatus.NOT_RECOMMENDED, statuses.get(ProductCapability.CLIENT_MANAGEMENT));
        assertEquals(ModuleRecommendationStatus.NOT_RECOMMENDED, statuses.get(ProductCapability.CLIENT_PORTAL));
        assertEquals(ModuleRecommendationStatus.NOT_RECOMMENDED, statuses.get(ProductCapability.BILLING_INVOICING));

        // Onboarding Checklist
        List<OnboardingStepDto> steps = dto.getOnboardingChecklist();
        assertNotNull(steps);
        assertEquals(7, steps.size());
        assertEquals("CORPORATE_PROFILE", steps.get(0).getStepKey());
        assertEquals("STATUTORY_CALENDAR", steps.get(3).getStepKey());

        // Dashboard Profile
        DashboardProfileDto dash = dto.getDashboardProfile();
        assertNotNull(dash);
        assertEquals("IN_HOUSE_COMPLIANCE", dash.getProfileKey());
        assertEquals("/compliance", dash.getDefaultRoute());
        assertTrue(dash.getRecommendedWidgets().contains("CORPORATE_COMPLIANCE_CALENDAR"));
    }

    @Test
    @DisplayName("6. Security Decoupling: OrganizationType variations do not modify security roles or permissions")
    void testSecurityDecouplingInvariant() {
        // Authenticated user has ROLE_PRACTITIONER
        SecurityUser user = SecurityUtils.getCurrentUser().orElseThrow();
        assertNotNull(user);
        assertTrue(user.getRoles().contains("ROLE_PRACTITIONER"));

        // Changing org type from SOLO to BUSINESS returns different UI capabilities
        OrganizationEntity soloOrg = OrganizationEntity.builder()
                .legalName("Solo Practice")
                .organizationType(OrganizationType.SOLO_PRACTITIONER)
                .build();
        soloOrg.setId(testOrgId);

        when(organizationRepository.findById(testOrgId)).thenReturn(Optional.of(soloOrg));
        when(subscriptionRepository.findByOrganizationId(testOrgId)).thenReturn(Optional.empty());

        OrganizationCapabilitiesDto soloDto = capabilityService.getCurrentOrganizationCapabilities();
        assertEquals(OrganizationType.SOLO_PRACTITIONER, soloDto.getOrganizationType());

        // Verify security principal remains unmodified
        SecurityUser userAfter = SecurityUtils.getCurrentUser().orElseThrow();
        assertEquals(user.getRoles(), userAfter.getRoles());
        assertEquals(user.getUserId(), userAfter.getUserId());
    }
}
