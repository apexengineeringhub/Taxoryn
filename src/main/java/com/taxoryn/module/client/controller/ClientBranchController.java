package com.taxoryn.module.client.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.client.dto.ClientBranchDto;
import com.taxoryn.module.client.dto.CreateClientBranchRequest;
import com.taxoryn.module.client.dto.UpdateClientBranchRequest;
import com.taxoryn.module.client.service.ClientBranchService;
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
@RequestMapping({"/api/v1/clients/{clientId}/branches", "/api/clients/{clientId}/branches"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Client Branches & Locations Management", description = "Management of client operating locations, registered offices, plants, and branch addresses")
@SecurityRequirement(name = "BearerAuth")
public class ClientBranchController {

    private final ClientBranchService branchService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "List client branches", description = "Retrieves all branches / business locations for a client within the authenticated tenant.")
    public ResponseEntity<ApiResponse<List<ClientBranchDto>>> getBranches(
            @PathVariable UUID clientId,
            @RequestParam(required = false) Boolean activeOnly) {
        List<ClientBranchDto> branches = branchService.getBranches(clientId, activeOnly);
        return ResponseEntity.ok(ApiResponse.success("Client branches retrieved successfully", branches));
    }

    @GetMapping("/{branchId}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get branch by ID", description = "Retrieves details of a specific client branch / business location.")
    public ResponseEntity<ApiResponse<ClientBranchDto>> getBranchById(
            @PathVariable UUID clientId,
            @PathVariable UUID branchId) {
        ClientBranchDto branch = branchService.getBranchById(clientId, branchId);
        return ResponseEntity.ok(ApiResponse.success("Client branch retrieved successfully", branch));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasAuthority('CLIENT_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Create client branch", description = "Adds a new branch / business location to the client.")
    public ResponseEntity<ApiResponse<ClientBranchDto>> createBranch(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateClientBranchRequest request) {
        ClientBranchDto created = branchService.createBranch(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Client branch created successfully", created));
    }

    @PutMapping("/{branchId}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Update client branch", description = "Updates details of an existing client branch / business location.")
    public ResponseEntity<ApiResponse<ClientBranchDto>> updateBranch(
            @PathVariable UUID clientId,
            @PathVariable UUID branchId,
            @Valid @RequestBody UpdateClientBranchRequest request) {
        ClientBranchDto updated = branchService.updateBranch(clientId, branchId, request);
        return ResponseEntity.ok(ApiResponse.success("Client branch updated successfully", updated));
    }

    @PatchMapping("/{branchId}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Partial update client branch", description = "Partially updates details of an existing client branch / business location.")
    public ResponseEntity<ApiResponse<ClientBranchDto>> patchBranch(
            @PathVariable UUID clientId,
            @PathVariable UUID branchId,
            @RequestBody UpdateClientBranchRequest request) {
        ClientBranchDto updated = branchService.updateBranch(clientId, branchId, request);
        return ResponseEntity.ok(ApiResponse.success("Client branch updated successfully", updated));
    }

    @PatchMapping("/{branchId}/status")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Update client branch status", description = "Activates or deactivates a client branch.")
    public ResponseEntity<ApiResponse<ClientBranchDto>> updateBranchStatus(
            @PathVariable UUID clientId,
            @PathVariable UUID branchId,
            @RequestParam boolean active) {
        ClientBranchDto updated = branchService.updateStatus(clientId, branchId, active);
        return ResponseEntity.ok(ApiResponse.success("Client branch status updated successfully", updated));
    }

    @PutMapping("/{branchId}/primary")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Set primary client branch", description = "Designates this branch as the primary / principal place of business for the client.")
    public ResponseEntity<ApiResponse<ClientBranchDto>> setPrimaryBranch(
            @PathVariable UUID clientId,
            @PathVariable UUID branchId) {
        ClientBranchDto updated = branchService.setPrimaryBranch(clientId, branchId);
        return ResponseEntity.ok(ApiResponse.success("Primary branch updated successfully", updated));
    }

    @DeleteMapping("/{branchId}")
    @PreAuthorize("hasAuthority('CLIENT_DELETE') or hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Deactivate/delete client branch", description = "Soft deletes/deactivates a client branch record.")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(
            @PathVariable UUID clientId,
            @PathVariable UUID branchId) {
        branchService.deleteBranch(clientId, branchId);
        return ResponseEntity.ok(ApiResponse.success("Client branch deactivated successfully", null));
    }
}
