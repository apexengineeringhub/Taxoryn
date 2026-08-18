package com.taxoryn.module.audit.controller;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.audit.dto.AuditLogDto;
import com.taxoryn.module.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Trail", description = "Endpoints for inspecting immutable tenant activity and security audit records")
@SecurityRequirement(name = "BearerAuth")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ') or hasRole('ORG_ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "List audit logs with pagination", description = "Retrieves paginated immutable audit records for the authenticated tenant.")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogDto>>> getAuditLogs(@Valid @ModelAttribute PageRequestDto pageRequest) {
        PagedResponse<AuditLogDto> response = auditService.getAuditLogs(pageRequest);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", response));
    }
}
