package com.taxoryn.module.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRecordRequest {

    private UUID organizationId;
    private UUID userId;
    private String action;
    private String entityType;
    private String entityName;
    private String entityId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String requestId;
    private String userAgent;
}
