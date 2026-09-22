package com.taxoryn.module.notice.dto;

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
public class AdjournHearingRequest {
    @NotNull(message = "Next hearing date is required when adjourning")
    private LocalDate nextHearingDate;
    private String nextHearingTime;
    private String reason;
    private String proceedingsSummary;
}
