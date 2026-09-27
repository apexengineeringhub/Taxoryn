package com.taxoryn.module.timetracking.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.timetracking.dto.CreateTimeEntryRequest;
import com.taxoryn.module.timetracking.dto.TimeEntryDto;
import com.taxoryn.module.timetracking.dto.TimeEntryFilterRequest;
import com.taxoryn.module.timetracking.dto.UpdateTimeEntryRequest;
import com.taxoryn.module.timetracking.service.TimeEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/time-entries", "/api/time-entries"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.TASKS)
@Tag(name = "Time Tracking", description = "Endpoints for recording, retrieving, and managing professional time tracking entries")
@SecurityRequirement(name = "BearerAuth")
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    @PostMapping
    @PreAuthorize("hasAuthority('TASK_CREATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT')")
    @Operation(summary = "Create time entry", description = "Logs professional time against a client, engagement, work item, or task.")
    public ResponseEntity<ApiResponse<TimeEntryDto>> createTimeEntry(@Valid @RequestBody CreateTimeEntryRequest request) {
        TimeEntryDto created = timeEntryService.createTimeEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Time entry created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT')")
    @Operation(summary = "List time entries with filters", description = "Retrieves paginated time tracking entries with filters by client, user, date range, billable status, and search keywords.")
    public ResponseEntity<ApiResponse<PagedResponse<TimeEntryDto>>> getTimeEntries(@Valid @ModelAttribute TimeEntryFilterRequest filterRequest) {
        PagedResponse<TimeEntryDto> response = timeEntryService.getTimeEntries(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Time entries retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT')")
    @Operation(summary = "Get time entry by ID", description = "Retrieves time entry details.")
    public ResponseEntity<ApiResponse<TimeEntryDto>> getTimeEntryById(@PathVariable UUID id) {
        TimeEntryDto dto = timeEntryService.getTimeEntryById(id);
        return ResponseEntity.ok(ApiResponse.success("Time entry retrieved successfully", dto));
    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT')")
    @Operation(summary = "Get time entries by client ID", description = "Retrieves all time tracking entries for a specific client (Client 360).")
    public ResponseEntity<ApiResponse<List<TimeEntryDto>>> getTimeEntriesByClientId(@PathVariable UUID clientId) {
        List<TimeEntryDto> list = timeEntryService.getTimeEntriesByClientId(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client time entries retrieved successfully", list));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT')")
    @Operation(summary = "Update time entry", description = "Updates recorded time entry details.")
    public ResponseEntity<ApiResponse<TimeEntryDto>> updateTimeEntry(@PathVariable UUID id, @Valid @RequestBody UpdateTimeEntryRequest request) {
        TimeEntryDto updated = timeEntryService.updateTimeEntry(id, request);
        return ResponseEntity.ok(ApiResponse.success("Time entry updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Delete time entry", description = "Deletes recorded time entry.")
    public ResponseEntity<ApiResponse<Void>> deleteTimeEntry(@PathVariable UUID id) {
        timeEntryService.deleteTimeEntry(id);
        return ResponseEntity.ok(ApiResponse.success("Time entry deleted successfully", null));
    }
}
