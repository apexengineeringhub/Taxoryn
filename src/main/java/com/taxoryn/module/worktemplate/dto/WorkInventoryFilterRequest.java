package com.taxoryn.module.worktemplate.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.worktemplate.model.WorkInventoryStatus;
import com.taxoryn.module.worktemplate.model.WorkInventoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Query filter parameters for unified work inventory")
public class WorkInventoryFilterRequest extends PageRequestDto {

    @Schema(description = "Filter by specific client")
    private UUID clientId;

    @Schema(description = "Filter by assigned user")
    private UUID assignedUserId;

    @Schema(description = "Filter by specific work type: WORK_INSTANCE, COMPLIANCE_OBLIGATION, TASK")
    private WorkInventoryType workType;

    @Schema(description = "Filter by normalized status")
    private WorkInventoryStatus status;

    @Schema(description = "Filter by priority: LOW, MEDIUM, HIGH, URGENT, CRITICAL")
    private String priority;

    @Schema(description = "Filter only blocked items")
    private Boolean blockedOnly;

    @Schema(description = "Due date range start (inclusive)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @Schema(description = "Due date range end (inclusive)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    @Schema(description = "Search term matching title, client name, rule code, or PAN")
    private String search;
}
