package com.taxoryn.module.tds.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.tds.entity.TdsChallanEntity.ChallanStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Filter parameters for searching TDS challans")
public class TdsChallanFilterRequest extends PageRequestDto {

    @Schema(description = "Filter by TDS Profile ID")
    private UUID tdsProfileId;

    @Schema(description = "Filter by Quarter (Q1, Q2, Q3, Q4)")
    private TdsQuarter quarter;

    @Schema(description = "Filter by Financial Year")
    private String financialYear;

    @Schema(description = "Filter by Utilization Status (UNUTILIZED, PARTIALLY_UTILIZED, FULLY_UTILIZED)")
    private ChallanStatus challanStatus;

    @Schema(description = "Filter by Section Code (e.g., 194C)")
    private String sectionCode;

    @Schema(description = "Search by BSR Code, Serial Number, or CIN")
    private String search;
}
