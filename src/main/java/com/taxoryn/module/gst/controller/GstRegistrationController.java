package com.taxoryn.module.gst.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.gst.dto.CreateGstRegistrationRequest;
import com.taxoryn.module.gst.dto.GstRegistrationDto;
import com.taxoryn.module.gst.dto.GstRegistrationFilterRequest;
import com.taxoryn.module.gst.dto.UpdateGstRegistrationRequest;
import com.taxoryn.module.gst.service.GstRegistrationService;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/gst/registrations", "/api/gst/registrations"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.GST)
@Tag(name = "GST Registrations", description = "Management of client statutory GSTIN registrations, scheme categories, and filing frequencies")
@SecurityRequirement(name = "BearerAuth")
public class GstRegistrationController {

    private final GstRegistrationService gstRegistrationService;

    @PostMapping
    @PreAuthorize("hasAuthority('GST_CREATE') or hasAuthority('GST_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create GST registration", description = "Onboards a new client GSTIN profile with state code, category, and filing frequency.")
    public ResponseEntity<ApiResponse<GstRegistrationDto>> createRegistration(
            @Valid @RequestBody CreateGstRegistrationRequest request) {
        GstRegistrationDto dto = gstRegistrationService.createRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("GST registration created successfully", dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List GST registrations", description = "Queries paginated GST registrations filtered by client, location, status, scheme, or search string.")
    public ResponseEntity<ApiResponse<PagedResponse<GstRegistrationDto>>> getRegistrations(
            @Valid @ModelAttribute GstRegistrationFilterRequest filterRequest) {
        PagedResponse<GstRegistrationDto> response = gstRegistrationService.getRegistrations(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("GST registrations retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get GST registration by ID", description = "Retrieves details of a specific GST registration.")
    public ResponseEntity<ApiResponse<GstRegistrationDto>> getRegistrationById(@PathVariable UUID id) {
        GstRegistrationDto dto = gstRegistrationService.getRegistrationById(id);
        return ResponseEntity.ok(ApiResponse.success("GST registration retrieved successfully", dto));
    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasAuthority('GST_VIEW') or hasAuthority('GST_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client GST registrations", description = "Retrieves all GST registrations associated with a specific client.")
    public ResponseEntity<ApiResponse<List<GstRegistrationDto>>> getRegistrationsByClientId(@PathVariable UUID clientId) {
        List<GstRegistrationDto> list = gstRegistrationService.getRegistrationsByClientId(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client GST registrations retrieved successfully", list));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GST_UPDATE') or hasAuthority('GST_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update GST registration", description = "Updates details, status, trade name, or scheme of a GST registration.")
    public ResponseEntity<ApiResponse<GstRegistrationDto>> updateRegistration(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGstRegistrationRequest request) {
        GstRegistrationDto dto = gstRegistrationService.updateRegistration(id, request);
        return ResponseEntity.ok(ApiResponse.success("GST registration updated successfully", dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('GST_DELETE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete GST registration", description = "Deletes an existing GST registration.")
    public ResponseEntity<ApiResponse<Void>> deleteRegistration(@PathVariable UUID id) {
        gstRegistrationService.deleteRegistration(id);
        return ResponseEntity.ok(ApiResponse.success("GST registration deleted successfully", null));
    }
}
