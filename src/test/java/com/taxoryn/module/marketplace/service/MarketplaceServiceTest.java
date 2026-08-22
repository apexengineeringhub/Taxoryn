package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.LeadStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.ProfessionalType;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VerificationStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VisibilityStatus;
import com.taxoryn.module.marketplace.mapper.MarketplaceMapper;
import com.taxoryn.module.marketplace.repository.*;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceTest {

    @Mock
    private MarketplaceProfileRepository profileRepository;

    @Mock
    private MarketplaceServiceRepository serviceRepository;

    @Mock
    private MarketplaceLeadRepository leadRepository;

    @Mock
    private MarketplaceConsultationRepository consultationRepository;

    @Mock
    private MarketplaceReviewRepository reviewRepository;

    @Mock
    private MarketplaceVerificationRepository verificationRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private MarketplaceMapper mapper;

    @Mock
    private AuditService auditService;

    @Mock
    private ProfileCompletenessCalculator completenessCalculator;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;

    private UUID organizationId;
    private UUID profileId;
    private MarketplaceProfileEntity sampleProfile;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        TenantContext.setTenantId(organizationId);
        SecurityUser user = SecurityUser.builder()
                .userId(userId)
                .organizationId(organizationId)
                .email("admin@apextax.com")
                .password("hash")
                .enabled(true)
                .roles(Set.of("ORG_ADMIN"))
                .permissions(Set.of("MARKETPLACE_VIEW", "MARKETPLACE_WRITE", "CLIENT_CREATE"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        sampleProfile = MarketplaceProfileEntity.builder()
                .organizationId(organizationId)
                .slug("apex-advisors")
                .displayName("Apex Corporate & Tax Advisors")
                .headline("Premier GST & ITR Practice")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .city("Mumbai")
                .state("Maharashtra")
                .phone("9820011223")
                .email("contact@apextax.com")
                .startingFee(new BigDecimal("999.00"))
                .averageRating(new BigDecimal("4.95"))
                .verificationStatus(VerificationStatus.VERIFIED)
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .isPublished(true)
                .build();
        sampleProfile.setId(profileId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should successfully search public marketplace profiles")
    void testSearchProfiles() {
        MarketplaceSearchRequest req = MarketplaceSearchRequest.builder()
                .city("Mumbai")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .build();

        when(profileRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleProfile)));

        PublicMarketplaceProfileDto dto = PublicMarketplaceProfileDto.builder()
                .id(profileId)
                .displayName(sampleProfile.getDisplayName())
                .city("Mumbai")
                .build();
        when(mapper.toProfileDto(sampleProfile)).thenReturn(dto);
        when(serviceRepository.findByMarketplaceProfileIdAndIsActiveTrue(profileId)).thenReturn(Collections.emptyList());
        when(reviewRepository.findByMarketplaceProfileIdAndStatusOrderByCreatedAtDesc(profileId, MarketplaceReviewEntity.ReviewStatus.APPROVED))
                .thenReturn(Collections.emptyList());

        PagedResponse<PublicMarketplaceProfileDto> result = marketplaceService.searchProfiles(req);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Apex Corporate & Tax Advisors", result.getContent().get(0).getDisplayName());
    }

    @Test
    @DisplayName("Should submit public lead inquiry")
    void testSubmitPublicLead() {
        CreateMarketplaceLeadRequest req = CreateMarketplaceLeadRequest.builder()
                .marketplaceProfileId(profileId)
                .clientName("Rohan Verma")
                .clientEmail("rohan@example.com")
                .clientPhone("9876543210")
                .requirementDescription("Need assistance with GST registration")
                .build();

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(sampleProfile));

        MarketplaceLeadEntity savedLead = MarketplaceLeadEntity.builder()
                .organizationId(organizationId)
                .marketplaceProfileId(profileId)
                .clientName("Rohan Verma")
                .clientEmail("rohan@example.com")
                .clientPhone("9876543210")
                .requirementDescription(req.getRequirementDescription())
                .leadStatus(LeadStatus.NEW)
                .build();
        savedLead.setId(UUID.randomUUID());

        when(leadRepository.save(any(MarketplaceLeadEntity.class))).thenReturn(savedLead);

        MarketplaceLeadDto dto = MarketplaceLeadDto.builder()
                .id(savedLead.getId())
                .clientName("Rohan Verma")
                .leadStatus(LeadStatus.NEW)
                .build();
        when(mapper.toLeadDto(savedLead)).thenReturn(dto);

        MarketplaceLeadDto result = marketplaceService.submitPublicLead(req);

        assertNotNull(result);
        assertEquals("Rohan Verma", result.getClientName());
        assertEquals(LeadStatus.NEW, result.getLeadStatus());
        verify(leadRepository).save(any(MarketplaceLeadEntity.class));
    }

    @Test
    @DisplayName("Should seamlessly convert Marketplace Lead into Active CRM Client and create Onboarding Task")
    void testConvertLeadToClient() {
        UUID leadId = UUID.randomUUID();
        MarketplaceLeadEntity lead = MarketplaceLeadEntity.builder()
                .organizationId(organizationId)
                .marketplaceProfileId(profileId)
                .clientName("Vikram Singhania")
                .clientEmail("vikram@scaletech.com")
                .clientPhone("9822114455")
                .pan("AABCS1234K")
                .serviceCategory("GST")
                .requirementDescription("Monthly return filing")
                .leadStatus(LeadStatus.NEW)
                .build();
        lead.setId(leadId);

        when(leadRepository.findByIdAndOrganizationId(leadId, organizationId)).thenReturn(Optional.of(lead));
        when(clientRepository.findByOrganizationIdAndPan(organizationId, "AABCS1234K")).thenReturn(Optional.empty());

        ClientEntity savedClient = ClientEntity.builder()
                .displayName("Vikram Singhania")
                .pan("AABCS1234K")
                .email("vikram@scaletech.com")
                .phone("9822114455")
                .status(ClientEntity.ClientStatus.ACTIVE)
                .build();
        savedClient.setId(UUID.randomUUID());
        savedClient.setOrganizationId(organizationId);

        when(clientRepository.save(any(ClientEntity.class))).thenReturn(savedClient);
        when(leadRepository.save(any(MarketplaceLeadEntity.class))).thenReturn(lead);
        when(profileRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(sampleProfile));

        MarketplaceLeadDto dto = MarketplaceLeadDto.builder()
                .id(leadId)
                .clientName("Vikram Singhania")
                .leadStatus(LeadStatus.CONVERTED)
                .convertedClientId(savedClient.getId())
                .build();
        when(mapper.toLeadDto(lead)).thenReturn(dto);

        ConvertLeadToClientRequest convertReq = ConvertLeadToClientRequest.builder()
                .createOnboardingTask(true)
                .build();

        MarketplaceLeadDto result = marketplaceService.convertLeadToClient(leadId, convertReq);

        assertNotNull(result);
        assertEquals(LeadStatus.CONVERTED, result.getLeadStatus());
        verify(clientRepository).save(any(ClientEntity.class));
        verify(taskRepository).save(any(TaskEntity.class));
    }

    @Test
    @DisplayName("Practice Profile Foundation: calculate profile completeness score and missing fields")
    void testGetMyPracticeProfile_CalculatesCompleteness() {
        when(profileRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(sampleProfile));
        when(serviceRepository.findByMarketplaceProfileIdAndIsActiveTrue(sampleProfile.getId())).thenReturn(List.of());
        when(reviewRepository.findByMarketplaceProfileIdAndStatusOrderByCreatedAtDesc(eq(sampleProfile.getId()), any()))
                .thenReturn(List.of());
        when(completenessCalculator.calculate(any(), any())).thenReturn(ProfileCompletenessDto.builder()
                .percentage(85)
                .completedItems(List.of("Practice name", "Phone", "Email"))
                .missingItems(List.of("Website"))
                .build());

        PublicMarketplaceProfileDto mockDto = PublicMarketplaceProfileDto.builder()
                .id(sampleProfile.getId())
                .displayName(sampleProfile.getDisplayName())
                .build();
        when(mapper.toProfileDto(sampleProfile)).thenReturn(mockDto);

        PublicMarketplaceProfileDto result = marketplaceService.getMyPracticeProfile();

        assertNotNull(result);
        assertNotNull(result.getCompletenessScore());
        assertEquals(85, result.getCompletenessScore());
        assertEquals(1, result.getMissingCompletenessFields().size());
        assertNotNull(result.getCompleteness());
        assertEquals(3, result.getCompleteness().getCompletedItems().size());
    }

    @Test
    @DisplayName("Profile Completeness: getMyProfileCompleteness delegates to calculator")
    void testGetMyProfileCompleteness() {
        when(profileRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(sampleProfile));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());
        when(completenessCalculator.calculate(any(), any())).thenReturn(ProfileCompletenessDto.builder()
                .percentage(70)
                .completedItems(List.of("Practice name", "Description", "Phone"))
                .missingItems(List.of("Website"))
                .build());

        ProfileCompletenessDto result = marketplaceService.getMyProfileCompleteness();

        assertNotNull(result);
        assertEquals(70, result.getPercentage());
        assertEquals(3, result.getCompletedItems().size());
        assertEquals(1, result.getMissingItems().size());
        assertTrue(result.getCompletedItems().contains("Practice name"));
        assertTrue(result.getMissingItems().contains("Website"));
    }

    @Test
    @DisplayName("Practice Profile Foundation: update profile, save as draft, and update slug")
    void testUpdateMyPracticeProfile_UpdatesProfileAndSlug() {
        when(profileRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(sampleProfile));
        when(profileRepository.existsBySlug("custom-apex-mumbai")).thenReturn(false);
        when(profileRepository.save(any(MarketplaceProfileEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(completenessCalculator.calculate(any(), any())).thenReturn(ProfileCompletenessDto.builder()
                .percentage(90)
                .completedItems(List.of("Practice name"))
                .missingItems(List.of())
                .build());

        PublicMarketplaceProfileDto mockDto = PublicMarketplaceProfileDto.builder()
                .id(sampleProfile.getId())
                .displayName("Updated Firm Name")
                .slug("custom-apex-mumbai")
                .isPublished(false)
                .build();
        when(mapper.toProfileDto(any(MarketplaceProfileEntity.class))).thenReturn(mockDto);
        when(serviceRepository.findByMarketplaceProfileIdAndIsActiveTrue(any())).thenReturn(List.of());
        when(reviewRepository.findByMarketplaceProfileIdAndStatusOrderByCreatedAtDesc(any(), any())).thenReturn(List.of());

        UpdateMarketplaceProfileRequest request = UpdateMarketplaceProfileRequest.builder()
                .displayName("Updated Firm Name")
                .slug("custom-apex-mumbai")
                .isPublished(false) // Save as Draft
                .city("Mumbai")
                .state("Maharashtra")
                .hourlyRate(new BigDecimal("3000.00"))
                .build();

        PublicMarketplaceProfileDto result = marketplaceService.updateMyPracticeProfile(request);

        assertNotNull(result);
        assertEquals("Updated Firm Name", sampleProfile.getDisplayName());
        assertEquals("custom-apex-mumbai", sampleProfile.getSlug());
        assertFalse(sampleProfile.getIsPublished());
        verify(profileRepository).save(sampleProfile);
        verify(auditService).logEvent(eq("PRACTICE_MARKETPLACE_PROFILE_UPDATED"), eq("MARKETPLACE_PROFILE"), any(), any(), any());
    }

    @Test
    @DisplayName("Practice Profile Foundation: duplicate slug is rejected")
    void testUpdateMyPracticeProfile_RejectsDuplicateSlug() {
        when(profileRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(sampleProfile));
        when(profileRepository.existsBySlug("taken-slug")).thenReturn(true);

        UpdateMarketplaceProfileRequest request = UpdateMarketplaceProfileRequest.builder()
                .displayName("Updated Firm Name")
                .slug("taken-slug")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                marketplaceService.updateMyPracticeProfile(request)
        );

        assertTrue(ex.getMessage().contains("already taken"));
    }

    @Test
    @DisplayName("Practice Profile Foundation: generate unique SEO public slug")
    void testGenerateUniqueSlug() {
        when(profileRepository.existsBySlug("apex-tax-mumbai")).thenReturn(true);
        when(profileRepository.existsBySlug("apex-tax-mumbai-1")).thenReturn(false);

        String slug = marketplaceService.generateUniqueSlug("Apex Tax", "Mumbai");

        assertEquals("apex-tax-mumbai-1", slug);
    }

    @Test
    @DisplayName("Status Design: publishing with complete information succeeds")
    void testPublishing_WhenAllRequiredFieldsPresent_Succeeds() {
        when(profileRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(sampleProfile));
        when(profileRepository.save(any(MarketplaceProfileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicMarketplaceProfileDto mockDto = PublicMarketplaceProfileDto.builder()
                .id(sampleProfile.getId())
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .isPublished(true)
                .build();
        when(mapper.toProfileDto(any())).thenReturn(mockDto);
        when(serviceRepository.findByMarketplaceProfileIdAndIsActiveTrue(any())).thenReturn(List.of());
        when(reviewRepository.findByMarketplaceProfileIdAndStatusOrderByCreatedAtDesc(any(), any())).thenReturn(List.of());

        UpdateMarketplaceProfileRequest request = UpdateMarketplaceProfileRequest.builder()
                .displayName("Apex Corporate & Tax Advisors")
                .city("Mumbai")
                .state("Maharashtra")
                .phone("9820011223")
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .build();

        PublicMarketplaceProfileDto result = marketplaceService.updateMyPracticeProfile(request);

        assertNotNull(result);
        assertEquals(VisibilityStatus.PUBLIC, sampleProfile.getVisibilityStatus());
        assertTrue(sampleProfile.getIsPublished());
    }

    @Test
    @DisplayName("Status Design: publishing when minimum required information is missing throws BusinessValidationException")
    void testPublishing_WhenMinimumFieldsMissing_ThrowsBusinessValidationException() {
        MarketplaceProfileEntity incompleteProfile = MarketplaceProfileEntity.builder()
                .organizationId(organizationId)
                .slug("incomplete-slug")
                .displayName("Incomplete Firm")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .city(null) // Missing City
                .state(null) // Missing State
                .phone(null)
                .email(null) // Missing Contact
                .visibilityStatus(VisibilityStatus.PRIVATE)
                .isPublished(false)
                .build();

        when(profileRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(incompleteProfile));

        UpdateMarketplaceProfileRequest request = UpdateMarketplaceProfileRequest.builder()
                .displayName("Incomplete Firm")
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .build();

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                marketplaceService.updateMyPracticeProfile(request)
        );

        assertTrue(ex.getMessage().contains("Cannot publish profile to Marketplace"));
        assertTrue(ex.getMessage().contains("City"));
    }

    @Test
    @DisplayName("Status Design: orthogonal status combinations are fully representable")
    void testOrthogonalStatusCombinations() {
        // 1. VERIFIED + PRIVATE
        MarketplaceProfileEntity p1 = MarketplaceProfileEntity.builder()
                .visibilityStatus(VisibilityStatus.PRIVATE)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        assertEquals(VisibilityStatus.PRIVATE, p1.getVisibilityStatus());
        assertEquals(VerificationStatus.VERIFIED, p1.getVerificationStatus());
        assertFalse(p1.getIsPublished());

        // 2. PUBLIC + PENDING
        MarketplaceProfileEntity p2 = MarketplaceProfileEntity.builder()
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .verificationStatus(VerificationStatus.PENDING)
                .build();
        assertEquals(VisibilityStatus.PUBLIC, p2.getVisibilityStatus());
        assertEquals(VerificationStatus.PENDING, p2.getVerificationStatus());

        // 3. PUBLIC + VERIFIED
        MarketplaceProfileEntity p3 = MarketplaceProfileEntity.builder()
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        assertEquals(VisibilityStatus.PUBLIC, p3.getVisibilityStatus());
        assertEquals(VerificationStatus.VERIFIED, p3.getVerificationStatus());

        // 4. PUBLIC + NOT_SUBMITTED
        MarketplaceProfileEntity p4 = MarketplaceProfileEntity.builder()
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .verificationStatus(VerificationStatus.NOT_SUBMITTED)
                .build();
        assertEquals(VisibilityStatus.PUBLIC, p4.getVisibilityStatus());
        assertEquals(VerificationStatus.NOT_SUBMITTED, p4.getVerificationStatus());

        // 5. SUSPENDED + VERIFIED
        MarketplaceProfileEntity p5 = MarketplaceProfileEntity.builder()
                .visibilityStatus(VisibilityStatus.SUSPENDED)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        assertEquals(VisibilityStatus.SUSPENDED, p5.getVisibilityStatus());
        assertEquals(VerificationStatus.VERIFIED, p5.getVerificationStatus());
        assertFalse(p5.getIsPublished());
    }
}
