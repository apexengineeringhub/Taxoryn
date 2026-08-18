package com.taxoryn.module.client.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.client.dto.AssignClientEmployeeRequest;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientFilterRequest;
import com.taxoryn.module.client.dto.ClientNoteDto;
import com.taxoryn.module.client.dto.ClientOverviewDto;
import com.taxoryn.module.client.dto.CreateClientNoteRequest;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/clients", "/api/clients"})
@RequiredArgsConstructor
@Tag(name = "Client Management (Central Hub)", description = "Central module for client onboarding, 360-degree overview, tax registrations, assigned practitioners, and communication history")
@SecurityRequirement(name = "BearerAuth")
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List & search clients with filters", description = "Retrieves paginated clients with keyword search and filtering by constitution type, status, assigned practitioner, city, or tax ID.")
    public ResponseEntity<ApiResponse<PagedResponse<ClientDto>>> getClients(@Valid @ModelAttribute ClientFilterRequest filterRequest) {
        PagedResponse<ClientDto> response = clientService.getClients(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Clients retrieved successfully", response));
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get client by ID", description = "Retrieves complete client profile details within the authenticated tenant.")
    public ResponseEntity<ApiResponse<ClientDto>> getClientById(@PathVariable UUID clientId) {
        ClientDto dto = clientService.getClientById(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client retrieved successfully", dto));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_CREATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create client", description = "Onboards a new client with constitution type, tax numbers, contact details, address, and optional assigned practitioner.")
    public ResponseEntity<ApiResponse<ClientDto>> createClient(@Valid @RequestBody CreateClientRequest request) {
        ClientDto created = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Client created successfully", created));
    }

    @PutMapping("/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update client", description = "Updates client profile, statutory numbers, address, and assignment within the authenticated tenant.")
    public ResponseEntity<ApiResponse<ClientDto>> updateClient(@PathVariable UUID clientId, @Valid @RequestBody UpdateClientRequest request) {
        ClientDto updated = clientService.updateClient(clientId, request);
        return ResponseEntity.ok(ApiResponse.success("Client updated successfully", updated));
    }

    @PatchMapping("/{clientId}/status")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update client status", description = "Transitions client status (ACTIVE, INACTIVE, PROSPECT, ARCHIVED).")
    public ResponseEntity<ApiResponse<ClientDto>> updateClientStatus(@PathVariable UUID clientId, @Valid @RequestBody UpdateClientStatusRequest request) {
        ClientDto updated = clientService.updateClientStatus(clientId, request);
        return ResponseEntity.ok(ApiResponse.success("Client status updated successfully to " + updated.getStatus(), updated));
    }

    @PutMapping("/{clientId}/assigned-employee")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Assign or reassign employee", description = "Assigns an internal practitioner / account manager employee to the client.")
    public ResponseEntity<ApiResponse<ClientDto>> assignEmployee(@PathVariable UUID clientId, @Valid @RequestBody AssignClientEmployeeRequest request) {
        ClientDto updated = clientService.assignEmployee(clientId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee assigned to client successfully", updated));
    }

    @DeleteMapping("/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_DELETE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Archive client", description = "Archives client record within the authenticated tenant.")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable UUID clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client archived successfully", null));
    }

    @GetMapping("/{clientId}/overview")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Client 360-Degree Overview", description = "Aggregates all modules related to a client into a single dashboard: Profile, Statutory, Active Tasks, Compliance, Documents, and Communication History.")
    public ResponseEntity<ApiResponse<ClientOverviewDto>> getClientOverview(@PathVariable UUID clientId) {
        ClientOverviewDto overview = clientService.getClientOverview(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client 360 overview retrieved successfully", overview));
    }

    @PostMapping("/{clientId}/notes")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Add communication note", description = "Records a client call, meeting, email interaction, or follow-up note in the client communication log.")
    public ResponseEntity<ApiResponse<ClientNoteDto>> addClientNote(@PathVariable UUID clientId, @Valid @RequestBody CreateClientNoteRequest request) {
        ClientNoteDto note = clientService.addClientNote(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Client note added successfully", note));
    }

    @GetMapping("/{clientId}/notes")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List communication notes", description = "Retrieves all interaction notes and communication history for the client.")
    public ResponseEntity<ApiResponse<List<ClientNoteDto>>> getClientNotes(@PathVariable UUID clientId) {
        List<ClientNoteDto> notes = clientService.getClientNotes(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client notes retrieved successfully", notes));
    }
}
