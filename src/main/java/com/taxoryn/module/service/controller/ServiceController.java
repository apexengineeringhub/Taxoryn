package com.taxoryn.module.service.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.service.dto.CreateServiceRequest;
import com.taxoryn.module.service.dto.ServiceDto;
import com.taxoryn.module.service.dto.UpdateServiceRequest;
import com.taxoryn.module.service.service.ServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Service Catalog Management", description = "Endpoints for managing the standard and custom CA practice service catalog")
@SecurityRequirement(name = "BearerAuth")
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get services catalog", description = "Retrieves active services available to the authenticated practice with entitlement availability.")
    public ResponseEntity<ApiResponse<List<ServiceDto>>> getServices() {
        List<ServiceDto> services = serviceCatalogService.getServices();
        return ResponseEntity.ok(ApiResponse.success("Services catalog retrieved successfully", services));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get service by ID", description = "Retrieves details of a specific service from the catalog.")
    public ResponseEntity<ApiResponse<ServiceDto>> getServiceById(@PathVariable UUID id) {
        ServiceDto service = serviceCatalogService.getServiceById(id);
        return ResponseEntity.ok(ApiResponse.success("Service details retrieved successfully", service));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Create custom practice service", description = "Creates a custom service definition for the practice.")
    public ResponseEntity<ApiResponse<ServiceDto>> createService(@Valid @RequestBody CreateServiceRequest request) {
        ServiceDto created = serviceCatalogService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Service created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Update service", description = "Updates service details.")
    public ResponseEntity<ApiResponse<ServiceDto>> updateService(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest request) {
        ServiceDto updated = serviceCatalogService.updateService(id, request);
        return ResponseEntity.ok(ApiResponse.success("Service updated successfully", updated));
    }
}
