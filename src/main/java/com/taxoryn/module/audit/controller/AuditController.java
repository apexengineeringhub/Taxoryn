package com.taxoryn.module.audit.controller;

import com.taxoryn.core.response.ApiResponse;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.audit.dto.AuditLogDto;
import com.taxoryn.module.audit.dto.AuditLogFilterRequest;
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
@RequestMapping({"/api/v1/audit-logs", "/api/audit-logs", "/api/v1/admin/audit-logs", "/api/admin/audit-logs"})
@RequiredArgsConstructor
@Tag(name = "Audit Trail", description = "Endpoints for inspecting immutable tenant activity and enterprise security audit logs")
@SecurityRequirement(name = "BearerAuth")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ') or hasAuthority('AUDIT_VIEW') or hasAuthority('SECURITY_VIEW') " +
            "or hasRole('SUPER_ADMIN') or hasRole('TAXORYN_SUPERADMIN') or hasRole('TAXORYN_OPERATIONS_ADMIN') " +
            "or hasRole('TAXORYN_SUPPORT_ADMIN') or hasRole('TAXORYN_FINANCE_ADMIN') " +
            "or hasRole('ORG_ADMIN') or hasRole('PRACTICE_OWNER') or hasRole('PRACTICE_ADMIN')")
    @Operation(summary = "List audit logs with filtering and pagination", description = "Retrieves paginated, immutable audit trail records with role-aware scope.")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogDto>>> getAuditLogs(
            @Valid @ModelAttribute AuditLogFilterRequest filterRequest) {
        PagedResponse<AuditLogDto> response = auditService.getAuditLogs(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", response));
    }
}
