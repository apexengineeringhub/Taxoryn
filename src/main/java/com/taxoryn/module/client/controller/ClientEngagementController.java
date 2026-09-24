package com.taxoryn.module.client.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.client.dto.ClientServiceDto;
import com.taxoryn.module.client.dto.CreateClientServiceRequest;
import com.taxoryn.module.client.dto.ServiceCatalogItemDto;
import com.taxoryn.module.client.dto.UpdateClientServiceRequest;
import com.taxoryn.module.client.entity.ClientServiceStatus;
import com.taxoryn.module.client.service.ClientEngagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Client Service Engagements", description = "Endpoints for managing CA practice service catalog and client-specific service engagements")
public class ClientEngagementController {

    private final ClientEngagementService clientEngagementService;

    @GetMapping("/client-services/catalog")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get service catalog", description = "Retrieves the standard CA practice service master catalog with tenant entitlement status.")
    public ResponseEntity<ApiResponse<List<ServiceCatalogItemDto>>> getServiceCatalog() {
        List<ServiceCatalogItemDto> catalog = clientEngagementService.getServiceCatalog();
        return ResponseEntity.ok(ApiResponse.success("Service catalog retrieved successfully", catalog));
    }

    @GetMapping("/clients/{clientId}/services")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get client services", description = "Retrieves all configured service engagements for the specified client.")
    public ResponseEntity<ApiResponse<List<ClientServiceDto>>> getClientServices(@PathVariable UUID clientId) {
        List<ClientServiceDto> services = clientEngagementService.getClientServices(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client services retrieved successfully", services));
    }

    @GetMapping("/clients/{clientId}/services/{serviceId}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get client service details", description = "Retrieves details of a specific service engagement.")
    public ResponseEntity<ApiResponse<ClientServiceDto>> getClientServiceById(
            @PathVariable UUID clientId,
            @PathVariable UUID serviceId) {
        ClientServiceDto service = clientEngagementService.getClientServiceById(clientId, serviceId);
        return ResponseEntity.ok(ApiResponse.success("Client service details retrieved successfully", service));
    }

    @PostMapping("/clients/{clientId}/services")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Create client service engagement", description = "Configures a new service engagement for the client.")
    public ResponseEntity<ApiResponse<ClientServiceDto>> createClientService(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateClientServiceRequest request) {
        ClientServiceDto created = clientEngagementService.createClientService(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Client service created successfully", created));
    }

    @PutMapping("/clients/{clientId}/services/{serviceId}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Update client service engagement", description = "Updates status, practitioner assignment, dates, or notes of a service engagement.")
    public ResponseEntity<ApiResponse<ClientServiceDto>> updateClientService(
            @PathVariable UUID clientId,
            @PathVariable UUID serviceId,
            @Valid @RequestBody UpdateClientServiceRequest request) {
        ClientServiceDto updated = clientEngagementService.updateClientService(clientId, serviceId, request);
        return ResponseEntity.ok(ApiResponse.success("Client service updated successfully", updated));
    }

    @PatchMapping("/clients/{clientId}/services/{serviceId}/status")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Update service status", description = "Updates lifecycle status (ACTIVE, INACTIVE, SUSPENDED, COMPLETED) of a service engagement.")
    public ResponseEntity<ApiResponse<ClientServiceDto>> updateClientServiceStatus(
            @PathVariable UUID clientId,
            @PathVariable UUID serviceId,
            @RequestParam ClientServiceStatus status) {
        ClientServiceDto updated = clientEngagementService.updateClientServiceStatus(clientId, serviceId, status);
        return ResponseEntity.ok(ApiResponse.success("Client service status updated successfully", updated));
    }

    @DeleteMapping("/clients/{clientId}/services/{serviceId}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasAuthority('CLIENT_DELETE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER')")
    @Operation(summary = "Deactivate client service", description = "Deactivates a client service engagement.")
    public ResponseEntity<ApiResponse<Void>> deleteClientService(
            @PathVariable UUID clientId,
            @PathVariable UUID serviceId) {
        clientEngagementService.deleteClientService(clientId, serviceId);
        return ResponseEntity.ok(ApiResponse.success("Client service deactivated successfully", null));
    }
}
