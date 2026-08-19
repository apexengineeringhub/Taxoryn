package com.taxoryn.module.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.filter.MdcLoggingFilter;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.dto.AuditLogDto;
import com.taxoryn.module.audit.dto.AuditLogFilterRequest;
import com.taxoryn.module.audit.dto.AuditRecordRequest;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogs(AuditLogFilterRequest filterRequest) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        Specification<AuditLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Strict Tenant Isolation
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            // 2. Entity Type filter
            if (StringUtils.hasText(filterRequest.getEntityType())) {
                String typePattern = filterRequest.getEntityType().trim().toUpperCase();
                predicates.add(cb.or(
                        cb.equal(cb.upper(root.get("entityType")), typePattern),
                        cb.equal(cb.upper(root.get("entityName")), typePattern)
                ));
            }

            // 3. Entity ID filter
            if (StringUtils.hasText(filterRequest.getEntityId())) {
                predicates.add(cb.equal(root.get("entityId"), filterRequest.getEntityId().trim()));
            }

            // 4. Action filter
            if (StringUtils.hasText(filterRequest.getAction())) {
                String actionPattern = "%" + filterRequest.getAction().trim().toUpperCase() + "%";
                predicates.add(cb.like(cb.upper(root.get("action")), actionPattern));
            }

            // 5. User ID filter
            if (filterRequest.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), filterRequest.getUserId()));
            }

            // 6. Request / Correlation ID filter
            if (StringUtils.hasText(filterRequest.getRequestId())) {
                predicates.add(cb.equal(root.get("requestId"), filterRequest.getRequestId().trim()));
            }

            // 7. Date Range filter (createdAt)
            if (filterRequest.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filterRequest.getStartDate()));
            }
            if (filterRequest.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filterRequest.getEndDate()));
            }

            // 8. Universal Search keyword filter
            if (StringUtils.hasText(filterRequest.getSearch())) {
                String searchPattern = "%" + filterRequest.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("action")), searchPattern),
                        cb.like(cb.lower(root.get("entityType")), searchPattern),
                        cb.like(cb.lower(root.get("entityName")), searchPattern),
                        cb.like(cb.lower(root.get("entityId")), searchPattern),
                        cb.like(cb.lower(root.get("ipAddress")), searchPattern),
                        cb.like(cb.lower(root.get("oldValue")), searchPattern),
                        cb.like(cb.lower(root.get("newValue")), searchPattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLogEntity> page = auditLogRepository.findAll(spec, filterRequest.toPageable());
        return PagedResponse.of(page, this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogs(PageRequestDto pageRequest) {
        AuditLogFilterRequest filter = AuditLogFilterRequest.builder()
                .page(pageRequest.getPage())
                .size(pageRequest.getSize())
                .sortBy(pageRequest.getSortBy())
                .sortDirection(pageRequest.getSortDirection())
                .build();
        return getAuditLogs(filter);
    }

    @Override
    @Transactional
    public AuditLogDto recordAudit(AuditRecordRequest request) {
        UUID organizationId = request.getOrganizationId() != null
                ? request.getOrganizationId()
                : resolveOrganizationId();

        UUID userId = request.getUserId() != null
                ? request.getUserId()
                : resolveUserId();

        String requestId = StringUtils.hasText(request.getRequestId())
                ? request.getRequestId()
                : resolveRequestId();

        String ipAddress = StringUtils.hasText(request.getIpAddress())
                ? request.getIpAddress()
                : resolveIpAddress();

        String userAgent = StringUtils.hasText(request.getUserAgent())
                ? request.getUserAgent()
                : resolveUserAgent();

        String entityType = StringUtils.hasText(request.getEntityType())
                ? request.getEntityType()
                : request.getEntityName();

        AuditLogEntity auditLog = AuditLogEntity.builder()
                .organizationId(organizationId)
                .userId(userId)
                .action(request.getAction())
                .entityType(entityType != null ? entityType : "GENERAL")
                .entityName(entityType != null ? entityType : "GENERAL")
                .entityId(request.getEntityId())
                .oldValue(request.getOldValue())
                .newValue(request.getNewValue())
                .ipAddress(ipAddress)
                .requestId(requestId)
                .userAgent(userAgent)
                .createdAt(Instant.now())
                .build();

        AuditLogEntity saved = auditLogRepository.save(auditLog);
        log.debug("Recorded audit log: action={}, entityType={}, entityId={}, tenant={}, user={}, requestId={}",
                saved.getAction(), saved.getEntityType(), saved.getEntityId(), saved.getOrganizationId(), saved.getUserId(), saved.getRequestId());

        return toDto(saved);
    }

    @Override
    @Transactional
    public AuditLogDto logEvent(String action, String entityType, String entityId, Object oldValue, Object newValue) {
        UUID orgId = resolveOrganizationId();
        UUID userId = resolveUserId();
        return logEvent(orgId, userId, action, entityType, entityId, oldValue, newValue);
    }

    @Override
    @Transactional
    public AuditLogDto logEvent(UUID organizationId, UUID userId, String action, String entityType, String entityId, Object oldValue, Object newValue) {
        AuditRecordRequest request = AuditRecordRequest.builder()
                .organizationId(organizationId)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityName(entityType)
                .entityId(entityId)
                .oldValue(serializeToString(oldValue))
                .newValue(serializeToString(newValue))
                .build();

        return recordAudit(request);
    }

    @Override
    @Transactional
    public void recordAudit(UUID organizationId, UUID userId, String action, String entityName, String entityId, String oldValue, String newValue, String ipAddress, String userAgent) {
        AuditRecordRequest request = AuditRecordRequest.builder()
                .organizationId(organizationId)
                .userId(userId)
                .action(action)
                .entityType(entityName)
                .entityName(entityName)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        recordAudit(request);
    }

    // =========================================================================
    // Helper & Context Extraction Methods
    // =========================================================================

    private UUID resolveOrganizationId() {
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                return tenantId;
            }
            return SecurityUtils.getCurrentOrganizationId();
        } catch (Exception e) {
            log.trace("No authenticated organization found in security context: {}", e.getMessage());
            return null;
        }
    }

    private UUID resolveUserId() {
        try {
            return SecurityUtils.getCurrentUser()
                    .map(com.taxoryn.core.security.SecurityUser::getUserId)
                    .orElse(null);
        } catch (Exception e) {
            log.trace("No authenticated user found in security context: {}", e.getMessage());
            return null;
        }
    }

    private String resolveRequestId() {
        // 1. Try MDC trace ID
        String traceId = MDC.get(MdcLoggingFilter.MDC_TRACE_ID_KEY);
        if (StringUtils.hasText(traceId)) {
            return traceId;
        }

        // 2. Try HTTP Request headers
        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            String headerTrace = request.getHeader(MdcLoggingFilter.TRACE_ID_HEADER);
            if (StringUtils.hasText(headerTrace)) {
                return headerTrace;
            }
            String headerReq = request.getHeader("X-Request-Id");
            if (StringUtils.hasText(headerReq)) {
                return headerReq;
            }
        }

        // 3. Fallback to fresh UUID
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveIpAddress() {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            return "127.0.0.1";
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            String[] ips = xForwardedFor.split(",");
            if (ips.length > 0 && StringUtils.hasText(ips[0])) {
                return ips[0].trim();
            }
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    private String resolveUserAgent() {
        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            return request.getHeader("User-Agent");
        }
        return null;
    }

    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String serializeToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit value of type {}: {}", value.getClass().getSimpleName(), e.getMessage());
            return String.valueOf(value);
        }
    }

    private AuditLogDto toDto(AuditLogEntity entity) {
        String entityType = StringUtils.hasText(entity.getEntityType())
                ? entity.getEntityType()
                : entity.getEntityName();

        return AuditLogDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .userId(entity.getUserId())
                .action(entity.getAction())
                .entityType(entityType)
                .entityName(entityType)
                .entityId(entity.getEntityId())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .ipAddress(entity.getIpAddress())
                .requestId(entity.getRequestId())
                .userAgent(entity.getUserAgent())
                .timestamp(entity.getCreatedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
