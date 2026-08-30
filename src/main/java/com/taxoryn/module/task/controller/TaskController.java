package com.taxoryn.module.task.controller;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.UpdateTaskRequest;
import com.taxoryn.module.task.service.TaskService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Management", description = "Endpoints for creating and tracking client tasks and assignments")
@SecurityRequirement(name = "BearerAuth")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "List tasks with filters and pagination", description = "Retrieves filtered and paginated tasks for the authenticated tenant.")
    public ResponseEntity<ApiResponse<PagedResponse<TaskDto>>> getTasks(@Valid @ModelAttribute com.taxoryn.module.task.dto.TaskFilterRequest filterRequest) {
        PagedResponse<TaskDto> response = taskService.getTasks(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", response));
    }

    @GetMapping("/worklist")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Get unified worklist with urgency buckets and sorting", description = "Retrieves actionable tasks categorized into buckets (OVERDUE, DUE_TODAY, DUE_THIS_WEEK, BLOCKED, COMPLETED, ALL).")
    public ResponseEntity<ApiResponse<PagedResponse<TaskDto>>> getWorklist(@Valid @ModelAttribute com.taxoryn.module.task.dto.TaskWorklistFilterRequest filterRequest) {
        PagedResponse<TaskDto> response = taskService.getWorklist(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Worklist retrieved successfully", response));
    }

    @GetMapping("/worklist/summary")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Get worklist summary metrics", description = "Calculates today's actionable work metrics (overdue, due today, due this week, blocked on docs, pending documents, team workload).")
    public ResponseEntity<ApiResponse<com.taxoryn.module.task.dto.WorklistSummaryDto>> getWorklistSummary() {
        com.taxoryn.module.task.dto.WorklistSummaryDto summary = taskService.getWorklistSummary();
        return ResponseEntity.ok(ApiResponse.success("Worklist summary metrics retrieved successfully", summary));
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Get task by ID", description = "Retrieves task details within the authenticated tenant.")
    public ResponseEntity<ApiResponse<TaskDto>> getTaskById(@PathVariable UUID taskId) {
        TaskDto dto = taskService.getTaskById(taskId);
        return ResponseEntity.ok(ApiResponse.success("Task retrieved successfully", dto));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TASK_CREATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Create task", description = "Creates a new workflow task within the authenticated tenant.")
    public ResponseEntity<ApiResponse<TaskDto>> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskDto created = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Task created successfully", created));
    }

    @PostMapping("/bulk-generator")
    @PreAuthorize("hasAuthority('TASK_CREATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Generate bulk tasks across multiple clients", description = "Generates recurring compliance tasks for all selected clients in a single batch.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.task.dto.BulkTaskImportResultDto>> generateBulkTasks(@Valid @RequestBody com.taxoryn.module.task.dto.BulkTaskCreateRequest request) {
        com.taxoryn.module.task.dto.BulkTaskImportResultDto result = taskService.generateBulkTasks(request);
        return ResponseEntity.ok(ApiResponse.success("Bulk task generation batch completed", result));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('TASK_CREATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Bulk import tasks from spreadsheet", description = "Imports a list of task records from CSV or Excel sheets.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.task.dto.BulkTaskImportResultDto>> bulkCreateTasks(@RequestBody java.util.List<CreateTaskRequest> requests) {
        com.taxoryn.module.task.dto.BulkTaskImportResultDto result = taskService.bulkCreateTasks(requests);
        return ResponseEntity.ok(ApiResponse.success("Bulk task import completed", result));
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('ARTICLE_ASSISTANT') or hasRole('STAFF')")
    @Operation(summary = "Update task", description = "Updates task status and details within the authenticated tenant.")
    public ResponseEntity<ApiResponse<TaskDto>> updateTask(@PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request) {
        TaskDto updated = taskService.updateTask(taskId, request);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", updated));
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Cancel task", description = "Cancels task within the authenticated tenant.")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable UUID taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok(ApiResponse.success("Task cancelled successfully", null));
    }
}
