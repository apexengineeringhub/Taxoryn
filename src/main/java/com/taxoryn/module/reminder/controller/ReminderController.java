package com.taxoryn.module.reminder.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.reminder.dto.CreateReminderRequest;
import com.taxoryn.module.reminder.dto.FollowUpEvaluationResultDto;
import com.taxoryn.module.reminder.dto.FollowUpFilterRequest;
import com.taxoryn.module.reminder.dto.FollowUpItemDto;
import com.taxoryn.module.reminder.dto.ReminderDto;
import com.taxoryn.module.reminder.dto.ReminderFilterRequest;
import com.taxoryn.module.reminder.dto.SendFollowUpNudgeRequest;
import com.taxoryn.module.reminder.dto.UpdateReminderRequest;
import com.taxoryn.module.reminder.service.FollowUpReminderService;
import com.taxoryn.module.reminder.service.ReminderService;
import com.taxoryn.core.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.REMINDERS)
@Tag(name = "Reminders", description = "Endpoints for managing reminders and follow-ups")
@SecurityRequirement(name = "BearerAuth")
public class ReminderController {

    private final ReminderService reminderService;
    private final FollowUpReminderService followUpReminderService;

    @PostMapping
    @PreAuthorize("hasAuthority('TASK_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE')")
    @Operation(summary = "Create a new reminder", description = "Creates a manual reminder for the current user or a specified target user.")
    public ResponseEntity<ApiResponse<ReminderDto>> createReminder(@Valid @RequestBody CreateReminderRequest request) {
        ReminderDto dto = reminderService.createReminder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Reminder created successfully", dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE')")
    @Operation(summary = "Update a reminder", description = "Updates a PENDING reminder's details.")
    public ResponseEntity<ApiResponse<ReminderDto>> updateReminder(@PathVariable UUID id, @Valid @RequestBody UpdateReminderRequest request) {
        ReminderDto dto = reminderService.updateReminder(id, request);
        return ResponseEntity.ok(ApiResponse.success("Reminder updated successfully", dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE')")
    @Operation(summary = "Get reminder by ID")
    public ResponseEntity<ApiResponse<ReminderDto>> getReminderById(@PathVariable UUID id) {
        ReminderDto dto = reminderService.getReminderById(id);
        return ResponseEntity.ok(ApiResponse.success("Reminder retrieved successfully", dto));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE')")
    @Operation(summary = "Get my reminders", description = "Retrieves paginated reminders assigned to the current user.")
    public ResponseEntity<ApiResponse<PagedResponse<ReminderDto>>> getMyReminders(@ModelAttribute ReminderFilterRequest filter) {
        PagedResponse<ReminderDto> response = reminderService.getMyReminders(filter);
        return ResponseEntity.ok(ApiResponse.success("Reminders retrieved successfully", response));
    }

    @GetMapping("/team")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Get team reminders", description = "Retrieves paginated reminders for the team (admin/manager/partner only).")
    public ResponseEntity<ApiResponse<PagedResponse<ReminderDto>>> getTeamReminders(@ModelAttribute ReminderFilterRequest filter) {
        PagedResponse<ReminderDto> response = reminderService.getTeamReminders(filter);
        return ResponseEntity.ok(ApiResponse.success("Team reminders retrieved successfully", response));
    }

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE')")
    @Operation(summary = "Get reminders for a task", description = "Returns active reminders linked to a specific task.")
    public ResponseEntity<ApiResponse<List<ReminderDto>>> getRemindersForTask(@PathVariable UUID taskId) {
        List<ReminderDto> reminders = reminderService.getRemindersForTask(taskId);
        return ResponseEntity.ok(ApiResponse.success("Task reminders retrieved successfully", reminders));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE')")
    @Operation(summary = "Complete a reminder", description = "Marks a PENDING or TRIGGERED reminder as COMPLETED.")
    public ResponseEntity<ApiResponse<ReminderDto>> completeReminder(@PathVariable UUID id) {
        ReminderDto dto = reminderService.completeReminder(id);
        return ResponseEntity.ok(ApiResponse.success("Reminder completed successfully", dto));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE')")
    @Operation(summary = "Cancel a reminder", description = "Marks a PENDING or TRIGGERED reminder as CANCELLED.")
    public ResponseEntity<ApiResponse<ReminderDto>> cancelReminder(@PathVariable UUID id) {
        ReminderDto dto = reminderService.cancelReminder(id);
        return ResponseEntity.ok(ApiResponse.success("Reminder cancelled successfully", dto));
    }

    @GetMapping("/overdue/count")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE')")
    @Operation(summary = "Get overdue reminder count", description = "Returns count of overdue PENDING reminders for the current user.")
    public ResponseEntity<ApiResponse<Long>> countOverdue() {
        long count = reminderService.countOverdueForCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Overdue count retrieved", count));
    }

    // =========================================================================
    // Phase 32.6: Collaboration Follow-up & Reminder Engine Endpoints
    // =========================================================================

    @GetMapping("/follow-ups")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE')")
    @Operation(summary = "Get collaboration follow-ups", description = "Retrieves paginated follow-up items across client approvals, document requests, messages, and compliance deadlines.")
    public ResponseEntity<ApiResponse<PagedResponse<FollowUpItemDto>>> getFollowUps(@ModelAttribute FollowUpFilterRequest filter) {
        PagedResponse<FollowUpItemDto> response = followUpReminderService.getFollowUps(filter);
        return ResponseEntity.ok(ApiResponse.success("Follow-ups retrieved successfully", response));
    }

    @PostMapping("/follow-ups/evaluate")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Evaluate collaboration follow-ups", description = "Triggers deterministic evaluation of pending client actions and generates reminder candidates.")
    public ResponseEntity<ApiResponse<FollowUpEvaluationResultDto>> evaluateFollowUps() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        FollowUpEvaluationResultDto result = followUpReminderService.evaluateFollowUps(orgId);
        return ResponseEntity.ok(ApiResponse.success("Follow-up evaluation completed", result));
    }

    @PostMapping("/follow-ups/{id}/nudge")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE')")
    @Operation(summary = "Send follow-up nudge", description = "Sends an on-demand follow-up nudge to the client for the specified action (enforces 12h cooldown).")
    public ResponseEntity<ApiResponse<FollowUpItemDto>> sendFollowUpNudge(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) SendFollowUpNudgeRequest request) {
        FollowUpItemDto dto = followUpReminderService.sendFollowUpNudge(id, request);
        return ResponseEntity.ok(ApiResponse.success("Follow-up nudge sent successfully", dto));
    }

    @PostMapping("/follow-ups/{id}/resolve")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE')")
    @Operation(summary = "Resolve follow-up item", description = "Marks a follow-up reminder as completed or dismissed.")
    public ResponseEntity<ApiResponse<FollowUpItemDto>> resolveFollowUp(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes) {
        FollowUpItemDto dto = followUpReminderService.resolveFollowUp(id, notes);
        return ResponseEntity.ok(ApiResponse.success("Follow-up resolved successfully", dto));
    }
}

