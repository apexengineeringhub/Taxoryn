package com.taxoryn.module.user.mapper;

import com.taxoryn.module.role.mapper.RoleMapper;
import com.taxoryn.module.user.dto.UserDto;
import com.taxoryn.module.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {RoleMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "fullName", expression = "java(entity.getFullName())")
    UserDto toDto(UserEntity entity);

    List<UserDto> toDtoList(List<UserEntity> entities);
}
