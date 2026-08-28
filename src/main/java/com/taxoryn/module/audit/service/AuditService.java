package com.taxoryn.module.audit.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.audit.dto.AuditLogDto;
import com.taxoryn.module.audit.dto.AuditLogFilterRequest;
import com.taxoryn.module.audit.dto.AuditRecordRequest;
import com.taxoryn.module.dashboard.dto.PlatformDashboardSummaryDto.RecentPlatformActivityDto;

import java.util.List;
import java.util.UUID;

/**
 * Authoritative Service for immutable enterprise audit logging and inspection.
 */
public interface AuditService {

    /**
     * Retrieve paginated and filtered audit logs with role-aware tenant/platform scope.
     */
    PagedResponse<AuditLogDto> getAuditLogs(AuditLogFilterRequest filterRequest);

    /**
     * Legacy overload for basic page requests.
     */
    PagedResponse<AuditLogDto> getAuditLogs(PageRequestDto pageRequest);

    /**
     * Retrieve recent important platform activities for the SuperAdmin overview cockpit.
     */
    List<RecentPlatformActivityDto> getRecentImportantActivity(int limit);

    /**
     * Record a comprehensive audit log entry.
     */
    AuditLogDto recordAudit(AuditRecordRequest request);

    /**
     * Log a state transition or system action with automatically resolved request and user context.
     */
    AuditLogDto logEvent(String action, String entityType, String entityId, Object oldValue, Object newValue);

    /**
     * Log an action with explicit organization and user identifiers.
     */
    AuditLogDto logEvent(UUID organizationId, UUID userId, String action, String entityType, String entityId, Object oldValue, Object newValue);

    /**
     * Legacy method for recording audit entries.
     */
    void recordAudit(UUID organizationId, UUID userId, String action, String entityName, String entityId, String oldValue, String newValue, String ipAddress, String userAgent);
}
