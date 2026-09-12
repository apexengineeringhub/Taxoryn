package com.taxoryn.module.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoticeResponseRequest {

    @NotBlank(message = "Response title is required")
    @Size(max = 255, message = "Response title cannot exceed 255 characters")
    private String responseTitle;

    private String responseSummary;
    private String legalGrounds;
    private String factsOfCase;

    @Builder.Default
    private Boolean submitForReview = false;
}
