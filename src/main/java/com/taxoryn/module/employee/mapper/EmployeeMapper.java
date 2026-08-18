package com.taxoryn.module.employee.mapper;

import com.taxoryn.module.employee.dto.EmployeeDto;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeMapper {

    EmployeeDto toDto(EmployeeEntity entity);

    List<EmployeeDto> toDtoList(List<EmployeeEntity> entities);
}
