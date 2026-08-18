package com.taxoryn.module.audit.dto;

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
@Schema(description = "Audit Log Entry Payload")
public class AuditLogDto {

    private UUID id;
    private UUID organizationId;
    private UUID userId;
    private String action;
    private String entityName;
    private String entityId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
}
