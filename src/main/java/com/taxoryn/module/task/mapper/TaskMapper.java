package com.taxoryn.module.task.mapper;

import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.entity.TaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskMapper {

    TaskDto toDto(TaskEntity entity);

    List<TaskDto> toDtoList(List<TaskEntity> entities);
}
