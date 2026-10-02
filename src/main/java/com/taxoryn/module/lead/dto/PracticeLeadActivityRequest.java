package com.taxoryn.module.lead.dto;

import com.taxoryn.module.lead.entity.PracticeLeadActivityEntity.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.Instant;

@Data public class PracticeLeadActivityRequest {
    @NotNull private ActivityType activityType;
    @Size(max = 255) private String subject;
    @NotBlank private String content;
    @NotNull private Instant occurredAt;
}
