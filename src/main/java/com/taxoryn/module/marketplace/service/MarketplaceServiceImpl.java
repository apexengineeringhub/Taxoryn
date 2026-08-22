package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity.ConsultationStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceServiceImpl implements MarketplaceService {

    private final MarketplaceProfileRepository profileRepository;
    private final MarketplaceServiceRepository serviceRepository;
    private final MarketplaceLeadRepository leadRepository;
    private final MarketplaceConsultationRepository consultationRepository;
    private final MarketplaceReviewRepository reviewRepository;
    private final MarketplaceVerificationRepository verificationRepository;
    private final OrganizationRepository organizationRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final MarketplaceMapper mapper;
    private final AuditService auditService;
    private final ProfileCompletenessCalculator completenessCalculator;
    private final PublicSlugGenerator slugGenerator;

    // =========================================================================
    // 1. Public Customer Discovery APIs
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PublicMarketplaceProfileDto> searchProfiles(MarketplaceSearchRequest request) {
        org.springframework.data.jpa.domain.Specification<MarketplaceProfileEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isPublished")));

            if (StringUtils.hasText(request.getCity())) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + request.getCity().trim().toLowerCase() + "%"));
            }
            if (request.getProfessionalType() != null) {
                predicates.add(cb.equal(root.get("professionalType"), request.getProfessionalType()));
            }
            if (StringUtils.hasText(request.getSpecialization())) {
                predicates.add(cb.like(cb.lower(root.get("specializations")), "%" + request.getSpecialization().trim().toLowerCase() + "%"));
            }
            if (Boolean.TRUE.equals(request.getVerifiedOnly())) {
                predicates.add(cb.equal(root.get("verificationStatus"), VerificationStatus.VERIFIED));
            }
            if (StringUtils.hasText(request.getSearch())) {
                String s = "%" + request.getSearch().trim().toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate nameMatch = cb.like(cb.lower(root.get("displayName")), s);
                jakarta.persistence.criteria.Predicate headlineMatch = cb.like(cb.lower(root.get("headline")), s);
                jakarta.persistence.criteria.Predicate cityMatch = cb.like(cb.lower(root.get("city")), s);
                predicates.add(cb.or(nameMatch, headlineMatch, cityMatch));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<MarketplaceProfileEntity> page = profileRepository.findAll(spec, request.toPageable());
        return PagedResponse.of(page, this::enrichPublicProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicMarketplaceProfileDto getProfileBySlug(String slug) {
        MarketplaceProfileEntity entity = profileRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "slug", slug));
        return enrichPublicProfile(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicMarketplaceProfileDto getProfileById(UUID id) {
        MarketplaceProfileEntity entity = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "id", id));
        return enrichPublicProfile(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicMarketplaceProfileDto> getFeaturedProfiles() {
        List<MarketplaceProfileEntity> entities = profileRepository.findTop6ByIsPublishedTrueAndIsFeaturedTrueOrderByAverageRatingDesc();
        return entities.stream().map(this::enrichPublicProfile).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MarketplaceLeadDto submitPublicLead(CreateMarketplaceLeadRequest request) {
        MarketplaceProfileEntity profile = profileRepository.findById(request.getMarketplaceProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "id", request.getMarketplaceProfileId()));

        MarketplaceLeadEntity entity = MarketplaceLeadEntity.builder()
                .organizationId(profile.getOrganizationId())
                .marketplaceProfileId(profile.getId())
                .serviceId(request.getServiceId())
                .clientName(request.getClientName().trim())
                .clientEmail(request.getClientEmail().trim().toLowerCase())
                .clientPhone(request.getClientPhone().trim())
                .city(request.getCity())
                .pan(request.getPan() != null ? request.getPan().toUpperCase().trim() : null)
                .gstin(request.getGstin() != null ? request.getGstin().toUpperCase().trim() : null)
                .serviceCategory(request.getServiceCategory())
                .requirementDescription(request.getRequirementDescription())
                .budgetRange(request.getBudgetRange())
                .urgency(request.getUrgency() != null ? request.getUrgency() : MarketplaceLeadEntity.Urgency.STANDARD)
                .leadStatus(LeadStatus.NEW)
                .build();

        MarketplaceLeadEntity saved = leadRepository.save(entity);
        log.info("New marketplace lead generated: {} for practice: {}", saved.getId(), profile.getDisplayName());
        auditService.logEvent("MARKETPLACE_LEAD_GENERATED", "MARKETPLACE_LEAD", saved.getId().toString(), null, "Lead from " + saved.getClientName());

        return enrichLeadDto(saved);
    }

    @Override
    @Transactional
    public MarketplaceConsultationDto bookPublicConsultation(BookConsultationRequest request) {
        MarketplaceProfileEntity profile = profileRepository.findById(request.getMarketplaceProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "id", request.getMarketplaceProfileId()));

        MarketplaceConsultationEntity consultation = MarketplaceConsultationEntity.builder()
                .organizationId(profile.getOrganizationId())
                .marketplaceProfileId(profile.getId())
                .clientName(request.getClientName().trim())
                .clientEmail(request.getClientEmail().trim().toLowerCase())
                .clientPhone(request.getClientPhone().trim())
                .topic(request.getTopic())
                .consultationMode(request.getConsultationMode())
                .bookingDate(request.getBookingDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .feeAmount(profile.getConsultationFee())
                .paymentStatus(MarketplaceConsultationEntity.PaymentStatus.PAID)
                .consultationStatus(ConsultationStatus.SCHEDULED)
                .notes(request.getNotes())
                .build();

        MarketplaceConsultationEntity saved = consultationRepository.save(consultation);
        log.info("New marketplace consultation booked: {} on {} {} with {}", saved.getId(), saved.getBookingDate(), saved.getStartTime(), profile.getDisplayName());
        auditService.logEvent("MARKETPLACE_CONSULTATION_BOOKED", "MARKETPLACE_CONSULTATION", saved.getId().toString(), null, "Consultation with " + saved.getClientName());

        // Unify into Practice Inbound Leads CRM so the practitioner immediately sees this booking in their Leads pipeline
        try {
            MarketplaceLeadEntity lead = MarketplaceLeadEntity.builder()
                    .organizationId(profile.getOrganizationId())
                    .marketplaceProfileId(profile.getId())
                    .clientName(request.getClientName().trim())
                    .clientEmail(request.getClientEmail().trim().toLowerCase())
                    .clientPhone(request.getClientPhone().trim())
                    .serviceCategory("Paid Consultation")
                    .requirementDescription("Booked 30-min strategy consultation on " + request.getBookingDate() + " at " + request.getStartTime() +
                            " (" + request.getConsultationMode() + "). Topic: " + request.getTopic() +
                            (StringUtils.hasText(request.getNotes()) ? " | Client Notes: " + request.getNotes() : ""))
                    .urgency(MarketplaceLeadEntity.Urgency.URGENT)
                    .leadStatus(LeadStatus.NEW)
                    .practitionerNotes("Paid consultation booked via Marketplace. Slot: " + request.getBookingDate() + " " + request.getStartTime())
                    .build();
            leadRepository.save(lead);
            log.info("Linked Inbound Lead created for consultation booking {}", saved.getId());
        } catch (Exception e) {
            log.warn("Could not create linked lead for consultation booking: {}", e.getMessage());
        }

        return enrichConsultationDto(saved);
    }

    @Override
    @Transactional
    public MarketplaceReviewDto submitPublicReview(SubmitMarketplaceReviewRequest request) {
        MarketplaceProfileEntity profile = profileRepository.findById(request.getMarketplaceProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "id", request.getMarketplaceProfileId()));

        MarketplaceReviewEntity review = MarketplaceReviewEntity.builder()
                .organizationId(profile.getOrganizationId())
                .marketplaceProfileId(profile.getId())
                .reviewerName(request.getReviewerName().trim())
                .reviewerDesignation(request.getReviewerDesignation())
                .reviewerCompany(request.getReviewerCompany())
                .rating(request.getRating())
                .reviewTitle(request.getReviewTitle())
                .reviewComment(request.getReviewComment().trim())
                .serviceTaken(request.getServiceTaken())
                .isVerifiedClient(true)
                .status(MarketplaceReviewEntity.ReviewStatus.APPROVED)
                .build();

        MarketplaceReviewEntity saved = reviewRepository.save(review);

        // Recalculate average rating
        List<MarketplaceReviewEntity> allReviews = reviewRepository.findByMarketplaceProfileIdAndStatusOrderByCreatedAtDesc(
                profile.getId(), MarketplaceReviewEntity.ReviewStatus.APPROVED
        );
        double avg = allReviews.stream().mapToInt(MarketplaceReviewEntity::getRating).average().orElse(5.0);
        profile.setAverageRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        profile.setTotalReviews(allReviews.size());
        profileRepository.save(profile);

        return mapper.toReviewDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketplaceServiceDto> getPublicServices(UUID marketplaceProfileId) {
        List<MarketplaceServiceEntity> services = serviceRepository.findByMarketplaceProfileIdAndIsActiveTrue(marketplaceProfileId);
        return mapper.toServiceDtoList(services);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketplaceReviewDto> getPublicReviews(UUID marketplaceProfileId) {
        List<MarketplaceReviewEntity> reviews = reviewRepository.findByMarketplaceProfileIdAndStatusOrderByCreatedAtDesc(
                marketplaceProfileId, MarketplaceReviewEntity.ReviewStatus.APPROVED
        );
        return mapper.toReviewDtoList(reviews);
    }

    // =========================================================================
    // 2. Practice Private Portal APIs
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PublicMarketplaceProfileDto getMyPracticeProfile() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceProfileEntity profile = profileRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> initializeDefaultProfile(organizationId));
        return enrichPublicProfile(profile);
    }

    @Override
    @Transactional
    public PublicMarketplaceProfileDto createPracticeProfile(CreatePracticeProfileRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (profileRepository.findByOrganizationId(organizationId).isPresent()) {
            throw new IllegalArgumentException("A marketplace profile already exists for this practice organization. Use PUT to update.");
        }

        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        String candidateSlug = StringUtils.hasText(request.getSlug())
                ? slugGenerator.sanitize(request.getSlug())
                : slugGenerator.generateUniqueSlug(request.getDisplayName(), null);

        if (slugGenerator.isSlugTaken(candidateSlug, null)) {
            candidateSlug = slugGenerator.generateUniqueSlug(candidateSlug, null);
        }

        MarketplaceProfileEntity profile = MarketplaceProfileEntity.builder()
                .organizationId(organizationId)
                .slug(candidateSlug)
                .displayName(request.getDisplayName().trim())
                .headline(request.getHeadline())
                .bio(request.resolveBio() != null ? request.resolveBio() : "Professional tax practice offering GST, Income Tax, TDS, and corporate compliance services.")
                .professionalType(request.getProfessionalType() != null ? request.getProfessionalType() : ProfessionalType.CHARTERED_ACCOUNTANT)
                .experienceYears(request.getExperienceYears() != null ? request.getExperienceYears() : 5)
                .city(request.getCity() != null ? request.getCity() : org.getCity())
                .state(request.getState() != null ? request.getState() : org.getState())
                .pincode(request.getPincode() != null ? request.getPincode() : org.getPincode())
                .address(request.getAddress() != null ? request.getAddress() : org.getAddress())
                .phone(request.getPhone() != null ? request.getPhone() : org.getPhone())
                .email(request.getEmail() != null ? request.getEmail() : org.getEmail())
                .websiteUrl(request.resolveWebsite())
                .avatarUrl(request.getAvatarUrl())
                .bannerUrl(request.getBannerUrl())
                .specializations(request.getSpecializations() != null ? String.join(", ", request.getSpecializations()) : "GST_FILING, ITR_FILING, TDS_COMPLIANCE")
                .languagesSpoken(request.getLanguagesSpoken() != null ? request.getLanguagesSpoken() : "English, Hindi")
                .startingFee(request.getStartingFee() != null ? request.getStartingFee() : new BigDecimal("999.00"))
                .hourlyRate(request.getHourlyRate() != null ? request.getHourlyRate() : new BigDecimal("1500.00"))
                .visibilityStatus(request.getVisibility() != null ? request.getVisibility() : VisibilityStatus.PRIVATE)
                .isPublished(request.getVisibility() == VisibilityStatus.PUBLIC)
                .consultationEnabled(request.getConsultationEnabled() != null ? request.getConsultationEnabled() : true)
                .consultationFee(request.getConsultationFee() != null ? request.getConsultationFee() : new BigDecimal("499.00"))
                .consultationDurationMinutes(request.getConsultationDurationMinutes() != null ? request.getConsultationDurationMinutes() : 30)
                .build();

        if (profile.getVisibilityStatus() == VisibilityStatus.PUBLIC) {
            validatePublishingEligibility(profile);
            profile.setIsPublished(true);
        }

        MarketplaceProfileEntity saved = profileRepository.save(profile);
        auditService.logEvent("PRACTICE_MARKETPLACE_PROFILE_CREATED", "MARKETPLACE_PROFILE", saved.getId() != null ? saved.getId().toString() : "NEW", null, "Created profile " + saved.getDisplayName());

        return enrichPublicProfile(saved);
    }

    @Override
    @Transactional
    public PublicMarketplaceProfileDto updateMyPracticeProfile(UpdateMarketplaceProfileRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceProfileEntity profile = profileRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> initializeDefaultProfile(organizationId));

        if (StringUtils.hasText(request.getDisplayName())) profile.setDisplayName(request.getDisplayName().trim());
        if (StringUtils.hasText(request.getSlug())) {
            String cleanSlug = slugGenerator.sanitize(request.getSlug());
            if (!cleanSlug.equalsIgnoreCase(profile.getSlug())) {
                if (slugGenerator.isSlugTaken(cleanSlug, profile.getId())) {
                    throw new IllegalArgumentException("The public slug '" + cleanSlug + "' is already taken. Please choose another unique slug.");
                }
                profile.setSlug(cleanSlug);
            }
        }
        if (request.getHeadline() != null) profile.setHeadline(request.getHeadline());
        if (request.resolveBio() != null) profile.setBio(request.resolveBio());
        if (request.getProfessionalType() != null) profile.setProfessionalType(request.getProfessionalType());
        if (request.getExperienceYears() != null) profile.setExperienceYears(request.getExperienceYears());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getState() != null) profile.setState(request.getState());
        if (request.getPincode() != null) profile.setPincode(request.getPincode());
        if (request.getAddress() != null) profile.setAddress(request.getAddress());
        if (request.getPhone() != null) profile.setPhone(request.getPhone());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());
        if (request.resolveWebsite() != null) profile.setWebsiteUrl(request.resolveWebsite());
        if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl());
        if (request.getBannerUrl() != null) profile.setBannerUrl(request.getBannerUrl());
        if (request.getSpecializations() != null) {
            profile.setSpecializations(String.join(", ", request.getSpecializations()));
        }
        if (request.getLanguagesSpoken() != null) profile.setLanguagesSpoken(request.getLanguagesSpoken());
        if (request.getStartingFee() != null) profile.setStartingFee(request.getStartingFee());
        if (request.getHourlyRate() != null) profile.setHourlyRate(request.getHourlyRate());
        
        VisibilityStatus visibility = request.resolveVisibility();
        if (visibility != null) {
            if (visibility == VisibilityStatus.PUBLIC) {
                validatePublishingEligibility(profile);
            }
            profile.setVisibilityStatus(visibility);
            profile.setIsPublished(visibility == VisibilityStatus.PUBLIC);
        } else if (Boolean.TRUE.equals(request.getIsPublished())) {
            validatePublishingEligibility(profile);
            profile.setIsPublished(true);
            profile.setVisibilityStatus(VisibilityStatus.PUBLIC);
        } else if (Boolean.FALSE.equals(request.getIsPublished())) {
            profile.setIsPublished(false);
            if (profile.getVisibilityStatus() == VisibilityStatus.PUBLIC) {
                profile.setVisibilityStatus(VisibilityStatus.PRIVATE);
            }
        }

        if (request.getConsultationEnabled() != null) profile.setConsultationEnabled(request.getConsultationEnabled());
        if (request.getConsultationFee() != null) profile.setConsultationFee(request.getConsultationFee());
        if (request.getConsultationDurationMinutes() != null) profile.setConsultationDurationMinutes(request.getConsultationDurationMinutes());

        MarketplaceProfileEntity saved = profileRepository.save(profile);
        auditService.logEvent("PRACTICE_MARKETPLACE_PROFILE_UPDATED", "MARKETPLACE_PROFILE", saved.getId().toString(), null, "Updated profile " + saved.getDisplayName());

        return enrichPublicProfile(saved);
    }

    @Override
    @Transactional
    public PublicMarketplaceProfileDto updateProfileVisibility(UpdateProfileVisibilityRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceProfileEntity profile = profileRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> initializeDefaultProfile(organizationId));

        if (request.getVisibility() == VisibilityStatus.PUBLIC) {
            validatePublishingEligibility(profile);
            profile.setVisibilityStatus(VisibilityStatus.PUBLIC);
            profile.setIsPublished(true);
        } else if (request.getVisibility() == VisibilityStatus.PRIVATE) {
            profile.setVisibilityStatus(VisibilityStatus.PRIVATE);
            profile.setIsPublished(false);
        } else if (request.getVisibility() == VisibilityStatus.SUSPENDED) {
            profile.setVisibilityStatus(VisibilityStatus.SUSPENDED);
            profile.setIsPublished(false);
        }

        MarketplaceProfileEntity saved = profileRepository.save(profile);
        auditService.logEvent("PRACTICE_MARKETPLACE_VISIBILITY_UPDATED", "MARKETPLACE_PROFILE", saved.getId().toString(), null, "Updated visibility to " + saved.getVisibilityStatus());

        return enrichPublicProfile(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateUniqueSlug(String baseName, String city) {
        String combined = (StringUtils.hasText(baseName) ? baseName : "") + (StringUtils.hasText(city) ? " " + city : "");
        return slugGenerator.generateUniqueSlug(combined, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileCompletenessDto getMyProfileCompleteness() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceProfileEntity profile = profileRepository.findByOrganizationId(organizationId).orElse(null);
        OrganizationEntity org = organizationRepository.findById(organizationId).orElse(null);
        return completenessCalculator.calculate(profile, org);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketplaceServiceDto> getMyPracticeServices() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<MarketplaceServiceEntity> services = serviceRepository.findByOrganizationId(organizationId);
        return mapper.toServiceDtoList(services);
    }

    @Override
    @Transactional
    public MarketplaceServiceDto createPracticeService(CreateMarketplaceServiceRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceProfileEntity profile = profileRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> initializeDefaultProfile(organizationId));

        MarketplaceServiceEntity entity = MarketplaceServiceEntity.builder()
                .organizationId(organizationId)
                .marketplaceProfileId(profile.getId())
                .title(request.getTitle().trim())
                .category(request.getCategory().toUpperCase().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .pricingType(request.getPricingType() != null ? request.getPricingType() : MarketplaceServiceEntity.PricingType.FIXED)
                .deliveryDays(request.getDeliveryDays() != null ? request.getDeliveryDays() : 3)
                .deliverables(request.getDeliverables())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        MarketplaceServiceEntity saved = serviceRepository.save(entity);
        auditService.logEvent("MARKETPLACE_SERVICE_CREATED", "MARKETPLACE_SERVICE", saved.getId().toString(), null, "Created package " + saved.getTitle());

        return mapper.toServiceDto(saved);
    }

    @Override
    @Transactional
    public MarketplaceServiceDto updatePracticeService(UUID serviceId, CreateMarketplaceServiceRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceServiceEntity entity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Service", "id", serviceId));

        if (!entity.getOrganizationId().equals(organizationId)) {
            throw new ResourceNotFoundException("Marketplace Service", "id", serviceId);
        }

        entity.setTitle(request.getTitle().trim());
        entity.setCategory(request.getCategory().toUpperCase().trim());
        entity.setDescription(request.getDescription());
        entity.setPrice(request.getPrice());
        if (request.getPricingType() != null) entity.setPricingType(request.getPricingType());
        if (request.getDeliveryDays() != null) entity.setDeliveryDays(request.getDeliveryDays());
        entity.setDeliverables(request.getDeliverables());
        if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());

        MarketplaceServiceEntity saved = serviceRepository.save(entity);
        return mapper.toServiceDto(saved);
    }

    @Override
    @Transactional
    public void deletePracticeService(UUID serviceId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceServiceEntity entity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Service", "id", serviceId));

        if (!entity.getOrganizationId().equals(organizationId)) {
            throw new ResourceNotFoundException("Marketplace Service", "id", serviceId);
        }

        serviceRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MarketplaceLeadDto> getMyLeads(LeadStatus status, String search, Pageable pageable) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        org.springframework.data.jpa.domain.Specification<MarketplaceLeadEntity> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (status != null) {
                predicates.add(cb.equal(root.get("leadStatus"), status));
            }
            if (StringUtils.hasText(search)) {
                String s = "%" + search.trim().toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate nameMatch = cb.like(cb.lower(root.get("clientName")), s);
                jakarta.persistence.criteria.Predicate emailMatch = cb.like(cb.lower(root.get("clientEmail")), s);
                jakarta.persistence.criteria.Predicate catMatch = cb.like(cb.lower(root.get("serviceCategory")), s);
                predicates.add(cb.or(nameMatch, emailMatch, catMatch));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<MarketplaceLeadEntity> page = leadRepository.findAll(spec, pageable);
        return PagedResponse.of(page, this::enrichLeadDto);
    }

    @Override
    @Transactional
    public MarketplaceLeadDto updateLeadStatus(UUID leadId, LeadStatus status, String notes, UUID assignedEmployeeId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceLeadEntity lead = leadRepository.findByIdAndOrganizationId(leadId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Lead", "id", leadId));

        if (status != null) lead.setLeadStatus(status);
        if (notes != null) lead.setPractitionerNotes(notes);
        if (assignedEmployeeId != null) lead.setAssignedEmployeeId(assignedEmployeeId);

        MarketplaceLeadEntity saved = leadRepository.save(lead);
        return enrichLeadDto(saved);
    }

    @Override
    @Transactional
    public MarketplaceLeadDto convertLeadToClient(UUID leadId, ConvertLeadToClientRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceLeadEntity lead = leadRepository.findByIdAndOrganizationId(leadId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Lead", "id", leadId));

        // 1. Create or Find Client in CRM
        ClientEntity client;
        Optional<ClientEntity> existingClient = StringUtils.hasText(lead.getPan())
                ? clientRepository.findByOrganizationIdAndPan(organizationId, lead.getPan().toUpperCase().trim())
                : Optional.empty();

        if (existingClient.isPresent()) {
            client = existingClient.get();
        } else {
            client = ClientEntity.builder()
                    .displayName(lead.getClientName())
                    .legalName(lead.getClientName())
                    .pan(lead.getPan())
                    .gstin(lead.getGstin())
                    .email(lead.getClientEmail())
                    .phone(lead.getClientPhone())
                    .city(lead.getCity())
                    .clientType(request.getClientType() != null ? request.getClientType() : ClientEntity.ClientType.INDIVIDUAL)
                    .assignedEmployeeId(request.getAssignedEmployeeId() != null ? request.getAssignedEmployeeId() : lead.getAssignedEmployeeId())
                    .status(ClientEntity.ClientStatus.ACTIVE)
                    .notes(request.getNotes() != null ? request.getNotes() : "Acquired via Taxoryn Marketplace. Requirement: " + lead.getRequirementDescription())
                    .build();
            client.setOrganizationId(organizationId);
            client = clientRepository.save(client);
        }

        // 2. Auto-create Onboarding Task if requested
        if (Boolean.TRUE.equals(request.getCreateOnboardingTask())) {
            TaskEntity task = TaskEntity.builder()
                    .clientId(client.getId())
                    .assignedTo(client.getAssignedEmployeeId())
                    .title("Onboard Marketplace Client: " + client.getDisplayName())
                    .description("Collect KYC documents and initiate " + (lead.getServiceCategory() != null ? lead.getServiceCategory() : "Tax") + " compliance filing.")
                    .taskCategory(resolveTaskCategory(lead.getServiceCategory()))
                    .status(TaskEntity.TaskStatus.TODO)
                    .priority(TaskEntity.TaskPriority.HIGH)
                    .dueDate(LocalDate.now().plusDays(3))
                    .build();
            task.setOrganizationId(organizationId);
            taskRepository.save(task);
        }

        // 3. Mark Lead as CONVERTED
        lead.setLeadStatus(LeadStatus.CONVERTED);
        lead.setConvertedClientId(client.getId());
        if (request.getAssignedEmployeeId() != null) lead.setAssignedEmployeeId(request.getAssignedEmployeeId());
        MarketplaceLeadEntity savedLead = leadRepository.save(lead);

        // Update profile served count
        profileRepository.findByOrganizationId(organizationId).ifPresent(p -> {
            p.setTotalClientsServed(p.getTotalClientsServed() + 1);
            profileRepository.save(p);
        });

        auditService.logEvent("MARKETPLACE_LEAD_CONVERTED", "CLIENT", client.getId().toString(), null, "Converted Lead " + lead.getClientName() + " to CRM Client");

        return enrichLeadDto(savedLead);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MarketplaceConsultationDto> getMyConsultations(Pageable pageable) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        Page<MarketplaceConsultationEntity> page = consultationRepository.findAllByOrganizationIdOrderByBookingDateDesc(organizationId, pageable);
        return PagedResponse.of(page, this::enrichConsultationDto);
    }

    @Override
    @Transactional
    public MarketplaceConsultationDto updateConsultationStatus(UUID consultationId, ConsultationStatus status, String meetingLink, String notes) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceConsultationEntity consultation = consultationRepository.findByIdAndOrganizationId(consultationId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Consultation", "id", consultationId));

        if (status != null) consultation.setConsultationStatus(status);
        if (StringUtils.hasText(meetingLink)) consultation.setMeetingLink(meetingLink);
        if (notes != null) consultation.setNotes(notes);

        MarketplaceConsultationEntity saved = consultationRepository.save(consultation);
        return enrichConsultationDto(saved);
    }

    @Override
    @Transactional
    public MarketplaceVerificationDto submitVerification(SubmitVerificationRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceProfileEntity profile = profileRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> initializeDefaultProfile(organizationId));

        MarketplaceVerificationEntity verification = MarketplaceVerificationEntity.builder()
                .organizationId(organizationId)
                .marketplaceProfileId(profile.getId())
                .professionalBody(request.getProfessionalBody().toUpperCase().trim())
                .membershipNumber(request.getMembershipNumber().trim())
                .copNumber(request.getCopNumber())
                .firmRegistrationNumber(request.getFirmRegistrationNumber())
                .documentUrl(request.getDocumentUrl())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        MarketplaceVerificationEntity saved = verificationRepository.save(verification);
        profile.setVerificationStatus(VerificationStatus.PENDING);
        profileRepository.save(profile);

        auditService.logEvent("MARKETPLACE_KYC_SUBMITTED", "MARKETPLACE_VERIFICATION", saved.getId().toString(), null, "Submitted KYC credentials for " + profile.getDisplayName());

        return enrichVerificationDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MarketplaceVerificationDto getMyVerificationStatus() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        MarketplaceVerificationEntity verification = verificationRepository.findTopByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .orElse(null);
        return verification != null ? enrichVerificationDto(verification) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public MarketplaceStatsDto getMyPracticeMarketplaceStats() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        long totalLeads = leadRepository.countByOrganizationId(organizationId);
        long converted = leadRepository.countByOrganizationIdAndLeadStatus(organizationId, LeadStatus.CONVERTED);
        long consultations = consultationRepository.countByOrganizationIdAndConsultationStatus(organizationId, ConsultationStatus.SCHEDULED)
                + consultationRepository.countByOrganizationIdAndConsultationStatus(organizationId, ConsultationStatus.COMPLETED);

        double rate = totalLeads > 0 ? ((double) converted / totalLeads) * 100.0 : 0.0;
        BigDecimal pipelineValue = BigDecimal.valueOf(totalLeads * 5000L);

        return MarketplaceStatsDto.builder()
                .totalInboundLeads(totalLeads)
                .totalConvertedClients(converted)
                .leadConversionRate(Math.round(rate * 10.0) / 10.0)
                .totalConsultationsBooked(consultations)
                .estimatedMarketplacePipelineValue(pipelineValue)
                .build();
    }

    // =========================================================================
    // 3. Platform Super Admin APIs
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MarketplaceVerificationDto> getPendingVerifications(Pageable pageable) {
        Page<MarketplaceVerificationEntity> page = verificationRepository.findByVerificationStatus(VerificationStatus.PENDING, pageable);
        return PagedResponse.of(page, this::enrichVerificationDto);
    }

    @Override
    @Transactional
    public MarketplaceVerificationDto processVerification(UUID verificationId, VerifyPractitionerRequest request) {
        MarketplaceVerificationEntity verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Verification", "id", verificationId));

        verification.setVerificationStatus(request.getVerificationStatus());
        verification.setRejectionReason(request.getRejectionReason());
        verification.setVerifiedAt(Instant.now());
        verification.setVerifiedBy("SuperAdmin");

        MarketplaceVerificationEntity saved = verificationRepository.save(verification);

        // Update profile status
        profileRepository.findById(verification.getMarketplaceProfileId()).ifPresent(p -> {
            p.setVerificationStatus(request.getVerificationStatus());
            profileRepository.save(p);
        });

        auditService.logEvent("MARKETPLACE_KYC_PROCESSED", "MARKETPLACE_VERIFICATION", saved.getId().toString(), null, "Verification status set to " + request.getVerificationStatus());

        return enrichVerificationDto(saved);
    }

    @Override
    @Transactional
    public PublicMarketplaceProfileDto toggleFeaturedStatus(UUID profileId, boolean isFeatured) {
        MarketplaceProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "id", profileId));
        profile.setIsFeatured(isFeatured);
        return enrichPublicProfile(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public PublicMarketplaceProfileDto togglePublishStatus(UUID profileId, boolean isPublished) {
        MarketplaceProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace Profile", "id", profileId));
        if (isPublished) {
            validatePublishingEligibility(profile);
        }
        profile.setIsPublished(isPublished);
        return enrichPublicProfile(profileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public MarketplaceStatsDto getPlatformMarketplaceStats() {
        long totalListed = profileRepository.countByIsPublishedTrue();
        long totalVerified = profileRepository.countByVerificationStatus(VerificationStatus.VERIFIED);
        long pendingVerifications = profileRepository.countByVerificationStatus(VerificationStatus.PENDING);
        long totalLeads = leadRepository.count();
        long converted = leadRepository.findAll().stream().filter(l -> l.getLeadStatus() == LeadStatus.CONVERTED).count();
        long consultations = consultationRepository.count();

        double rate = totalLeads > 0 ? ((double) converted / totalLeads) * 100.0 : 0.0;

        return MarketplaceStatsDto.builder()
                .totalListedPractitioners(totalListed)
                .totalVerifiedPractitioners(totalVerified)
                .totalPendingVerifications(pendingVerifications)
                .totalInboundLeads(totalLeads)
                .totalConvertedClients(converted)
                .leadConversionRate(Math.round(rate * 10.0) / 10.0)
                .totalConsultationsBooked(consultations)
                .estimatedMarketplacePipelineValue(BigDecimal.valueOf(totalLeads * 5000L))
                .build();
    }

    @Override
    @Transactional
    public List<PublicMarketplaceProfileDto> seedDemoMarketplaceData() {
        UUID organizationId;
        try {
            organizationId = SecurityUtils.getCurrentOrganizationId();
        } catch (Exception e) {
            organizationId = null;
        }

        if (organizationId == null) {
            organizationId = organizationRepository.findAll().stream().findFirst().map(OrganizationEntity::getId).orElseGet(() -> {
                OrganizationEntity org = OrganizationEntity.builder()
                        .name("Apex Corporate & Tax Advisors")
                        .legalName("Apex Corporate & Tax Advisors LLP")
                        .email("admin@apextax.com")
                        .phone("+91 98201 12233")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .pincode("400001")
                        .pan("AABCA1234F")
                        .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                        .build();
                return organizationRepository.save(org).getId();
            });
        }
        final UUID targetOrgId = organizationId;
        List<PublicMarketplaceProfileDto> seeded = new ArrayList<>();

        record DemoFirm(
                String slug, String displayName, String headline, String bio,
                ProfessionalType profType, int exp, String city, String state,
                String spec, BigDecimal startFee, BigDecimal hourly, BigDecimal avgRate,
                int reviews, int clients, String phone, String email
        ) {}

        List<DemoFirm> firms = List.of(
                new DemoFirm(
                        "apex-tax-solutions", "Apex Corporate & Tax Advisors",
                        "Leading Chartered Accountancy Firm for Enterprise GST, TDS & Statutory Audit",
                        "With over 12 years of hands-on experience, Apex Advisors helps startups and mid-market enterprises streamline Indian tax compliance, cross-border M&A, and direct taxation.",
                        ProfessionalType.CHARTERED_ACCOUNTANT, 12, "Mumbai", "Maharashtra",
                        "GST_FILING, ITR_FILING, TDS_COMPLIANCE, AUDIT_ASSURANCE, TRANSFER_PRICING",
                        new BigDecimal("999.00"), new BigDecimal("2500.00"), new BigDecimal("4.95"), 48, 320, "+91 98201 12233", "contact@apextax.in"
                ),
                new DemoFirm(
                        "delhi-fintech-advisors", "Delhi FinTax & Legal LLP",
                        "Startup Company Formation, Angel Tax Advisory & ITR Filings",
                        "We specialize in technology companies, DPIIT startup recognition, seed funding documentation, TDS reconciliations, and Form 16 / 26Q management.",
                        ProfessionalType.CHARTERED_ACCOUNTANT, 8, "New Delhi", "Delhi",
                        "COMPANY_INCORPORATION, ITR_FILING, STARTUP_ADVISORY, TDS_COMPLIANCE",
                        new BigDecimal("1499.00"), new BigDecimal("2000.00"), new BigDecimal("4.88"), 36, 210, "+91 98112 23344", "delhi@fintax.in"
                ),
                new DemoFirm(
                        "bengaluru-corporate-cs", "Zenith Secretarial & Corporate Advisors",
                        "Fellow Company Secretaries specializing in RoC Compliance, RBI FDI & SEBI",
                        "Trusted legal and secretarial partner for fast-growing Indian tech ventures. Comprehensive corporate governance, board resolutions, and ESOP schemes.",
                        ProfessionalType.COMPANY_SECRETARY, 10, "Bengaluru", "Karnataka",
                        "COMPANY_INCORPORATION, ROC_COMPLIANCE, STARTUP_ADVISORY, TRADEMARK_IP",
                        new BigDecimal("1999.00"), new BigDecimal("1800.00"), new BigDecimal("4.92"), 29, 185, "+91 98445 56677", "info@zenithcs.com"
                ),
                new DemoFirm(
                        "pune-indirect-tax-hub", "Kulkarni & Associates Tax Advocates",
                        "GST Litigation, Appellate Tribunals & High Court Tax Matters",
                        "Senior advocates with 20+ years in Maharashtra commercial tax, GST departmental summons, show-cause notices (SCN), and search/seizure appeals.",
                        ProfessionalType.TAX_ADVOCATE, 20, "Pune", "Maharashtra",
                        "GST_LITIGATION, TAX_DISPUTES, APPEALS, ADVANCE_RULINGS",
                        new BigDecimal("2999.00"), new BigDecimal("3500.00"), new BigDecimal("4.97"), 54, 450, "+91 98223 34455", "advocate@kulkarnitax.in"
                )
        );

        for (DemoFirm f : firms) {
            MarketplaceProfileEntity profile = profileRepository.findBySlug(f.slug())
                    .orElseGet(() -> {
                        MarketplaceProfileEntity p = MarketplaceProfileEntity.builder()
                                .organizationId(targetOrgId)
                                .slug(f.slug())
                                .displayName(f.displayName())
                                .headline(f.headline())
                                .bio(f.bio())
                                .professionalType(f.profType())
                                .experienceYears(f.exp())
                                .city(f.city())
                                .state(f.state())
                                .pincode("400001")
                                .specializations(f.spec())
                                .startingFee(f.startFee())
                                .hourlyRate(f.hourly())
                                .averageRating(f.avgRate())
                                .totalReviews(f.reviews())
                                .totalClientsServed(f.clients())
                                .verificationStatus(VerificationStatus.VERIFIED)
                                .isPublished(true)
                                .isFeatured(true)
                                .consultationEnabled(true)
                                .consultationFee(new BigDecimal("499.00"))
                                .consultationDurationMinutes(30)
                                .phone(f.phone())
                                .email(f.email())
                                .build();
                        return profileRepository.save(p);
                    });

            // Seed services for this firm
            if (serviceRepository.findByMarketplaceProfileIdAndIsActiveTrue(profile.getId()).isEmpty()) {
                serviceRepository.save(MarketplaceServiceEntity.builder()
                        .organizationId(targetOrgId)
                        .marketplaceProfileId(profile.getId())
                        .title("Comprehensive ITR Filing (Salary + Capital Gains)")
                        .category("INCOME_TAX")
                        .description("End-to-end AIS/TIS reconciliation, capital gain computation, foreign asset reporting, and e-verification.")
                        .price(new BigDecimal("1499.00"))
                        .pricingType(MarketplaceServiceEntity.PricingType.FIXED)
                        .deliveryDays(2)
                        .deliverables("ITR Acknowledgement (ITR-V), Computation Sheet, AIS Verification")
                        .isActive(true)
                        .build());

                serviceRepository.save(MarketplaceServiceEntity.builder()
                        .organizationId(targetOrgId)
                        .marketplaceProfileId(profile.getId())
                        .title("Monthly GST Compliance & ITC 2B Matching")
                        .category("GST")
                        .description("Monthly GSTR-1, GSTR-3B filing, 2B vs Purchase Register automated mismatch reconciliation.")
                        .price(new BigDecimal("2999.00"))
                        .pricingType(MarketplaceServiceEntity.PricingType.MONTHLY_RETAINER)
                        .deliveryDays(3)
                        .deliverables("GSTR-3B ARN, GSTR-1 ARN, ITC Optimization Report")
                        .isActive(true)
                        .build());
            }

            // Seed sample reviews
            if (reviewRepository.findByMarketplaceProfileIdAndStatusOrderByCreatedAtDesc(profile.getId(), MarketplaceReviewEntity.ReviewStatus.APPROVED).isEmpty()) {
                reviewRepository.save(MarketplaceReviewEntity.builder()
                        .organizationId(targetOrgId)
                        .marketplaceProfileId(profile.getId())
                        .reviewerName("Vikram Singhania")
                        .reviewerDesignation("Founder & CEO")
                        .reviewerCompany("ScaleTech Solutions")
                        .rating(5)
                        .reviewTitle("Saved us over ₹4.5 Lakhs in GST ITC mismatches!")
                        .reviewComment("Remarkable turnaround time and deep tax domain expertise. The practice digitized our entire filing process seamlessly.")
                        .serviceTaken("GST Compliance")
                        .isVerifiedClient(true)
                        .status(MarketplaceReviewEntity.ReviewStatus.APPROVED)
                        .build());
            }

            seeded.add(enrichPublicProfile(profile));
        }

        return seeded;
    }

    // =========================================================================
    // Helpers & Enrichers
    // =========================================================================

    private MarketplaceProfileEntity initializeDefaultProfile(UUID organizationId) {
        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        String uniqueSlug = slugGenerator.generateUniqueSlug(org.getName(), null);

        MarketplaceProfileEntity profile = MarketplaceProfileEntity.builder()
                .organizationId(organizationId)
                .slug(uniqueSlug)
                .displayName(org.getName())
                .headline("Expert Chartered Accountants & Tax Consultants")
                .bio("Professional tax practice offering GST, Income Tax, TDS, and corporate compliance services.")
                .professionalType(ProfessionalType.CHARTERED_ACCOUNTANT)
                .city(org.getCity() != null ? org.getCity() : "Mumbai")
                .state(org.getState() != null ? org.getState() : "Maharashtra")
                .pincode(org.getPincode() != null ? org.getPincode() : "400001")
                .address(org.getAddress())
                .phone(org.getPhone())
                .email(org.getEmail())
                .specializations("GST_FILING, ITR_FILING, TDS_COMPLIANCE, AUDIT_ASSURANCE")
                .startingFee(new BigDecimal("999.00"))
                .hourlyRate(new BigDecimal("1500.00"))
                .averageRating(new BigDecimal("4.90"))
                .totalReviews(0)
                .totalClientsServed(0)
                .verificationStatus(VerificationStatus.NOT_SUBMITTED)
                .visibilityStatus(VisibilityStatus.PRIVATE)
                .isPublished(false)
                .isFeatured(false)
                .consultationEnabled(true)
                .consultationFee(new BigDecimal("499.00"))
                .consultationDurationMinutes(30)
                .build();

        return profileRepository.save(profile);
    }

    private PublicMarketplaceProfileDto enrichPublicProfile(MarketplaceProfileEntity entity) {
        PublicMarketplaceProfileDto dto = mapper.toProfileDto(entity);

        dto.setPublicSlug(entity.getSlug());
        dto.setDescription(entity.getBio());
        dto.setWebsite(entity.getWebsiteUrl());

        List<MarketplaceServiceEntity> services = serviceRepository.findByMarketplaceProfileIdAndIsActiveTrue(entity.getId());
        dto.setServices(mapper.toServiceDtoList(services));

        List<MarketplaceReviewEntity> reviews = reviewRepository.findByMarketplaceProfileIdAndStatusOrderByCreatedAtDesc(
                entity.getId(), MarketplaceReviewEntity.ReviewStatus.APPROVED
        );
        dto.setRecentReviews(mapper.toReviewDtoList(reviews));
        dto.setVisibilityStatus(entity.getVisibilityStatus());

        // Calculate Profile Completeness (percentage, completedItems, missingItems) via modular calculator
        OrganizationEntity org = organizationRepository.findById(entity.getOrganizationId()).orElse(null);
        ProfileCompletenessDto completeness = completenessCalculator != null
                ? completenessCalculator.calculate(entity, org)
                : null;
        if (completeness != null) {
            dto.setCompleteness(completeness);
            dto.setProfileCompleteness(completeness);
            dto.setCompletenessScore(completeness.getPercentage());
            dto.setMissingCompletenessFields(completeness.getMissingItems());
        } else {
            ProfileCompletenessDto emptyCompleteness = ProfileCompletenessDto.builder()
                    .percentage(0)
                    .completedItems(Collections.emptyList())
                    .missingItems(Collections.emptyList())
                    .build();
            dto.setCompleteness(emptyCompleteness);
            dto.setProfileCompleteness(emptyCompleteness);
            dto.setCompletenessScore(0);
            dto.setMissingCompletenessFields(Collections.emptyList());
        }

        return dto;
    }

    private MarketplaceLeadDto enrichLeadDto(MarketplaceLeadEntity entity) {
        MarketplaceLeadDto dto = mapper.toLeadDto(entity);

        if (entity.getServiceId() != null) {
            serviceRepository.findById(entity.getServiceId())
                    .ifPresent(s -> dto.setServiceTitle(s.getTitle()));
        }
        if (entity.getConvertedClientId() != null) {
            clientRepository.findById(entity.getConvertedClientId())
                    .ifPresent(c -> dto.setConvertedClientName(c.getDisplayName() != null ? c.getDisplayName() : c.getLegalName()));
        }
        if (entity.getAssignedEmployeeId() != null) {
            employeeRepository.findById(entity.getAssignedEmployeeId())
                    .ifPresent(e -> dto.setAssignedEmployeeName(e.getFullName()));
        }
        return dto;
    }

    private MarketplaceConsultationDto enrichConsultationDto(MarketplaceConsultationEntity entity) {
        MarketplaceConsultationDto dto = mapper.toConsultationDto(entity);

        profileRepository.findById(entity.getMarketplaceProfileId())
                .ifPresent(p -> dto.setPracticeDisplayName(p.getDisplayName()));

        if (entity.getAssignedEmployeeId() != null) {
            employeeRepository.findById(entity.getAssignedEmployeeId())
                    .ifPresent(e -> dto.setAssignedEmployeeName(e.getFullName()));
        }
        return dto;
    }

    private MarketplaceVerificationDto enrichVerificationDto(MarketplaceVerificationEntity entity) {
        MarketplaceVerificationDto dto = mapper.toVerificationDto(entity);

        organizationRepository.findById(entity.getOrganizationId())
                .ifPresent(org -> dto.setOrganizationName(org.getName()));

        return dto;
    }

    private TaskEntity.TaskCategory resolveTaskCategory(String serviceCategory) {
        if (serviceCategory == null) return TaskEntity.TaskCategory.OTHER;
        String cat = serviceCategory.toUpperCase();
        if (cat.contains("GST")) return TaskEntity.TaskCategory.GST;
        if (cat.contains("ITR") || cat.contains("INCOME")) return TaskEntity.TaskCategory.ITR;
        if (cat.contains("TDS")) return TaskEntity.TaskCategory.TDS;
        if (cat.contains("AUDIT")) return TaskEntity.TaskCategory.AUDIT;
        if (cat.contains("ROC") || cat.contains("COMPANY")) return TaskEntity.TaskCategory.COMPLIANCE;
        return TaskEntity.TaskCategory.OTHER;
    }

    private void validatePublishingEligibility(MarketplaceProfileEntity profile) {
        List<String> missingFields = new ArrayList<>();
        if (!StringUtils.hasText(profile.getDisplayName())) {
            missingFields.add("Firm / Display Name");
        }
        if (!StringUtils.hasText(profile.getSlug())) {
            missingFields.add("Public Slug");
        }
        if (!StringUtils.hasText(profile.getCity())) {
            missingFields.add("City");
        }
        if (!StringUtils.hasText(profile.getState())) {
            missingFields.add("State");
        }
        if (!StringUtils.hasText(profile.getEmail()) && !StringUtils.hasText(profile.getPhone())) {
            missingFields.add("Contact Phone or Email");
        }
        if (profile.getProfessionalType() == null) {
            missingFields.add("Professional Designation");
        }

        if (!missingFields.isEmpty()) {
            throw new BusinessValidationException("Cannot publish profile to Marketplace. Minimum required fields: " + String.join(", ", missingFields));
        }
    }
}
