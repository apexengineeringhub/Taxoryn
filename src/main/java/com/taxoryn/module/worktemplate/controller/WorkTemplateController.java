package com.taxoryn.module.worktemplate.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.worktemplate.dto.CreateWorkTemplateRequest;
import com.taxoryn.module.worktemplate.dto.CreateWorkTemplateTaskRequest;
import com.taxoryn.module.worktemplate.dto.ReorderTasksRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkTemplateRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkTemplateStatusRequest;
import com.taxoryn.module.worktemplate.dto.UpdateWorkTemplateTaskRequest;
import com.taxoryn.module.worktemplate.dto.WorkTemplateDto;
import com.taxoryn.module.worktemplate.dto.WorkTemplateFilterRequest;
import com.taxoryn.module.worktemplate.dto.WorkTemplateTaskDto;
import com.taxoryn.module.worktemplate.service.WorkTemplateService;
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
@RequestMapping({"/api/v1/work-templates", "/api/work-templates"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Work Templates Management", description = "Endpoints for managing reusable practice work templates and template tasks")
@SecurityRequirement(name = "BearerAuth")
public class WorkTemplateController {

    private final WorkTemplateService workTemplateService;

    @PostMapping
    @PreAuthorize("hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Create work template", description = "Creates a new practice work template with optional initial tasks.")
    public ResponseEntity<ApiResponse<WorkTemplateDto>> createTemplate(@Valid @RequestBody CreateWorkTemplateRequest request) {
        WorkTemplateDto created = workTemplateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Work template created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "List work templates", description = "Retrieves paginated work templates for the practice with filtering support.")
    public ResponseEntity<ApiResponse<PagedResponse<WorkTemplateDto>>> getTemplates(@Valid @ModelAttribute WorkTemplateFilterRequest filterRequest) {
        PagedResponse<WorkTemplateDto> response = workTemplateService.getTemplates(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Work templates retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get work template by ID", description = "Retrieves work template header and ordered tasks.")
    public ResponseEntity<ApiResponse<WorkTemplateDto>> getTemplateById(@PathVariable UUID id) {
        WorkTemplateDto dto = workTemplateService.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.success("Work template retrieved successfully", dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Update work template", description = "Updates work template metadata and recurrence properties.")
    public ResponseEntity<ApiResponse<WorkTemplateDto>> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkTemplateRequest request) {
        WorkTemplateDto updated = workTemplateService.updateTemplate(id, request);
        return ResponseEntity.ok(ApiResponse.success("Work template updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Update template status", description = "Activates, deactivates, or archives a work template.")
    public ResponseEntity<ApiResponse<WorkTemplateDto>> updateTemplateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkTemplateStatusRequest request) {
        WorkTemplateDto updated = workTemplateService.updateTemplateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Work template status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Delete work template", description = "Deletes a practice work template.")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable UUID id) {
        workTemplateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Work template deleted successfully", null));
    }

    // --- Template Tasks Sub-resource Endpoints ---

    @GetMapping("/{templateId}/tasks")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "List template tasks", description = "Retrieves all task definitions for a template.")
    public ResponseEntity<ApiResponse<List<WorkTemplateTaskDto>>> getTemplateTasks(@PathVariable UUID templateId) {
        List<WorkTemplateTaskDto> tasks = workTemplateService.getTemplateTasks(templateId);
        return ResponseEntity.ok(ApiResponse.success("Template tasks retrieved successfully", tasks));
    }

    @PostMapping("/{templateId}/tasks")
    @PreAuthorize("hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Add task to template", description = "Appends a new task definition to the template.")
    public ResponseEntity<ApiResponse<WorkTemplateTaskDto>> addTemplateTask(
            @PathVariable UUID templateId,
            @Valid @RequestBody CreateWorkTemplateTaskRequest request) {
        WorkTemplateTaskDto created = workTemplateService.addTemplateTask(templateId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Template task added successfully", created));
    }

    @PutMapping("/{templateId}/tasks/{taskId}")
    @PreAuthorize("hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Update template task", description = "Updates an existing template task definition.")
    public ResponseEntity<ApiResponse<WorkTemplateTaskDto>> updateTemplateTask(
            @PathVariable UUID templateId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateWorkTemplateTaskRequest request) {
        WorkTemplateTaskDto updated = workTemplateService.updateTemplateTask(templateId, taskId, request);
        return ResponseEntity.ok(ApiResponse.success("Template task updated successfully", updated));
    }

    @DeleteMapping("/{templateId}/tasks/{taskId}")
    @PreAuthorize("hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Delete template task", description = "Removes a task definition from the template.")
    public ResponseEntity<ApiResponse<Void>> deleteTemplateTask(
            @PathVariable UUID templateId,
            @PathVariable UUID taskId) {
        workTemplateService.deleteTemplateTask(templateId, taskId);
        return ResponseEntity.ok(ApiResponse.success("Template task removed successfully", null));
    }

    @PatchMapping("/{templateId}/tasks/reorder")
    @PreAuthorize("hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Reorder template tasks", description = "Reorders sequence orders of template tasks.")
    public ResponseEntity<ApiResponse<List<WorkTemplateTaskDto>>> reorderTemplateTasks(
            @PathVariable UUID templateId,
            @Valid @RequestBody ReorderTasksRequest request) {
        List<WorkTemplateTaskDto> reordered = workTemplateService.reorderTemplateTasks(templateId, request);
        return ResponseEntity.ok(ApiResponse.success("Template tasks reordered successfully", reordered));
    }
}
