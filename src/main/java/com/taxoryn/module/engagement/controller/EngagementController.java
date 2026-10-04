package com.taxoryn.module.engagement.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.engagement.dto.CreateEngagementRequest;
import com.taxoryn.module.engagement.dto.EngagementDto;
import com.taxoryn.module.engagement.dto.EngagementFilterRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementAssignmentRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementStatusRequest;
import com.taxoryn.module.engagement.service.EngagementService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.task.dto.TaskDto;
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
@RequestMapping({"/api/v1/engagements", "/api/engagements"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Client Engagement Management", description = "Endpoints for managing client service engagements and professional mandates")
@SecurityRequirement(name = "BearerAuth")
public class EngagementController {

    private final EngagementService engagementService;
    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_CREATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Create engagement", description = "Creates a new client service engagement.")
    public ResponseEntity<ApiResponse<EngagementDto>> createEngagement(@Valid @RequestBody CreateEngagementRequest request) {
        EngagementDto created = engagementService.createEngagement(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Engagement created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "List engagements with filters", description = "Retrieves paginated engagements for the authenticated practice tenant.")
    public ResponseEntity<ApiResponse<PagedResponse<EngagementDto>>> getEngagements(@Valid @ModelAttribute EngagementFilterRequest filterRequest) {
        PagedResponse<EngagementDto> response = engagementService.getEngagements(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Engagements retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get engagement by ID", description = "Retrieves engagement details.")
    public ResponseEntity<ApiResponse<EngagementDto>> getEngagementById(@PathVariable UUID id) {
        EngagementDto dto = engagementService.getEngagementById(id);
        return ResponseEntity.ok(ApiResponse.success("Engagement retrieved successfully", dto));
    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get engagements by client ID", description = "Retrieves all engagements for a specific client (Client 360).")
    public ResponseEntity<ApiResponse<List<EngagementDto>>> getEngagementsByClientId(@PathVariable UUID clientId) {
        List<EngagementDto> list = engagementService.getEngagementsByClientId(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client engagements retrieved successfully", list));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Update engagement", description = "Updates engagement details.")
    public ResponseEntity<ApiResponse<EngagementDto>> updateEngagement(@PathVariable UUID id, @Valid @RequestBody UpdateEngagementRequest request) {
        EngagementDto updated = engagementService.updateEngagement(id, request);
        return ResponseEntity.ok(ApiResponse.success("Engagement updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Update engagement status", description = "Updates engagement lifecycle status.")
    public ResponseEntity<ApiResponse<EngagementDto>> updateEngagementStatus(@PathVariable UUID id, @Valid @RequestBody UpdateEngagementStatusRequest request) {
        EngagementDto updated = engagementService.updateEngagementStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Engagement status updated successfully", updated));
    }

    @PatchMapping("/{id}/assignment")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Update engagement assignment", description = "Updates assigned practitioner and/or reviewer.")
    public ResponseEntity<ApiResponse<EngagementDto>> updateEngagementAssignment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEngagementAssignmentRequest request) {
        EngagementDto updated = engagementService.updateEngagementAssignment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Engagement assignment updated successfully", updated));
    }

    @GetMapping("/{id}/tasks")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get tasks by engagement ID", description = "Retrieves all tasks associated with a specific engagement.")
    public ResponseEntity<ApiResponse<List<TaskDto>>> getTasksByEngagementId(@PathVariable UUID id) {
        List<TaskDto> tasks = taskService.getTasksByEngagementId(id);
        return ResponseEntity.ok(ApiResponse.success("Engagement tasks retrieved successfully", tasks));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_DELETE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Delete engagement", description = "Deletes engagement record.")
    public ResponseEntity<ApiResponse<Void>> deleteEngagement(@PathVariable UUID id) {
        engagementService.deleteEngagement(id);
        return ResponseEntity.ok(ApiResponse.success("Engagement deleted successfully", null));
    }
}
