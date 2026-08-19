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
@Schema(description = "Enterprise Audit Log Record")
public class AuditLogDto {

    @Schema(description = "Unique audit record ID")
    private UUID id;

    @Schema(description = "Organization / Tenant ID")
    private UUID organizationId;

    @Schema(description = "User ID who performed the action, null if system automated")
    private UUID userId;

    @Schema(description = "Action performed (e.g. CLIENT_CREATED, INVOICE_UPDATED, GST_FILING_SUBMITTED)")
    private String action;

    @Schema(description = "Target entity type (e.g. CLIENT, GST_PROFILE, INVOICE, DOCUMENT, EMPLOYEE, ROLE)")
    private String entityType;

    @Schema(description = "Entity name alias for backward compatibility")
    private String entityName;

    @Schema(description = "Target entity ID")
    private String entityId;

    @Schema(description = "Previous state snapshot (JSON/string)")
    private String oldValue;

    @Schema(description = "New state snapshot (JSON/string)")
    private String newValue;

    @Schema(description = "Client IP address")
    private String ipAddress;

    @Schema(description = "Trace / Correlation / Request ID")
    private String requestId;

    @Schema(description = "Client HTTP User-Agent")
    private String userAgent;

    @Schema(description = "Audit event creation timestamp")
    private Instant timestamp;

    @Schema(description = "Record creation timestamp")
    private Instant createdAt;
}
