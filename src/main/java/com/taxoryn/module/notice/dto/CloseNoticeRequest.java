package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.NoticeStatus;
import jakarta.validation.constraints.NotNull;
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
public class CloseNoticeRequest {

    @NotNull(message = "Closure status is required (e.g. RESOLVED, DEMAND_DROPPED, APPEAL_FILED, CLOSED)")
    private NoticeStatus closureStatus;

    private LocalDate closureDate;
    private String closureRemarks;
    private UUID orderDocumentId;
}
