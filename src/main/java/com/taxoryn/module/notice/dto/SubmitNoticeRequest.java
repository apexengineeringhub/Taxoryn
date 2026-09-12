package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.SubmissionMode;
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
public class SubmitNoticeRequest {

    @NotNull(message = "Submission mode is required")
    private SubmissionMode submissionMode;

    @Size(max = 100, message = "Acknowledgement number cannot exceed 100 characters")
    private String portalAcknowledgementNumber;

    private UUID responseId;
    private LocalDate acknowledgementDate;
    private UUID acknowledgementDocumentId;
    private UUID submissionProofDocumentId;
    private String remarks;
}
