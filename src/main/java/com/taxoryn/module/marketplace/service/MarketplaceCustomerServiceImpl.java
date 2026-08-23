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
    private final MarketplaceProfileRepository practiceProfileRepository;
    private final CustomerTaxRequirementRepository requirementRepository;
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
