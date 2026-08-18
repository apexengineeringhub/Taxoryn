package com.taxoryn.module.role.mapper;

import com.taxoryn.module.role.dto.PermissionDto;
import com.taxoryn.module.role.dto.RoleDto;
import com.taxoryn.module.role.entity.PermissionEntity;
import com.taxoryn.module.role.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleMapper {

    RoleDto toDto(RoleEntity entity);

    PermissionDto toDto(PermissionEntity entity);

    List<RoleDto> toDtoList(List<RoleEntity> entities);

    List<PermissionDto> toPermissionDtoList(List<PermissionEntity> entities);
}
