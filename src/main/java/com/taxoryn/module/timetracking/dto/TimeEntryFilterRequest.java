package com.taxoryn.module.timetracking.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.timetracking.model.TimeEntryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Time Entry Query Filter Parameters")
public class TimeEntryFilterRequest extends PageRequestDto {

    private UUID clientId;
    private UUID locationId;
    private UUID engagementId;
    private UUID workItemId;
    private UUID taskId;
    private UUID userId;
    private Boolean billable;
    private TimeEntryStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String search;
}
