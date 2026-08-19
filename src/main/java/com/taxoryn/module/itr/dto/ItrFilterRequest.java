package com.taxoryn.module.itr.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
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
@Schema(description = "ITR Return Search & Filter Parameters")
public class ItrFilterRequest extends PageRequestDto {

    @Schema(description = "Search term across client name, PAN, ACK number, AY")
    private String search;

    @Schema(description = "Filter by Client ID")
    private UUID clientId;

    @Schema(description = "Filter by Assessment Year (AY, e.g. 2026-27)")
    private String assessmentYear;

    @Schema(description = "Filter by Financial Year (FY, e.g. 2025-26)")
    private String financialYear;

    @Schema(description = "Filter by ITR Type (ITR_1 to ITR_7)")
    private ItrType itrType;

    @Schema(description = "Filter by Taxpayer Type")
    private TaxpayerType taxpayerType;

    @Schema(description = "Filter by workflow status")
    private ItrStatus status;

    @Schema(description = "Filter by assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Filter only overdue returns (dueDate < today and not filed/completed)")
    private Boolean isOverdue;

    @Schema(description = "Filter only upcoming returns (dueDate >= today)")
    private Boolean isUpcoming;
}
