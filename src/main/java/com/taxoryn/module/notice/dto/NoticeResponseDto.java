package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.ReviewStatus;
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
public class NoticeResponseDto {
    private UUID id;
    private UUID organizationId;
    private UUID noticeId;
    private Integer version;
    private String responseTitle;
    private String responseSummary;
    private String legalGrounds;
    private String factsOfCase;

    private UUID preparedByUserId;
    private String preparedByUserName;
    private UUID reviewedByUserId;
    private String reviewedByUserName;
    private UUID approvedByUserId;
    private String approvedByUserName;

    private ReviewStatus reviewStatus;
    private String reviewComments;
    private String submissionReference;
    private Instant submittedAt;
    private String acknowledgementNumber;
    private LocalDate acknowledgementDate;

    private Instant createdAt;
    private Instant updatedAt;
}
