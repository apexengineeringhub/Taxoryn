package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.HearingMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleHearingRequest {

    @NotNull(message = "Hearing date is required")
    private LocalDate hearingDate;

    @Size(max = 20, message = "Hearing time cannot exceed 20 characters")
    private String hearingTime;

    @NotNull(message = "Hearing mode is required")
    private HearingMode hearingMode;

    @Size(max = 500, message = "Hearing link cannot exceed 500 characters")
    private String hearingLink;

    @Size(max = 255, message = "Authority name cannot exceed 255 characters")
    private String authorityName;

    @Size(max = 150, message = "Officer name cannot exceed 150 characters")
    private String officerName;

    private UUID designatedEmployeeId;
    private UUID designatedPartnerId;

    private String proceedingsSummary;
}
