package com.taxoryn.module.notice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewNoticeResponseRequest {

    @NotBlank(message = "Action is required (e.g. SUBMIT_FOR_REVIEW, APPROVE_REVIEW, REQUEST_REVISION, APPROVE_PARTNER)")
    private String action;

    private String comments;
}
