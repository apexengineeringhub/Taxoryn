package com.taxoryn.module.audit.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.audit.dto.AuditLogDto;

import java.util.UUID;

public interface AuditService {

    PagedResponse<AuditLogDto> getAuditLogs(PageRequestDto pageRequest);

    void recordAudit(UUID organizationId, UUID userId, String action, String entityName, String entityId, String oldValue, String newValue, String ipAddress, String userAgent);
}
