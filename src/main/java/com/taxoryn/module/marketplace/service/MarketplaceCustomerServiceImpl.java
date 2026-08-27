package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.exception.AppException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ErrorCode;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity.CustomerProfileStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity.CustomerType;
import com.taxoryn.module.marketplace.mapper.CustomerTaxRequirementMapper;
import com.taxoryn.module.marketplace.mapper.MarketplaceMapper;
import com.taxoryn.module.marketplace.repository.*;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceCustomerServiceImpl implements MarketplaceCustomerService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MarketplaceCustomerProfileRepository customerProfileRepository;
    private final MarketplaceLeadRepository leadRepository;
    private final MarketplaceConsultationRepository consultationRepository;
    private final MarketplaceProposalRepository proposalRepository;
    private final MarketplaceReviewRepository reviewRepository;
    private final MarketplaceEnquiryMessageRepository enquiryMessageRepository;
    private final com.taxoryn.module.employee.repository.EmployeeRepository employeeRepository;
    private final MarketplaceProfileRepository practiceProfileRepository;
    private final CustomerTaxRequirementRepository requirementRepository;
    private final TaxServiceRepository taxServiceRepository;
    private final com.taxoryn.module.notification.service.NotificationService notificationService;
    private final CustomerTaxRequirementMapper requirementMapper;
    private final MarketplaceMapper mapper;
    private final CustomerProfileCompletenessCalculator completenessCalculator;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    @Value("${taxoryn.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Override
    @Transactional
    public CustomerAuthResponseDto registerCustomer(RegisterCustomerRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent() ||
            customerProfileRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("User", "email", normalizedEmail);
        }

        RoleEntity customerRole = roleRepository.findByCodeAndIsSystemRoleTrue("MARKETPLACE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("MARKETPLACE_CUSTOMER")
                        .name("Marketplace Customer")
                        .description("Individual or Business accessing tax marketplace services")
                        .isSystemRole(true)
                        .build()));

        UserEntity user = UserEntity.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(StringUtils.hasText(request.getLastName()) ? request.getLastName().trim() : null)
                .phone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null)
                .status(UserStatus.ACTIVE)
                .organizationId(null)
                .roles(new HashSet<>(Set.of(customerRole)))
                .build();

        UserEntity savedUser = userRepository.save(user);

        String displayName = StringUtils.hasText(savedUser.getLastName())
                ? savedUser.getFirstName() + " " + savedUser.getLastName()
                : savedUser.getFirstName();

        MarketplaceCustomerProfileEntity profile = MarketplaceCustomerProfileEntity.builder()
                .userId(savedUser.getId())
                .customerType(request.getCustomerType() != null ? request.getCustomerType() : CustomerType.INDIVIDUAL)
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .displayName(displayName)
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .preferredLanguage(StringUtils.hasText(request.getPreferredLanguage()) ? request.getPreferredLanguage() : "English")
                .businessName(request.getBusinessName())
                .status(CustomerProfileStatus.ACTIVE)
                .build();

        MarketplaceCustomerProfileEntity savedProfile = customerProfileRepository.save(profile);

        String profileIdStr = savedProfile.getId() != null ? savedProfile.getId().toString() : savedUser.getId().toString();
        auditService.logEvent("CUSTOMER_ACCOUNT_CREATED", "USER", savedUser.getId().toString(), null,
                "Registered new marketplace customer: " + savedUser.getEmail());
        auditService.logEvent("CUSTOMER_PROFILE_CREATED", "MARKETPLACE_CUSTOMER_PROFILE", profileIdStr, null,
                "Created customer profile for user: " + savedUser.getId());

        Set<String> roleCodes = Set.of("MARKETPLACE_CUSTOMER");
        Set<String> permissionCodes = (customerRole.getPermissions() != null && !customerRole.getPermissions().isEmpty())
                ? customerRole.getPermissions().stream()
                        .map(PermissionEntity::getCode)
                        .collect(Collectors.toSet())
                : Set.of("MARKETPLACE_CUSTOMER_ACCESS");

        String accessToken = jwtTokenProvider.generateAccessToken(
                savedUser.getId(),
                null,
                null,
                savedUser.getEmail(),
                roleCodes,
                permissionCodes
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(
                savedUser.getId(),
                null,
                null,
                savedUser.getEmail()
        );

        CustomerProfileDto profileDto = mapper.toCustomerProfileDto(savedProfile);
        profileDto.setProfileCompleteness(completenessCalculator.calculate(savedProfile));

        log.info("Marketplace customer account successfully created: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        return CustomerAuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .customer(profileDto)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileDto getCurrentCustomerProfile() {
        UUID userId = SecurityUtils.getCurrentUserId();
        MarketplaceCustomerProfileEntity profile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceCustomerProfile", "userId", userId));

        CustomerProfileDto dto = mapper.toCustomerProfileDto(profile);
        dto.setProfileCompleteness(completenessCalculator.calculate(profile));
        return dto;
    }

    @Override
    @Transactional
    public CustomerProfileDto updateCurrentCustomerProfile(UpdateCustomerProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MarketplaceCustomerProfileEntity profile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceCustomerProfile", "userId", userId));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (StringUtils.hasText(request.getFirstName())) {
            profile.setFirstName(request.getFirstName().trim());
            user.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName().trim());
            user.setLastName(request.getLastName().trim());
        }

        if (StringUtils.hasText(request.getDisplayName())) {
            profile.setDisplayName(request.getDisplayName().trim());
        } else if (StringUtils.hasText(profile.getFirstName())) {
            profile.setDisplayName(StringUtils.hasText(profile.getLastName())
                    ? profile.getFirstName() + " " + profile.getLastName()
                    : profile.getFirstName());
        }

        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone().trim());
            user.setPhone(request.getPhone().trim());
        }

        if (request.getProfilePhotoUrl() != null) {
            profile.setProfilePhotoUrl(request.getProfilePhotoUrl().trim());
        }

        if (request.getCity() != null) {
            profile.setCity(request.getCity().trim());
        }

        if (request.getState() != null) {
            profile.setState(request.getState().trim());
        }

        if (request.getPincode() != null) {
            profile.setPincode(request.getPincode().trim());
        }

        if (request.getPreferredLanguage() != null) {
            profile.setPreferredLanguage(request.getPreferredLanguage().trim());
        }

        if (request.getCustomerType() != null) {
            profile.setCustomerType(request.getCustomerType());
        }

        if (request.getBusinessName() != null) {
            profile.setBusinessName(request.getBusinessName().trim());
        }

        userRepository.save(user);
        MarketplaceCustomerProfileEntity saved = customerProfileRepository.save(profile);

        String profileIdStr = saved.getId() != null ? saved.getId().toString() : userId.toString();
        auditService.logEvent("CUSTOMER_PROFILE_UPDATED", "MARKETPLACE_CUSTOMER_PROFILE", profileIdStr, null,
                "Updated marketplace profile for customer: " + userId);

        CustomerProfileDto dto = mapper.toCustomerProfileDto(saved);
        dto.setProfileCompleteness(completenessCalculator.calculate(saved));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDashboardDto getCustomerDashboard() {
        UUID userId = SecurityUtils.getCurrentUserId();
        MarketplaceCustomerProfileEntity profile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceCustomerProfile", "userId", userId));

        CustomerProfileDto profileDto = mapper.toCustomerProfileDto(profile);
        profileDto.setProfileCompleteness(completenessCalculator.calculate(profile));

        long totalRequirements = requirementRepository.countByCustomerId(profile.getId());
        long totalRequests = leadRepository.countByCustomerId(userId);
        long totalConsultations = consultationRepository.countByCustomerId(userId);
        long totalProposals = proposalRepository.countByCustomerId(userId);
        long totalReviews = reviewRepository.countByCustomerId(userId);

        List<CustomerTaxRequirementEntity> requirements = requirementRepository.findRecentByCustomerId(profile.getId(), PageRequest.of(0, 5));
        List<MarketplaceLeadEntity> leads = leadRepository.findAllByCustomerIdOrderByCreatedAtDesc(userId);
        List<MarketplaceConsultationEntity> consultations = consultationRepository.findAllByCustomerIdOrderByBookingDateDesc(userId);
        List<MarketplaceProposalEntity> proposals = proposalRepository.findAllByCustomerIdOrderByCreatedAtDesc(userId);
        List<MarketplaceReviewEntity> reviews = reviewRepository.findAllByCustomerIdOrderByCreatedAtDesc(userId);

        List<CustomerTaxRequirementSummaryDto> requirementDtos = requirementMapper.toSummaryDtoList(requirements);
        List<MarketplaceLeadDto> leadDtos = mapper.toLeadDtoList(leads.stream().limit(5).toList());
        List<MarketplaceConsultationDto> consultationDtos = mapper.toConsultationDtoList(consultations.stream().limit(5).toList());
        List<MarketplaceProposalDto> proposalDtos = mapper.toProposalDtoList(proposals.stream().limit(5).toList());
        List<MarketplaceReviewDto> reviewDtos = mapper.toReviewDtoList(reviews.stream().limit(5).toList());

        // Populate practice display names on DTOs
        populatePracticeDisplayNames(consultationDtos, proposalDtos);

        return CustomerDashboardDto.builder()
                .profile(profileDto)
                .totalRequirements(totalRequirements)
                .totalRequests(totalRequests)
                .totalConsultations(totalConsultations)
                .totalProposals(totalProposals)
                .totalReviews(totalReviews)
                .recentTaxRequirements(requirementDtos)
                .recentLeads(leadDtos)
                .recentConsultations(consultationDtos)
                .recentProposals(proposalDtos)
                .recentReviews(reviewDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketplaceLeadDto> getCustomerLeads() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<MarketplaceLeadEntity> leads = leadRepository.findAllByCustomerIdOrderByCreatedAtDesc(userId);
        return mapper.toLeadDtoList(leads);
    }

    @Override
    @Transactional(readOnly = true)
    public com.taxoryn.core.response.PagedResponse<EnquiryDetailDto> getCustomerEnquiries(EnquiryStatus status, org.springframework.data.domain.Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        org.springframework.data.domain.Page<MarketplaceLeadEntity> page = status != null
                ? leadRepository.findByCustomerIdAndEnquiryStatusOrderByCreatedAtDesc(userId, status, pageable)
                : leadRepository.findByCustomerIdOrderByCreatedAtDesc(userId, pageable);

        return com.taxoryn.core.response.PagedResponse.of(page, this::enrichCustomerEnquiryDetailDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EnquiryDetailDto getCustomerEnquiryDetail(UUID enquiryId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MarketplaceLeadEntity lead = leadRepository.findByIdAndCustomerId(enquiryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Enquiry", "id", enquiryId));
        return enrichCustomerEnquiryDetailDto(lead);
    }

    @Override
    @Transactional
    public EnquiryDetailDto cancelCustomerEnquiry(UUID enquiryId, CancelEnquiryRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MarketplaceLeadEntity lead = leadRepository.findByIdAndCustomerId(enquiryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Enquiry", "id", enquiryId));

        if (!lead.getEnquiryStatus().isCustomerCancellable()) {
            throw new com.taxoryn.core.exception.BusinessValidationException(
                    "Enquiry cannot be cancelled once work has begun. Current status: " + lead.getEnquiryStatus().getDisplayName()
            );
        }

        lead.setEnquiryStatus(EnquiryStatus.CANCELLED);
        lead.setLeadStatus(MarketplaceLeadEntity.LeadStatus.ARCHIVED);
        lead.setCancelledAt(java.time.Instant.now());
        if (request != null && StringUtils.hasText(request.getCancellationReason())) {
            lead.setCancellationReason(request.getCancellationReason().trim());
        }

        MarketplaceLeadEntity saved = leadRepository.save(lead);
        auditService.logEvent("ENQUIRY_CANCELLED", "MARKETPLACE_LEAD", saved.getId().toString(), saved.getOrganizationId(),
                "Enquiry " + saved.getReferenceNumber() + " cancelled by customer");

        // Notify Practice
        if (notificationService != null) {
            try {
                UUID practiceUserId = userRepository.findAllByOrganizationId(saved.getOrganizationId()).stream()
                        .findFirst()
                        .map(UserEntity::getId)
                        .orElse(null);
                if (practiceUserId != null) {
                    notificationService.notify(
                            saved.getOrganizationId(),
                            practiceUserId,
                            null,
                            com.taxoryn.module.notification.entity.NotificationEntity.NotificationType.GENERAL,
                            "Enquiry Cancelled (" + saved.getReferenceNumber() + ")",
                            "The client has cancelled enquiry " + saved.getReferenceNumber() + (StringUtils.hasText(saved.getCancellationReason()) ? ". Reason: " + saved.getCancellationReason() : ""),
                            null,
                            "/practice/marketplace/leads",
                            "{\"enquiryId\":\"" + saved.getId() + "\"}"
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to notify practice of enquiry cancellation: {}", e.getMessage());
            }
        }

        return enrichCustomerEnquiryDetailDto(saved);
    }

    @Override
    @Transactional
    public MarketplaceReviewDto submitVerifiedEnquiryReview(UUID enquiryId, SubmitEnquiryReviewRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MarketplaceLeadEntity lead = leadRepository.findByIdAndCustomerId(enquiryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Enquiry", "id", enquiryId));

        if (lead.getEnquiryStatus() != EnquiryStatus.COMPLETED) {
            throw new com.taxoryn.core.exception.BusinessValidationException(
                    "You can only submit a verified review after your tax enquiry has been completed by the practice. Current status: " + lead.getEnquiryStatus().getDisplayName()
            );
        }

        if (lead.getReviewId() != null || reviewRepository.existsByLeadId(lead.getId())) {
            throw new com.taxoryn.core.exception.BusinessValidationException("A review has already been submitted for this completed enquiry.");
        }

        MarketplaceCustomerProfileEntity customerProfile = customerProfileRepository.findByUserId(userId).orElse(null);
        String reviewerName = customerProfile != null && StringUtils.hasText(customerProfile.getDisplayName())
                ? customerProfile.getDisplayName()
                : (StringUtils.hasText(lead.getClientName()) ? lead.getClientName() : "Verified Client");

        String designation = customerProfile != null && customerProfile.getCustomerType() != null
                ? (customerProfile.getCustomerType() == CustomerType.BUSINESS ? "Business Owner" : "Individual Taxpayer")
                : "Taxpayer";

        MarketplaceReviewEntity review = MarketplaceReviewEntity.builder()
                .organizationId(lead.getOrganizationId())
                .marketplaceProfileId(lead.getMarketplaceProfileId())
                .customerId(userId)
                .leadId(lead.getId())
                .reviewerName(reviewerName)
                .reviewerDesignation(designation)
                .reviewerCompany(customerProfile != null ? customerProfile.getBusinessName() : null)
                .rating(request.getRating())
                .reviewTitle(StringUtils.hasText(request.getReviewTitle()) ? request.getReviewTitle().trim() : null)
                .reviewComment(request.getReviewComment().trim())
                .serviceTaken(lead.getServiceCategory())
                .isVerifiedClient(true)
                .status(MarketplaceReviewEntity.ReviewStatus.APPROVED)
                .build();

        MarketplaceReviewEntity savedReview = reviewRepository.save(review);
        lead.setReviewId(savedReview.getId());
        leadRepository.save(lead);

        // Update Practice Profile Rating Stats
        practiceProfileRepository.findById(lead.getMarketplaceProfileId()).ifPresent(profile -> {
            List<MarketplaceReviewEntity> allApproved = reviewRepository.findByMarketplaceProfileIdAndStatusOrderByCreatedAtDesc(
                    profile.getId(), MarketplaceReviewEntity.ReviewStatus.APPROVED
            );
            double avg = allApproved.stream().mapToInt(MarketplaceReviewEntity::getRating).average().orElse(5.0);
            profile.setAverageRating(java.math.BigDecimal.valueOf(Math.round(avg * 100.0) / 100.0));
            profile.setTotalReviews(allApproved.size());
            practiceProfileRepository.save(profile);
        });

        auditService.logEvent("VERIFIED_REVIEW_SUBMITTED", "MARKETPLACE_REVIEW", savedReview.getId().toString(), lead.getOrganizationId(),
                "Verified review submitted for enquiry " + lead.getReferenceNumber());

        // Notify Practice of new review
        if (notificationService != null) {
            try {
                UUID practiceUserId = userRepository.findAllByOrganizationId(lead.getOrganizationId()).stream()
                        .findFirst()
                        .map(UserEntity::getId)
                        .orElse(null);
                if (practiceUserId != null) {
                    notificationService.notify(
                            lead.getOrganizationId(),
                            practiceUserId,
                            null,
                            com.taxoryn.module.notification.entity.NotificationEntity.NotificationType.GENERAL,
                            "New Verified Review Received (" + request.getRating() + "★)",
                            reviewerName + " submitted a " + request.getRating() + "★ verified review for enquiry " + lead.getReferenceNumber(),
                            null,
                            "/practice/marketplace/profile",
                            "{\"reviewId\":\"" + savedReview.getId() + "\"}"
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to notify practice of new review: {}", e.getMessage());
            }
        }

        return mapper.toReviewDto(savedReview);
    }

    private EnquiryDetailDto enrichCustomerEnquiryDetailDto(MarketplaceLeadEntity entity) {
        EnquiryStatus status = entity.getEnquiryStatus() != null ? entity.getEnquiryStatus() : EnquiryStatus.NEW;

        String practiceName = null;
        String practiceSlug = null;
        String practiceCity = null;
        if (entity.getMarketplaceProfileId() != null) {
            var profileOpt = practiceProfileRepository.findById(entity.getMarketplaceProfileId());
            if (profileOpt.isPresent()) {
                practiceName = profileOpt.get().getDisplayName();
                practiceSlug = profileOpt.get().getSlug();
                practiceCity = profileOpt.get().getCity();
            }
        }

        String taxServiceName = null;
        String taxServiceCode = null;
        String serviceCategoryName = entity.getServiceCategory();
        if (entity.getTaxServiceId() != null && taxServiceRepository != null) {
            var svcOpt = taxServiceRepository.findById(entity.getTaxServiceId());
            if (svcOpt.isPresent()) {
                taxServiceName = svcOpt.get().getName();
                taxServiceCode = svcOpt.get().getCode();
                if (svcOpt.get().getCategory() != null) {
                    serviceCategoryName = svcOpt.get().getCategory().getName();
                }
            }
        }

        boolean canCancel = status.isCustomerCancellable();
        boolean canReview = status.isReviewEligible() && (entity.getReviewId() == null && !reviewRepository.existsByLeadId(entity.getId()));

        return EnquiryDetailDto.builder()
                .id(entity.getId())
                .referenceNumber(entity.getReferenceNumber() != null ? entity.getReferenceNumber() : "TXN-" + entity.getId().toString().substring(0, 8).toUpperCase())
                .organizationId(entity.getOrganizationId())
                .practiceName(practiceName)
                .practiceSlug(practiceSlug)
                .practiceCity(practiceCity)
                .marketplaceProfileId(entity.getMarketplaceProfileId())
                .customerId(entity.getCustomerId())
                .clientName(entity.getClientName())
                .clientEmail(entity.getClientEmail())
                .clientPhone(entity.getClientPhone())
                .city(entity.getCity())
                .taxServiceId(entity.getTaxServiceId())
                .taxServiceName(taxServiceName)
                .taxServiceCode(taxServiceCode)
                .serviceCategory(serviceCategoryName)
                .financialYear(entity.getFinancialYear())
                .customerType(entity.getCustomerType())
                .requirementDescription(entity.getEarlyEnquiryMessage() != null ? entity.getEarlyEnquiryMessage() : entity.getRequirementDescription())
                .earlyEnquiryMessage(entity.getEarlyEnquiryMessage())
                .budgetRange(entity.getBudgetRange())
                .urgency(entity.getUrgency())
                .sourceType(entity.getSourceType())
                .enquiryStatus(status)
                .rejectionReason(entity.getRejectionReason())
                .rejectionNote(entity.getRejectionNote())
                .cancellationReason(entity.getCancellationReason())
                .practitionerNotes(null) // Keep internal notes private from customer
                .assignedEmployeeId(null) // Keep internal staff ID private
                .assignedEmployeeName(null)
                .createdAt(entity.getCreatedAt())
                .receivedAt(entity.getReceivedAt() != null ? entity.getReceivedAt() : entity.getCreatedAt())
                .acceptedAt(entity.getAcceptedAt())
                .rejectedAt(entity.getRejectedAt())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .cancelledAt(entity.getCancelledAt())
                .timeline(buildCustomerTimeline(entity))
                .canCancel(canCancel)
                .canReview(canReview)
                .reviewId(entity.getReviewId())
                .build();
    }

    private List<EnquiryTimelineItemDto> buildCustomerTimeline(MarketplaceLeadEntity lead) {
        List<EnquiryTimelineItemDto> items = new ArrayList<>();
        EnquiryStatus status = lead.getEnquiryStatus() != null ? lead.getEnquiryStatus() : EnquiryStatus.NEW;

        // 1. Submitted
        items.add(EnquiryTimelineItemDto.builder()
                .status(EnquiryStatus.NEW)
                .title("Enquiry Submitted")
                .description("Your enquiry was submitted securely to Taxoryn")
                .timestamp(lead.getCreatedAt())
                .completed(true)
                .current(status == EnquiryStatus.NEW)
                .build());

        // 2. Delivered to Practice
        boolean receivedDone = status != EnquiryStatus.NEW;
        items.add(EnquiryTimelineItemDto.builder()
                .status(EnquiryStatus.RECEIVED)
                .title("Delivered to Practice")
                .description("Delivered to practice inbox for review")
                .timestamp(lead.getReceivedAt() != null ? lead.getReceivedAt() : lead.getCreatedAt())
                .completed(receivedDone)
                .current(status == EnquiryStatus.RECEIVED)
                .build());

        // 3. Accepted / Rejected / Cancelled
        if (status == EnquiryStatus.REJECTED) {
            items.add(EnquiryTimelineItemDto.builder()
                    .status(EnquiryStatus.REJECTED)
                    .title("Enquiry Declined")
                    .description(lead.getRejectionReason() != null ? lead.getRejectionReason().getDisplayName() : "Declined by practice")
                    .timestamp(lead.getRejectedAt() != null ? lead.getRejectedAt() : lead.getUpdatedAt())
                    .completed(true)
                    .current(true)
                    .build());
            return items;
        } else if (status == EnquiryStatus.CANCELLED) {
            items.add(EnquiryTimelineItemDto.builder()
                    .status(EnquiryStatus.CANCELLED)
                    .title("Enquiry Cancelled")
                    .description(StringUtils.hasText(lead.getCancellationReason()) ? lead.getCancellationReason() : "Cancelled by you")
                    .timestamp(lead.getCancelledAt() != null ? lead.getCancelledAt() : lead.getUpdatedAt())
                    .completed(true)
                    .current(true)
                    .build());
            return items;
        }

        boolean acceptedDone = lead.getAcceptedAt() != null || status == EnquiryStatus.ACCEPTED || status == EnquiryStatus.IN_PROGRESS || status == EnquiryStatus.COMPLETED;
        items.add(EnquiryTimelineItemDto.builder()
                .status(EnquiryStatus.ACCEPTED)
                .title("Practice Accepted")
                .description("Practice accepted and reviewed your tax requirement")
                .timestamp(lead.getAcceptedAt())
                .completed(acceptedDone)
                .current(status == EnquiryStatus.ACCEPTED)
                .build());

        // 4. In Progress
        boolean inProgressDone = lead.getStartedAt() != null || status == EnquiryStatus.IN_PROGRESS || status == EnquiryStatus.COMPLETED;
        items.add(EnquiryTimelineItemDto.builder()
                .status(EnquiryStatus.IN_PROGRESS)
                .title("Work in Progress")
                .description("Tax professional is preparing your compliance filing")
                .timestamp(lead.getStartedAt())
                .completed(inProgressDone)
                .current(status == EnquiryStatus.IN_PROGRESS)
                .build());

        // 5. Completed
        boolean completedDone = lead.getCompletedAt() != null || status == EnquiryStatus.COMPLETED;
        items.add(EnquiryTimelineItemDto.builder()
                .status(EnquiryStatus.COMPLETED)
                .title("Completed")
                .description("Compliance completed and verified")
                .timestamp(lead.getCompletedAt())
                .completed(completedDone)
                .current(status == EnquiryStatus.COMPLETED)
                .build());

        return items;
    }

    @Override
    @Transactional(readOnly = true)
    public EnquiryMessageThreadDto getCustomerEnquiryMessages(UUID enquiryId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MarketplaceLeadEntity lead = leadRepository.findByIdAndCustomerId(enquiryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Enquiry", "id", enquiryId));

        List<MarketplaceEnquiryMessageEntity> messages = enquiryMessageRepository.findByEnquiryIdOrderByCreatedAtAsc(enquiryId);
        long unreadCustomer = enquiryMessageRepository.countByEnquiryIdAndIsReadByCustomerFalse(enquiryId);
        long unreadPractice = enquiryMessageRepository.countByEnquiryIdAndIsReadByPracticeFalse(enquiryId);

        String practiceName = practiceProfileRepository.findById(lead.getMarketplaceProfileId())
                .map(prof -> prof.getDisplayName())
                .orElse("Practice");

        String assignedEmployeeName = null;
        if (lead.getAssignedEmployeeId() != null) {
            assignedEmployeeName = employeeRepository.findById(lead.getAssignedEmployeeId())
                    .map(emp -> emp.getFullName())
                    .orElse(null);
        }

        List<EnquiryMessageDto> messageDtos = messages.stream()
                .map(this::toEnquiryMessageDto)
                .collect(Collectors.toList());

        return EnquiryMessageThreadDto.builder()
                .enquiryId(lead.getId())
                .referenceNumber(lead.getReferenceNumber())
                .enquiryStatus(lead.getEnquiryStatus())
                .clientName(lead.getClientName())
                .practiceName(practiceName)
                .assignedEmployeeName(assignedEmployeeName)
                .unreadCountForCustomer(unreadCustomer)
                .unreadCountForPractice(unreadPractice)
                .isMessagingActive(!lead.getEnquiryStatus().isTerminal())
                .messages(messageDtos)
                .build();
    }

    @Override
    @Transactional
    public EnquiryMessageDto sendCustomerMessage(UUID enquiryId, SendEnquiryMessageRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MarketplaceLeadEntity lead = leadRepository.findByIdAndCustomerId(enquiryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Enquiry", "id", enquiryId));

        if (lead.getEnquiryStatus().isTerminal()) {
            throw new com.taxoryn.core.exception.BusinessValidationException(
                    "Cannot send messages on a " + lead.getEnquiryStatus().getDisplayName() + " enquiry"
            );
        }

        String senderName = userRepository.findById(userId)
                .map(UserEntity::getFullName)
                .orElse(lead.getClientName());

        MarketplaceEnquiryMessageEntity message = MarketplaceEnquiryMessageEntity.builder()
                .enquiryId(lead.getId())
                .senderType(MessageSenderType.CUSTOMER)
                .senderUserId(userId)
                .senderName(senderName)
                .messageBody(request.getMessageBody().trim())
                .attachmentsJson(request.getAttachmentsJson())
                .isReadByCustomer(true)
                .isReadByPractice(false)
                .build();

        MarketplaceEnquiryMessageEntity saved = enquiryMessageRepository.save(message);

        // Audit Log
        auditService.logEvent("ENQUIRY_MESSAGE_SENT", "MARKETPLACE_LEAD", lead.getId().toString(), lead.getOrganizationId(),
                "Customer message sent on enquiry " + lead.getReferenceNumber() + " by " + senderName);

        // Notify Practice & Assigned Employee
        if (notificationService != null) {
            try {
                // If assigned to a specific employee with a user account, notify them directly
                UUID assignedUserId = null;
                if (lead.getAssignedEmployeeId() != null) {
                    assignedUserId = employeeRepository.findById(lead.getAssignedEmployeeId())
                            .map(emp -> emp.getUserId())
                            .orElse(null);
                }

                if (assignedUserId == null) {
                    assignedUserId = userRepository.findAllByOrganizationId(lead.getOrganizationId()).stream()
                            .findFirst()
                            .map(UserEntity::getId)
                            .orElse(null);
                }

                if (assignedUserId != null) {
                    notificationService.notify(
                            lead.getOrganizationId(),
                            assignedUserId,
                            null,
                            com.taxoryn.module.notification.entity.NotificationEntity.NotificationType.GENERAL,
                            "New Customer Message (" + lead.getReferenceNumber() + ")",
                            senderName + ": " + (saved.getMessageBody().length() > 80 ? saved.getMessageBody().substring(0, 77) + "..." : saved.getMessageBody()),
                            null,
                            "/practice/marketplace/leads",
                            "{\"enquiryId\":\"" + lead.getId() + "\",\"messageId\":\"" + saved.getId() + "\"}"
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to notify practice of customer message: {}", e.getMessage());
            }
        }

        return toEnquiryMessageDto(saved);
    }

    @Override
    @Transactional
    public void markMessagesReadByCustomer(UUID enquiryId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MarketplaceLeadEntity lead = leadRepository.findByIdAndCustomerId(enquiryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Enquiry", "id", enquiryId));

        enquiryMessageRepository.markAllAsReadByCustomer(lead.getId(), java.time.Instant.now());
    }

    private EnquiryMessageDto toEnquiryMessageDto(MarketplaceEnquiryMessageEntity entity) {
        return EnquiryMessageDto.builder()
                .id(entity.getId())
                .enquiryId(entity.getEnquiryId())
                .senderType(entity.getSenderType())
                .senderUserId(entity.getSenderUserId())
                .senderName(entity.getSenderName())
                .messageBody(entity.getMessageBody())
                .attachmentsJson(entity.getAttachmentsJson())
                .isReadByCustomer(entity.getIsReadByCustomer())
                .isReadByPractice(entity.getIsReadByPractice())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketplaceConsultationDto> getCustomerConsultations() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<MarketplaceConsultationEntity> consultations = consultationRepository.findAllByCustomerIdOrderByBookingDateDesc(userId);
        List<MarketplaceConsultationDto> dtos = mapper.toConsultationDtoList(consultations);
        populatePracticeDisplayNames(dtos, Collections.emptyList());
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketplaceProposalDto> getCustomerProposals() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<MarketplaceProposalEntity> proposals = proposalRepository.findAllByCustomerIdOrderByCreatedAtDesc(userId);
        List<MarketplaceProposalDto> dtos = mapper.toProposalDtoList(proposals);
        populatePracticeDisplayNames(Collections.emptyList(), dtos);
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketplaceReviewDto> getCustomerReviews() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<MarketplaceReviewEntity> reviews = reviewRepository.findAllByCustomerIdOrderByCreatedAtDesc(userId);
        return mapper.toReviewDtoList(reviews);
    }

    private void populatePracticeDisplayNames(List<MarketplaceConsultationDto> consultations, List<MarketplaceProposalDto> proposals) {
        consultations.forEach(c -> {
            if (c.getMarketplaceProfileId() != null) {
                practiceProfileRepository.findById(c.getMarketplaceProfileId())
                        .ifPresent(p -> c.setPracticeDisplayName(p.getDisplayName()));
            }
        });

        proposals.forEach(p -> {
            if (p.getMarketplaceProfileId() != null) {
                practiceProfileRepository.findById(p.getMarketplaceProfileId())
                        .ifPresent(prof -> p.setPracticeDisplayName(prof.getDisplayName()));
            }
        });
    }
}
