package com.taxoryn.module.marketplace.service;

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
                .startingFee(new BigDecimal("999.00"))
                .averageRating(new BigDecimal("4.95"))
                .verificationStatus(VerificationStatus.VERIFIED)
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
}
