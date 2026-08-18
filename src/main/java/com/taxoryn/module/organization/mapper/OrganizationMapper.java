package com.taxoryn.module.organization.mapper;

import com.taxoryn.module.organization.dto.OrganizationDto;
import com.taxoryn.module.organization.dto.OrganizationSettingsDto;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationSettingsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizationMapper {

    OrganizationDto toDto(OrganizationEntity entity);

    OrganizationSettingsDto toSettingsDto(OrganizationSettingsEntity entity);

    List<OrganizationDto> toDtoList(List<OrganizationEntity> entities);
}
