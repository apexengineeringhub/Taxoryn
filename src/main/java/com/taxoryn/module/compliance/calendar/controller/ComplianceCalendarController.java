package com.taxoryn.module.compliance.calendar.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.compliance.calendar.dto.ClientComplianceDeadlineSummaryDto;
import com.taxoryn.module.compliance.calendar.dto.ComplianceCalendarQueryFilter;
import com.taxoryn.module.compliance.calendar.dto.ComplianceDeadlineDto;
import com.taxoryn.module.compliance.calendar.dto.ComplianceDeadlineRadarDto;
import com.taxoryn.module.compliance.calendar.dto.ComplianceDeadlineSummaryDto;
import com.taxoryn.module.compliance.calendar.service.ComplianceCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST Controller for Compliance Calendar and Deadline Radar operations.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Compliance Calendar & Deadline Radar", description = "Phase 29.6: Projection and prioritization layer for statutory compliance deadlines")
@SecurityRequirement(name = "BearerAuth")
public class ComplianceCalendarController {

    private final ComplianceCalendarService calendarService;

    @GetMapping("/compliance/calendar")
    @PreAuthorize("hasAnyAuthority('COMPLIANCE_VIEW', 'CLIENT_VIEW', 'TASK_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get compliance calendar deadlines", description = "Retrieves paginated compliance deadlines with filters by date range, domain, deadline status, client, rule, and search term.")
    public ResponseEntity<ApiResponse<PagedResponse<ComplianceDeadlineDto>>> getCalendar(
            @ModelAttribute ComplianceCalendarQueryFilter filter) {
        PagedResponse<ComplianceDeadlineDto> response = calendarService.getCalendar(filter);
        return ResponseEntity.ok(ApiResponse.success("Compliance calendar retrieved successfully", response));
    }

    @GetMapping("/compliance/calendar/radar")
    @PreAuthorize("hasAnyAuthority('COMPLIANCE_VIEW', 'CLIENT_VIEW', 'TASK_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get practice deadline radar overview", description = "Returns practice-wide deadline radar with summary counts, domain breakdowns, and top urgent deadlines.")
    public ResponseEntity<ApiResponse<ComplianceDeadlineRadarDto>> getPracticeDeadlineRadar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {
        ComplianceDeadlineRadarDto radar = calendarService.getPracticeDeadlineRadar(referenceDate);
        return ResponseEntity.ok(ApiResponse.success("Practice compliance deadline radar retrieved successfully", radar));
    }

    @GetMapping("/compliance/calendar/summary")
    @PreAuthorize("hasAnyAuthority('COMPLIANCE_VIEW', 'CLIENT_VIEW', 'TASK_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get compliance deadline summary counts", description = "Returns aggregate deadline counts across overdue, today, tomorrow, this week, and upcoming buckets.")
    public ResponseEntity<ApiResponse<ComplianceDeadlineSummaryDto>> getDeadlineSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {
        ComplianceDeadlineSummaryDto summary = calendarService.getDeadlineSummary(referenceDate);
        return ResponseEntity.ok(ApiResponse.success("Compliance deadline summary retrieved successfully", summary));
    }

    @GetMapping("/clients/{clientId}/compliance/calendar")
    @PreAuthorize("hasAnyAuthority('COMPLIANCE_VIEW', 'CLIENT_VIEW', 'TASK_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get client compliance calendar", description = "Retrieves paginated compliance deadlines specifically for the specified client.")
    public ResponseEntity<ApiResponse<PagedResponse<ComplianceDeadlineDto>>> getClientCalendar(
            @PathVariable UUID clientId,
            @ModelAttribute ComplianceCalendarQueryFilter filter) {
        PagedResponse<ComplianceDeadlineDto> response = calendarService.getClientCalendar(clientId, filter);
        return ResponseEntity.ok(ApiResponse.success("Client compliance calendar retrieved successfully", response));
    }

    @GetMapping("/clients/{clientId}/compliance/calendar/summary")
    @PreAuthorize("hasAnyAuthority('COMPLIANCE_VIEW', 'CLIENT_VIEW', 'TASK_VIEW', 'GST_VIEW', 'ITR_VIEW', 'TDS_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    @Operation(summary = "Get Client 360 compliance deadline summary", description = "Returns compact deadline summary and next deadline for a specific client.")
    public ResponseEntity<ApiResponse<ClientComplianceDeadlineSummaryDto>> getClientDeadlineSummary(
            @PathVariable UUID clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {
        ClientComplianceDeadlineSummaryDto summary = calendarService.getClientDeadlineSummary(clientId, referenceDate);
        return ResponseEntity.ok(ApiResponse.success("Client compliance deadline summary retrieved successfully", summary));
    }
}
