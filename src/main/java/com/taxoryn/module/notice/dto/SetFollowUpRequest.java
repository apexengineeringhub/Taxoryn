package com.taxoryn.module.notice.dto;

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
public class SetFollowUpRequest {

    @NotNull(message = "Follow up date is required")
    private LocalDate followUpDate;

    private String followUpNotes;

    private UUID responsibleEmployeeId;
}
