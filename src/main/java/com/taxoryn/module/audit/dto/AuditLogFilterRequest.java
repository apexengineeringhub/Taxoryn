package com.taxoryn.module.audit.dto;

import com.taxoryn.core.dto.PageRequestDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Filter and Pagination Parameters for Enterprise Audit Trail")
public class AuditLogFilterRequest extends PageRequestDto {

    @Schema(description = "Filter by entity type (e.g. CLIENT, GST_PROFILE, ITR_RETURN, DOCUMENT, EMPLOYEE, ROLE, INVOICE)", example = "CLIENT")
    private String entityType;

    @Schema(description = "Filter by target entity ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String entityId;

    @Schema(description = "Filter by action keyword (e.g. CLIENT_CREATED, INVOICE_UPDATED)", example = "CLIENT_CREATED")
    private String action;

    @Schema(description = "Filter by actor user ID")
    private UUID userId;

    @Schema(description = "Filter by request / correlation ID")
    private String requestId;

    @Schema(description = "Start timestamp for date range filtering (ISO-8601 UTC)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant startDate;

    @Schema(description = "End timestamp for date range filtering (ISO-8601 UTC)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant endDate;

    @Schema(description = "Search term across action, entityType, entityId, oldValue, newValue, ipAddress", example = "UPDATE")
    private String search;
}
