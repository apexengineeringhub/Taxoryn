package com.taxoryn.module.marketplace.mapper;

import com.taxoryn.module.marketplace.dto.CustomerTaxRequirementDto;
import com.taxoryn.module.marketplace.dto.CustomerTaxRequirementSummaryDto;
import com.taxoryn.module.marketplace.entity.CustomerTaxRequirementEntity;
import com.taxoryn.module.marketplace.util.FinancialYearUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {TaxServiceMapper.class})
public interface CustomerTaxRequirementMapper {

    @Mapping(target = "service", source = "taxService")
    @Mapping(target = "statusDisplayName", expression = "java(entity.getStatus() != null ? entity.getStatus().getDisplayName() : null)")
    @Mapping(target = "customerTypeDisplayName", expression = "java(entity.getCustomerType() != null ? entity.getCustomerType().getDisplayName() : null)")
    @Mapping(target = "financialYearDisplay", source = "financialYear", qualifiedByName = "fyToDisplay")
    @Mapping(target = "editable", expression = "java(entity.getStatus() != null && entity.getStatus().isEditable())")
    @Mapping(target = "cancellable", expression = "java(entity.getStatus() != null && entity.getStatus().isCancellable())")
    CustomerTaxRequirementDto toDto(CustomerTaxRequirementEntity entity);

    @Mapping(target = "taxServiceId", source = "taxService.id")
    @Mapping(target = "taxServiceCode", source = "taxService.code")
    @Mapping(target = "taxServiceName", source = "taxService.name")
    @Mapping(target = "categoryName", source = "taxService.category.name")
    @Mapping(target = "statusDisplayName", expression = "java(entity.getStatus() != null ? entity.getStatus().getDisplayName() : null)")
    @Mapping(target = "customerTypeDisplayName", expression = "java(entity.getCustomerType() != null ? entity.getCustomerType().getDisplayName() : null)")
    @Mapping(target = "financialYearDisplay", source = "financialYear", qualifiedByName = "fyToDisplay")
    @Mapping(target = "editable", expression = "java(entity.getStatus() != null && entity.getStatus().isEditable())")
    @Mapping(target = "cancellable", expression = "java(entity.getStatus() != null && entity.getStatus().isCancellable())")
    CustomerTaxRequirementSummaryDto toSummaryDto(CustomerTaxRequirementEntity entity);

    List<CustomerTaxRequirementSummaryDto> toSummaryDtoList(List<CustomerTaxRequirementEntity> entities);

    @Named("fyToDisplay")
    default String fyToDisplay(String fy) {
        return FinancialYearUtils.toDisplayString(fy);
    }
}
