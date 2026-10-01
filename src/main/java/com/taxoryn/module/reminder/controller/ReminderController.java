package com.taxoryn.module.reminder.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.reminder.dto.CreateReminderRequest;
import com.taxoryn.module.reminder.dto.ReminderDto;
import com.taxoryn.module.reminder.dto.ReminderFilterRequest;
import com.taxoryn.module.reminder.dto.UpdateReminderRequest;
import com.taxoryn.module.reminder.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@Tag(name = "Reminders", description = "Endpoints for managing reminders and follow-ups")
@SecurityRequirement(name = "BearerAuth")
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Create a new reminder", description = "Creates a manual reminder for the current user or a specified target user.")
    public ResponseEntity<ApiResponse<ReminderDto>> createReminder(@Valid @RequestBody CreateReminderRequest request) {
        ReminderDto dto = reminderService.createReminder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Reminder created successfully", dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Update a reminder", description = "Updates a PENDING reminder's details.")
    public ResponseEntity<ApiResponse<ReminderDto>> updateReminder(@PathVariable UUID id, @Valid @RequestBody UpdateReminderRequest request) {
        ReminderDto dto = reminderService.updateReminder(id, request);
        return ResponseEntity.ok(ApiResponse.success("Reminder updated successfully", dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Get reminder by ID")
    public ResponseEntity<ApiResponse<ReminderDto>> getReminderById(@PathVariable UUID id) {
        ReminderDto dto = reminderService.getReminderById(id);
        return ResponseEntity.ok(ApiResponse.success("Reminder retrieved successfully", dto));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Get my reminders", description = "Retrieves paginated reminders assigned to the current user.")
    public ResponseEntity<ApiResponse<PagedResponse<ReminderDto>>> getMyReminders(@ModelAttribute ReminderFilterRequest filter) {
        PagedResponse<ReminderDto> response = reminderService.getMyReminders(filter);
        return ResponseEntity.ok(ApiResponse.success("Reminders retrieved successfully", response));
    }

    @GetMapping("/team")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Get team reminders", description = "Retrieves paginated reminders for the team (admin/practitioner only).")
    public ResponseEntity<ApiResponse<PagedResponse<ReminderDto>>> getTeamReminders(@ModelAttribute ReminderFilterRequest filter) {
        PagedResponse<ReminderDto> response = reminderService.getTeamReminders(filter);
        return ResponseEntity.ok(ApiResponse.success("Team reminders retrieved successfully", response));
    }

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Get reminders for a task", description = "Returns active reminders linked to a specific task.")
    public ResponseEntity<ApiResponse<List<ReminderDto>>> getRemindersForTask(@PathVariable UUID taskId) {
        List<ReminderDto> reminders = reminderService.getRemindersForTask(taskId);
        return ResponseEntity.ok(ApiResponse.success("Task reminders retrieved successfully", reminders));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Complete a reminder", description = "Marks a PENDING or TRIGGERED reminder as COMPLETED.")
    public ResponseEntity<ApiResponse<ReminderDto>> completeReminder(@PathVariable UUID id) {
        ReminderDto dto = reminderService.completeReminder(id);
        return ResponseEntity.ok(ApiResponse.success("Reminder completed successfully", dto));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Cancel a reminder", description = "Marks a PENDING or TRIGGERED reminder as CANCELLED.")
    public ResponseEntity<ApiResponse<ReminderDto>> cancelReminder(@PathVariable UUID id) {
        ReminderDto dto = reminderService.cancelReminder(id);
        return ResponseEntity.ok(ApiResponse.success("Reminder cancelled successfully", dto));
    }

    @GetMapping("/overdue/count")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Get overdue reminder count", description = "Returns count of overdue PENDING reminders for the current user.")
    public ResponseEntity<ApiResponse<Long>> countOverdue() {
        long count = reminderService.countOverdueForCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Overdue count retrieved", count));
    }
}
