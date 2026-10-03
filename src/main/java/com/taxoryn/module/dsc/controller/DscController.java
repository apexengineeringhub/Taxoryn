package com.taxoryn.module.dsc.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.dsc.dto.CreateDscRequest;
import com.taxoryn.module.dsc.dto.DscDto;
import com.taxoryn.module.dsc.dto.DscFilterRequest;
import com.taxoryn.module.dsc.dto.DscSummaryDto;
import com.taxoryn.module.dsc.dto.UpdateDscRequest;
import com.taxoryn.module.dsc.model.DscCertificateType;
import com.taxoryn.module.dsc.model.DscStatus;
import com.taxoryn.module.dsc.service.DscService;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dsc")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "DSC Register Management", description = "Endpoints for managing practice Digital Signature Certificates (DSC) register, lifecycle, and expiry tracking")
@SecurityRequirement(name = "BearerAuth")
public class DscController {

    private final DscService dscService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get DSC register entries", description = "Retrieves paginated and filtered DSC entries for the practice.")
    public ResponseEntity<ApiResponse<PagedResponse<DscDto>>> getDscList(
            @Parameter(description = "Search term for holder name, certificate serial number, or issuer")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by status (ACTIVE, EXPIRING, EXPIRED, REVOKED, INACTIVE)")
            @RequestParam(required = false) DscStatus status,
            @Parameter(description = "Filter by associated client ID")
            @RequestParam(required = false) UUID clientId,
            @Parameter(description = "Filter by certificate type")
            @RequestParam(required = false) DscCertificateType certificateType,
            @Parameter(description = "Filter by applicable service (e.g. GST, ITR, TDS)")
            @RequestParam(required = false) String service,
            @Parameter(description = "Filter certificates expiring within specified number of days")
            @RequestParam(required = false) Integer expiringWithinDays,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "expiryDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        DscFilterRequest filter = DscFilterRequest.builder()
                .search(search)
                .status(status)
                .clientId(clientId)
                .certificateType(certificateType)
                .service(service)
                .expiringWithinDays(expiringWithinDays)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PagedResponse<DscDto> response = dscService.getDscList(filter);
        return ResponseEntity.ok(ApiResponse.success("DSC register entries retrieved successfully", response));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get DSC metrics summary", description = "Retrieves count breakdown of total, active, expiring soon, expired, and revoked DSCs.")
    public ResponseEntity<ApiResponse<DscSummaryDto>> getDscSummary() {
        DscSummaryDto summary = dscService.getDscSummary();
        return ResponseEntity.ok(ApiResponse.success("DSC summary retrieved successfully", summary));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get DSC details by ID", description = "Retrieves complete metadata of a single DSC register entry.")
    public ResponseEntity<ApiResponse<DscDto>> getDscById(@PathVariable UUID id) {
        DscDto dsc = dscService.getDscById(id);
        return ResponseEntity.ok(ApiResponse.success("DSC details retrieved successfully", dsc));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasAuthority('CLIENT_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Register new DSC", description = "Registers a new Digital Signature Certificate in the practice catalog.")
    public ResponseEntity<ApiResponse<DscDto>> createDsc(@Valid @RequestBody CreateDscRequest request) {
        DscDto created = dscService.createDsc(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Digital Signature Certificate registered successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasAuthority('CLIENT_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Update DSC metadata", description = "Updates metadata of an existing DSC.")
    public ResponseEntity<ApiResponse<DscDto>> updateDsc(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDscRequest request) {
        DscDto updated = dscService.updateDsc(id, request);
        return ResponseEntity.ok(ApiResponse.success("DSC updated successfully", updated));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasAuthority('CLIENT_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Activate DSC", description = "Re-activates a previously inactive DSC.")
    public ResponseEntity<ApiResponse<DscDto>> activateDsc(@PathVariable UUID id) {
        DscDto activated = dscService.activateDsc(id);
        return ResponseEntity.ok(ApiResponse.success("DSC activated successfully", activated));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasAuthority('CLIENT_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Deactivate DSC", description = "Deactivates a DSC without deleting historical references.")
    public ResponseEntity<ApiResponse<DscDto>> deactivateDsc(@PathVariable UUID id) {
        DscDto deactivated = dscService.deactivateDsc(id);
        return ResponseEntity.ok(ApiResponse.success("DSC deactivated successfully", deactivated));
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasAuthority('CLIENT_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Revoke DSC", description = "Marks a DSC as revoked with an optional reason.")
    public ResponseEntity<ApiResponse<DscDto>> revokeDsc(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        DscDto revoked = dscService.revokeDsc(id, reason);
        return ResponseEntity.ok(ApiResponse.success("DSC revoked successfully", revoked));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Delete DSC", description = "Deletes a DSC register entry.")
    public ResponseEntity<ApiResponse<Void>> deleteDsc(@PathVariable UUID id) {
        dscService.deleteDsc(id);
        return ResponseEntity.ok(ApiResponse.success("DSC deleted successfully", null));
    }

    @PostMapping("/trigger-expiry-reminders")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Trigger DSC Expiry Reminders", description = "Checks all active practice DSCs and generates expiry alerts for certificates expiring within 30 days.")
    public ResponseEntity<ApiResponse<Integer>> triggerExpiryReminders() {
        int count = dscService.checkAndTriggerExpiryReminders();
        return ResponseEntity.ok(ApiResponse.success("Processed DSC expiry checks; dispatched " + count + " reminder alerts", count));
    }
}
