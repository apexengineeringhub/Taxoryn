package com.taxoryn.module.billing.mapper;

import com.taxoryn.module.billing.dto.InvoiceDto;
import com.taxoryn.module.billing.dto.InvoiceItemDto;
import com.taxoryn.module.billing.dto.InvoicePaymentDto;
import com.taxoryn.module.billing.entity.InvoiceEntity;
import com.taxoryn.module.billing.entity.InvoiceItemEntity;
import com.taxoryn.module.billing.entity.InvoicePaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper {

    InvoiceDto toDto(InvoiceEntity entity);

    List<InvoiceDto> toDtoList(List<InvoiceEntity> entities);

    InvoiceItemDto toItemDto(InvoiceItemEntity entity);

    List<InvoiceItemDto> toItemDtoList(List<InvoiceItemEntity> entities);

    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "recordedBy", source = "createdBy")
    @Mapping(target = "recordedAt", source = "createdAt")
    InvoicePaymentDto toPaymentDto(InvoicePaymentEntity entity);

    List<InvoicePaymentDto> toPaymentDtoList(List<InvoicePaymentEntity> entities);
}
