package com.taxoryn.module.tds.mapper;

import com.taxoryn.module.tds.dto.*;
import com.taxoryn.module.tds.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TdsMapper {

    TdsProfileDto toProfileDto(TdsProfileEntity entity);

    List<TdsProfileDto> toProfileDtoList(List<TdsProfileEntity> entities);

    TdsReturnDto toReturnDto(TdsReturnEntity entity);

    List<TdsReturnDto> toReturnDtoList(List<TdsReturnEntity> entities);

    TdsChallanDto toChallanDto(TdsChallanEntity entity);

    List<TdsChallanDto> toChallanDtoList(List<TdsChallanEntity> entities);

    TdsDeducteeEntryDto toDeducteeDto(TdsDeducteeEntryEntity entity);

    List<TdsDeducteeEntryDto> toDeducteeDtoList(List<TdsDeducteeEntryEntity> entities);

    TdsCertificateDto toCertificateDto(TdsCertificateEntity entity);

    List<TdsCertificateDto> toCertificateDtoList(List<TdsCertificateEntity> entities);
}
