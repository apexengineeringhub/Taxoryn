package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.mapper.CustomerTaxRequirementMapper;
import com.taxoryn.module.marketplace.repository.CustomerTaxRequirementRepository;
import com.taxoryn.module.marketplace.repository.MarketplaceCustomerProfileRepository;
import com.taxoryn.module.marketplace.repository.TaxServiceRepository;
import com.taxoryn.module.marketplace.util.FinancialYearUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerTaxRequirementServiceImpl implements CustomerTaxRequirementService {

    private final CustomerTaxRequirementRepository requirementRepository;
    private final MarketplaceCustomerProfileRepository customerProfileRepository;
    private final TaxServiceRepository taxServiceRepository;
    private final CustomerTaxRequirementMapper mapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public CustomerTaxRequirementDto createRequirement(CreateTaxRequirementRequest request) {
        MarketplaceCustomerProfileEntity customer = getAuthenticatedCustomerProfile();
        TaxServiceEntity service = resolveAndValidateTaxService(request.getTaxServiceId(), request.getTaxServiceCode());
        String normalizedFy = validateAndNormalizeFinancialYear(request.getFinancialYear());
        String sanitizedDesc = sanitizeDescription(request.getDescription());

        // Check for duplicate active requirement (DRAFT or SUBMITTED) for same customer + service + FY
        if (requirementRepository.existsByCustomerIdAndTaxServiceIdAndFinancialYearAndStatusIn(
                customer.getId(),
                service.getId(),
                normalizedFy,
                Set.of(TaxRequirementStatus.DRAFT, TaxRequirementStatus.SUBMITTED)
        )) {
            String fyDisplay = normalizedFy != null ? FinancialYearUtils.toDisplayString(normalizedFy) : "current period";
            throw new DuplicateResourceException(String.format(
                    "An active tax requirement already exists for '%s' (%s). Please continue your existing draft or view your submitted requirement.",
                    service.getName(), fyDisplay
            ));
        }

        CustomerTaxRequirementEntity entity = CustomerTaxRequirementEntity.builder()
                .customerId(customer.getId())
                .customerProfile(customer)
                .taxServiceId(service.getId())
                .taxService(service)
                .status(TaxRequirementStatus.DRAFT)
                .customerType(request.getCustomerType())
                .financialYear(normalizedFy)
                .description(sanitizedDesc)
                .city(StringUtils.hasText(request.getCity()) ? request.getCity().trim() : customer.getCity())
                .state(StringUtils.hasText(request.getState()) ? request.getState().trim() : customer.getState())
                .pincode(StringUtils.hasText(request.getPincode()) ? request.getPincode().trim() : customer.getPincode())
                .searchRadiusKm(request.getSearchRadiusKm())
                .build();

        CustomerTaxRequirementEntity saved = requirementRepository.save(entity);

        // Safe operational audit logging without leaking private description
        auditService.logEvent(
                "CUSTOMER_REQUIREMENT_CREATED",
                "CUSTOMER_TAX_REQUIREMENT",
                saved.getId().toString(),
                null,
                String.format("Customer %s created draft tax requirement for service %s (FY: %s)",
                        customer.getId(), service.getCode(), normalizedFy != null ? normalizedFy : "N/A")
        );

        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CustomerTaxRequirementSummaryDto> getMyRequirements(Pageable pageable, TaxRequirementStatus status) {
        MarketplaceCustomerProfileEntity customer = getAuthenticatedCustomerProfile();
        Page<CustomerTaxRequirementEntity> page;

        if (status != null) {
            page = requirementRepository.findByCustomerIdAndStatus(customer.getId(), status, pageable);
        } else {
            page = requirementRepository.findByCustomerId(customer.getId(), pageable);
        }

        return PagedResponse.of(page, mapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerTaxRequirementDto getRequirementById(UUID id) {
        MarketplaceCustomerProfileEntity customer = getAuthenticatedCustomerProfile();
        CustomerTaxRequirementEntity entity = requirementRepository.findByIdAndCustomerId(id, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer Tax Requirement", "id", id));
        return mapper.toDto(entity);
    }

    @Override
    @Transactional
    public CustomerTaxRequirementDto updateRequirement(UUID id, UpdateTaxRequirementRequest request) {
        MarketplaceCustomerProfileEntity customer = getAuthenticatedCustomerProfile();
        CustomerTaxRequirementEntity entity = requirementRepository.findByIdAndCustomerId(id, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer Tax Requirement", "id", id));

        if (!entity.getStatus().isEditable()) {
            throw new BadRequestException(String.format(
                    "Only requirements in DRAFT status can be modified. Current status: %s", entity.getStatus()
            ));
        }

        if (request.getTaxServiceId() != null || StringUtils.hasText(request.getTaxServiceCode())) {
            TaxServiceEntity service = resolveAndValidateTaxService(request.getTaxServiceId(), request.getTaxServiceCode());
            entity.setTaxServiceId(service.getId());
            entity.setTaxService(service);
        }

        if (request.getCustomerType() != null) {
            entity.setCustomerType(request.getCustomerType());
        }

        if (request.getFinancialYear() != null) {
            String normalizedFy = validateAndNormalizeFinancialYear(request.getFinancialYear());
            entity.setFinancialYear(normalizedFy);
        }

        if (request.getDescription() != null) {
            entity.setDescription(sanitizeDescription(request.getDescription()));
        }

        if (request.getCity() != null) {
            entity.setCity(request.getCity().trim());
        }
        if (request.getState() != null) {
            entity.setState(request.getState().trim());
        }
        if (request.getPincode() != null) {
            entity.setPincode(request.getPincode().trim());
        }
        if (request.getSearchRadiusKm() != null) {
            entity.setSearchRadiusKm(request.getSearchRadiusKm());
        }

        CustomerTaxRequirementEntity saved = requirementRepository.save(entity);

        auditService.logEvent(
                "CUSTOMER_REQUIREMENT_UPDATED",
                "CUSTOMER_TAX_REQUIREMENT",
                saved.getId().toString(),
                null,
                String.format("Customer %s updated draft tax requirement %s", customer.getId(), saved.getId())
        );

        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public CustomerTaxRequirementDto submitRequirement(UUID id) {
        MarketplaceCustomerProfileEntity customer = getAuthenticatedCustomerProfile();
        CustomerTaxRequirementEntity entity = requirementRepository.findByIdAndCustomerId(id, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer Tax Requirement", "id", id));

        if (entity.getStatus() == TaxRequirementStatus.SUBMITTED) {
            throw new BadRequestException("Requirement is already submitted");
        }
        if (!entity.getStatus().isEditable()) {
            throw new BadRequestException(String.format("Cannot submit a %s requirement", entity.getStatus()));
        }

        // Validate service remains active at time of submission
        if (entity.getTaxService() != null && !Boolean.TRUE.equals(entity.getTaxService().getIsActive())) {
            throw new BadRequestException("Cannot submit requirement: Selected tax service is currently inactive");
        }

        entity.setStatus(TaxRequirementStatus.SUBMITTED);
        CustomerTaxRequirementEntity saved = requirementRepository.save(entity);

        auditService.logEvent(
                "CUSTOMER_REQUIREMENT_SUBMITTED",
                "CUSTOMER_TAX_REQUIREMENT",
                saved.getId().toString(),
                null,
                String.format("Customer %s submitted tax requirement %s (Service: %s)",
                        customer.getId(), saved.getId(), entity.getTaxService() != null ? entity.getTaxService().getCode() : "N/A")
        );

        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public CustomerTaxRequirementDto cancelRequirement(UUID id) {
        MarketplaceCustomerProfileEntity customer = getAuthenticatedCustomerProfile();
        CustomerTaxRequirementEntity entity = requirementRepository.findByIdAndCustomerId(id, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer Tax Requirement", "id", id));

        if (entity.getStatus() == TaxRequirementStatus.CANCELLED) {
            throw new BadRequestException("Requirement is already cancelled");
        }
        if (entity.getStatus() == TaxRequirementStatus.CLOSED) {
            throw new BadRequestException("Cannot cancel a closed requirement");
        }

        entity.setStatus(TaxRequirementStatus.CANCELLED);
        CustomerTaxRequirementEntity saved = requirementRepository.save(entity);

        auditService.logEvent(
                "CUSTOMER_REQUIREMENT_CANCELLED",
                "CUSTOMER_TAX_REQUIREMENT",
                saved.getId().toString(),
                null,
                String.format("Customer %s cancelled tax requirement %s", customer.getId(), saved.getId())
        );

        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialYearOptionDto> getAvailableFinancialYears() {
        String currentFy = FinancialYearUtils.getCurrentFinancialYear();
        List<String> years = FinancialYearUtils.getStandardFinancialYears();

        return years.stream()
                .map(fy -> FinancialYearOptionDto.builder()
                        .code(fy)
                        .label(FinancialYearUtils.toDisplayString(fy))
                        .isCurrent(fy.equals(currentFy))
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerTaxRequirementSummaryDto> getRecentRequirements(int limit) {
        MarketplaceCustomerProfileEntity customer = getAuthenticatedCustomerProfile();
        List<CustomerTaxRequirementEntity> recent = requirementRepository.findRecentByCustomerId(
                customer.getId(),
                PageRequest.of(0, Math.max(1, limit))
        );
        return mapper.toSummaryDtoList(recent);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyRequirements() {
        MarketplaceCustomerProfileEntity customer = getAuthenticatedCustomerProfile();
        return requirementRepository.countByCustomerId(customer.getId());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private MarketplaceCustomerProfileEntity getAuthenticatedCustomerProfile() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceCustomerProfile", "userId", userId));
    }

    private TaxServiceEntity resolveAndValidateTaxService(UUID taxServiceId, String taxServiceCode) {
        TaxServiceEntity service = null;

        if (taxServiceId != null) {
            service = taxServiceRepository.findById(taxServiceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tax Service", "id", taxServiceId));
        } else if (StringUtils.hasText(taxServiceCode)) {
            service = taxServiceRepository.findByCodeIgnoreCase(taxServiceCode.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Tax Service", "code", taxServiceCode));
        } else {
            throw new BadRequestException("Either taxServiceId or taxServiceCode must be provided");
        }

        if (!Boolean.TRUE.equals(service.getIsActive())) {
            throw new BadRequestException(String.format("Cannot select inactive tax service: %s (%s)", service.getName(), service.getCode()));
        }

        return service;
    }

    private String validateAndNormalizeFinancialYear(String fyInput) {
        if (!StringUtils.hasText(fyInput)) {
            return null;
        }
        if (!FinancialYearUtils.isValid(fyInput)) {
            throw new BadRequestException(String.format(
                    "Invalid Financial Year format: '%s'. Expected format e.g. 'FY 2025-26' or '2025-26'", fyInput
            ));
        }
        return FinancialYearUtils.normalize(fyInput);
    }

    private String sanitizeDescription(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        // Strip HTML tags and normalize whitespace to prevent XSS
        String stripped = input.replaceAll("<[^>]*>", "").trim();
        if (stripped.length() > 2000) {
            throw new BadRequestException("Requirement description exceeds maximum limit of 2000 characters");
        }
        return stripped;
    }
}
