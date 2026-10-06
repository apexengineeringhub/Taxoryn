package com.taxoryn.module.client.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.client.dto.ClientRelationshipDto;
import com.taxoryn.module.client.dto.CreateClientRelationshipRequest;
import com.taxoryn.module.client.service.ClientRelationshipService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/clients/{clientId}/relationships", "/api/clients/{clientId}/relationships"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENTS)
@Tag(name = "Client Relationships & Groups Management", description = "Management of client corporate relationships, holding/subsidiary structures, and client groups")
@SecurityRequirement(name = "BearerAuth")
public class ClientRelationshipController {

    private final ClientRelationshipService relationshipService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PARTNER') or hasRole('MANAGER') or hasRole('PRACTITIONER') or hasRole('TAX_PROFESSIONAL') or hasRole('STAFF') or hasRole('ARTICLE_ASSISTANT') or hasRole('PRACTICE_EMPLOYEE') or hasRole('ACCOUNTANT')")
    @Operation(summary = "List client relationships & group links", description = "Retrieves all corporate relationships and group associations involving this client within the authenticated tenant.")
    public ResponseEntity<ApiResponse<List<ClientRelationshipDto>>> getRelationships(@PathVariable UUID clientId) {
        List<ClientRelationshipDto> relationships = relationshipService.getRelationships(clientId);
        return ResponseEntity.ok(ApiResponse.success("Client relationships retrieved successfully", relationships));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasAuthority('CLIENT_CREATE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Create client relationship", description = "Establishes a new relationship (PARENT, SUBSIDIARY, GROUP_MEMBER, etc.) between two clients.")
    public ResponseEntity<ApiResponse<ClientRelationshipDto>> createRelationship(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateClientRelationshipRequest request) {
        ClientRelationshipDto created = relationshipService.createRelationship(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Client relationship established successfully", created));
    }

    @DeleteMapping("/{relationshipId}")
    @PreAuthorize("hasAuthority('CLIENT_DELETE') or hasAuthority('CLIENT_UPDATE') or hasAuthority('CLIENT_WRITE') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Remove client relationship", description = "Removes a relationship/group link between clients.")
    public ResponseEntity<ApiResponse<Void>> deleteRelationship(
            @PathVariable UUID clientId,
            @PathVariable UUID relationshipId) {
        relationshipService.deleteRelationship(clientId, relationshipId);
        return ResponseEntity.ok(ApiResponse.success("Client relationship removed successfully", null));
    }
}
