package com.taxoryn.module.workflow.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.workflow.dto.AssignWorkflowRequest;
import com.taxoryn.module.workflow.dto.ClientServicePeriodDto;
import com.taxoryn.module.workflow.dto.ClientServiceWorkflowDto;
import com.taxoryn.module.workflow.dto.ClientServiceWorkflowStepDto;
import com.taxoryn.module.workflow.dto.CreateServicePeriodRequest;
import com.taxoryn.module.workflow.dto.GenerateWorkflowRequest;
import com.taxoryn.module.workflow.dto.ServiceWorkflowTemplateDto;
import com.taxoryn.module.workflow.dto.UpdateWorkflowPriorityRequest;
import com.taxoryn.module.workflow.dto.UpdateWorkflowStatusRequest;
import com.taxoryn.module.workflow.dto.UpdateWorkflowStepStatusRequest;
import com.taxoryn.module.workflow.dto.WorkflowFilterRequest;
import com.taxoryn.module.workflow.service.ClientServiceWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClientServiceWorkflowController {

    private final ClientServiceWorkflowService workflowService;

    @PostMapping("/client-services/{serviceId}/periods")
    @PreAuthorize("hasAnyAuthority('CLIENT_UPDATE', 'TASK_CREATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ClientServicePeriodDto>> createServicePeriod(
            @PathVariable UUID serviceId,
            @Valid @RequestBody CreateServicePeriodRequest request
    ) {
        request.setClientServiceId(serviceId);
        ClientServicePeriodDto created = workflowService.createServicePeriod(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service compliance period created successfully", created));
    }

    @GetMapping("/client-services/{serviceId}/periods")
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'TASK_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<ClientServicePeriodDto>>> getServicePeriods(
            @PathVariable UUID serviceId
    ) {
        List<ClientServicePeriodDto> list = workflowService.getServicePeriods(serviceId);
        return ResponseEntity.ok(ApiResponse.success("Service compliance periods retrieved successfully", list));
    }

    @PostMapping("/client-services/{serviceId}/workflows/generate")
    @PreAuthorize("hasAnyAuthority('CLIENT_UPDATE', 'TASK_CREATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ClientServiceWorkflowDto>> generateWorkflow(
            @PathVariable UUID serviceId,
            @Valid @RequestBody GenerateWorkflowRequest request
    ) {
        request.setClientServiceId(serviceId);
        ClientServiceWorkflowDto workflow = workflowService.generateWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Client service workflow generated successfully", workflow));
    }

    @GetMapping("/client-services/{serviceId}/workflows")
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'TASK_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<ClientServiceWorkflowDto>>> getWorkflowsForService(
            @PathVariable UUID serviceId
    ) {
        List<ClientServiceWorkflowDto> list = workflowService.getWorkflowsForService(serviceId);
        return ResponseEntity.ok(ApiResponse.success("Service workflows retrieved successfully", list));
    }

    @GetMapping("/client-services/{serviceId}/workflows/active")
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'TASK_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ClientServiceWorkflowDto>> getActiveWorkflowForService(
            @PathVariable UUID serviceId
    ) {
        ClientServiceWorkflowDto workflow = workflowService.getActiveWorkflowForService(serviceId);
        return ResponseEntity.ok(ApiResponse.success("Active service workflow retrieved successfully", workflow));
    }

    @GetMapping("/service-workflows/{workflowId}")
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'TASK_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ClientServiceWorkflowDto>> getWorkflowById(
            @PathVariable UUID workflowId
    ) {
        ClientServiceWorkflowDto workflow = workflowService.getWorkflowById(workflowId);
        return ResponseEntity.ok(ApiResponse.success("Workflow retrieved successfully", workflow));
    }

    @GetMapping("/service-workflows/worklist")
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'TASK_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<PagedResponse<ClientServiceWorkflowDto>>> getWorklist(
            WorkflowFilterRequest filter,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PagedResponse<ClientServiceWorkflowDto> paged = workflowService.getWorklist(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success("Operational worklist retrieved successfully", paged));
    }

    @PatchMapping("/service-workflows/{workflowId}/status")
    @PreAuthorize("hasAnyAuthority('CLIENT_UPDATE', 'TASK_UPDATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ClientServiceWorkflowDto>> updateWorkflowStatus(
            @PathVariable UUID workflowId,
            @Valid @RequestBody UpdateWorkflowStatusRequest request
    ) {
        ClientServiceWorkflowDto updated = workflowService.updateWorkflowStatus(workflowId, request);
        return ResponseEntity.ok(ApiResponse.success("Workflow status updated successfully", updated));
    }

    @PatchMapping("/service-workflows/{workflowId}/steps/{stepId}/status")
    @PreAuthorize("hasAnyAuthority('CLIENT_UPDATE', 'TASK_UPDATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ClientServiceWorkflowStepDto>> updateStepStatus(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @Valid @RequestBody UpdateWorkflowStepStatusRequest request
    ) {
        ClientServiceWorkflowStepDto updated = workflowService.updateStepStatus(workflowId, stepId, request);
        return ResponseEntity.ok(ApiResponse.success("Workflow step status updated successfully", updated));
    }

    @PatchMapping("/service-workflows/{workflowId}/assignment")
    @PreAuthorize("hasAnyAuthority('CLIENT_UPDATE', 'TASK_UPDATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ClientServiceWorkflowDto>> assignWorkflow(
            @PathVariable UUID workflowId,
            @Valid @RequestBody AssignWorkflowRequest request
    ) {
        ClientServiceWorkflowDto updated = workflowService.assignWorkflow(workflowId, request);
        return ResponseEntity.ok(ApiResponse.success("Workflow assigned successfully", updated));
    }

    @PatchMapping("/service-workflows/{workflowId}/steps/{stepId}/assignment")
    @PreAuthorize("hasAnyAuthority('CLIENT_UPDATE', 'TASK_UPDATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ClientServiceWorkflowDto>> assignStep(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @Valid @RequestBody AssignWorkflowRequest request
    ) {
        ClientServiceWorkflowDto updated = workflowService.assignStep(workflowId, stepId, request);
        return ResponseEntity.ok(ApiResponse.success("Step assigned successfully", updated));
    }

    @PatchMapping("/service-workflows/{workflowId}/priority")
    @PreAuthorize("hasAnyAuthority('CLIENT_UPDATE', 'TASK_UPDATE', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ClientServiceWorkflowDto>> updateWorkflowPriority(
            @PathVariable UUID workflowId,
            @Valid @RequestBody UpdateWorkflowPriorityRequest request
    ) {
        ClientServiceWorkflowDto updated = workflowService.updateWorkflowPriority(workflowId, request);
        return ResponseEntity.ok(ApiResponse.success("Workflow priority updated successfully", updated));
    }

    @GetMapping("/service-workflows/templates")
    @PreAuthorize("hasAnyAuthority('CLIENT_VIEW', 'TASK_VIEW', 'ROLE_ORG_ADMIN', 'ROLE_ADMIN', 'ROLE_PRACTITIONER', 'ROLE_STAFF', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<ServiceWorkflowTemplateDto>>> getWorkflowTemplates(
            @RequestParam(required = false) ClientServiceType serviceType
    ) {
        List<ServiceWorkflowTemplateDto> list = workflowService.getWorkflowTemplates(serviceType);
        return ResponseEntity.ok(ApiResponse.success("Workflow templates retrieved successfully", list));
    }
}
