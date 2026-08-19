package com.taxoryn.module.document.mapper;

import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.document.entity.DocumentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    DocumentDto toDto(DocumentEntity entity);

    List<DocumentDto> toDtoList(List<DocumentEntity> entities);
}
