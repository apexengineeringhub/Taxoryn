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
import com.taxoryn.module.dashboard.dto.PlatformDashboardSummaryDto.RecentPlatformActivityDto;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogs(AuditLogFilterRequest filterRequest) {
        boolean isPlatformUser = SecurityUtils.isTaxorynPlatformUser() || SecurityUtils.isTaxorynSuperAdmin();
        UUID currentOrgId = SecurityUtils.getCurrentOrganizationId();

        log.debug("Fetching audit logs: isPlatformUser={}, currentOrgId={}, search={}, action={}, entityType={}",
                isPlatformUser, currentOrgId, filterRequest.getSearch(), filterRequest.getAction(), filterRequest.getEntityType());

        Specification<AuditLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Role-aware Scope Isolation:
            if (!isPlatformUser) {
                // Practice Admin or Practice Staff: Strictly constrained to their own practice
                if (currentOrgId != null) {
                    predicates.add(cb.equal(root.get("organizationId"), currentOrgId));
                } else {
                    predicates.add(cb.isNull(root.get("organizationId")));
                }
            } else {
                // Platform SuperAdmin / Operations Admin:
                // Platform-wide visibility by default, or filtered by practice if explicitly requested
                if (filterRequest.getOrganizationId() != null) {
                    predicates.add(cb.equal(root.get("organizationId"), filterRequest.getOrganizationId()));
                }
            }

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
                        cb.like(cb.lower(root.get("ipAddress")), searchPattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLogEntity> page = auditLogRepository.findAll(spec, filterRequest.toPageable());

        // Batch resolve organization and user details for the current page
        Set<UUID> orgIds = page.getContent().stream()
                .map(AuditLogEntity::getOrganizationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> userIds = page.getContent().stream()
                .map(AuditLogEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> orgNames = organizationRepository.findAllById(orgIds).stream()
                .collect(Collectors.toMap(OrganizationEntity::getId, OrganizationEntity::getName, (a, b) -> a));

        Map<UUID, UserEntity> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u, (a, b) -> a));

        return PagedResponse.of(page, logEntity -> toEnrichedDto(logEntity, orgNames, userMap));
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
    @Transactional(readOnly = true)
    public List<RecentPlatformActivityDto> getRecentImportantActivity(int limit) {
        try {
            int fetchSize = limit > 0 ? limit : 6;
            List<AuditLogEntity> recentLogs = auditLogRepository.findAll(
                    PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();

            Set<UUID> orgIds = recentLogs.stream()
                    .map(AuditLogEntity::getOrganizationId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<UUID> userIds = recentLogs.stream()
                    .map(AuditLogEntity::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<UUID, String> orgNames = organizationRepository.findAllById(orgIds).stream()
                    .collect(Collectors.toMap(OrganizationEntity::getId, OrganizationEntity::getName, (a, b) -> a));

            Map<UUID, String> userNames = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName, (a, b) -> a));

            List<RecentPlatformActivityDto> results = new ArrayList<>();
            for (AuditLogEntity logItem : recentLogs) {
                results.add(mapToRecentActivityDto(logItem, orgNames, userNames));
            }
            return results;
        } catch (Exception ex) {
            log.warn("Failed retrieving recent platform activity from audit service: {}", ex.getMessage());
            return Collections.emptyList();
        }
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

        String action = request.getAction() != null ? request.getAction() : "EVENT";
        if (action.length() > 100) action = action.substring(0, 100);

        String entityType = StringUtils.hasText(request.getEntityType())
                ? request.getEntityType()
                : (request.getEntityName() != null ? request.getEntityName() : "GENERAL");
        if (entityType.length() > 100) entityType = entityType.substring(0, 100);

        String entityId = request.getEntityId();
        if (entityId != null && entityId.length() > 255) entityId = entityId.substring(0, 255);

        if (ipAddress != null && ipAddress.length() > 50) ipAddress = ipAddress.substring(0, 50);
        if (requestId != null && requestId.length() > 100) requestId = requestId.substring(0, 100);
        if (userAgent != null && userAgent.length() > 500) userAgent = userAgent.substring(0, 500);

        AuditLogEntity auditLog = AuditLogEntity.builder()
                .organizationId(organizationId)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityName(entityType)
                .entityId(entityId)
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

    private RecentPlatformActivityDto mapToRecentActivityDto(AuditLogEntity logItem, Map<UUID, String> orgNames, Map<UUID, String> userNames) {
        String action = logItem.getAction() != null ? logItem.getAction() : "EVENT";
        String title = formatDisplayAction(action);
        String targetName = "Platform Operations";
        String severity = "INFO";
        String status = "SUCCESS";
        String nav = "/audit-logs";

        UUID entityUuid = null;
        if (logItem.getEntityId() != null) {
            try {
                entityUuid = UUID.fromString(logItem.getEntityId());
            } catch (Exception ignored) {}
        }

        if (action.contains("PRACTICE") || action.contains("ORGANIZATION")) {
            nav = "/admin/practices";
            if (entityUuid != null && orgNames.containsKey(entityUuid)) {
                targetName = orgNames.get(entityUuid);
            } else if (logItem.getOrganizationId() != null && orgNames.containsKey(logItem.getOrganizationId())) {
                targetName = orgNames.get(logItem.getOrganizationId());
            } else {
                targetName = "Tax Practice Tenant";
            }
            if (action.contains("SUSPEND")) {
                severity = "WARNING";
                status = "ALERT";
            } else if (action.contains("VERIF")) {
                severity = "SUCCESS";
            }
        } else if (action.contains("FEEDBACK")) {
            nav = "/admin/feedback";
            targetName = "Application Feedback";
            if (action.contains("RESOLV") || action.contains("CLOSE")) {
                severity = "SUCCESS";
            } else if (action.contains("ESCALAT")) {
                severity = "WARNING";
                status = "ALERT";
            }
        } else if (action.contains("MARKETPLACE") || action.contains("LEAD") || action.contains("REQUIREMENT") || action.contains("CONSULTATION")) {
            nav = "/admin/marketplace";
            targetName = "Marketplace Services";
        } else if (action.contains("SUBSCRIPTION") || action.contains("PAYMENT") || action.contains("BILLING")) {
            nav = "/admin/subscriptions";
            if (logItem.getOrganizationId() != null && orgNames.containsKey(logItem.getOrganizationId())) {
                targetName = orgNames.get(logItem.getOrganizationId());
            } else {
                targetName = "SaaS Subscription";
            }
        } else if (action.contains("USER") || action.contains("CUSTOMER") || action.contains("ACCOUNT")) {
            nav = "/admin/users";
            if (entityUuid != null && userNames.containsKey(entityUuid)) {
                targetName = userNames.get(entityUuid);
            } else if (logItem.getUserId() != null && userNames.containsKey(logItem.getUserId())) {
                targetName = userNames.get(logItem.getUserId());
            } else {
                targetName = "Platform User";
            }
        } else if (action.contains("SECURITY") || action.contains("TOKEN") || action.contains("AUTH")) {
            nav = "/audit-logs";
            targetName = "Platform Security";
            severity = "WARNING";
        }

        return RecentPlatformActivityDto.builder()
                .id(logItem.getId() != null ? logItem.getId().toString() : UUID.randomUUID().toString())
                .displayTitle(title)
                .description(targetName)
                .targetDisplayName(targetName)
                .timestamp(logItem.getCreatedAt() != null ? logItem.getCreatedAt() : Instant.now())
                .severity(severity)
                .status(status)
                .navigationTarget(nav)
                .build();
    }

    private String formatDisplayAction(String action) {
        if (action == null) return "Platform Action";
        return switch (action) {
            case "PRACTICE_CREATED", "ORGANIZATION_CREATED" -> "New practice registered";
            case "PRACTICE_VERIFIED" -> "Practice verified";
            case "PRACTICE_SUSPENDED" -> "Practice suspended";
            case "APPLICATION_FEEDBACK_CREATED", "FEEDBACK_CREATED" -> "New feedback received";
            case "FEEDBACK_STATUS_UPDATED", "FEEDBACK_RESOLVED" -> "Feedback resolved";
            case "FEEDBACK_ESCALATED" -> "Feedback escalated to engineering";
            case "CUSTOMER_PROFILE_CREATED", "CUSTOMER_REGISTERED" -> "New marketplace customer registered";
            case "CUSTOMER_PROFILE_UPDATED" -> "Customer profile updated";
            case "CUSTOMER_ACCOUNT_CREATED" -> "New customer account created";
            case "MARKETPLACE_CONSULTATION_BOOKED", "CONSULTATION_BOOKED" -> "Marketplace consultation booked";
            case "MARKETPLACE_LEAD_CREATED", "REQUIREMENT_SUBMITTED" -> "New marketplace enquiry";
            case "SUBSCRIPTION_CREATED", "SUBSCRIPTION_UPDATED" -> "Subscription updated";
            case "SUBSCRIPTION_UPGRADED" -> "Subscription upgraded";
            case "ROLE_CREATED", "USER_ROLES_ASSIGNED", "ROLE_CHANGED" -> "Administrator role changed";
            case "SECURITY_EVENT", "SECURITY_ALERT" -> "Security event detected";
            case "USER_CREATED" -> "New user registered";
            case "USER_UPDATED" -> "User account updated";
            case "USER_STATUS_UPDATED" -> "User status changed";
            case "CLIENT_CREATED" -> "Client record created";
            case "CLIENT_UPDATED" -> "Client record updated";
            case "INVOICE_CREATED" -> "Invoice issued";
            case "INVOICE_UPDATED" -> "Invoice status updated";
            case "GST_FILING_SUBMITTED" -> "GST return submitted";
            case "ITR_RETURN_SUBMITTED" -> "ITR computation prepared";
            default -> action.replace('_', ' ').toLowerCase().replaceFirst("^\\w", String.valueOf(Character.toUpperCase(action.replace('_', ' ').charAt(0))));
        };
    }

    private String formatDisplayEntityType(String entityType) {
        if (entityType == null) return "General";
        return switch (entityType) {
            case "ORGANIZATION", "PRACTICE" -> "Practice";
            case "USER" -> "User";
            case "CUSTOMER", "MARKETPLACE_CUSTOMER_PROFILE" -> "Customer";
            case "APPLICATION_FEEDBACK", "FEEDBACK" -> "Feedback";
            case "MARKETPLACE_LEAD", "MARKETPLACE_REQUIREMENT" -> "Marketplace";
            case "MARKETPLACE_CONSULTATION" -> "Consultation";
            case "SUBSCRIPTION" -> "Subscription";
            case "ROLE", "USER_ROLE" -> "Role & Permission";
            case "CLIENT" -> "Practice Client";
            case "INVOICE" -> "Invoice";
            case "GST_PROFILE", "GST_RETURN" -> "GST";
            case "ITR_PROFILE", "ITR_RETURN" -> "ITR";
            case "DOCUMENT" -> "Document";
            default -> entityType.replace('_', ' ').toLowerCase().replaceFirst("^\\w", String.valueOf(Character.toUpperCase(entityType.replace('_', ' ').charAt(0))));
        };
    }

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
        String traceId = MDC.get(MdcLoggingFilter.MDC_TRACE_ID_KEY);
        if (StringUtils.hasText(traceId)) {
            return traceId;
        }

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
                .displayAction(formatDisplayAction(entity.getAction()))
                .entityType(entityType)
                .displayEntityType(formatDisplayEntityType(entityType))
                .entityName(entityType)
                .entityId(entity.getEntityId())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .ipAddress(entity.getIpAddress())
                .requestId(entity.getRequestId())
                .userAgent(entity.getUserAgent())
                .timestamp(entity.getCreatedAt())
                .createdAt(entity.getCreatedAt())
                .status("SUCCESS")
                .severity("INFO")
                .build();
    }

    private AuditLogDto toEnrichedDto(AuditLogEntity entity, Map<UUID, String> orgNames, Map<UUID, UserEntity> userMap) {
        String entityType = StringUtils.hasText(entity.getEntityType())
                ? entity.getEntityType()
                : entity.getEntityName();

        String orgName = entity.getOrganizationId() != null && orgNames.containsKey(entity.getOrganizationId())
                ? orgNames.get(entity.getOrganizationId())
                : "Taxoryn Platform Global";

        UserEntity user = entity.getUserId() != null ? userMap.get(entity.getUserId()) : null;
        String actorName = user != null ? user.getFullName() : "System Automated";
        String actorEmail = user != null ? user.getEmail() : "system@taxoryn.com";
        String actorRole = user != null && user.getRoles() != null && !user.getRoles().isEmpty()
                ? user.getRoles().stream().findFirst().map(RoleEntity::getCode).orElse("USER")
                : "SYSTEM";

        String status = "SUCCESS";
        String severity = "INFO";
        String action = entity.getAction() != null ? entity.getAction() : "";

        if (action.contains("SUSPEND") || action.contains("FAIL") || action.contains("ALERT") || action.contains("REJECT")) {
            status = "ALERT";
            severity = "WARNING";
        } else if (action.contains("VERIF") || action.contains("RESOLV") || action.contains("CONVERT")) {
            severity = "SUCCESS";
        } else if (action.contains("SECURITY")) {
            severity = "CRITICAL";
            status = "ALERT";
        }

        return AuditLogDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .organizationName(orgName)
                .practiceName(orgName)
                .userId(entity.getUserId())
                .actor(actorName)
                .actorName(actorName)
                .actorEmail(actorEmail)
                .actorRole(actorRole)
                .action(entity.getAction())
                .displayAction(formatDisplayAction(entity.getAction()))
                .entityType(entityType)
                .displayEntityType(formatDisplayEntityType(entityType))
                .entityName(entityType)
                .entityId(entity.getEntityId())
                .targetDisplayName(orgName)
                .status(status)
                .severity(severity)
                .description("Action " + formatDisplayAction(entity.getAction()) + " on " + formatDisplayEntityType(entityType))
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
