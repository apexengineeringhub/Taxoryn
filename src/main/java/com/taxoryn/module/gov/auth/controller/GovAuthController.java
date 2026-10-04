package com.taxoryn.module.gov.auth.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.gov.auth.dto.GovAuthContinueRequest;
import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.dto.GovAuthStartRequest;
import com.taxoryn.module.gov.auth.model.GovAuthorizationState;
import com.taxoryn.module.gov.auth.service.GovernmentAuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gov/auth")
@RequiredArgsConstructor
@Tag(name = "Government Authentication", description = "Endpoints for initiating, querying, continuing, and revoking government authentication & authorization sessions across GST, Income Tax, and TDS")
@SecurityRequirement(name = "BearerAuth")
public class GovAuthController {

    private final GovernmentAuthenticationService authService;

    @PostMapping("/sessions")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Initiate government authentication session", description = "Starts an authentication flow (OAuth, OTP, EVC, DSC) for a configured government connection.")
    public ResponseEntity<ApiResponse<GovAuthSessionDto>> startAuthentication(
            @Valid @RequestBody GovAuthStartRequest request) {
        GovAuthSessionDto session = authService.startAuthentication(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Government authentication session initiated", session));
    }

    @PostMapping("/sessions/{sessionId}/authorize")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Continue/Complete authorization flow", description = "Submits authorization challenge action or continues interactive authorization stage.")
    public ResponseEntity<ApiResponse<GovAuthSessionDto>> continueAuthorization(
            @PathVariable UUID sessionId,
            @RequestBody(required = false) GovAuthContinueRequest request) {
        GovAuthSessionDto session = authService.continueAuthorization(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success("Government authorization flow continued", session));
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get authentication session status", description = "Retrieves live or persisted authentication lifecycle status.")
    public ResponseEntity<ApiResponse<GovAuthSessionDto>> getSessionStatus(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String mockOutcome) {
        java.util.Map<String, Object> options = mockOutcome != null
                ? java.util.Map.of("mockOutcome", mockOutcome)
                : java.util.Collections.emptyMap();
        GovAuthSessionDto session = authService.getAuthenticationStatus(sessionId, options);
        return ResponseEntity.ok(ApiResponse.success("Authentication session status retrieved", session));
    }

    @GetMapping("/sessions/{sessionId}/authorization")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get authorization session state", description = "Retrieves the fine-grained authorization interaction state for the session.")
    public ResponseEntity<ApiResponse<GovAuthSessionDto>> getAuthorizationSession(
            @PathVariable UUID sessionId) {
        GovAuthSessionDto session = authService.getAuthenticationStatus(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Government authorization session state retrieved", session));
    }

    @PostMapping("/sessions/{sessionId}/revoke")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Revoke government authentication session", description = "Revokes an active authentication session on the gateway and marks it as REVOKED.")
    public ResponseEntity<ApiResponse<GovAuthSessionDto>> revokeSession(
            @PathVariable UUID sessionId) {
        GovAuthSessionDto session = authService.revokeAuthentication(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Government authentication session revoked", session));
    }

    @GetMapping("/connections/{connectionId}/session")
    @PreAuthorize("hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get active session for connection", description = "Retrieves current active authentication session for the specified government connection.")
    public ResponseEntity<ApiResponse<GovAuthSessionDto>> getActiveSessionForConnection(
            @PathVariable UUID connectionId) {
        GovAuthSessionDto session = authService.getActiveSessionForConnection(connectionId);
        return ResponseEntity.ok(ApiResponse.success("Active authentication session retrieved", session));
    }
}
