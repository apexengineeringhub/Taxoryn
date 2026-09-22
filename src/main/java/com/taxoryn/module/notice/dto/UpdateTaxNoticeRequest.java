package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.HearingMode;
import com.taxoryn.module.notice.enums.HearingStatus;
import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeResponseStatus;
import com.taxoryn.module.notice.enums.NoticeStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaxNoticeRequest {

    private UUID clientId;

    @Size(max = 100, message = "Notice number cannot exceed 100 characters")
    private String noticeNumber;

    @Size(max = 100, message = "DIN cannot exceed 100 characters")
    private String dinNumber;

    private NoticeDepartment department;

    @Size(max = 100, message = "Notice type cannot exceed 100 characters")
    private String noticeType;

    @Size(max = 100, message = "Section cannot exceed 100 characters")
    private String section;

    @Size(max = 255, message = "Subject cannot exceed 255 characters")
    private String subject;

    private String description;

    @Size(max = 20, message = "Assessment Year cannot exceed 20 characters")
    private String assessmentYear;

    @Size(max = 20, message = "Financial Year cannot exceed 20 characters")
    private String financialYear;

    @Size(max = 50, message = "Tax Period cannot exceed 50 characters")
    private String taxPeriod;

    private BigDecimal demandAmount;

    private LocalDate noticeDate;
    private LocalDate receivedDate;
    private LocalDate responseDueDate;

    // Response Fields
    private Boolean responseRequired;
    private NoticeResponseStatus responseStatus;
    private String responseDraft;
    private String submissionReference;
    private String submissionNotes;

    // Hearing Fields
    private Boolean hearingRequired;
    private LocalDate hearingDate;

    @Size(max = 20, message = "Hearing time cannot exceed 20 characters")
    private String hearingTime;
    private HearingMode hearingMode;
    private String hearingLocation;
    private String hearingReference;
    private String hearingNotes;
    private String hearingOutcome;
    private HearingStatus hearingStatus;

    private NoticeStatus status;
    private NoticePriority priority;

    private UUID assignedEmployeeId;
    private UUID reviewerEmployeeId;
    private UUID partnerEmployeeId;

    @Size(max = 255, message = "Issuing authority cannot exceed 255 characters")
    private String issuingAuthority;

    @Size(max = 150, message = "Issuing officer name cannot exceed 150 characters")
    private String issuingOfficerName;

    private String internalNotes;
}
