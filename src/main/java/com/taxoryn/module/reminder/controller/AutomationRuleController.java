package com.taxoryn.module.reminder.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.reminder.dto.AutomationRuleDto;
import com.taxoryn.module.reminder.dto.SaveAutomationRuleRequest;
import com.taxoryn.module.reminder.service.AutomationService;
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
@RequestMapping("/api/v1/automation-rules")
@RequiredArgsConstructor
@Tag(name = "Automation Rules", description = "Endpoints for managing automation rules that generate reminders from business events")
@SecurityRequirement(name = "BearerAuth")
public class AutomationRuleController {

    private final AutomationService automationService;

    @GetMapping
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "List all automation rules", description = "Returns org-specific rules and system defaults.")
    public ResponseEntity<ApiResponse<List<AutomationRuleDto>>> listRules() {
        List<AutomationRuleDto> rules = automationService.listRules();
        return ResponseEntity.ok(ApiResponse.success("Automation rules retrieved successfully", rules));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Get automation rule by ID")
    public ResponseEntity<ApiResponse<AutomationRuleDto>> getRule(@PathVariable UUID id) {
        AutomationRuleDto rule = automationService.getRule(id);
        return ResponseEntity.ok(ApiResponse.success("Automation rule retrieved successfully", rule));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Create an automation rule", description = "Creates a new org-specific automation rule.")
    public ResponseEntity<ApiResponse<AutomationRuleDto>> createRule(@Valid @RequestBody SaveAutomationRuleRequest request) {
        AutomationRuleDto rule = automationService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Automation rule created successfully", rule));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Update an automation rule", description = "Updates an org-specific automation rule. System defaults cannot be modified.")
    public ResponseEntity<ApiResponse<AutomationRuleDto>> updateRule(@PathVariable UUID id, @Valid @RequestBody SaveAutomationRuleRequest request) {
        AutomationRuleDto rule = automationService.updateRule(id, request);
        return ResponseEntity.ok(ApiResponse.success("Automation rule updated successfully", rule));
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Enable an automation rule")
    public ResponseEntity<ApiResponse<AutomationRuleDto>> enableRule(@PathVariable UUID id) {
        AutomationRuleDto rule = automationService.enableRule(id);
        return ResponseEntity.ok(ApiResponse.success("Automation rule enabled", rule));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Disable an automation rule")
    public ResponseEntity<ApiResponse<AutomationRuleDto>> disableRule(@PathVariable UUID id) {
        AutomationRuleDto rule = automationService.disableRule(id);
        return ResponseEntity.ok(ApiResponse.success("Automation rule disabled", rule));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Delete an automation rule", description = "Deletes an org-specific automation rule. System defaults cannot be deleted.")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable UUID id) {
        automationService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Automation rule deleted successfully", null));
    }
}
