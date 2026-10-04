package com.taxoryn.module.gov.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.gov.dto.CreateGovConnectionRequest;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.dto.GovCredentialReferenceDto;
import com.taxoryn.module.gov.dto.RegisterGovCredentialRequest;
import com.taxoryn.module.gov.dto.UpdateGovConnectionRequest;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.service.GovernmentConnectionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gov/connections")
@RequiredArgsConstructor
@Tag(name = "Government Connections", description = "Endpoints for managing tenant-scoped government integration connections and credentials")
@SecurityRequirement(name = "BearerAuth")
public class GovConnectionController {

    private final GovernmentConnectionService connectionService;
    private final com.taxoryn.module.gov.service.GovernmentHealthService healthService;

    @PostMapping("/{id}/health-check")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Check connection health", description = "Executes a provider-neutral gateway handshake to determine connection health.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.gov.dto.GovConnectionHealthDto>> checkConnectionHealth(@PathVariable UUID id) {
        com.taxoryn.module.gov.dto.GovConnectionHealthDto health = healthService.checkConnectionHealth(id);
        return ResponseEntity.ok(ApiResponse.success("Government connection health check completed", health));
    }

    @PostMapping("/health-check/all")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Poll active connection health", description = "Polls and checks health across all active government connections for the tenant.")
    public ResponseEntity<ApiResponse<List<com.taxoryn.module.gov.dto.GovConnectionHealthDto>>> checkAllActiveConnections() {
        List<com.taxoryn.module.gov.dto.GovConnectionHealthDto> results = healthService.checkAllActiveConnections();
        return ResponseEntity.ok(ApiResponse.success("Active connections health poll completed", results));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create government connection", description = "Creates a tenant-scoped government connection profile.")
    public ResponseEntity<ApiResponse<GovConnectionDto>> createConnection(
            @Valid @RequestBody CreateGovConnectionRequest request) {
        GovConnectionDto created = connectionService.createConnection(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Government connection created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List government connections", description = "Retrieves all government connections for the active organization.")
    public ResponseEntity<ApiResponse<List<GovConnectionDto>>> listConnections(
            @RequestParam(required = false) GovProviderType providerType) {
        List<GovConnectionDto> connections = providerType != null
                ? connectionService.listConnectionsByProvider(providerType)
                : connectionService.listConnections();
        return ResponseEntity.ok(ApiResponse.success("Government connections retrieved successfully", connections));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get government connection", description = "Retrieves connection details by ID.")
    public ResponseEntity<ApiResponse<GovConnectionDto>> getConnection(@PathVariable UUID id) {
        GovConnectionDto connection = connectionService.getConnection(id);
        return ResponseEntity.ok(ApiResponse.success("Government connection retrieved successfully", connection));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update government connection", description = "Updates connection metadata.")
    public ResponseEntity<ApiResponse<GovConnectionDto>> updateConnection(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGovConnectionRequest request) {
        GovConnectionDto updated = connectionService.updateConnection(id, request);
        return ResponseEntity.ok(ApiResponse.success("Government connection updated successfully", updated));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Activate connection", description = "Activates a configured government connection.")
    public ResponseEntity<ApiResponse<GovConnectionDto>> activateConnection(@PathVariable UUID id) {
        GovConnectionDto activated = connectionService.activateConnection(id);
        return ResponseEntity.ok(ApiResponse.success("Government connection activated successfully", activated));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Deactivate connection", description = "Deactivates an active government connection.")
    public ResponseEntity<ApiResponse<GovConnectionDto>> deactivateConnection(@PathVariable UUID id) {
        GovConnectionDto deactivated = connectionService.deactivateConnection(id);
        return ResponseEntity.ok(ApiResponse.success("Government connection deactivated successfully", deactivated));
    }

    @PostMapping("/credentials")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Register government credential", description = "Securely registers and encrypts a credential reference for a connection. Never returns plaintext secrets.")
    public ResponseEntity<ApiResponse<GovCredentialReferenceDto>> registerCredential(
            @Valid @RequestBody RegisterGovCredentialRequest request) {
        GovCredentialReferenceDto cred = connectionService.registerCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Government credential registered securely", cred));
    }

    @GetMapping("/credentials/{id}")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get credential metadata", description = "Retrieves non-sensitive credential reference metadata.")
    public ResponseEntity<ApiResponse<GovCredentialReferenceDto>> getCredentialMetadata(@PathVariable UUID id) {
        GovCredentialReferenceDto cred = connectionService.getCredentialMetadata(id);
        return ResponseEntity.ok(ApiResponse.success("Government credential metadata retrieved", cred));
    }

    @PostMapping("/credentials/{id}/invalidate")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Invalidate credential", description = "Invalidates and revokes a credential reference.")
    public ResponseEntity<ApiResponse<GovCredentialReferenceDto>> invalidateCredential(@PathVariable UUID id) {
        GovCredentialReferenceDto cred = connectionService.invalidateCredential(id);
        return ResponseEntity.ok(ApiResponse.success("Government credential invalidated successfully", cred));
    }
}
