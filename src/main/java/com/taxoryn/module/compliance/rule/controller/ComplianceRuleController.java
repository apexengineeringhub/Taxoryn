package com.taxoryn.module.compliance.rule.controller;

import com.taxoryn.module.compliance.rule.dto.ComplianceRuleCatalogSummaryDto;
import com.taxoryn.module.compliance.rule.dto.ComplianceRuleDto;
import com.taxoryn.module.compliance.rule.dto.CreateComplianceRuleRequest;
import com.taxoryn.module.compliance.rule.dto.UpdateComplianceRuleRequest;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleStatus;
import com.taxoryn.module.compliance.rule.service.ComplianceRuleCatalogService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/compliance/rules")
@RequiredArgsConstructor
@Validated
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Compliance Rule Catalog", description = "Endpoints for exploring and managing reusable compliance rules")
@SecurityRequirement(name = "BearerAuth")
public class ComplianceRuleController {

    private final ComplianceRuleCatalogService complianceRuleCatalogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'ACCOUNTANT')")
    @Operation(summary = "List and filter compliance rules from the catalog")
    public ResponseEntity<List<ComplianceRuleDto>> getRules(
            @RequestParam(required = false) ComplianceRuleDomain domain,
            @RequestParam(required = false) ComplianceRuleFrequency frequency,
            @RequestParam(required = false) ComplianceRuleStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "true") Boolean includeSystem
    ) {
        List<ComplianceRuleDto> rules = complianceRuleCatalogService.getRules(domain, frequency, status, search, includeSystem);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'ACCOUNTANT')")
    @Operation(summary = "Get Compliance Rule Catalog summary metrics")
    public ResponseEntity<ComplianceRuleCatalogSummaryDto> getCatalogSummary() {
        return ResponseEntity.ok(complianceRuleCatalogService.getCatalogSummary());
    }

    @GetMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'ACCOUNTANT')")
    @Operation(summary = "Get a compliance rule by ID")
    public ResponseEntity<ComplianceRuleDto> getRuleById(@PathVariable UUID ruleId) {
        return ResponseEntity.ok(complianceRuleCatalogService.getRuleById(ruleId));
    }

    @GetMapping("/by-code/{ruleCode}")
    @PreAuthorize("hasAnyRole('TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'ACCOUNTANT')")
    @Operation(summary = "Get a compliance rule by code")
    public ResponseEntity<ComplianceRuleDto> getRuleByCode(@PathVariable String ruleCode) {
        return ResponseEntity.ok(complianceRuleCatalogService.getRuleByCode(ruleCode));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER')")
    @Operation(summary = "Create a custom compliance rule in the practice catalog")
    public ResponseEntity<ComplianceRuleDto> createCustomRule(@Valid @RequestBody CreateComplianceRuleRequest request) {
        ComplianceRuleDto created = complianceRuleCatalogService.createCustomRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER')")
    @Operation(summary = "Update an existing custom compliance rule")
    public ResponseEntity<ComplianceRuleDto> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateComplianceRuleRequest request
    ) {
        ComplianceRuleDto updated = complianceRuleCatalogService.updateRule(ruleId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER')")
    @Operation(summary = "Deactivate/Delete a custom compliance rule")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId) {
        complianceRuleCatalogService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }
}
