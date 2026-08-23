package com.taxoryn.module.marketplace.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.marketplace.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxServiceMasterService {

    // --- Admin Category Operations ---
    List<TaxServiceCategoryDto> getAllCategories();
    TaxServiceCategoryDto getCategoryById(UUID id);
    TaxServiceCategoryDto createCategory(CreateTaxServiceCategoryRequest request);
    TaxServiceCategoryDto updateCategory(UUID id, UpdateTaxServiceCategoryRequest request);
    TaxServiceCategoryDto toggleCategoryStatus(UUID id, boolean isActive);

    // --- Admin Service Operations ---
    PagedResponse<TaxServiceDto> getTaxServices(UUID categoryId, Boolean isActive, String search, Pageable pageable);
    TaxServiceDto getTaxServiceById(UUID id);
    TaxServiceDto createTaxService(CreateTaxServiceRequest request);
    TaxServiceDto updateTaxService(UUID id, UpdateTaxServiceRequest request);
    TaxServiceDto toggleTaxServiceStatus(UUID id, boolean isActive);

    // --- Admin Alias Operations ---
    List<TaxServiceAliasDto> getAliasesForService(UUID taxServiceId);
    TaxServiceAliasDto addAlias(UUID taxServiceId, CreateTaxServiceAliasRequest request);
    void deleteAlias(UUID taxServiceId, UUID aliasId);

    // --- Public Discovery & Normalization ---
    List<PublicTaxServiceCategoryDto> getPublicCategoriesWithServices();
    List<PublicTaxServiceDto> getPublicActiveServices();
    Optional<PublicTaxServiceDto> resolveQueryToService(String query);

    // --- Practice Service Selection ---
    List<PracticeServiceDto> getMyPracticeServices();
    List<PracticeServiceDto> updateMyPracticeServices(UpdatePracticeServicesRequest request);
    PracticeServiceDto addServiceToPractice(UUID taxServiceId);
    void removeServiceFromPractice(UUID taxServiceId);
    List<PublicTaxServiceDto> getPublicPracticeOfferedServices(UUID marketplaceProfileId);
}
