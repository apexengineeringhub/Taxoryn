package com.taxoryn.module.notice.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.notice.enums.HearingMode;
import com.taxoryn.module.notice.enums.HearingStatus;
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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "notice_hearings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeHearingEntity extends TenantAuditableEntity {

    @Column(name = "notice_id", nullable = false)
    private UUID noticeId;

    @Column(name = "hearing_date", nullable = false)
    private LocalDate hearingDate;

    @Column(name = "hearing_time", length = 20)
    private String hearingTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "hearing_mode", nullable = false, length = 50)
    @Builder.Default
    private HearingMode hearingMode = HearingMode.VIRTUAL_VC;

    @Column(name = "hearing_link", length = 500)
    private String hearingLink;

    @Column(name = "authority_name", length = 255)
    private String authorityName;

    @Column(name = "officer_name", length = 150)
    private String officerName;

    @Column(name = "designated_employee_id")
    private UUID designatedEmployeeId;

    @Column(name = "designated_partner_id")
    private UUID designatedPartnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private HearingStatus status = HearingStatus.SCHEDULED;

    @Column(name = "proceedings_summary", columnDefinition = "TEXT")
    private String proceedingsSummary;

    @Column(name = "outcome_summary", columnDefinition = "TEXT")
    private String outcomeSummary;

    @Column(name = "next_action", columnDefinition = "TEXT")
    private String nextAction;

    @Column(name = "next_hearing_date")
    private LocalDate nextHearingDate;
}
