package com.taxoryn.module.subscription.mapper;

import com.taxoryn.module.subscription.dto.SubscriptionDto;
import com.taxoryn.module.subscription.entity.SubscriptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubscriptionMapper {

    SubscriptionDto toDto(SubscriptionEntity entity);

    List<SubscriptionDto> toDtoList(List<SubscriptionEntity> entities);
}
