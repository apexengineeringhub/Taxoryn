package com.taxoryn.module.billing.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.billing.dto.BillingProfileDto;
import com.taxoryn.module.billing.dto.CreateBillingProfileRequest;
import com.taxoryn.module.billing.dto.UpdateBillingProfileRequest;
import com.taxoryn.module.billing.service.BillingProfileService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/billing-profiles", "/api/billing-profiles"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.BILLING)
@Tag(name = "Client Billing Profiles", description = "Endpoints for configuring client billing frequencies, default pricing rates, and currency defaults")
@SecurityRequirement(name = "BearerAuth")
public class BillingProfileController {

    private final BillingProfileService billingProfileService;

    @PostMapping
    @PreAuthorize("hasAuthority('BILLING_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create billing profile", description = "Sets up a billing profile for a client or engagement.")
    public ResponseEntity<ApiResponse<BillingProfileDto>> createBillingProfile(@Valid @RequestBody CreateBillingProfileRequest request) {
        BillingProfileDto created = billingProfileService.createBillingProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Billing profile created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List billing profiles", description = "Retrieves all billing profiles within the authenticated practice tenant.")
    public ResponseEntity<ApiResponse<List<BillingProfileDto>>> getAllBillingProfiles() {
        List<BillingProfileDto> list = billingProfileService.getAllBillingProfiles();
        return ResponseEntity.ok(ApiResponse.success("Billing profiles retrieved successfully", list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get billing profile by ID", description = "Retrieves billing profile details.")
    public ResponseEntity<ApiResponse<BillingProfileDto>> getBillingProfileById(@PathVariable UUID id) {
        BillingProfileDto dto = billingProfileService.getBillingProfileById(id);
        return ResponseEntity.ok(ApiResponse.success("Billing profile retrieved successfully", dto));
    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get billing profiles by client ID", description = "Retrieves all billing profiles for a specific client (Client 360).")
    public ResponseEntity<ApiResponse<List<BillingProfileDto>>> getBillingProfilesByClientId(@PathVariable UUID clientId) {
        List<BillingProfileDto> list = billingProfileService.getBillingProfilesByClientId(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client billing profiles retrieved successfully", list));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BILLING_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update billing profile", description = "Modifies billing profile pricing or frequency settings.")
    public ResponseEntity<ApiResponse<BillingProfileDto>> updateBillingProfile(@PathVariable UUID id, @Valid @RequestBody UpdateBillingProfileRequest request) {
        BillingProfileDto updated = billingProfileService.updateBillingProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success("Billing profile updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BILLING_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete billing profile", description = "Deletes a billing profile.")
    public ResponseEntity<ApiResponse<Void>> deleteBillingProfile(@PathVariable UUID id) {
        billingProfileService.deleteBillingProfile(id);
        return ResponseEntity.ok(ApiResponse.success("Billing profile deleted successfully", null));
    }
}
