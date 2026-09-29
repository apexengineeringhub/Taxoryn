package com.taxoryn.module.worktemplate.controller;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.worktemplate.dto.EnableEngagementTemplateRequest;
import com.taxoryn.module.worktemplate.dto.EngagementWorkTemplateDto;
import com.taxoryn.module.worktemplate.dto.GenerateWorkInstanceRequest;
import com.taxoryn.module.worktemplate.dto.WorkInstanceDto;
import com.taxoryn.module.worktemplate.dto.WorkTemplateDto;
import com.taxoryn.module.worktemplate.service.EngagementWorkService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/engagements", "/api/engagements"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Engagement Work & Templates", description = "Endpoints for attaching work templates to engagements and generating recurring work instances")
@SecurityRequirement(name = "BearerAuth")
public class EngagementWorkController {

    private final EngagementWorkService engagementWorkService;

    @GetMapping("/{engagementId}/work-templates/available")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get available work templates", description = "Retrieves active work templates compatible with the engagement's service.")
    public ResponseEntity<ApiResponse<List<WorkTemplateDto>>> getAvailableTemplates(@PathVariable UUID engagementId) {
        List<WorkTemplateDto> list = engagementWorkService.getAvailableTemplatesForEngagement(engagementId);
        return ResponseEntity.ok(ApiResponse.success("Available templates retrieved successfully", list));
    }

    @GetMapping("/{engagementId}/work-templates")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get enabled work templates", description = "Retrieves templates configured for the engagement.")
    public ResponseEntity<ApiResponse<List<EngagementWorkTemplateDto>>> getEnabledTemplates(@PathVariable UUID engagementId) {
        List<EngagementWorkTemplateDto> list = engagementWorkService.getEnabledTemplatesForEngagement(engagementId);
        return ResponseEntity.ok(ApiResponse.success("Enabled engagement templates retrieved successfully", list));
    }

    @PostMapping("/{engagementId}/work-templates/{templateId}/enable")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Enable template for engagement", description = "Enables a compatible work template for an engagement.")
    public ResponseEntity<ApiResponse<EngagementWorkTemplateDto>> enableTemplate(
            @PathVariable UUID engagementId,
            @PathVariable UUID templateId,
            @RequestBody(required = false) EnableEngagementTemplateRequest request) {
        EngagementWorkTemplateDto enabled = engagementWorkService.enableTemplateForEngagement(engagementId, templateId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Template enabled for engagement successfully", enabled));
    }

    @DeleteMapping("/{engagementId}/work-templates/{templateId}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Disable template for engagement", description = "Disables an enabled work template from recurring work generation.")
    public ResponseEntity<ApiResponse<Void>> disableTemplate(
            @PathVariable UUID engagementId,
            @PathVariable UUID templateId) {
        engagementWorkService.disableTemplateForEngagement(engagementId, templateId);
        return ResponseEntity.ok(ApiResponse.success("Template disabled for engagement successfully", null));
    }

    @PostMapping("/{engagementId}/work/generate")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasAuthority('TASK_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Generate work instance", description = "Generates a concrete work instance and unified tasks from a template with duplicate protection.")
    public ResponseEntity<ApiResponse<WorkInstanceDto>> generateWorkInstance(
            @PathVariable UUID engagementId,
            @Valid @RequestBody GenerateWorkInstanceRequest request) {
        WorkInstanceDto generated = engagementWorkService.generateWorkInstance(engagementId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Work instance generated successfully", generated));
    }

    @GetMapping("/{engagementId}/work")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "List work instances for engagement", description = "Retrieves paginated work instances generated under this engagement.")
    public ResponseEntity<ApiResponse<PagedResponse<WorkInstanceDto>>> getWorkInstances(
            @PathVariable UUID engagementId,
            @Valid @ModelAttribute PageRequestDto pageRequest) {
        PagedResponse<WorkInstanceDto> response = engagementWorkService.getWorkInstancesForEngagement(engagementId, pageRequest);
        return ResponseEntity.ok(ApiResponse.success("Work instances retrieved successfully", response));
    }
}
