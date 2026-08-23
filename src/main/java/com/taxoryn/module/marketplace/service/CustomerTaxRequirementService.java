package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.TaxRequirementStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CustomerTaxRequirementService {

    CustomerTaxRequirementDto createRequirement(CreateTaxRequirementRequest request);

    PagedResponse<CustomerTaxRequirementSummaryDto> getMyRequirements(Pageable pageable, TaxRequirementStatus status);

    CustomerTaxRequirementDto getRequirementById(UUID id);

    CustomerTaxRequirementDto updateRequirement(UUID id, UpdateTaxRequirementRequest request);

    CustomerTaxRequirementDto submitRequirement(UUID id);

    CustomerTaxRequirementDto cancelRequirement(UUID id);

    List<FinancialYearOptionDto> getAvailableFinancialYears();

    List<CustomerTaxRequirementSummaryDto> getRecentRequirements(int limit);

    long countMyRequirements();
}
