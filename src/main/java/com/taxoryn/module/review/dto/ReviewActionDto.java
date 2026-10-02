package com.taxoryn.module.review.dto;

import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ReviewActionDto {
    String action;
    UUID actorId;
    Instant occurredAt;
    String comment;
}
