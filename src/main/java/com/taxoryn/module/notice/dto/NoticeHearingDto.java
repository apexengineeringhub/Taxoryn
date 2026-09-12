package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.HearingMode;
import com.taxoryn.module.notice.enums.HearingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeHearingDto {
    private UUID id;
    private UUID organizationId;
    private UUID noticeId;
    private LocalDate hearingDate;
    private String hearingTime;
    private HearingMode hearingMode;
    private String hearingLink;
    private String authorityName;
    private String officerName;

    private UUID designatedEmployeeId;
    private String designatedEmployeeName;
    private UUID designatedPartnerId;
    private String designatedPartnerName;

    private HearingStatus status;
    private String proceedingsSummary;
    private String outcomeSummary;
    private String nextAction;
    private LocalDate nextHearingDate;

    private Instant createdAt;
    private Instant updatedAt;
}
