package com.taxoryn.module.tds.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.dto.GovDscSigningSessionDto;
import com.taxoryn.module.gov.auth.dto.GovDscVerifyResultDto;
import com.taxoryn.module.gov.auth.dto.GovEvcChallengeDto;
import com.taxoryn.module.gov.auth.dto.GovEvcVerifyResultDto;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.tds.dto.TdsDscSignRequest;
import com.taxoryn.module.tds.dto.TdsDscVerifyRequest;
import com.taxoryn.module.tds.dto.TdsEvcStartRequest;
import com.taxoryn.module.tds.dto.TdsEvcVerifyRequest;
import com.taxoryn.module.tds.service.TdsAuthenticationService;
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
@RequestMapping({"/api/v1/tds/auth", "/api/tds/auth"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.TDS)
@Tag(name = "TDS EVC & DSC Authentication", description = "Endpoints for TDS/TRACES EVC verification challenges and DSC cryptographic signing flows")
@SecurityRequirement(name = "BearerAuth")
public class TdsAuthController {

    private final TdsAuthenticationService tdsAuthService;

    @PostMapping("/evc/start")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Start TDS EVC Challenge", description = "Initiates an Electronic Verification Code (EVC) challenge for TDS return filing.")
    public ResponseEntity<ApiResponse<GovEvcChallengeDto>> startEvc(@Valid @RequestBody TdsEvcStartRequest request) {
        GovEvcChallengeDto response = tdsAuthService.startEvc(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("TDS EVC challenge initiated", response));
    }

    @PostMapping("/evc/{sessionId}/verify")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Verify TDS EVC Code", description = "Submits the 6-digit EVC code to verify and authorize the TDS filing session.")
    public ResponseEntity<ApiResponse<GovEvcVerifyResultDto>> verifyEvc(
            @PathVariable UUID sessionId,
            @Valid @RequestBody TdsEvcVerifyRequest request) {
        GovEvcVerifyResultDto response = tdsAuthService.verifyEvc(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success("TDS EVC verification processed", response));
    }

    @PostMapping("/dsc/sign")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Start TDS DSC Signing Session", description = "Initiates a Digital Signature Certificate (DSC) signing challenge with document digest.")
    public ResponseEntity<ApiResponse<GovDscSigningSessionDto>> startDsc(@Valid @RequestBody TdsDscSignRequest request) {
        GovDscSigningSessionDto response = tdsAuthService.startDsc(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("TDS DSC signing session initiated", response));
    }

    @PostMapping("/dsc/{sessionId}/verify")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Verify TDS DSC Digital Signature", description = "Verifies the cryptographic signature reference against the registered certificate.")
    public ResponseEntity<ApiResponse<GovDscVerifyResultDto>> verifyDsc(
            @PathVariable UUID sessionId,
            @Valid @RequestBody TdsDscVerifyRequest request) {
        GovDscVerifyResultDto response = tdsAuthService.verifyDsc(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success("TDS DSC signature verified", response));
    }

    @GetMapping("/{sessionId}/status")
    @PreAuthorize("hasAuthority('TDS_VIEW') or hasAuthority('TDS_READ') or hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get TDS Auth Flow Status", description = "Retrieves live status and authorization state of the TDS EVC or DSC session.")
    public ResponseEntity<ApiResponse<GovAuthSessionDto>> getStatus(@PathVariable UUID sessionId) {
        GovAuthSessionDto response = tdsAuthService.getStatus(sessionId);
        return ResponseEntity.ok(ApiResponse.success("TDS auth session status retrieved", response));
    }

    @PostMapping("/{sessionId}/cancel")
    @PreAuthorize("hasAuthority('TDS_CREATE') or hasAuthority('TDS_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Cancel TDS Auth Session", description = "Explicitly cancels and revokes an in-progress or pending TDS EVC or DSC session.")
    public ResponseEntity<ApiResponse<GovAuthSessionDto>> cancel(@PathVariable UUID sessionId) {
        GovAuthSessionDto response = tdsAuthService.cancel(sessionId);
        return ResponseEntity.ok(ApiResponse.success("TDS auth session cancelled", response));
    }
}
