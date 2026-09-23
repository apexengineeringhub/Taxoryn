package com.taxoryn.module.compliance.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.compliance.dto.AssignComplianceWorkRequest;
import com.taxoryn.module.compliance.dto.ComplianceWorkFilterRequest;
import com.taxoryn.module.compliance.dto.ComplianceWorkItemDto;
import com.taxoryn.module.compliance.dto.CreateComplianceWorkItemRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceWorkItemRequest;
import com.taxoryn.module.compliance.dto.UpdateComplianceWorkStatusRequest;
import com.taxoryn.module.compliance.service.ComplianceWorkItemService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ComplianceWorkItemController {

    private final ComplianceWorkItemService complianceWorkItemService;

    @PostMapping("/compliance-work")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ComplianceWorkItemDto>> createComplianceWork(
            @Valid @RequestBody CreateComplianceWorkItemRequest request
    ) {
        ComplianceWorkItemDto created = complianceWorkItemService.createComplianceWorkItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Compliance work item created successfully", created));
    }

    @GetMapping("/compliance-work/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ComplianceWorkItemDto>> getComplianceWorkById(
            @PathVariable UUID id
    ) {
        ComplianceWorkItemDto item = complianceWorkItemService.getComplianceWorkItemById(id);
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    @GetMapping("/compliance-work")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<ComplianceWorkItemDto>>> getComplianceWorkItems(
            @ModelAttribute ComplianceWorkFilterRequest filterRequest
    ) {
        PagedResponse<ComplianceWorkItemDto> response = complianceWorkItemService.getComplianceWorkItems(filterRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/clients/{clientId}/compliance-work")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ComplianceWorkItemDto>>> getComplianceWorkByClient(
            @PathVariable UUID clientId
    ) {
        List<ComplianceWorkItemDto> items = complianceWorkItemService.getComplianceWorkItemsByClient(clientId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/client-services/{serviceId}/compliance-work")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ComplianceWorkItemDto>>> getComplianceWorkByService(
            @PathVariable UUID serviceId
    ) {
        List<ComplianceWorkItemDto> items = complianceWorkItemService.getComplianceWorkItemsByService(serviceId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PutMapping("/compliance-work/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ComplianceWorkItemDto>> updateComplianceWork(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateComplianceWorkItemRequest request
    ) {
        ComplianceWorkItemDto updated = complianceWorkItemService.updateComplianceWorkItem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Compliance work item updated successfully", updated));
    }

    @PatchMapping("/compliance-work/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ComplianceWorkItemDto>> updateComplianceWorkStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateComplianceWorkStatusRequest request
    ) {
        ComplianceWorkItemDto updated = complianceWorkItemService.updateComplianceWorkStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Compliance work status updated successfully", updated));
    }

    @PatchMapping("/compliance-work/{id}/assignment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ComplianceWorkItemDto>> assignComplianceWork(
            @PathVariable UUID id,
            @Valid @RequestBody AssignComplianceWorkRequest request
    ) {
        ComplianceWorkItemDto updated = complianceWorkItemService.assignComplianceWork(id, request);
        return ResponseEntity.ok(ApiResponse.success("Compliance work assigned successfully", updated));
    }

    @DeleteMapping("/compliance-work/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteComplianceWork(
            @PathVariable UUID id
    ) {
        complianceWorkItemService.deleteComplianceWorkItem(id);
        return ResponseEntity.ok(ApiResponse.success("Compliance work item deleted successfully", null));
    }
}
