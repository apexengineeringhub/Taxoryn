package com.taxoryn.module.notice.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.notice.enums.NoticeDepartment;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.enums.NoticeStatus;
import com.taxoryn.module.notice.enums.SubmissionMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tax_notices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxNoticeEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "notice_number", nullable = false, length = 100)
    private String noticeNumber;

    @Column(name = "din_number", length = 100)
    private String dinNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false, length = 50)
    private NoticeDepartment department;

    @Column(name = "notice_type", nullable = false, length = 100)
    private String noticeType;

    @Column(name = "section", length = 100)
    private String section;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "assessment_year", length = 20)
    private String assessmentYear;

    @Column(name = "financial_year", length = 20)
    private String financialYear;

    @Column(name = "tax_period", length = 50)
    private String taxPeriod;

    @Column(name = "demand_amount", precision = 15, scale = 2)
    private BigDecimal demandAmount;

    @Column(name = "notice_date")
    private LocalDate noticeDate;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    @Column(name = "response_due_date", nullable = false)
    private LocalDate responseDueDate;

    @Column(name = "hearing_date")
    private LocalDate hearingDate;

    @Column(name = "hearing_time", length = 20)
    private String hearingTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private NoticeStatus status = NoticeStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    @Builder.Default
    private NoticePriority priority = NoticePriority.MEDIUM;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "reviewer_employee_id")
    private UUID reviewerEmployeeId;

    @Column(name = "partner_employee_id")
    private UUID partnerEmployeeId;

    @Column(name = "issuing_authority", length = 255)
    private String issuingAuthority;

    @Column(name = "issuing_officer_name", length = 150)
    private String issuingOfficerName;

    @Column(name = "portal_acknowledgement_number", length = 100)
    private String portalAcknowledgementNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_mode", length = 50)
    private SubmissionMode submissionMode;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "closure_date")
    private LocalDate closureDate;

    @Column(name = "closure_remarks", columnDefinition = "TEXT")
    private String closureRemarks;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;
}
