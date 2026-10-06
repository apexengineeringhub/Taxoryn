package com.taxoryn.module.consent.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.consent.dto.*;
import com.taxoryn.module.consent.service.ConsentAuthorizationService;
import com.taxoryn.module.consent.service.ConsentManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/consents", "/api/consents"})
@RequiredArgsConstructor
@Tag(name = "Taxpayer Consent & Delegation", description = "Endpoints for managing taxpayer consent mandates, practitioner delegations, scope authorization, and revocations")
@SecurityRequirement(name = "BearerAuth")
public class ConsentController {

    private final ConsentManagementService consentManagementService;
    private final ConsentAuthorizationService consentAuthorizationService;

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_MANAGE') or hasAuthority('CLIENT_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create Taxpayer Consent / Delegation", description = "Establishes a new scoped consent or practitioner delegation for a taxpayer client.")
    public ResponseEntity<ApiResponse<ConsentDto>> createConsent(@Valid @RequestBody CreateConsentRequest request) {
        ConsentDto created = consentManagementService.createConsent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Taxpayer consent delegation created", created));
    }

    @GetMapping("/{consentId}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Consent by ID", description = "Retrieves complete details and status of a taxpayer consent record.")
    public ResponseEntity<ApiResponse<ConsentDto>> getConsentById(@PathVariable UUID consentId) {
        ConsentDto consent = consentManagementService.getConsentById(consentId);
        return ResponseEntity.ok(ApiResponse.success("Taxpayer consent retrieved", consent));
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List Consents for Client", description = "Retrieves all consent records associated with a specific taxpayer client.")
    public ResponseEntity<ApiResponse<List<ConsentDto>>> getConsentsForClient(@PathVariable UUID clientId) {
        List<ConsentDto> list = consentManagementService.getConsentsForClient(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client consents retrieved", list));
    }

    @PostMapping("/{consentId}/approve")
    @PreAuthorize("hasAuthority('CLIENT_MANAGE') or hasAuthority('CLIENT_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Approve Taxpayer Consent", description = "Approves and activates a pending taxpayer consent delegation.")
    public ResponseEntity<ApiResponse<ConsentDto>> approveConsent(
            @PathVariable UUID consentId,
            @RequestBody(required = false) ApproveConsentRequest request) {
        ConsentDto approved = consentManagementService.approveConsent(consentId, request);
        return ResponseEntity.ok(ApiResponse.success("Taxpayer consent approved", approved));
    }

    @PostMapping("/{consentId}/reject")
    @PreAuthorize("hasAuthority('CLIENT_MANAGE') or hasAuthority('CLIENT_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reject Taxpayer Consent", description = "Rejects a pending taxpayer consent delegation.")
    public ResponseEntity<ApiResponse<ConsentDto>> rejectConsent(
            @PathVariable UUID consentId,
            @RequestParam(required = false, defaultValue = "Rejected by user") String reason) {
        ConsentDto rejected = consentManagementService.rejectConsent(consentId, reason);
        return ResponseEntity.ok(ApiResponse.success("Taxpayer consent rejected", rejected));
    }

    @PostMapping("/{consentId}/revoke")
    @PreAuthorize("hasAuthority('CLIENT_MANAGE') or hasAuthority('CLIENT_WRITE') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Revoke Taxpayer Consent", description = "Explicitly revokes an active taxpayer consent delegation.")
    public ResponseEntity<ApiResponse<ConsentDto>> revokeConsent(
            @PathVariable UUID consentId,
            @Valid @RequestBody RevokeConsentRequest request) {
        ConsentDto revoked = consentManagementService.revokeConsent(consentId, request);
        return ResponseEntity.ok(ApiResponse.success("Taxpayer consent revoked", revoked));
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('GOV_INTEGRATION_VIEW') or hasAuthority('GOV_INTEGRATION_MANAGE') or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Evaluate Consent Scope Authorization", description = "Checks whether an acting delegate has active taxpayer consent for a specific operation scope.")
    public ResponseEntity<ApiResponse<ConsentAuthorizationResult>> evaluateConsent(
            @Valid @RequestBody ConsentAuthorizationRequest request) {
        ConsentAuthorizationResult result = consentAuthorizationService.evaluateConsent(request);
        return ResponseEntity.ok(ApiResponse.success("Consent authorization evaluated", result));
    }
}
