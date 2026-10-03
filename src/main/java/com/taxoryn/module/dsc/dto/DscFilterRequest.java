package com.taxoryn.module.dsc.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.dsc.model.DscCertificateType;
import com.taxoryn.module.dsc.model.DscStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Filter and Pagination parameters for DSC Register query")
public class DscFilterRequest extends PageRequestDto {

    private String search;
    private DscStatus status;
    private UUID clientId;
    private DscCertificateType certificateType;
    private String service;
    private Integer expiringWithinDays;
}
