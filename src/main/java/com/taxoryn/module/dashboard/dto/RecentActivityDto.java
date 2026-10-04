package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Operational activity log entry for practice dashboard")
public class RecentActivityDto {

    private UUID id;
    private String action;
    private String entityType;
    private String entityName;
    private String entityId;
    private String description;
    private UUID userId;
    private String userName;
    private Instant createdAt;
}
