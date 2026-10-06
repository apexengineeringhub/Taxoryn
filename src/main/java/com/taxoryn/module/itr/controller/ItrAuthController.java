package com.taxoryn.module.itr.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.dto.GovDscSigningSessionDto;
import com.taxoryn.module.gov.auth.dto.GovDscVerifyResultDto;
import com.taxoryn.module.gov.auth.dto.GovEvcChallengeDto;
import com.taxoryn.module.gov.auth.dto.GovEvcVerifyResultDto;
import com.taxoryn.module.itr.dto.ItrDscSignRequest;
import com.taxoryn.module.itr.dto.ItrDscVerifyRequest;
import com.taxoryn.module.itr.dto.ItrEvcStartRequest;
import com.taxoryn.module.itr.dto.ItrEvcVerifyRequest;
import com.taxoryn.module.itr.service.ItrAuthenticationService;
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

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/itr/auth", "/api/itr/auth"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.ITR)
@Tag(name = "ITR EVC & DSC Authentication", description = "Endpoints for Income Tax Return EVC verification challenges and DSC cryptographic signing flows")
@SecurityRequirement(name = "BearerAuth")
public class ItrAuthController {

    private final ItrAuthenticationService itrAuthService;

    @PostMapping("/evc/start")
    @PreAuthorize("hasAuthority('ITR_CREATE') or hasAuthority('ITR_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Start ITR EVC Challenge", description = "Initiates an Electronic Verification Code (EVC) challenge for ITR return filing.")
    public ResponseEntity<ApiResponse<GovEvcChallengeDto>> startEvc(@Valid @RequestBody ItrEvcStartRequest request) {
        GovEvcChallengeDto response = itrAuthService.startEvc(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("ITR EVC challenge initiated", response));
    }

    @PostMapping("/evc/{sessionId}/verify")
    @PreAuthorize("hasAuthority('ITR_CREATE') or hasAuthority('ITR_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Verify ITR EVC Code", description = "Submits the 6-digit EVC code to verify and authorize the ITR filing session.")
    public ResponseEntity<ApiResponse<GovEvcVerifyResultDto>> verifyEvc(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ItrEvcVerifyRequest request) {
        GovEvcVerifyResultDto response = itrAuthService.verifyEvc(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success("ITR EVC verification processed", response));
    }

    @PostMapping("/dsc/sign")
    @PreAuthorize("hasAuthority('ITR_CREATE') or hasAuthority('ITR_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Start ITR DSC Signing Session", description = "Initiates a Digital Signature Certificate (DSC) signing challenge with document digest.")
    public ResponseEntity<ApiResponse<GovDscSigningSessionDto>> startDsc(@Valid @RequestBody ItrDscSignRequest request) {
        GovDscSigningSessionDto response = itrAuthService.startDsc(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("ITR DSC signing session initiated", response));
    }

    @PostMapping("/dsc/{sessionId}/verify")
    @PreAuthorize("hasAuthority('ITR_CREATE') or hasAuthority('ITR_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Verify ITR DSC Digital Signature", description = "Verifies the cryptographic signature reference against the registered certificate.")
    public ResponseEntity<ApiResponse<GovDscVerifyResultDto>> verifyDsc(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ItrDscVerifyRequest request) {
        GovDscVerifyResultDto response = itrAuthService.verifyDsc(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success("ITR DSC signature verified", response));
    }

    @GetMapping("/{sessionId}/status")
    @PreAuthorize("hasAuthority('ITR_VIEW') or hasAuthority('ITR_READ') or hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get ITR Auth Flow Status", description = "Retrieves live status and authorization state of the ITR EVC or DSC session.")
    public ResponseEntity<ApiResponse<GovAuthSessionDto>> getStatus(@PathVariable UUID sessionId) {
        GovAuthSessionDto response = itrAuthService.getStatus(sessionId);
        return ResponseEntity.ok(ApiResponse.success("ITR auth session status retrieved", response));
    }

    @PostMapping("/{sessionId}/cancel")
    @PreAuthorize("hasAuthority('ITR_CREATE') or hasAuthority('ITR_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Cancel ITR Auth Session", description = "Explicitly cancels and revokes an in-progress or pending ITR EVC or DSC session.")
    public ResponseEntity<ApiResponse<GovAuthSessionDto>> cancel(@PathVariable UUID sessionId) {
        GovAuthSessionDto response = itrAuthService.cancel(sessionId);
        return ResponseEntity.ok(ApiResponse.success("ITR auth session cancelled", response));
    }
}
