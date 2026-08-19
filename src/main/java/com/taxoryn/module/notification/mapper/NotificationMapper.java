package com.taxoryn.module.notification.mapper;

import com.taxoryn.module.notification.dto.NotificationDto;
import com.taxoryn.module.notification.entity.NotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    @Mapping(target = "channels", expression = "java(splitChannels(entity.getChannels()))")
    NotificationDto toDto(NotificationEntity entity);

    List<NotificationDto> toDtoList(List<NotificationEntity> entities);

    default Set<String> splitChannels(String channels) {
        if (channels == null || channels.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(channels.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
