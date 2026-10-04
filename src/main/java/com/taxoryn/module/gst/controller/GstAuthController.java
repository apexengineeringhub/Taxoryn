package com.taxoryn.module.gst.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.gst.dto.GstAuthContinueRequest;
import com.taxoryn.module.gst.dto.GstAuthSessionDto;
import com.taxoryn.module.gst.dto.GstAuthSessionRequest;
import com.taxoryn.module.gst.model.GstAuthenticationPurpose;
import com.taxoryn.module.gst.service.GstAuthenticationService;
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
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gst/authentication")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.GST)
@Tag(name = "GST Authentication & Token Lifecycle", description = "Endpoints for GST, E-Way Bill, and E-Invoice authentication, token management, and authorization lifecycles")
@SecurityRequirement(name = "BearerAuth")
public class GstAuthController {

    private final GstAuthenticationService gstAuthService;

    @PostMapping("/sessions")
    @PreAuthorize("hasAuthority('GST_MANAGE') or hasAuthority('GST_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Initiate GST / E-Way / E-Invoice authentication session", description = "Starts an authentication or authorization flow for GST returns, E-Way Bill, or E-Invoice operations.")
    public ResponseEntity<ApiResponse<GstAuthSessionDto>> authenticate(
            @Valid @RequestBody GstAuthSessionRequest request) {
        GstAuthSessionDto session = gstAuthService.authenticate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("GST authentication session initiated", session));
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get GST authentication & token status", description = "Retrieves live or persisted authentication lifecycle status and service token state.")
    public ResponseEntity<ApiResponse<GstAuthSessionDto>> getSessionStatus(
            @PathVariable UUID sessionId,
            @RequestParam(required = false, defaultValue = "GST") GstAuthenticationPurpose purpose,
            @RequestParam(required = false) String mockOutcome) {
        Map<String, Object> options = mockOutcome != null
                ? Map.of("mockOutcome", mockOutcome)
                : Collections.emptyMap();
        GstAuthSessionDto session = gstAuthService.getAuthenticationStatus(sessionId, purpose, options);
        return ResponseEntity.ok(ApiResponse.success("GST authentication session status retrieved", session));
    }

    @PostMapping("/sessions/{sessionId}/continue")
    @PreAuthorize("hasAuthority('GST_MANAGE') or hasAuthority('GST_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Continue/Complete GST authorization challenge", description = "Submits authorization challenge action (OTP, OAuth callback, DSC response) to complete authentication.")
    public ResponseEntity<ApiResponse<GstAuthSessionDto>> continueAuthorization(
            @PathVariable UUID sessionId,
            @RequestParam(required = false, defaultValue = "GST") GstAuthenticationPurpose purpose,
            @RequestBody(required = false) GstAuthContinueRequest request) {
        GstAuthSessionDto session = gstAuthService.continueAuthorization(sessionId, request, purpose);
        return ResponseEntity.ok(ApiResponse.success("GST authorization flow continued", session));
    }

    @PostMapping("/sessions/{sessionId}/refresh")
    @PreAuthorize("hasAuthority('GST_MANAGE') or hasAuthority('GST_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Refresh GST / E-Way / E-Invoice service token", description = "Refreshes an active or expiring token reference without requiring full re-authentication.")
    public ResponseEntity<ApiResponse<GstAuthSessionDto>> refreshToken(
            @PathVariable UUID sessionId,
            @RequestParam(required = false, defaultValue = "GST") GstAuthenticationPurpose purpose) {
        GstAuthSessionDto session = gstAuthService.refreshToken(sessionId, purpose, Collections.emptyMap());
        return ResponseEntity.ok(ApiResponse.success("GST token reference refreshed", session));
    }

    @PostMapping("/sessions/{sessionId}/revoke")
    @PreAuthorize("hasAuthority('GST_MANAGE') or hasAuthority('GST_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Revoke GST authentication session", description = "Explicitly revokes active authentication session and invalidates associated service token.")
    public ResponseEntity<ApiResponse<GstAuthSessionDto>> revokeSession(
            @PathVariable UUID sessionId,
            @RequestParam(required = false, defaultValue = "GST") GstAuthenticationPurpose purpose) {
        GstAuthSessionDto session = gstAuthService.revoke(sessionId, purpose);
        return ResponseEntity.ok(ApiResponse.success("GST authentication session revoked", session));
    }

    @GetMapping("/connections/{connectionId}/token")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get active GST service token for connection", description = "Retrieves current active token reference for the specified GST connection and purpose scope.")
    public ResponseEntity<ApiResponse<GstAuthSessionDto>> getActiveToken(
            @PathVariable UUID connectionId,
            @RequestParam(required = false, defaultValue = "GST") GstAuthenticationPurpose purpose) {
        GstAuthSessionDto session = gstAuthService.getActiveToken(connectionId, purpose);
        return ResponseEntity.ok(ApiResponse.success("Active GST token reference retrieved", session));
    }
}
