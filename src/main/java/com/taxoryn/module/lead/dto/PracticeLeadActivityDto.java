package com.taxoryn.module.lead.dto;

import com.taxoryn.module.lead.entity.PracticeLeadActivityEntity.ActivityType;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data @Builder public class PracticeLeadActivityDto {
    private UUID id;
    private UUID leadId;
    private ActivityType activityType;
    private String subject;
    private String content;
    private Instant occurredAt;
    private String authorName;
    private Instant createdAt;
}
