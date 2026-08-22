package com.taxoryn.module.tds.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.tds.entity.TdsProfileEntity.DeductorType;
import com.taxoryn.module.tds.entity.TdsProfileEntity.TdsProfileStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Filter parameters for searching TAN Deductor Profiles")
public class TdsProfileFilterRequest extends PageRequestDto {

    @Schema(description = "Filter by Client ID")
    private UUID clientId;

    @Schema(description = "Filter by Deductor Constitution Type")
    private DeductorType deductorType;

    @Schema(description = "Filter by Profile Status")
    private TdsProfileStatus status;

    @Schema(description = "Search by TAN or Responsible Person Name")
    private String search;
}
