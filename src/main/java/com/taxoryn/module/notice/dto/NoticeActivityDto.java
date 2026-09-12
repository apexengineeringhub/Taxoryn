package com.taxoryn.module.notice.dto;

import com.taxoryn.module.notice.enums.NoticeActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeActivityDto {
    private UUID id;
    private UUID organizationId;
    private UUID noticeId;
    private NoticeActivityType activityType;
    private UUID performedByUserId;
    private String performerName;
    private String description;
    private String fromState;
    private String toState;
    private String metadata;
    private Instant createdAt;
}
