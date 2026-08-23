package com.taxoryn.module.marketplace.mapper;

import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaxServiceMapper {

    TaxServiceCategoryDto toCategoryDto(TaxServiceCategoryEntity entity);

    List<TaxServiceCategoryDto> toCategoryDtoList(List<TaxServiceCategoryEntity> entities);

    PublicTaxServiceCategoryDto toPublicCategoryDto(TaxServiceCategoryEntity entity);

    List<PublicTaxServiceCategoryDto> toPublicCategoryDtoList(List<TaxServiceCategoryEntity> entities);

    @Mapping(target = "categoryCode", source = "category.code")
    @Mapping(target = "categoryName", source = "category.name")
    TaxServiceDto toTaxServiceDto(TaxServiceEntity entity);

    List<TaxServiceDto> toTaxServiceDtoList(List<TaxServiceEntity> entities);

    @Mapping(target = "category", source = "category.code")
    @Mapping(target = "categoryName", source = "category.name")
    PublicTaxServiceDto toPublicTaxServiceDto(TaxServiceEntity entity);

    List<PublicTaxServiceDto> toPublicTaxServiceDtoList(List<TaxServiceEntity> entities);

    TaxServiceAliasDto toAliasDto(TaxServiceAliasEntity entity);

    List<TaxServiceAliasDto> toAliasDtoList(List<TaxServiceAliasEntity> entities);

    @Mapping(target = "taxServiceCode", source = "taxService.code")
    @Mapping(target = "taxServiceName", source = "taxService.name")
    @Mapping(target = "categoryCode", source = "taxService.category.code")
    @Mapping(target = "categoryName", source = "taxService.category.name")
    @Mapping(target = "description", source = "taxService.description")
    PracticeServiceDto toPracticeServiceDto(PracticeServiceEntity entity);

    List<PracticeServiceDto> toPracticeServiceDtoList(List<PracticeServiceEntity> entities);
}
