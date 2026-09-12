package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeStatus;
import com.taxoryn.module.notice.enums.SubmissionMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxNoticeDto {
    private UUID id;
    private UUID organizationId;
    private UUID clientId;
    private String clientName;
    private String clientPan;
    private String clientGstin;
    private String clientEmail;
    private String clientPhone;

    private String noticeNumber;
    private String dinNumber;
    private NoticeDepartment department;
    private String noticeType;
    private String section;
    private String subject;
    private String description;
    private String assessmentYear;
    private String financialYear;
    private String taxPeriod;
    private BigDecimal demandAmount;
    private LocalDate noticeDate;
    private LocalDate receivedDate;
    private LocalDate responseDueDate;
    private Long daysRemaining;
    private Boolean isOverdue;
    private LocalDate hearingDate;
    private String hearingTime;
    private NoticeStatus status;
    private NoticePriority priority;

    private UUID assignedEmployeeId;
    private String assignedEmployeeName;
    private UUID reviewerEmployeeId;
    private String reviewerEmployeeName;
    private UUID partnerEmployeeId;
    private String partnerEmployeeName;

    private String issuingAuthority;
    private String issuingOfficerName;
    private String portalAcknowledgementNumber;
    private SubmissionMode submissionMode;
    private Instant submittedAt;
    private LocalDate closureDate;
    private String closureRemarks;
    private String internalNotes;

    private Integer responsesCount;
    private Integer hearingsCount;
    private Integer openTasksCount;
    private Integer documentsCount;
    private Integer pendingRequestsCount;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
