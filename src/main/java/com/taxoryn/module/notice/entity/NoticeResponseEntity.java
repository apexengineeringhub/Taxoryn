package com.taxoryn.module.notice.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import com.taxoryn.module.notice.enums.ReviewStatus;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "notice_responses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeResponseEntity extends TenantAuditableEntity {

    @Column(name = "notice_id", nullable = false)
    private UUID noticeId;

    @Column(name = "response_version", nullable = false)
    @Builder.Default
    private Integer responseVersion = 1;

    @Column(name = "response_title", nullable = false, length = 255)
    private String responseTitle;

    @Column(name = "response_summary", columnDefinition = "TEXT")
    private String responseSummary;

    @Column(name = "legal_grounds", columnDefinition = "TEXT")
    private String legalGrounds;

    @Column(name = "facts_of_case", columnDefinition = "TEXT")
    private String factsOfCase;

    @Column(name = "prepared_by_user_id")
    private UUID preparedByUserId;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 50)
    @Builder.Default
    private ReviewStatus reviewStatus = ReviewStatus.DRAFT;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;

    @Column(name = "submission_reference", length = 100)
    private String submissionReference;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "acknowledgement_number", length = 100)
    private String acknowledgementNumber;

    @Column(name = "acknowledgement_date")
    private LocalDate acknowledgementDate;
}
