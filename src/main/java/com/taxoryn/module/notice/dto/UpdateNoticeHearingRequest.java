package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.HearingMode;
import com.taxoryn.module.notice.enums.HearingStatus;
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
public class UpdateNoticeHearingRequest {
    private Boolean hearingRequired;
    private LocalDate hearingDate;
    private String hearingTime;
    private HearingMode hearingMode;
    private String hearingLocation;
    private String hearingReference;
    private String hearingLink;
    private String authorityName;
    private String officerName;
    private HearingStatus hearingStatus;
    private String hearingNotes;
    private String hearingOutcome;
    private UUID designatedEmployeeId;
    private UUID designatedPartnerId;
    private LocalDate nextHearingDate;
}
