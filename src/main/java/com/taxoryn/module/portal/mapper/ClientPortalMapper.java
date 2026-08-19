package com.taxoryn.module.portal.mapper;

import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.portal.dto.ClientDocumentRequestDto;
import com.taxoryn.module.portal.dto.ClientNotificationDto;
import com.taxoryn.module.portal.dto.ClientPortalProfileDto;
import com.taxoryn.module.portal.entity.ClientDocumentRequestEntity;
import com.taxoryn.module.portal.entity.ClientNotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClientPortalMapper {

    @Mapping(target = "clientId", source = "id")
    ClientPortalProfileDto toProfileDto(ClientEntity client);

    ClientNotificationDto toNotificationDto(ClientNotificationEntity entity);

    List<ClientNotificationDto> toNotificationDtoList(List<ClientNotificationEntity> list);

    ClientDocumentRequestDto toDocRequestDto(ClientDocumentRequestEntity entity);

    List<ClientDocumentRequestDto> toDocRequestDtoList(List<ClientDocumentRequestEntity> list);
}
