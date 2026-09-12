package com.taxoryn.module.notice.mapper;

import com.taxoryn.module.notice.dto.ClientNoticeDto;
import com.taxoryn.module.notice.dto.NoticeActivityDto;
import com.taxoryn.module.notice.dto.NoticeHearingDto;
import com.taxoryn.module.notice.dto.NoticeResponseDto;
import com.taxoryn.module.notice.dto.TaxNoticeDto;
import com.taxoryn.module.notice.entity.NoticeActivityEntity;
import com.taxoryn.module.notice.entity.NoticeHearingEntity;
import com.taxoryn.module.notice.entity.NoticeResponseEntity;
import com.taxoryn.module.notice.entity.TaxNoticeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaxNoticeMapper {

    TaxNoticeDto toDto(TaxNoticeEntity entity);

    List<TaxNoticeDto> toDtoList(List<TaxNoticeEntity> entities);

    ClientNoticeDto toClientDto(TaxNoticeEntity entity);

    List<ClientNoticeDto> toClientDtoList(List<TaxNoticeEntity> entities);

    @org.mapstruct.Mapping(source = "responseVersion", target = "version")
    NoticeResponseDto toResponseDto(NoticeResponseEntity entity);

    List<NoticeResponseDto> toResponseDtoList(List<NoticeResponseEntity> entities);

    NoticeHearingDto toHearingDto(NoticeHearingEntity entity);

    List<NoticeHearingDto> toHearingDtoList(List<NoticeHearingEntity> entities);

    NoticeActivityDto toActivityDto(NoticeActivityEntity entity);

    List<NoticeActivityDto> toActivityDtoList(List<NoticeActivityEntity> entities);
}
