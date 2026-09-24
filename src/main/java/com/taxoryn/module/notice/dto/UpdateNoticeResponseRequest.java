package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.NoticeResponseStatus;
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
public class UpdateNoticeResponseRequest {
    private Boolean responseRequired;
    private LocalDate responseDueDate;
    private NoticeResponseStatus responseStatus;
    private String responseDraft;
    private String submissionReference;
    private String submissionNotes;
    private UUID originalDocumentId;
    private UUID responseDocumentId;
}
