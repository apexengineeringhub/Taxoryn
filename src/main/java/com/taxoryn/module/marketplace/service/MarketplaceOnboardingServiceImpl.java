package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
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
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.task.entity.TaskEntity;
import com.taxoryn.module.task.repository.TaskRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceOnboardingServiceImpl implements MarketplaceOnboardingService {

    private final MarketplaceProposalRepository proposalRepository;
    private final MarketplaceOnboardingRepository onboardingRepository;
    private final MarketplaceOnboardingDocumentRepository onboardingDocumentRepository;
    private final MarketplaceLeadRepository leadRepository;
    private final MarketplaceProfileRepository profileRepository;
    private final MarketplaceServiceRepository serviceRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MarketplaceMapper mapper;
    private final AuditService auditService;

    // =========================================================================
    // 1. Practice Operations (Authenticated Practice Portal)
    // =========================================================================

    @Override
    @Transactional
    public MarketplaceProposalDto sendProposal(CreateProposalRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        MarketplaceLeadEntity lead = leadRepository.findByIdAndOrganizationId(request.getLeadId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Lead", "id", request.getLeadId()));

        MarketplaceProfileEntity profile = profileRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "organizationId", organizationId));

        String token = "prop_" + UUID.randomUUID().toString().replace("-", "");

        MarketplaceProposalEntity proposal = MarketplaceProposalEntity.builder()
                .organizationId(organizationId)
                .marketplaceProfileId(profile.getId())
                .leadId(lead.getId())
                .serviceId(request.getServiceId())
                .proposalTitle(request.getProposalTitle())
                .scopeOfWork(request.getScopeOfWork())
                .deliverables(request.getDeliverables())
                .feeAmount(request.getFeeAmount())
                .pricingType(request.getPricingType())
                .estimatedTimelineDays(request.getEstimatedTimelineDays() != null ? request.getEstimatedTimelineDays() : 7)
                .proposalStatus(ProposalStatus.SENT)
                .accessToken(token)
                .validUntil(request.getValidUntil() != null ? request.getValidUntil() : LocalDate.now().plusDays(14))
                .build();

        proposal = proposalRepository.save(proposal);

        lead.setLeadStatus(LeadStatus.PROPOSAL_SENT);
        lead.setPractitionerNotes("Formal proposal sent: " + proposal.getProposalTitle());
        leadRepository.save(lead);

        auditService.logEvent("PROPOSAL_SENT", "MarketplaceProposal", proposal.getId().toString(), null,
                "Sent engagement proposal to lead: " + lead.getClientName());

        log.info("Engagement proposal {} created and dispatched for lead {}", proposal.getId(), lead.getId());
        return enrichProposalDto(proposal, lead, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MarketplaceProposalDto> getPracticeProposals(Pageable pageable) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Page<MarketplaceProposalEntity> page = proposalRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable);
        return PagedResponse.of(page, p -> {
            MarketplaceLeadEntity lead = leadRepository.findById(p.getLeadId()).orElse(null);
            MarketplaceProfileEntity prof = profileRepository.findById(p.getMarketplaceProfileId()).orElse(null);
            return enrichProposalDto(p, lead, prof);
        });
    }

    @Override
    @Transactional
    public MarketplaceOnboardingDto initiateOnboarding(InitiateOnboardingRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        MarketplaceLeadEntity lead = leadRepository.findByIdAndOrganizationId(request.getLeadId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Lead", "id", request.getLeadId()));

        MarketplaceProfileEntity profile = profileRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "organizationId", organizationId));

        // Check if an active onboarding already exists for this lead
        Optional<MarketplaceOnboardingEntity> existing = onboardingRepository.findByLeadId(lead.getId());
        if (existing.isPresent()) {
            return enrichOnboardingDto(existing.get(), profile);
        }

        String onbToken = "onb_" + UUID.randomUUID().toString().replace("-", "");

        MarketplaceOnboardingEntity onboarding = MarketplaceOnboardingEntity.builder()
                .organizationId(organizationId)
                .marketplaceProfileId(profile.getId())
                .leadId(lead.getId())
                .proposalId(request.getProposalId())
                .accessToken(onbToken)
                .clientName(lead.getClientName())
                .clientEmail(lead.getClientEmail())
                .clientPhone(lead.getClientPhone())
                .entityType(request.getEntityType() != null ? request.getEntityType() : ClientEntity.ClientType.INDIVIDUAL)
                .onboardingStatus(OnboardingStatus.INITIATED)
                .assignedEmployeeId(request.getAssignedEmployeeId())
                .build();

        onboarding = onboardingRepository.save(onboarding);

        // Auto-seed default KYC checklist items
        seedDefaultKycDocuments(onboarding.getId(), onboarding.getEntityType());

        lead.setLeadStatus(LeadStatus.CONTACTED);
        leadRepository.save(lead);

        auditService.logEvent("ONBOARDING_INITIATED", "MarketplaceOnboarding", onboarding.getId().toString(), null,
                "Initiated client onboarding checklist for " + lead.getClientName());

        log.info("Onboarding pipeline {} initialized for lead {}", onboarding.getId(), lead.getId());
        return enrichOnboardingDto(onboarding, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MarketplaceOnboardingDto> getPracticeOnboardings(OnboardingStatus status, String search, Pageable pageable) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        Specification<MarketplaceOnboardingEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (status != null) {
                predicates.add(cb.equal(root.get("onboardingStatus"), status));
            }
            if (StringUtils.hasText(search)) {
                String s = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("clientName")), s);
                Predicate emailMatch = cb.like(cb.lower(root.get("clientEmail")), s);
                Predicate panMatch = cb.like(cb.lower(root.get("pan")), s);
                predicates.add(cb.or(nameMatch, emailMatch, panMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<MarketplaceOnboardingEntity> page = onboardingRepository.findAll(spec, pageable);
        return PagedResponse.of(page, o -> {
            MarketplaceProfileEntity prof = profileRepository.findById(o.getMarketplaceProfileId()).orElse(null);
            return enrichOnboardingDto(o, prof);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public MarketplaceOnboardingDto getPracticeOnboardingById(UUID onboardingId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceOnboardingEntity onboarding = onboardingRepository.findByIdAndOrganizationId(onboardingId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Onboarding", "id", onboardingId));

        MarketplaceProfileEntity profile = profileRepository.findById(onboarding.getMarketplaceProfileId()).orElse(null);
        return enrichOnboardingDto(onboarding, profile);
    }

    @Override
    @Transactional
    public OnboardingDocumentDto verifyDocument(UUID onboardingId, UUID documentId, VerifyOnboardingDocumentRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceOnboardingEntity onboarding = onboardingRepository.findByIdAndOrganizationId(onboardingId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Onboarding", "id", onboardingId));

        MarketplaceOnboardingDocumentEntity doc = onboardingDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding Document", "id", documentId));

        if (!doc.getOnboardingId().equals(onboardingId)) {
            throw new IllegalArgumentException("Document does not belong to specified onboarding record");
        }

        doc.setVerificationStatus(request.getVerificationStatus());
        doc.setRejectionReason(request.getRejectionReason());
        doc.setVerifiedAt(Instant.now());
        doc.setVerifiedBy(SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId().toString() : "PRACTICE_ADMIN");
        doc = onboardingDocumentRepository.save(doc);

        // Check overall onboarding status progression
        List<MarketplaceOnboardingDocumentEntity> allDocs = onboardingDocumentRepository.findByOnboardingIdOrderByCreatedAtAsc(onboardingId);
        boolean allRequiredVerified = allDocs.stream()
                .filter(MarketplaceOnboardingDocumentEntity::getIsRequired)
                .allMatch(d -> d.getVerificationStatus() == VerificationStatus.VERIFIED);

        if (allRequiredVerified && onboarding.getEngagementLetterSigned() && onboarding.getOnboardingStatus() != OnboardingStatus.APPROVED) {
            onboarding.setOnboardingStatus(OnboardingStatus.UNDER_REVIEW);
            onboardingRepository.save(onboarding);
        }

        auditService.logEvent("DOCUMENT_VERIFIED", "MarketplaceOnboardingDocument", doc.getId().toString(), null,
                "Document " + doc.getDocumentName() + " verification status: " + doc.getVerificationStatus());

        return mapper.toOnboardingDocumentDto(doc);
    }

    @Override
    @Transactional
    public MarketplaceOnboardingDto approveAndPromoteToClient(UUID onboardingId, ApproveAndPromoteClientRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceOnboardingEntity onboarding = onboardingRepository.findByIdAndOrganizationId(onboardingId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Onboarding", "id", onboardingId));

        if (onboarding.getPromotedClientId() != null) {
            throw new IllegalStateException("This onboarding record has already been promoted to Client Master!");
        }

        // =========================================================================
        // ARCHITECTURAL GUARANTEE:
        // ONLY NOW is a record created in the practice's `clients` master table.
        // =========================================================================
        UUID assignedEmpId = request.getAssignedEmployeeId() != null
                ? request.getAssignedEmployeeId()
                : onboarding.getAssignedEmployeeId();

        ClientEntity client = ClientEntity.builder()
                .clientType(onboarding.getEntityType() != null ? onboarding.getEntityType() : ClientEntity.ClientType.INDIVIDUAL)
                .displayName(onboarding.getClientName())
                .legalName(StringUtils.hasText(onboarding.getLegalName()) ? onboarding.getLegalName() : onboarding.getClientName())
                .email(onboarding.getClientEmail())
                .phone(onboarding.getClientPhone())
                .pan(onboarding.getPan())
                .gstin(onboarding.getGstin())
                .tan(onboarding.getTan())
                .status(ClientStatus.ACTIVE)
                .assignedEmployeeId(assignedEmpId)
                .addressLine1(onboarding.getAddressLine1())
                .addressLine2(onboarding.getAddressLine2())
                .city(onboarding.getCity())
                .state(onboarding.getState())
                .pincode(onboarding.getPincode())
                .build();

        final ClientEntity savedClient = clientRepository.save(client);

        // Update onboarding record with promoted pointer
        onboarding.setPromotedClientId(savedClient.getId());
        onboarding.setOnboardingStatus(OnboardingStatus.APPROVED);
        onboarding.setCompletedAt(Instant.now());
        if (StringUtils.hasText(request.getReviewerNotes())) {
            onboarding.setReviewerNotes(request.getReviewerNotes());
        }

        // Auto-provision initial onboarding compliance task if requested
        if (Boolean.TRUE.equals(request.getCreateOnboardingTask())) {
            TaskEntity task = TaskEntity.builder()
                    .title("Complete Client Onboarding: " + savedClient.getDisplayName())
                    .description("Initiate statutory compliance setup, verify bank details, and review engagement deliverables.")
                    .clientId(savedClient.getId())
                    .assignedTo(assignedEmpId)
                    .taskCategory(TaskEntity.TaskCategory.OTHER)
                    .priority(TaskEntity.TaskPriority.HIGH)
                    .status(TaskEntity.TaskStatus.TODO)
                    .dueDate(LocalDate.now().plusDays(3))
                    .build();
            taskRepository.save(task);
        }

        // Auto-provision Client Portal User account if requested
        if (Boolean.TRUE.equals(request.getProvisionClientPortalUser())) {
            UUID portalUserId = provisionClientPortalUser(organizationId, savedClient, request.getInitialPortalPassword());
            onboarding.setPortalUserId(portalUserId);
        }

        onboardingRepository.save(onboarding);

        // Update Lead status to CONVERTED
        leadRepository.findById(onboarding.getLeadId()).ifPresent(l -> {
            l.setLeadStatus(LeadStatus.CONVERTED);
            l.setConvertedClientId(savedClient.getId());
            l.setPractitionerNotes("Successfully onboarded and promoted to Client Master.");
            leadRepository.save(l);
        });

        // Update Marketplace Profile stats
        profileRepository.findById(onboarding.getMarketplaceProfileId()).ifPresent(prof -> {
            prof.setTotalClientsServed((prof.getTotalClientsServed() != null ? prof.getTotalClientsServed() : 0) + 1);
            profileRepository.save(prof);
        });

        auditService.logEvent("CLIENT_PROMOTED", "Client", savedClient.getId().toString(), null,
                "Promoted marketplace onboarding " + onboarding.getId() + " to Client Master: " + savedClient.getDisplayName());

        log.info("Onboarding {} successfully approved & promoted to Client Master with ID {}", onboarding.getId(), savedClient.getId());
        MarketplaceProfileEntity profile = profileRepository.findById(onboarding.getMarketplaceProfileId()).orElse(null);
        return enrichOnboardingDto(onboarding, profile);
    }

    // =========================================================================
    // 2. Public Self-Serve Customer Operations (Secured by accessToken)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public MarketplaceProposalDto getPublicProposalByToken(String token) {
        MarketplaceProposalEntity proposal = proposalRepository.findByAccessToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Proposal", "token", token));

        MarketplaceLeadEntity lead = leadRepository.findById(proposal.getLeadId()).orElse(null);
        MarketplaceProfileEntity prof = profileRepository.findById(proposal.getMarketplaceProfileId()).orElse(null);
        return enrichProposalDto(proposal, lead, prof);
    }

    @Override
    @Transactional
    public MarketplaceProposalDto acceptOrRejectProposal(String token, AcceptProposalRequest request) {
        MarketplaceProposalEntity proposal = proposalRepository.findByAccessToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Proposal", "token", token));

        UUID propLeadId = proposal.getLeadId();
        MarketplaceLeadEntity lead = leadRepository.findById(propLeadId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Lead", "id", propLeadId));

        UUID propProfileId = proposal.getMarketplaceProfileId();
        MarketplaceProfileEntity profile = profileRepository.findById(propProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "id", propProfileId));

        if (Boolean.TRUE.equals(request.getIsAccepted())) {
            proposal.setProposalStatus(ProposalStatus.ACCEPTED);
            proposal.setAcceptedAt(Instant.now());
            lead.setLeadStatus(LeadStatus.ACCEPTED);
            lead.setPractitionerNotes("Client accepted proposal: " + proposal.getProposalTitle());

            // Automatically initialize the Onboarding record
            Optional<MarketplaceOnboardingEntity> existingOnb = onboardingRepository.findByLeadId(lead.getId());
            if (existingOnb.isEmpty()) {
                String onbToken = "onb_" + UUID.randomUUID().toString().replace("-", "");
                MarketplaceOnboardingEntity onb = MarketplaceOnboardingEntity.builder()
                        .organizationId(proposal.getOrganizationId())
                        .marketplaceProfileId(proposal.getMarketplaceProfileId())
                        .leadId(lead.getId())
                        .proposalId(proposal.getId())
                        .accessToken(onbToken)
                        .clientName(lead.getClientName())
                        .clientEmail(lead.getClientEmail())
                        .clientPhone(lead.getClientPhone())
                        .entityType(ClientEntity.ClientType.INDIVIDUAL)
                        .onboardingStatus(OnboardingStatus.INITIATED)
                        .feeAgreementAgreed(true)
                        .build();

                onb = onboardingRepository.save(onb);
                seedDefaultKycDocuments(onb.getId(), onb.getEntityType());
            }
        } else {
            proposal.setProposalStatus(ProposalStatus.REJECTED);
            proposal.setRejectionReason(request.getRejectionReason());
            lead.setLeadStatus(LeadStatus.CLOSED_LOST);
            lead.setPractitionerNotes("Client rejected proposal. Reason: " + request.getRejectionReason());
        }

        proposal = proposalRepository.save(proposal);
        leadRepository.save(lead);

        auditService.logEvent("PROPOSAL_RESPONDED", "MarketplaceProposal", proposal.getId().toString(), null,
                "Client response to proposal: " + proposal.getProposalStatus());

        return enrichProposalDto(proposal, lead, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public MarketplaceOnboardingDto getPublicOnboardingByToken(String token) {
        MarketplaceOnboardingEntity onboarding = onboardingRepository.findByAccessToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Onboarding", "token", token));

        MarketplaceProfileEntity profile = profileRepository.findById(onboarding.getMarketplaceProfileId()).orElse(null);
        return enrichOnboardingDto(onboarding, profile);
    }

    @Override
    @Transactional
    public MarketplaceOnboardingDto updatePublicOnboardingDetails(String token, UpdateOnboardingDetailsRequest request) {
        MarketplaceOnboardingEntity onboarding = onboardingRepository.findByAccessToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Onboarding", "token", token));

        if (StringUtils.hasText(request.getClientName())) onboarding.setClientName(request.getClientName());
        if (StringUtils.hasText(request.getLegalName())) onboarding.setLegalName(request.getLegalName());
        if (request.getEntityType() != null) onboarding.setEntityType(request.getEntityType());
        if (StringUtils.hasText(request.getPan())) onboarding.setPan(request.getPan().toUpperCase());
        if (StringUtils.hasText(request.getGstin())) onboarding.setGstin(request.getGstin().toUpperCase());
        if (StringUtils.hasText(request.getTan())) onboarding.setTan(request.getTan().toUpperCase());
        if (StringUtils.hasText(request.getAddressLine1())) onboarding.setAddressLine1(request.getAddressLine1());
        if (StringUtils.hasText(request.getAddressLine2())) onboarding.setAddressLine2(request.getAddressLine2());
        if (StringUtils.hasText(request.getCity())) onboarding.setCity(request.getCity());
        if (StringUtils.hasText(request.getState())) onboarding.setState(request.getState());
        if (StringUtils.hasText(request.getPincode())) onboarding.setPincode(request.getPincode());

        if (onboarding.getOnboardingStatus() == OnboardingStatus.INITIATED) {
            onboarding.setOnboardingStatus(OnboardingStatus.DOCUMENTS_PENDING);
        }

        onboarding = onboardingRepository.save(onboarding);

        MarketplaceProfileEntity profile = profileRepository.findById(onboarding.getMarketplaceProfileId()).orElse(null);
        return enrichOnboardingDto(onboarding, profile);
    }

    @Override
    @Transactional
    public MarketplaceOnboardingDto signPublicEngagementLetter(String token, SignEngagementLetterRequest request) {
        MarketplaceOnboardingEntity onboarding = onboardingRepository.findByAccessToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Onboarding", "token", token));

        if (Boolean.TRUE.equals(request.getSignedConsent())) {
            onboarding.setEngagementLetterSigned(true);
            onboarding.setEngagementSignedAt(Instant.now());
        }
        if (Boolean.TRUE.equals(request.getAgreedToFees())) {
            onboarding.setFeeAgreementAgreed(true);
        }

        onboarding = onboardingRepository.save(onboarding);

        MarketplaceProfileEntity profile = profileRepository.findById(onboarding.getMarketplaceProfileId()).orElse(null);
        return enrichOnboardingDto(onboarding, profile);
    }

    @Override
    @Transactional
    public OnboardingDocumentDto uploadPublicOnboardingDocument(
            String token,
            DocumentType docType,
            String docName,
            String filePath,
            Long fileSizeBytes,
            String contentType
    ) {
        MarketplaceOnboardingEntity onboarding = onboardingRepository.findByAccessToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Onboarding", "token", token));

        // Find existing doc item of same type or create new
        List<MarketplaceOnboardingDocumentEntity> docs = onboardingDocumentRepository.findByOnboardingIdOrderByCreatedAtAsc(onboarding.getId());
        MarketplaceOnboardingDocumentEntity doc = docs.stream()
                .filter(d -> d.getDocumentType() == docType)
                .findFirst()
                .orElseGet(() -> MarketplaceOnboardingDocumentEntity.builder()
                        .onboardingId(onboarding.getId())
                        .documentType(docType)
                        .isRequired(true)
                        .build());

        doc.setDocumentName(docName);
        doc.setFilePath(filePath);
        doc.setFileSizeBytes(fileSizeBytes != null ? fileSizeBytes : 0L);
        doc.setContentType(contentType);
        doc.setVerificationStatus(VerificationStatus.PENDING);
        doc.setRejectionReason(null);
        doc = onboardingDocumentRepository.save(doc);

        if (onboarding.getOnboardingStatus() == OnboardingStatus.INITIATED || onboarding.getOnboardingStatus() == OnboardingStatus.DOCUMENTS_PENDING) {
            onboarding.setOnboardingStatus(OnboardingStatus.UNDER_REVIEW);
            onboardingRepository.save(onboarding);
        }

        return mapper.toOnboardingDocumentDto(doc);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void seedDefaultKycDocuments(UUID onboardingId, ClientEntity.ClientType entityType) {
        List<MarketplaceOnboardingDocumentEntity> defaultDocs = new ArrayList<>();

        defaultDocs.add(MarketplaceOnboardingDocumentEntity.builder()
                .onboardingId(onboardingId)
                .documentType(DocumentType.PAN_CARD)
                .documentName("Permanent Account Number (PAN) Card Copy")
                .filePath("")
                .isRequired(true)
                .verificationStatus(VerificationStatus.PENDING)
                .build());

        defaultDocs.add(MarketplaceOnboardingDocumentEntity.builder()
                .onboardingId(onboardingId)
                .documentType(DocumentType.ADDRESS_PROOF)
                .documentName("Address Proof (Electricity Bill / Bank Statement / Rent Agreement)")
                .filePath("")
                .isRequired(true)
                .verificationStatus(VerificationStatus.PENDING)
                .build());

        if (entityType != ClientEntity.ClientType.INDIVIDUAL) {
            defaultDocs.add(MarketplaceOnboardingDocumentEntity.builder()
                    .onboardingId(onboardingId)
                    .documentType(DocumentType.CERTIFICATE_OF_INCORPORATION)
                    .documentName("Certificate of Incorporation / Partnership Deed")
                    .filePath("")
                    .isRequired(true)
                    .verificationStatus(VerificationStatus.PENDING)
                    .build());

            defaultDocs.add(MarketplaceOnboardingDocumentEntity.builder()
                    .onboardingId(onboardingId)
                    .documentType(DocumentType.GST_CERTIFICATE)
                    .documentName("GST Registration Certificate (REG-06)")
                    .filePath("")
                    .isRequired(false)
                    .verificationStatus(VerificationStatus.PENDING)
                    .build());
        }

        onboardingDocumentRepository.saveAll(defaultDocs);
    }

    private UUID provisionClientPortalUser(UUID organizationId, ClientEntity client, String explicitPassword) {
        String email = client.getEmail();
        if (!StringUtils.hasText(email)) {
            return null;
        }

        Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(email);
        if (existingUser.isPresent()) {
            UserEntity user = existingUser.get();
            user.setClientId(client.getId());
            userRepository.save(user);
            return user.getId();
        }

        String rawPassword = StringUtils.hasText(explicitPassword) ? explicitPassword : "ClientPass@" + UUID.randomUUID().toString().substring(0, 6);
        String name = client.getDisplayName();
        String[] parts = name.split(" ", 2);
        String first = parts[0];
        String last = parts.length > 1 ? parts[1] : "";

        Set<RoleEntity> roles = new HashSet<>();
        roleRepository.findByCodeAndIsSystemRoleTrue("ROLE_CLIENT_USER").ifPresent(roles::add);

        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .firstName(first)
                .lastName(last)
                .phone(client.getPhone())
                .clientId(client.getId())
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .build();
        user.setOrganizationId(organizationId);

        user = userRepository.save(user);
        log.info("Provisioned Client Portal user account {} for client {}", user.getId(), client.getId());
        return user.getId();
    }

    private MarketplaceProposalDto enrichProposalDto(
            MarketplaceProposalEntity p,
            MarketplaceLeadEntity lead,
            MarketplaceProfileEntity profile
    ) {
        MarketplaceProposalDto dto = mapper.toProposalDto(p);
        if (profile != null) {
            dto.setPracticeDisplayName(profile.getDisplayName());
        }
        if (lead != null) {
            dto.setClientName(lead.getClientName());
            dto.setClientEmail(lead.getClientEmail());
            dto.setClientPhone(lead.getClientPhone());
        }
        if (p.getServiceId() != null) {
            serviceRepository.findById(p.getServiceId()).ifPresent(s -> dto.setServiceTitle(s.getTitle()));
        }
        return dto;
    }

    private MarketplaceOnboardingDto enrichOnboardingDto(
            MarketplaceOnboardingEntity o,
            MarketplaceProfileEntity profile
    ) {
        MarketplaceOnboardingDto dto = mapper.toOnboardingDto(o);
        if (profile != null) {
            dto.setPracticeDisplayName(profile.getDisplayName());
        }
        if (o.getProposalId() != null) {
            proposalRepository.findById(o.getProposalId()).ifPresent(p -> dto.setProposalTitle(p.getProposalTitle()));
        }
        if (o.getAssignedEmployeeId() != null) {
            employeeRepository.findById(o.getAssignedEmployeeId())
                    .ifPresent(e -> dto.setAssignedEmployeeName(e.getFirstName() + " " + (e.getLastName() != null ? e.getLastName() : "")));
        }

        List<MarketplaceOnboardingDocumentEntity> docs = onboardingDocumentRepository.findByOnboardingIdOrderByCreatedAtAsc(o.getId());
        dto.setDocuments(mapper.toOnboardingDocumentDtoList(docs));
        return dto;
    }
}
