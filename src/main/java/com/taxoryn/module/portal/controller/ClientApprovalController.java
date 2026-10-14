package com.taxoryn.module.portal.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.module.moduleconfig.annotation.RequiresModule;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;
import com.taxoryn.module.portal.dto.ClientApprovalDto;
import com.taxoryn.module.portal.dto.CreateClientApprovalRequest;
import com.taxoryn.module.portal.dto.DecideClientApprovalRequest;
import com.taxoryn.module.portal.dto.RevokeClientApprovalRequest;
import com.taxoryn.module.portal.entity.ClientApprovalStatus;
import com.taxoryn.module.portal.entity.MessageContextType;
import com.taxoryn.module.portal.service.ClientApprovalService;
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
@RequestMapping({"/api/v1/portal", "/api/portal"})
@RequiredArgsConstructor
@RequiresModule(ProductModuleCode.CLIENT_PORTAL)
@Tag(name = "Client Approvals & Consent", description = "Endpoints for managing client sign-offs, filing consents, tax computation approvals, and decision tracking")
@SecurityRequirement(name = "BearerAuth")
public class ClientApprovalController {

    private final ClientApprovalService clientApprovalService;

    // --- Client Portal User Endpoints ---

    @GetMapping("/approvals")
    @PreAuthorize("hasRole('CLIENT_USER') or hasRole('CLIENT_ADMIN') or hasAuthority('CLIENT_PORTAL_VIEW')")
    @Operation(summary = "List My Approval Requests", description = "Retrieves all approval and consent requests assigned to the authenticated client.")
    public ResponseEntity<ApiResponse<List<ClientApprovalDto>>> getMyApprovals(
            @RequestParam(required = false) ClientApprovalStatus status) {
        List<ClientApprovalDto> approvals = clientApprovalService.getClientOwnApprovalRequests(status);
        return ResponseEntity.ok(ApiResponse.success("Client approvals retrieved", approvals));
    }

    @GetMapping("/approvals/{approvalId}")
    @PreAuthorize("hasRole('CLIENT_USER') or hasRole('CLIENT_ADMIN') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('ORG_ADMIN') or hasRole('PRACTITIONER') or hasAuthority('CLIENT_VIEW')")
    @Operation(summary = "Get Approval Request by ID", description = "Retrieves complete details of an approval request with decision provenance and document links.")
    public ResponseEntity<ApiResponse<ClientApprovalDto>> getApprovalById(@PathVariable UUID approvalId) {
        ClientApprovalDto dto = clientApprovalService.getApprovalRequestById(approvalId);
        return ResponseEntity.ok(ApiResponse.success("Approval request retrieved", dto));
    }

    @PostMapping("/approvals/{approvalId}/decide")
    @PreAuthorize("hasRole('CLIENT_USER') or hasRole('CLIENT_ADMIN')")
    @Operation(summary = "Decide Approval Request", description = "Submits client approval or rejection on an approval request with server-side actor identity.")
    public ResponseEntity<ApiResponse<ClientApprovalDto>> decideApproval(
            @PathVariable UUID approvalId,
            @Valid @RequestBody DecideClientApprovalRequest request) {
        ClientApprovalDto decided = clientApprovalService.decideApprovalRequest(approvalId, request);
        return ResponseEntity.ok(ApiResponse.success("Approval decision recorded", decided));
    }

    // --- Practice Practitioner Endpoints ---

    @PostMapping("/clients/{clientId}/approvals")
    @PreAuthorize("hasAuthority('CLIENT_MANAGE') or hasAuthority('CLIENT_WRITE') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('ORG_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Create Client Approval Request", description = "Initiates a new approval or consent request for a client from the practice side.")
    public ResponseEntity<ApiResponse<ClientApprovalDto>> createApproval(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateClientApprovalRequest request) {
        ClientApprovalDto created = clientApprovalService.createApprovalRequest(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Client approval request created", created));
    }

    @GetMapping("/clients/{clientId}/approvals")
    @PreAuthorize("hasAuthority('CLIENT_VIEW') or hasAuthority('CLIENT_READ') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('ORG_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "List Client Approvals (Practice)", description = "Retrieves approval requests for a specific client with status and context filters.")
    public ResponseEntity<ApiResponse<List<ClientApprovalDto>>> getPracticeClientApprovals(
            @PathVariable UUID clientId,
            @RequestParam(required = false) ClientApprovalStatus status,
            @RequestParam(required = false) MessageContextType contextType,
            @RequestParam(required = false) UUID contextId) {
        List<ClientApprovalDto> list = clientApprovalService.getApprovalRequestsForClient(clientId, status, contextType, contextId);
        return ResponseEntity.ok(ApiResponse.success("Client approvals retrieved", list));
    }

    @PostMapping("/clients/{clientId}/approvals/{approvalId}/revoke")
    @PreAuthorize("hasAuthority('CLIENT_MANAGE') or hasAuthority('CLIENT_WRITE') or hasRole('PRACTICE_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('ORG_ADMIN') or hasRole('PRACTITIONER')")
    @Operation(summary = "Revoke Client Approval Request", description = "Cancels or revokes a pending client approval request before client decision.")
    public ResponseEntity<ApiResponse<ClientApprovalDto>> revokeApproval(
            @PathVariable UUID clientId,
            @PathVariable UUID approvalId,
            @Valid @RequestBody RevokeClientApprovalRequest request) {
        ClientApprovalDto revoked = clientApprovalService.revokeApprovalRequest(clientId, approvalId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Approval request revoked", revoked));
    }
}
