package com.taxoryn.module.organization.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.organization.dto.AssignLocationEmployeesRequest;
import com.taxoryn.module.organization.dto.CreateLocationRequest;
import com.taxoryn.module.organization.dto.LocationDto;
import com.taxoryn.module.organization.dto.UpdateLocationRequest;
import com.taxoryn.module.organization.service.LocationService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Practice Location Management", description = "Endpoints for managing multiple practice branches, offices, and employee-location assignments")
@SecurityRequirement(name = "BearerAuth")
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "Get all locations for current practice", description = "Retrieves all active and registered offices/branches belonging to authenticated practice.")
    public ResponseEntity<ApiResponse<List<LocationDto>>> getLocations() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        List<LocationDto> locations = locationService.getLocations(orgId);
        return ResponseEntity.ok(ApiResponse.success("Practice locations retrieved successfully", locations));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN')")
    @Operation(summary = "Create a new practice location", description = "Provisions a new branch/office for the practice within subscription plan location limits.")
    public ResponseEntity<ApiResponse<LocationDto>> createLocation(@Valid @RequestBody CreateLocationRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        LocationDto created = locationService.createLocation(orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Practice location created successfully", created));
    }

    @GetMapping("/{locationId}")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW') or hasAuthority('ORG_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF')")
    @Operation(summary = "Get location by ID", description = "Retrieves details of a specific office location.")
    public ResponseEntity<ApiResponse<LocationDto>> getLocationById(@PathVariable UUID locationId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        LocationDto location = locationService.getLocationById(orgId, locationId);
        return ResponseEntity.ok(ApiResponse.success("Location retrieved successfully", location));
    }

    @PutMapping("/{locationId}")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN')")
    @Operation(summary = "Update location details", description = "Updates address, contact info, head office designation, or status of a practice location.")
    public ResponseEntity<ApiResponse<LocationDto>> updateLocation(
            @PathVariable UUID locationId,
            @Valid @RequestBody UpdateLocationRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        LocationDto updated = locationService.updateLocation(orgId, locationId, request);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", updated));
    }

    @DeleteMapping("/{locationId}")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN')")
    @Operation(summary = "Deactivate location", description = "Soft-deletes/deactivates a practice location.")
    public ResponseEntity<ApiResponse<Void>> deleteLocation(@PathVariable UUID locationId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        locationService.deleteLocation(orgId, locationId);
        return ResponseEntity.ok(ApiResponse.success("Location deactivated successfully", null));
    }

    @PostMapping("/{locationId}/employees")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN')")
    @Operation(summary = "Assign employees to location", description = "Updates employee roster assigned to this location.")
    public ResponseEntity<ApiResponse<LocationDto>> assignEmployees(
            @PathVariable UUID locationId,
            @Valid @RequestBody AssignLocationEmployeesRequest request) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        LocationDto updated = locationService.assignEmployees(orgId, locationId, request.getEmployeeIds());
        return ResponseEntity.ok(ApiResponse.success("Employees assigned to location successfully", updated));
    }
}
