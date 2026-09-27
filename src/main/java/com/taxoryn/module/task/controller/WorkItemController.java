package com.taxoryn.module.task.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.task.dto.AssignWorkItemRequest;
import com.taxoryn.module.task.dto.CreateTaskRequest;
import com.taxoryn.module.task.dto.CreateWorkItemRequest;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.UpdateWorkItemRequest;
import com.taxoryn.module.task.dto.UpdateWorkItemStatusRequest;
import com.taxoryn.module.task.dto.WorkItemDto;
import com.taxoryn.module.task.dto.WorkItemFilterRequest;
import com.taxoryn.module.task.service.WorkItemService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-items")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.TASKS)
@Tag(name = "Work Item Management", description = "Endpoints for generic work item breakdown, lifecycle tracking, and workflow tasks")
@SecurityRequirement(name = "BearerAuth")
public class WorkItemController {

    private final WorkItemService workItemService;

    @PostMapping
    @PreAuthorize("hasAuthority('TASK_CREATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Create Work Item", description = "Creates a new generic work item within the authenticated tenant.")
    public ResponseEntity<ApiResponse<WorkItemDto>> createWorkItem(@Valid @RequestBody CreateWorkItemRequest request) {
        WorkItemDto created = workItemService.createWorkItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Work Item created successfully", created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Get Work Item by ID", description = "Retrieves a single work item with task execution metrics.")
    public ResponseEntity<ApiResponse<WorkItemDto>> getWorkItemById(@PathVariable UUID id) {
        WorkItemDto dto = workItemService.getWorkItemById(id);
        return ResponseEntity.ok(ApiResponse.success("Work Item retrieved successfully", dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Update Work Item", description = "Updates details of an existing work item.")
    public ResponseEntity<ApiResponse<WorkItemDto>> updateWorkItem(@PathVariable UUID id, @Valid @RequestBody UpdateWorkItemRequest request) {
        WorkItemDto updated = workItemService.updateWorkItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Work Item updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Update Work Item status", description = "Transitions the status of a work item.")
    public ResponseEntity<ApiResponse<WorkItemDto>> patchWorkItemStatus(@PathVariable UUID id, @Valid @RequestBody UpdateWorkItemStatusRequest request) {
        WorkItemDto updated = workItemService.updateWorkItemStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Work Item status updated successfully", updated));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Update Work Item status (PUT)", description = "Transitions the status of a work item.")
    public ResponseEntity<ApiResponse<WorkItemDto>> putWorkItemStatus(@PathVariable UUID id, @Valid @RequestBody UpdateWorkItemStatusRequest request) {
        WorkItemDto updated = workItemService.updateWorkItemStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Work Item status updated successfully", updated));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Assign Work Item", description = "Assigns a work item to a user.")
    public ResponseEntity<ApiResponse<WorkItemDto>> patchWorkItemAssignment(@PathVariable UUID id, @Valid @RequestBody AssignWorkItemRequest request) {
        WorkItemDto updated = workItemService.assignWorkItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Work Item assigned successfully", updated));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Assign Work Item (PUT)", description = "Assigns a work item to a user.")
    public ResponseEntity<ApiResponse<WorkItemDto>> putWorkItemAssignment(@PathVariable UUID id, @Valid @RequestBody AssignWorkItemRequest request) {
        WorkItemDto updated = workItemService.assignWorkItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Work Item assigned successfully", updated));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "List Work Items", description = "Retrieves filtered and paginated work items.")
    public ResponseEntity<ApiResponse<PagedResponse<WorkItemDto>>> getWorkItems(@Valid @ModelAttribute WorkItemFilterRequest filterRequest) {
        PagedResponse<WorkItemDto> response = workItemService.getWorkItems(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Work items retrieved successfully", response));
    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Get Work Items for Client", description = "Retrieves all work items associated with a client (Client 360).")
    public ResponseEntity<ApiResponse<List<WorkItemDto>>> getWorkItemsByClientId(@PathVariable UUID clientId) {
        List<WorkItemDto> items = workItemService.getWorkItemsByClientId(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client work items retrieved successfully", items));
    }

    @GetMapping("/workflows/{workflowId}")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Get Work Items for Workflow", description = "Retrieves all work items associated with a compliance workflow.")
    public ResponseEntity<ApiResponse<List<WorkItemDto>>> getWorkItemsByWorkflowId(@PathVariable UUID workflowId) {
        List<WorkItemDto> items = workItemService.getWorkItemsByWorkflowId(workflowId);
        return ResponseEntity.ok(ApiResponse.success("Workflow work items retrieved successfully", items));
    }

    @PostMapping("/{id}/tasks")
    @PreAuthorize("hasAuthority('TASK_CREATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "Create task under Work Item", description = "Creates a new task linked directly to this work item.")
    public ResponseEntity<ApiResponse<TaskDto>> createTaskForWorkItem(@PathVariable UUID id, @Valid @RequestBody CreateTaskRequest request) {
        TaskDto created = workItemService.createTaskForWorkItem(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Task created under Work Item successfully", created));
    }

    @GetMapping("/{id}/tasks")
    @PreAuthorize("hasAuthority('TASK_VIEW') or hasAuthority('TASK_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTITIONER') or hasRole('STAFF')")
    @Operation(summary = "List tasks for Work Item", description = "Retrieves all tasks under this work item.")
    public ResponseEntity<ApiResponse<List<TaskDto>>> getTasksForWorkItem(@PathVariable UUID id) {
        List<TaskDto> tasks = workItemService.getTasksForWorkItem(id);
        return ResponseEntity.ok(ApiResponse.success("Tasks for Work Item retrieved successfully", tasks));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_UPDATE') or hasAuthority('TASK_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Cancel Work Item", description = "Cancels a work item.")
    public ResponseEntity<ApiResponse<Void>> deleteWorkItem(@PathVariable UUID id) {
        workItemService.deleteWorkItem(id);
        return ResponseEntity.ok(ApiResponse.success("Work Item cancelled successfully", null));
    }
}
