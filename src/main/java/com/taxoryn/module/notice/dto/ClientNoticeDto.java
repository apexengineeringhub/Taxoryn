package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.NoticeDepartment;
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

/**
 * Sanitized Notice DTO specifically for Client Portal exposure.
 * Excludes internal review notes, draft comments, and internal operational details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientNoticeDto {
    private UUID id;
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
    private LocalDate hearingDate;
    private String hearingTime;
    private NoticeStatus status;
    private String issuingAuthority;
    private String portalAcknowledgementNumber;
    private SubmissionMode submissionMode;
    private Instant submittedAt;
    private LocalDate closureDate;
    private Instant createdAt;
    private Instant updatedAt;
}
