package com.taxoryn.module.gst.mapper;

import com.taxoryn.module.gst.dto.GstMonthlySummaryDto;
import com.taxoryn.module.gst.dto.GstProfileDto;
import com.taxoryn.module.gst.dto.GstReturnFilingDto;
import com.taxoryn.module.gst.entity.GstMonthlySummaryEntity;
import com.taxoryn.module.gst.entity.GstProfileEntity;
import com.taxoryn.module.gst.entity.GstReturnFilingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GstMapper {

    GstProfileDto toDto(GstProfileEntity entity);

    List<GstProfileDto> toProfileDtoList(List<GstProfileEntity> entities);

    GstReturnFilingDto toFilingDto(GstReturnFilingEntity entity);

    List<GstReturnFilingDto> toFilingDtoList(List<GstReturnFilingEntity> entities);

    GstMonthlySummaryDto toSummaryDto(GstMonthlySummaryEntity entity);

    List<GstMonthlySummaryDto> toSummaryDtoList(List<GstMonthlySummaryEntity> entities);
}
