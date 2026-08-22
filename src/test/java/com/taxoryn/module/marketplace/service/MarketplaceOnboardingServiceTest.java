package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.security.SecurityUser;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.LeadStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingDocumentEntity.DocumentType;
import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingDocumentEntity.VerificationStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingEntity.OnboardingStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceProposalEntity.ProposalStatus;
import com.taxoryn.module.marketplace.mapper.MarketplaceMapper;
import com.taxoryn.module.marketplace.repository.*;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceOnboardingServiceTest {

    @Mock private MarketplaceProposalRepository proposalRepository;
    @Mock private MarketplaceOnboardingRepository onboardingRepository;
    @Mock private MarketplaceOnboardingDocumentRepository onboardingDocumentRepository;
    @Mock private MarketplaceLeadRepository leadRepository;
    @Mock private MarketplaceProfileRepository profileRepository;
    @Mock private MarketplaceServiceRepository serviceRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MarketplaceMapper mapper;
    @Mock private AuditService auditService;

    @InjectMocks
    private MarketplaceOnboardingServiceImpl onboardingService;

    private UUID organizationId;
    private UUID leadId;
    private UUID profileId;
    private MarketplaceLeadEntity sampleLead;
    private MarketplaceProfileEntity sampleProfile;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        leadId = UUID.randomUUID();
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
                .permissions(Set.of("CLIENT_VIEW", "CLIENT_MANAGE"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        sampleLead = MarketplaceLeadEntity.builder()
                .organizationId(organizationId)
                .marketplaceProfileId(profileId)
                .clientName("Rohit Sharma")
                .clientEmail("rohit@sharma-enterprises.in")
                .clientPhone("+91 98200 11223")
                .serviceCategory("GST")
                .leadStatus(LeadStatus.NEW)
                .build();
        sampleLead.setId(leadId);

        sampleProfile = MarketplaceProfileEntity.builder()
                .organizationId(organizationId)
                .displayName("Apex Tax Advisors")
                .build();
        sampleProfile.setId(profileId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Should create and send formal proposal without creating a Client in Client Master")
    void testSendProposal_DoesNotCreateClient() {
        CreateProposalRequest req = CreateProposalRequest.builder()
                .leadId(leadId)
                .proposalTitle("Comprehensive GST Filing Engagement")
                .scopeOfWork("Monthly filing, ITC reconciliation")
                .deliverables("GSTR-3B, GSTR-1, ITC Reports")
                .feeAmount(new BigDecimal("3500.00"))
                .build();

        when(leadRepository.findByIdAndOrganizationId(leadId, organizationId)).thenReturn(Optional.of(sampleLead));
        when(profileRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(sampleProfile));
        when(proposalRepository.save(any(MarketplaceProposalEntity.class))).thenAnswer(i -> {
            MarketplaceProposalEntity p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        MarketplaceProposalDto dto = MarketplaceProposalDto.builder()
                .proposalTitle(req.getProposalTitle())
                .proposalStatus(ProposalStatus.SENT)
                .build();
        when(mapper.toProposalDto(any(MarketplaceProposalEntity.class))).thenReturn(dto);

        MarketplaceProposalDto result = onboardingService.sendProposal(req);

        assertNotNull(result);
        assertEquals(ProposalStatus.SENT, result.getProposalStatus());
        assertEquals(LeadStatus.PROPOSAL_SENT, sampleLead.getLeadStatus());

        // CRITICAL CHECK: ClientRepository was NEVER called
        verify(clientRepository, never()).save(any(ClientEntity.class));
    }

    @Test
    @DisplayName("2. Should auto-initialize Onboarding on proposal acceptance without creating Client")
    void testAcceptProposal_InitializesOnboarding_DoesNotCreateClient() {
        String token = "prop_123456";
        MarketplaceProposalEntity proposal = MarketplaceProposalEntity.builder()
                .organizationId(organizationId)
                .marketplaceProfileId(profileId)
                .leadId(leadId)
                .proposalTitle("Tax Advisory")
                .scopeOfWork("Advisory")
                .accessToken(token)
                .proposalStatus(ProposalStatus.SENT)
                .build();
        proposal.setId(UUID.randomUUID());

        when(proposalRepository.findByAccessToken(token)).thenReturn(Optional.of(proposal));
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(sampleLead));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(sampleProfile));
        when(onboardingRepository.findByLeadId(leadId)).thenReturn(Optional.empty());
        when(onboardingRepository.save(any(MarketplaceOnboardingEntity.class))).thenAnswer(i -> {
            MarketplaceOnboardingEntity o = i.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(proposalRepository.save(any(MarketplaceProposalEntity.class))).thenReturn(proposal);

        MarketplaceProposalDto dto = MarketplaceProposalDto.builder()
                .proposalStatus(ProposalStatus.ACCEPTED)
                .build();
        when(mapper.toProposalDto(any(MarketplaceProposalEntity.class))).thenReturn(dto);

        AcceptProposalRequest acceptReq = AcceptProposalRequest.builder()
                .isAccepted(true)
                .build();

        MarketplaceProposalDto result = onboardingService.acceptOrRejectProposal(token, acceptReq);

        assertNotNull(result);
        assertEquals(ProposalStatus.ACCEPTED, proposal.getProposalStatus());
        assertEquals(LeadStatus.ACCEPTED, sampleLead.getLeadStatus());

        // CRITICAL CHECK: Onboarding documents seeded, but ClientRepository NEVER saved
        verify(onboardingDocumentRepository, times(1)).saveAll(any());
        verify(clientRepository, never()).save(any(ClientEntity.class));
    }

    @Test
    @DisplayName("Proposal Security: Cannot accept an expired proposal")
    void testAcceptProposal_WhenExpired_ThrowsBusinessValidationException() {
        String token = "prop_expired_999";
        MarketplaceProposalEntity proposal = MarketplaceProposalEntity.builder()
                .organizationId(organizationId)
                .marketplaceProfileId(profileId)
                .leadId(leadId)
                .proposalTitle("Tax Advisory")
                .accessToken(token)
                .proposalStatus(ProposalStatus.SENT)
                .validUntil(java.time.LocalDate.now().minusDays(2)) // Expired
                .build();
        proposal.setId(UUID.randomUUID());

        when(proposalRepository.findByAccessToken(token)).thenReturn(Optional.of(proposal));

        AcceptProposalRequest acceptReq = AcceptProposalRequest.builder()
                .isAccepted(true)
                .build();

        assertThrows(com.taxoryn.core.exception.BusinessValidationException.class, () ->
                onboardingService.acceptOrRejectProposal(token, acceptReq)
        );
    }

    @Test
    @DisplayName("Proposal Security: Cannot re-accept an already accepted proposal")
    void testAcceptProposal_WhenAlreadyAccepted_ThrowsBusinessValidationException() {
        String token = "prop_accepted_999";
        MarketplaceProposalEntity proposal = MarketplaceProposalEntity.builder()
                .organizationId(organizationId)
                .marketplaceProfileId(profileId)
                .leadId(leadId)
                .proposalTitle("Tax Advisory")
                .accessToken(token)
                .proposalStatus(ProposalStatus.ACCEPTED)
                .build();
        proposal.setId(UUID.randomUUID());

        when(proposalRepository.findByAccessToken(token)).thenReturn(Optional.of(proposal));

        AcceptProposalRequest acceptReq = AcceptProposalRequest.builder()
                .isAccepted(true)
                .build();

        assertThrows(com.taxoryn.core.exception.BusinessValidationException.class, () ->
                onboardingService.acceptOrRejectProposal(token, acceptReq)
        );
    }

    @Test
    @DisplayName("3. Should promote to Client Master ONLY when practice approves onboarding")
    void testApproveAndPromote_CreatesClientInClientMaster() {
        UUID onbId = UUID.randomUUID();
        MarketplaceOnboardingEntity onboarding = MarketplaceOnboardingEntity.builder()
                .organizationId(organizationId)
                .marketplaceProfileId(profileId)
                .leadId(leadId)
                .clientName("Rohit Sharma")
                .clientEmail("rohit@sharma-enterprises.in")
                .clientPhone("+91 98200 11223")
                .entityType(ClientEntity.ClientType.INDIVIDUAL)
                .pan("ABCDE1234F")
                .gstin("27ABCDE1234F1Z5")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .onboardingStatus(OnboardingStatus.UNDER_REVIEW)
                .engagementLetterSigned(true)
                .feeAgreementAgreed(true)
                .build();
        onboarding.setId(onbId);

        when(onboardingRepository.findByIdAndOrganizationId(onbId, organizationId)).thenReturn(Optional.of(onboarding));
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(i -> {
            ClientEntity c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(sampleLead));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(sampleProfile));

        MarketplaceOnboardingDto dto = MarketplaceOnboardingDto.builder()
                .onboardingStatus(OnboardingStatus.APPROVED)
                .clientName("Rohit Sharma")
                .build();
        when(mapper.toOnboardingDto(any(MarketplaceOnboardingEntity.class))).thenReturn(dto);

        ApproveAndPromoteClientRequest promoteReq = ApproveAndPromoteClientRequest.builder()
                .createOnboardingTask(true)
                .provisionClientPortalUser(false)
                .reviewerNotes("All KYC verified")
                .build();

        MarketplaceOnboardingDto result = onboardingService.approveAndPromoteToClient(onbId, promoteReq);

        assertNotNull(result);
        assertEquals(OnboardingStatus.APPROVED, onboarding.getOnboardingStatus());
        assertNotNull(onboarding.getPromotedClientId());
        assertEquals(LeadStatus.CONVERTED, sampleLead.getLeadStatus());

        // CRITICAL CHECK: Client master record was created now
        verify(clientRepository, times(1)).save(any(ClientEntity.class));
        verify(taskRepository, times(1)).save(any());
    }
}
