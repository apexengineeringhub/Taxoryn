package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.HearingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordHearingOutcomeRequest {

    @NotNull(message = "Hearing status is required")
    private HearingStatus status;

    private String proceedingsSummary;
    private String outcomeSummary;
    private String nextAction;
    private LocalDate nextHearingDate;
}
