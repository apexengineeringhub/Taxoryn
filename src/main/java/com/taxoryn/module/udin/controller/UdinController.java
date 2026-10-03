package com.taxoryn.module.udin.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.udin.dto.CancelUdinRequest;
import com.taxoryn.module.udin.dto.CreateUdinRequest;
import com.taxoryn.module.udin.dto.UdinDto;
import com.taxoryn.module.udin.dto.UdinFilterRequest;
import com.taxoryn.module.udin.dto.UdinSummaryDto;
import com.taxoryn.module.udin.dto.UpdateUdinRequest;
import com.taxoryn.module.udin.dto.UpdateUdinVerificationRequest;
import com.taxoryn.module.udin.model.UdinDocumentType;
import com.taxoryn.module.udin.model.UdinStatus;
import com.taxoryn.module.udin.model.UdinVerificationStatus;
import com.taxoryn.module.udin.service.UdinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/udins")
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "UDIN Register Management", description = "Endpoints for recording, tracking, and verifying practice Unique Document Identification Numbers (UDIN)")
@SecurityRequirement(name = "BearerAuth")
public class UdinController {

    private final UdinService udinService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get UDIN register entries", description = "Retrieves paginated and filtered UDIN entries for the practice.")
    public ResponseEntity<ApiResponse<PagedResponse<UdinDto>>> getUdins(
            @Parameter(description = "Search term for 18-digit UDIN, document title, or signatory name")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by associated client ID")
            @RequestParam(required = false) UUID clientId,
            @Parameter(description = "Filter by lifecycle status (ACTIVE, CANCELLED, REVOKED, ARCHIVED)")
            @RequestParam(required = false) UdinStatus status,
            @Parameter(description = "Filter by verification status (NOT_VERIFIED, VERIFIED, FAILED, NOT_APPLICABLE)")
            @RequestParam(required = false) UdinVerificationStatus verificationStatus,
            @Parameter(description = "Filter by document type")
            @RequestParam(required = false) UdinDocumentType documentType,
            @Parameter(description = "Filter by signatory ICAI membership number")
            @RequestParam(required = false) String signatoryMembershipNo,
            @Parameter(description = "Start generation date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End generation date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "generationDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        UdinFilterRequest filter = UdinFilterRequest.builder()
                .search(search)
                .clientId(clientId)
                .status(status)
                .verificationStatus(verificationStatus)
                .documentType(documentType)
                .signatoryMembershipNo(signatoryMembershipNo)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        PagedResponse<UdinDto> response = udinService.getUdins(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success("UDIN register entries retrieved successfully", response));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get UDIN metrics summary", description = "Retrieves counts of total, active, verified, unverified, failed, and cancelled UDINs.")
    public ResponseEntity<ApiResponse<UdinSummaryDto>> getUdinSummary() {
        UdinSummaryDto summary = udinService.getUdinSummary();
        return ResponseEntity.ok(ApiResponse.success("UDIN summary retrieved successfully", summary));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get UDIN details by ID", description = "Retrieves complete metadata and linked details for a single UDIN register record.")
    public ResponseEntity<ApiResponse<UdinDto>> getUdinById(@PathVariable UUID id) {
        UdinDto udin = udinService.getUdinById(id);
        return ResponseEntity.ok(ApiResponse.success("UDIN details retrieved successfully", udin));
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get UDINs by Client ID", description = "Retrieves all UDIN records registered for a specific client.")
    public ResponseEntity<ApiResponse<List<UdinDto>>> getUdinsByClientId(@PathVariable UUID clientId) {
        List<UdinDto> udins = udinService.getUdinsByClientId(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client UDIN records retrieved successfully", udins));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasAuthority('CLIENT_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Register new UDIN", description = "Records a new Unique Document Identification Number in the practice register.")
    public ResponseEntity<ApiResponse<UdinDto>> createUdin(@Valid @RequestBody CreateUdinRequest request) {
        UdinDto created = udinService.createUdin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("UDIN record registered successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasAuthority('CLIENT_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Update UDIN details", description = "Updates details and metadata of an existing UDIN record.")
    public ResponseEntity<ApiResponse<UdinDto>> updateUdin(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUdinRequest request) {
        UdinDto updated = udinService.updateUdin(id, request);
        return ResponseEntity.ok(ApiResponse.success("UDIN record updated successfully", updated));
    }

    @PatchMapping("/{id}/verification")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasAuthority('CLIENT_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Update UDIN verification status", description = "Updates the verification state and remarks for a UDIN record.")
    public ResponseEntity<ApiResponse<UdinDto>> updateVerification(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUdinVerificationRequest request) {
        UdinDto updated = udinService.updateVerification(id, request);
        return ResponseEntity.ok(ApiResponse.success("UDIN verification updated successfully", updated));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasAuthority('CLIENT_UPDATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL')")
    @Operation(summary = "Cancel UDIN record", description = "Marks a UDIN as cancelled with a reason.")
    public ResponseEntity<ApiResponse<UdinDto>> cancelUdin(
            @PathVariable UUID id,
            @Valid @RequestBody CancelUdinRequest request) {
        UdinDto cancelled = udinService.cancelUdin(id, request);
        return ResponseEntity.ok(ApiResponse.success("UDIN cancelled successfully", cancelled));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE') or hasAuthority('ORG_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER')")
    @Operation(summary = "Delete UDIN record", description = "Deletes a UDIN register record.")
    public ResponseEntity<ApiResponse<Void>> deleteUdin(@PathVariable UUID id) {
        udinService.deleteUdin(id);
        return ResponseEntity.ok(ApiResponse.success("UDIN record deleted successfully", null));
    }
}
