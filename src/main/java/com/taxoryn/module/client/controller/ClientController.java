package com.taxoryn.module.client.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.client.dto.AssignClientEmployeeRequest;
import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientFilterRequest;
import com.taxoryn.module.client.dto.ClientLifecycleSummaryDto;
import com.taxoryn.module.client.dto.ClientNoteDto;
import com.taxoryn.module.client.dto.ClientOverviewDto;
import com.taxoryn.module.client.dto.ClientProfileCompletenessDto;
import com.taxoryn.module.client.dto.ClientProfileDto;
import com.taxoryn.module.client.dto.CreateClientNoteRequest;
import com.taxoryn.module.client.dto.CreateClientRequest;
import com.taxoryn.module.client.dto.ClientCommunicationDto;
import com.taxoryn.module.client.dto.ClientCommunicationRequest;
import com.taxoryn.module.client.dto.UpdateClientProfileRequest;
import com.taxoryn.module.client.entity.ClientNoteEntity.NoteType;
import com.taxoryn.module.client.service.ClientCommunicationTimelineService;
import com.taxoryn.module.client.service.ClientLifecycleService;
import com.taxoryn.module.client.dto.UpdateClientRequest;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;
import com.taxoryn.module.client.dto.ClientContextSummaryDto;
import com.taxoryn.module.client.service.ClientContextService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

