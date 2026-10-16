package com.taxoryn.module.reminder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to send an immediate follow-up nudge to a client")
public class SendFollowUpNudgeRequest {

    @Size(max = 500, message = "Custom nudge message must not exceed 500 characters")
    private String customMessage;
}
