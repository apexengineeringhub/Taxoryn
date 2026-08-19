package com.taxoryn.module.compliance.mapper;

import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceRuleDto;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ComplianceMapper {

    ComplianceRuleDto toRuleDto(ComplianceRuleEntity entity);

    List<ComplianceRuleDto> toRuleDtoList(List<ComplianceRuleEntity> entities);

    ComplianceObligationDto toObligationDto(ComplianceObligationEntity entity);

    List<ComplianceObligationDto> toObligationDtoList(List<ComplianceObligationEntity> entities);
}