@RestController
@RequestMapping({"/api/v1/clients", "/api/clients"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Client Management (Central Hub)", description = "Central module for client onboarding, 360-degree overview, tax registrations, assigned practitioners, and communication history")
@SecurityRequirement(name = "BearerAuth")
public class ClientController {

    private final ClientService clientService;
    private final ClientContextService clientContextService;
    private final ClientLifecycleService clientLifecycleService;
    private final ClientCommunicationTimelineService communicationTimelineService;

    @GetMapping("/{clientId}/context")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('TASK_CREATE') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get lightweight client context", description = "Retrieves lightweight client identity, tenancy, status, and classification summary.")
    public ResponseEntity<ApiResponse<ClientContextSummaryDto>> getClientContext(@PathVariable UUID clientId) {
        ClientContextSummaryDto context = clientContextService.requireClientContext(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client context retrieved successfully", context));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('TASK_CREATE') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "List & search clients with filters", description = "Retrieves paginated clients with keyword search and filtering by constitution type, status, assigned practitioner, city, or tax ID.")
    public ResponseEntity<ApiResponse<PagedResponse<ClientDto>>> getClients(@Valid @ModelAttribute ClientFilterRequest filterRequest) {
        PagedResponse<ClientDto> response = clientService.getClients(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Clients retrieved successfully", response));
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('TASK_CREATE') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get client by ID", description = "Retrieves complete client profile details within the authenticated tenant.")
    public ResponseEntity<ApiResponse<ClientDto>> getClientById(@PathVariable UUID clientId) {
        ClientDto dto = clientService.getClientById(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client retrieved successfully", dto));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_CREATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Create client", description = "Onboards a new client with constitution type, tax numbers, contact details, address, and optional assigned practitioner.")
    public ResponseEntity<ApiResponse<ClientDto>> createClient(@Valid @RequestBody CreateClientRequest request) {
        ClientDto created = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Client created successfully", created));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('CLIENT_CREATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Bulk import clients", description = "Imports a batch of client records from CSV or Excel migrations with validation and duplicate skipping.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.client.dto.BulkImportResultDto>> bulkCreateClients(@RequestBody List<CreateClientRequest> requests) {
        com.taxoryn.module.client.dto.BulkImportResultDto result = clientService.bulkCreateClients(requests);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Bulk client migration batch processed successfully", result));
    }

    @PutMapping("/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Update client", description = "Updates client profile, statutory numbers, address, and assignment within the authenticated tenant.")
    public ResponseEntity<ApiResponse<ClientDto>> updateClient(@PathVariable UUID clientId, @Valid @RequestBody UpdateClientRequest request) {
        ClientDto updated = clientService.updateClient(clientId, request);
        return ResponseEntity.ok(ApiResponse.success("Client updated successfully", updated));
    }

    @GetMapping("/{clientId}/profile")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('TASK_CREATE') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get client business profile", description = "Retrieves structured client business profile including entity classification, statutory info, and completeness score.")
    public ResponseEntity<ApiResponse<ClientProfileDto>> getClientProfile(@PathVariable UUID clientId) {
        ClientProfileDto profile = clientService.getClientProfile(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client profile retrieved successfully", profile));
    }

    @PutMapping("/{clientId}/profile")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Update client business profile", description = "Updates structured client business profile details within the authenticated tenant.")
    public ResponseEntity<ApiResponse<ClientProfileDto>> updateClientProfile(
            @PathVariable UUID clientId,
            @Valid @RequestBody UpdateClientProfileRequest request) {
        ClientProfileDto updated = clientService.updateClientProfile(clientId, request);
        return ResponseEntity.ok(ApiResponse.success("Client profile updated successfully", updated));
    }

    @GetMapping("/{clientId}/profile/completeness")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('TASK_CREATE') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get client profile completeness", description = "Retrieves deterministic profile completeness evaluation across identity, contact, address, statutory, and business dimensions.")
    public ResponseEntity<ApiResponse<ClientProfileCompletenessDto>> getProfileCompleteness(@PathVariable UUID clientId) {
        ClientProfileCompletenessDto completeness = clientService.getProfileCompleteness(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client profile completeness evaluated successfully", completeness));
    }

    @GetMapping("/{clientId}/lifecycle")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('TASK_CREATE') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get client lifecycle state & transitions", description = "Retrieves authoritative client lifecycle state, transition metadata, and valid next states.")
    public ResponseEntity<ApiResponse<ClientLifecycleSummaryDto>> getClientLifecycle(@PathVariable UUID clientId) {
        ClientLifecycleSummaryDto summary = clientLifecycleService.getLifecycleSummary(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client lifecycle summary retrieved successfully", summary));
    }

    @PatchMapping("/{clientId}/status")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Transition client lifecycle status", description = "Transitions client status (ONBOARDING, ACTIVE, INACTIVE, SUSPENDED, PROSPECT, ARCHIVED) according to transition rules.")
    public ResponseEntity<ApiResponse<ClientDto>> updateClientStatus(@PathVariable UUID clientId, @Valid @RequestBody UpdateClientStatusRequest request) {
        ClientDto updated = clientLifecycleService.transitionStatus(clientId, request);
        return ResponseEntity.ok(ApiResponse.success("Client status updated successfully to " + updated.getStatus(), updated));
    }

    @PutMapping("/{clientId}/status")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Transition client lifecycle status (PUT)", description = "Transitions client status (ONBOARDING, ACTIVE, INACTIVE, SUSPENDED, PROSPECT, ARCHIVED) according to transition rules.")
    public ResponseEntity<ApiResponse<ClientDto>> putClientStatus(@PathVariable UUID clientId, @Valid @RequestBody UpdateClientStatusRequest request) {
        ClientDto updated = clientLifecycleService.transitionStatus(clientId, request);
        return ResponseEntity.ok(ApiResponse.success("Client status updated successfully to " + updated.getStatus(), updated));
    }

    @PutMapping("/{clientId}/assigned-employee")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Assign or reassign employee", description = "Assigns an internal practitioner / account manager employee to the client.")
    public ResponseEntity<ApiResponse<ClientDto>> assignEmployee(@PathVariable UUID clientId, @Valid @RequestBody AssignClientEmployeeRequest request) {
        ClientDto updated = clientService.assignEmployee(clientId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee assigned to client successfully", updated));
    }

    @PatchMapping("/{clientId}/portal-status")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Update client portal access status", description = "Updates the client's portal access lifecycle (ACTIVE, SUSPENDED, INACTIVE). Revokes sessions and dispatches access notifications.")
    public ResponseEntity<ApiResponse<ClientDto>> updateClientPortalStatus(
            @PathVariable UUID clientId,
            @Valid @RequestBody com.taxoryn.module.client.dto.UpdateClientPortalStatusRequest request) {
        ClientDto updated = clientService.updateClientPortalStatus(clientId, request);
        return ResponseEntity.ok(ApiResponse.success("Client portal status updated successfully to " + updated.getPortalStatus(), updated));
    }

    @PostMapping("/{clientId}/portal-status/suspend")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Suspend client portal access", description = "Temporarily blocks client portal login and access while retaining all historical documents and records.")
    public ResponseEntity<ApiResponse<ClientDto>> suspendClientPortal(@PathVariable UUID clientId) {
        ClientDto updated = clientService.updateClientPortalStatus(clientId,
                com.taxoryn.module.client.dto.UpdateClientPortalStatusRequest.builder()
                        .portalStatus(com.taxoryn.module.user.entity.UserEntity.UserStatus.SUSPENDED)
                        .build());
        return ResponseEntity.ok(ApiResponse.success("Client portal access suspended successfully", updated));
    }

    @PostMapping("/{clientId}/portal-status/restore")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Restore client portal access", description = "Restores client portal login access for suspended or inactive accounts.")
    public ResponseEntity<ApiResponse<ClientDto>> restoreClientPortal(@PathVariable UUID clientId) {
        ClientDto updated = clientService.updateClientPortalStatus(clientId,
                com.taxoryn.module.client.dto.UpdateClientPortalStatusRequest.builder()
                        .portalStatus(com.taxoryn.module.user.entity.UserEntity.UserStatus.ACTIVE)
                        .build());
        return ResponseEntity.ok(ApiResponse.success("Client portal access restored successfully", updated));
    }

    @PostMapping("/{clientId}/portal-status/deactivate")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Deactivate client portal access", description = "Permanently disables client portal access while keeping all data intact.")
    public ResponseEntity<ApiResponse<ClientDto>> deactivateClientPortal(@PathVariable UUID clientId) {
        ClientDto updated = clientService.updateClientPortalStatus(clientId,
                com.taxoryn.module.client.dto.UpdateClientPortalStatusRequest.builder()
                        .portalStatus(com.taxoryn.module.user.entity.UserEntity.UserStatus.INACTIVE)
                        .build());
        return ResponseEntity.ok(ApiResponse.success("Client portal access deactivated successfully", updated));
    }

    @PostMapping("/{clientId}/portal-invitation/resend")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Resend client portal invitation email", description = "Invalidates previous activation tokens and dispatches a fresh client portal setup invitation.")
    public ResponseEntity<ApiResponse<Void>> resendPortalInvitation(@PathVariable UUID clientId) {
        clientService.resendPortalInvitation(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client portal activation email resent successfully", null));
    }

    @DeleteMapping("/{clientId}")
    @PreAuthorize("hasAuthority('CLIENT_DELETE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Archive client", description = "Archives client record within the authenticated tenant.")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable UUID clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client archived successfully", null));
    }

    @GetMapping("/{clientId}/overview")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('TASK_CREATE') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Client 360-Degree Overview", description = "Aggregates all modules related to a client into a single dashboard: Profile, Statutory, Active Services, Tasks, Compliance (GST/ITR/TDS), Documents, Document Requests, Billing, Notices, and Activity Timeline.")
    public ResponseEntity<ApiResponse<ClientOverviewDto>> getClientOverview(@PathVariable UUID clientId) {
        ClientOverviewDto overview = clientService.getClientOverview(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client 360 overview retrieved successfully", overview));
    }

    @GetMapping("/{clientId}/360")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('TASK_CREATE') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Client 360 Foundation Read Model", description = "Retrieves unified client foundation: profile, identifiers, primary/assigned locations, assigned user portfolio, and configured services.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.client.dto.Client360Dto>> getClient360(@PathVariable UUID clientId) {
        com.taxoryn.module.client.dto.Client360Dto client360 = clientService.getClient360(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client 360 retrieved successfully", client360));
    }

    @PostMapping("/{clientId}/notes")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasAuthority('TASK_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Add communication note", description = "Records a client call, meeting, email interaction, or follow-up note in the client communication log.")
    public ResponseEntity<ApiResponse<ClientNoteDto>> addClientNote(@PathVariable UUID clientId, @Valid @RequestBody CreateClientNoteRequest request) {
        ClientNoteDto note = clientService.addClientNote(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Client note added successfully", note));
    }

    @GetMapping("/{clientId}/notes")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasAuthority('TASK_VIEW') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get client notes", description = "Retrieves communication notes and internal interaction history for a client.")
    public ResponseEntity<ApiResponse<List<ClientNoteDto>>> getClientNotes(@PathVariable UUID clientId) {
        List<ClientNoteDto> notes = clientService.getClientNotes(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client notes retrieved successfully", notes));
    }

    @GetMapping("/{clientId}/communications")
    @PreAuthorize("hasAuthority('CLIENT_COMMUNICATION_VIEW')")
    @Operation(summary = "List client communication timeline entries", description = "Returns a tenant- and client-scope-checked, paginated interaction timeline.")
    public ResponseEntity<ApiResponse<PagedResponse<ClientCommunicationDto>>> getClientCommunications(
            @PathVariable UUID clientId,
            @RequestParam(required = false) NoteType type,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(required = false) Boolean followUpRequired,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ClientCommunicationDto> response = communicationTimelineService.listPracticeEntries(
                clientId, type, dateFrom, dateTo, followUpRequired, page, size);
        return ResponseEntity.ok(ApiResponse.success("Client communications retrieved", response));
    }

    @GetMapping("/{clientId}/communications/{communicationId}")
    @PreAuthorize("hasAuthority('CLIENT_COMMUNICATION_VIEW')")
    public ResponseEntity<ApiResponse<ClientCommunicationDto>> getClientCommunication(
            @PathVariable UUID clientId, @PathVariable UUID communicationId) {
        return ResponseEntity.ok(ApiResponse.success("Client communication retrieved",
                communicationTimelineService.getPracticeEntry(clientId, communicationId)));
    }

    @PostMapping("/{clientId}/communications")
    @PreAuthorize("hasAuthority('CLIENT_COMMUNICATION_CREATE')")
    public ResponseEntity<ApiResponse<ClientCommunicationDto>> createClientCommunication(
            @PathVariable UUID clientId, @Valid @RequestBody ClientCommunicationRequest request) {
        ClientCommunicationDto created = communicationTimelineService.create(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Communication recorded", created));
    }

    @PutMapping("/{clientId}/communications/{communicationId}")
    @PreAuthorize("hasAuthority('CLIENT_COMMUNICATION_UPDATE')")
    public ResponseEntity<ApiResponse<ClientCommunicationDto>> updateClientCommunication(
            @PathVariable UUID clientId, @PathVariable UUID communicationId,
            @Valid @RequestBody ClientCommunicationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Communication updated",
                communicationTimelineService.update(clientId, communicationId, request)));
    }

    @DeleteMapping("/{clientId}/communications/{communicationId}")
    @PreAuthorize("hasAuthority('CLIENT_COMMUNICATION_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteClientCommunication(
            @PathVariable UUID clientId, @PathVariable UUID communicationId) {
        communicationTimelineService.delete(clientId, communicationId);
        return ResponseEntity.ok(ApiResponse.success("Communication deleted", null));
    }

    @GetMapping("/{clientId}/locations")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get client location assignments", description = "Retrieves all locations assigned to the client.")
    public ResponseEntity<ApiResponse<List<com.taxoryn.module.client.dto.ClientLocationAssignmentDto>>> getClientLocations(@PathVariable UUID clientId) {
        List<com.taxoryn.module.client.dto.ClientLocationAssignmentDto> list = clientService.getClientLocations(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client locations retrieved successfully", list));
    }

    @PostMapping("/{clientId}/locations")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN')")
    @Operation(summary = "Assign location to client", description = "Assigns an operating/branch location to a client.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.client.dto.ClientLocationAssignmentDto>> assignClientLocation(
            @PathVariable UUID clientId,
            @Valid @RequestBody com.taxoryn.module.client.dto.AssignClientLocationRequest request) {
        com.taxoryn.module.client.dto.ClientLocationAssignmentDto dto = clientService.assignClientLocation(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Location assigned to client successfully", dto));
    }

    @DeleteMapping("/{clientId}/locations/{locationId}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN')")
    @Operation(summary = "Remove location from client", description = "Removes a location assignment from a client.")
    public ResponseEntity<ApiResponse<Void>> removeClientLocation(
            @PathVariable UUID clientId,
            @PathVariable UUID locationId) {
        clientService.removeClientLocation(clientId, locationId);
        return ResponseEntity.ok(ApiResponse.success("Location removed from client successfully", null));
    }

    @PutMapping("/{clientId}/locations/{locationId}/primary")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN')")
    @Operation(summary = "Set primary location for client", description = "Designates a specific assigned location as the client's primary servicing location.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.client.dto.ClientLocationAssignmentDto>> setPrimaryLocation(
            @PathVariable UUID clientId,
            @PathVariable UUID locationId) {
        com.taxoryn.module.client.dto.ClientLocationAssignmentDto dto = clientService.setPrimaryLocation(clientId, locationId);
        return ResponseEntity.ok(ApiResponse.success("Primary location updated successfully", dto));
    }

    @GetMapping("/{clientId}/users")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get client user portfolio assignments", description = "Retrieves all staff users assigned to the client portfolio.")
    public ResponseEntity<ApiResponse<List<com.taxoryn.module.client.dto.ClientUserAssignmentDto>>> getClientUsers(@PathVariable UUID clientId) {
        List<com.taxoryn.module.client.dto.ClientUserAssignmentDto> list = clientService.getClientUsers(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client user assignments retrieved successfully", list));
    }

    @PostMapping("/{clientId}/users")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN')")
    @Operation(summary = "Assign user to client portfolio", description = "Assigns a practice user (Primary/Supporting/Reviewer/Partner) to the client portfolio.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.client.dto.ClientUserAssignmentDto>> assignClientUser(
            @PathVariable UUID clientId,
            @Valid @RequestBody com.taxoryn.module.client.dto.AssignClientUserRequest request) {
        com.taxoryn.module.client.dto.ClientUserAssignmentDto dto = clientService.assignClientUser(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User assigned to client portfolio successfully", dto));
    }

    @DeleteMapping("/{clientId}/users/{userId}")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN')")
    @Operation(summary = "Remove user from client portfolio", description = "Removes a user from the client portfolio assignment.")
    public ResponseEntity<ApiResponse<Void>> removeClientUser(
            @PathVariable UUID clientId,
            @PathVariable UUID userId) {
        clientService.removeClientUser(clientId, userId);
        return ResponseEntity.ok(ApiResponse.success("User removed from client portfolio successfully", null));
    }

    @PutMapping("/{clientId}/users/{userId}/primary")
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN')")
    @Operation(summary = "Set primary responsible user for client", description = "Designates an assigned user as the primary responsible practitioner for the client.")
    public ResponseEntity<ApiResponse<com.taxoryn.module.client.dto.ClientUserAssignmentDto>> setPrimaryResponsibleUser(
            @PathVariable UUID clientId,
            @PathVariable UUID userId) {
        com.taxoryn.module.client.dto.ClientUserAssignmentDto dto = clientService.setPrimaryResponsibleUser(clientId, userId);
        return ResponseEntity.ok(ApiResponse.success("Primary responsible user updated successfully", dto));
    }
}
