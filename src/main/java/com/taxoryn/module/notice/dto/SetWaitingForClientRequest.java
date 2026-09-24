package com.taxoryn.module.notice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetWaitingForClientRequest {

    @NotBlank(message = "Waiting reason is required")
    private String waitingReason;

    private LocalDate expectedResponseDate;

    @Builder.Default
    private Boolean createDocumentRequest = false;

    private String documentRequestTitle;

    private String documentRequestDescription;
}
