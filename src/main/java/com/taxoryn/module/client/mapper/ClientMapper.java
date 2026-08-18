package com.taxoryn.module.client.mapper;

import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientNoteDto;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientNoteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClientMapper {

    ClientDto toDto(ClientEntity entity);

    List<ClientDto> toDtoList(List<ClientEntity> entities);

    ClientNoteDto toNoteDto(ClientNoteEntity entity);

    List<ClientNoteDto> toNoteDtoList(List<ClientNoteEntity> entities);
}
