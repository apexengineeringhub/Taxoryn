package com.taxoryn.module.content.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleContentRequest {

    @NotNull(message = "Scheduled publish date/time is required")
    @Future(message = "Scheduled publication time must be in the future")
    private Instant scheduledPublishAt;
}
