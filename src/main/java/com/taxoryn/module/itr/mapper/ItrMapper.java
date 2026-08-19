package com.taxoryn.module.itr.mapper;

import com.taxoryn.module.itr.dto.ItrProfileDto;
import com.taxoryn.module.itr.dto.ItrReturnDto;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.entity.ItrReturnEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItrMapper {

    ItrProfileDto toProfileDto(ItrProfileEntity entity);

    List<ItrProfileDto> toProfileDtoList(List<ItrProfileEntity> entities);

    ItrReturnDto toReturnDto(ItrReturnEntity entity);

    List<ItrReturnDto> toReturnDtoList(List<ItrReturnEntity> entities);
}
