package com.taxoryn.module.client.controller;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.service.ClientService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Client Management", description = "Endpoints for onboarding and managing tax clients")
@SecurityRequirement(name = "BearerAuth")
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List clients with pagination", description = "Retrieves paginated list of clients for the authenticated tenant.")
    public ResponseEntity<ApiResponse<PagedResponse<ClientDto>>> getClients(@Valid @ModelAttribute PageRequestDto pageRequest) {
        PagedResponse<ClientDto> response = clientService.getClients(pageRequest);
        return ResponseEntity.ok(ApiResponse.success("Clients retrieved successfully", response));
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client by ID", description = "Retrieves client details within the authenticated tenant.")
    public ResponseEntity<ApiResponse<ClientDto>> getClientById(@PathVariable UUID clientId) {
        ClientDto dto = clientService.getClientById(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client retrieved successfully", dto));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_CREATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create client", description = "Creates a new client record within the authenticated tenant.")
    public ResponseEntity<ApiResponse<ClientDto>> createClient(@Valid @RequestBody CreateClientRequest request) {
        ClientDto created = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Client created successfully", created));
    }

    @PutMapping("/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update client", description = "Updates client details within the authenticated tenant.")
    public ResponseEntity<ApiResponse<ClientDto>> updateClient(@PathVariable UUID clientId, @Valid @RequestBody UpdateClientRequest request) {
        ClientDto updated = clientService.updateClient(clientId, request);
        return ResponseEntity.ok(ApiResponse.success("Client updated successfully", updated));
    }

    @DeleteMapping("/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_DELETE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Archive client", description = "Archives client record within the authenticated tenant.")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable UUID clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client archived successfully", null));
    }
}
