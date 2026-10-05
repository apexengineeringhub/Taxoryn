package com.taxoryn.module.client.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.client.dto.ClientContactDto;
import com.taxoryn.module.client.dto.CreateClientContactRequest;
import com.taxoryn.module.client.dto.UpdateClientContactRequest;
import com.taxoryn.module.client.service.ClientContactService;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping({"/api/v1/clients/{clientId}/contacts", "/api/clients/{clientId}/contacts"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Client Contacts Management", description = "Management of client personnel, authorized representatives, billing contacts, and roles")
@SecurityRequirement(name = "BearerAuth")
public class ClientContactController {

    private final ClientContactService contactService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "List client contacts", description = "Retrieves all contact persons for a client within the authenticated tenant.")
    public ResponseEntity<ApiResponse<List<ClientContactDto>>> getContacts(
            @PathVariable UUID clientId,
            @RequestParam(required = false) Boolean activeOnly) {
        List<ClientContactDto> contacts = contactService.getContacts(clientId, activeOnly);
        return ResponseEntity.ok(ApiResponse.success("Client contacts retrieved successfully", contacts));
    }

    @GetMapping("/{contactId}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get contact by ID", description = "Retrieves details of a specific contact person.")
    public ResponseEntity<ApiResponse<ClientContactDto>> getContactById(
            @PathVariable UUID clientId,
            @PathVariable UUID contactId) {
        ClientContactDto contact = contactService.getContactById(clientId, contactId);
        return ResponseEntity.ok(ApiResponse.success("Client contact retrieved successfully", contact));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasAuthority('CLIENT_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Create client contact", description = "Adds a new contact person to the client.")
    public ResponseEntity<ApiResponse<ClientContactDto>> createContact(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateClientContactRequest request) {
        ClientContactDto created = contactService.createContact(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Client contact created successfully", created));
    }

    @PutMapping("/{contactId}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Update client contact", description = "Updates details of an existing client contact person.")
    public ResponseEntity<ApiResponse<ClientContactDto>> updateContact(
            @PathVariable UUID clientId,
            @PathVariable UUID contactId,
            @Valid @RequestBody UpdateClientContactRequest request) {
        ClientContactDto updated = contactService.updateContact(clientId, contactId, request);
        return ResponseEntity.ok(ApiResponse.success("Client contact updated successfully", updated));
    }

    @PatchMapping("/{contactId}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Partial update client contact", description = "Partially updates details of an existing client contact person.")
    public ResponseEntity<ApiResponse<ClientContactDto>> patchContact(
            @PathVariable UUID clientId,
            @PathVariable UUID contactId,
            @RequestBody UpdateClientContactRequest request) {
        ClientContactDto updated = contactService.updateContact(clientId, contactId, request);
        return ResponseEntity.ok(ApiResponse.success("Client contact updated successfully", updated));
    }

    @PatchMapping("/{contactId}/status")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Update client contact status", description = "Activates or deactivates a client contact.")
    public ResponseEntity<ApiResponse<ClientContactDto>> updateContactStatus(
            @PathVariable UUID clientId,
            @PathVariable UUID contactId,
            @RequestParam boolean active) {
        ClientContactDto updated = contactService.updateStatus(clientId, contactId, active);
        return ResponseEntity.ok(ApiResponse.success("Client contact status updated successfully", updated));
    }

    @PutMapping("/{contactId}/primary")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Set primary client contact", description = "Designates this contact as the primary contact person for the client.")
    public ResponseEntity<ApiResponse<ClientContactDto>> setPrimaryContact(
            @PathVariable UUID clientId,
            @PathVariable UUID contactId) {
        ClientContactDto updated = contactService.setPrimaryContact(clientId, contactId);
        return ResponseEntity.ok(ApiResponse.success("Primary contact updated successfully", updated));
    }

    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAuthority('CLIENT_DELETE') or hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Deactivate/delete client contact", description = "Soft deletes/deactivates a client contact record.")
    public ResponseEntity<ApiResponse<Void>> deleteContact(
            @PathVariable UUID clientId,
            @PathVariable UUID contactId) {
        contactService.deleteContact(clientId, contactId);
        return ResponseEntity.ok(ApiResponse.success("Client contact deactivated successfully", null));
    }
}
