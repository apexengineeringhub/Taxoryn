package com.taxoryn.module.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.filter.MdcLoggingFilter;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.dto.AuditLogDto;
import com.taxoryn.module.audit.dto.AuditLogFilterRequest;
import com.taxoryn.module.audit.dto.AuditRecordRequest;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> SECURITY_ACTIONS = Set.of(
            "REFRESH_TOKEN_ROTATED",
            "REFRESH_TOKEN_EXPIRED",
            "REFRESH_TOKEN_REVOKED",
            "TOKEN_REUSE_DETECTED",
            "SESSION_REVOKED",
            "ALL_SESSIONS_REVOKED",
            "SESSION_ROTATED",
            "SESSION_EXPIRED",
            "JWT_VALIDATION_FAILURE",
            "PASSWORD_HASH_UPDATED",
            "SECURITY_EVENT",
            "SECURITY_ALERT",
            "SUSPICIOUS_LOGIN",
            "ACCESS_REVOKED"
    );

    private static final Set<String> ACCESS_ACTIONS = Set.of(
            "LOGIN_SUCCESS",
            "LOGIN_FAILURE",
            "LOGOUT",
            "LOGOUT_SUCCESS",
            "PASSWORD_CHANGED",
            "PASSWORD_RESET_REQUESTED",
            "PASSWORD_RESET_COMPLETED",
            "CLIENT_PORTAL_USER_INVITED",
            "CLIENT_PORTAL_USER_REGISTERED",
            "CLIENT_PORTAL_ACCESS_ENABLED",
            "CLIENT_PORTAL_ACCESS_DISABLED",
            "USER_ROLES_ASSIGNED",
            "EMPLOYEE_ROLE_UPDATED",
            "ROLE_CREATED",
            "ROLE_UPDATED",
            "ROLE_DELETED",
            "USER_CREATED",
            "USER_UPDATED",
            "USER_STATUS_UPDATED",
            "USER_DISABLED",
            "PORTAL_USER_CREATED",
            "PORTAL_USER_UPDATED"
    );

    private static final Set<String> SYSTEM_ACTIONS = Set.of(
            "SYSTEM_INITIALIZED",
            "DATABASE_MIGRATION",
            "BATCH_CLEANUP_JOB",
            "WELCOME_EMAIL_SENT"
    );

    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile(
            "\"(refreshToken|accessToken|password|passwordHash|secret|jwt|token|clientSecret|apiKey)\"\\s*:\\s*\"([^\"]*)\"",
            Pattern.CASE_INSENSITIVE
    );

    public static String resolveCategory(String action) {
        if (!StringUtils.hasText(action)) {
            return "BUSINESS";
        }
        String upper = action.trim().toUpperCase();
        if (SECURITY_ACTIONS.contains(upper)
                || upper.startsWith("REFRESH_TOKEN_")
                || upper.startsWith("TOKEN_REUSE_")
                || upper.startsWith("SESSION_")
                || upper.startsWith("SECURITY_")) {
            return "SECURITY";
        }
        if (ACCESS_ACTIONS.contains(upper)
                || upper.startsWith("LOGIN_")
                || upper.startsWith("LOGOUT")
                || upper.startsWith("PASSWORD_")
                || upper.startsWith("CLIENT_PORTAL_")
                || upper.startsWith("PORTAL_USER_")
                || upper.startsWith("USER_ROLES_")
                || upper.startsWith("EMPLOYEE_ROLE_")
                || upper.startsWith("ROLE_")) {
            return "ACCESS";
        }
        if (SYSTEM_ACTIONS.contains(upper)
                || upper.startsWith("SYSTEM_")
                || upper.startsWith("DATABASE_")
                || upper.startsWith("BATCH_")) {
            return "SYSTEM";
        }
        return "BUSINESS";
    }

    private boolean canViewSecurityAudit() {
        return SecurityUtils.isTaxorynPlatformUser()
                || SecurityUtils.isTaxorynSuperAdmin()
                || SecurityUtils.hasRole("PRACTICE_ADMIN")
                || SecurityUtils.hasRole("ORG_ADMIN")
                || SecurityUtils.hasRole("PRACTICE_OWNER")
                || SecurityUtils.hasRole("TAXORYN_SECURITY_ADMIN")
                || SecurityUtils.hasAuthority("SECURITY_VIEW")
                || SecurityUtils.hasAuthority("AUDIT_SECURITY_VIEW");
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogs(AuditLogFilterRequest filterRequest) {
        boolean isPlatformUser = SecurityUtils.isTaxorynPlatformUser() || SecurityUtils.isTaxorynSuperAdmin();
        UUID currentOrgId = SecurityUtils.getCurrentOrganizationId();

        // Enforce role authorization if SECURITY category is requested
        if (StringUtils.hasText(filterRequest.getCategory()) && "SECURITY".equalsIgnoreCase(filterRequest.getCategory().trim())) {
            if (!canViewSecurityAudit()) {
                throw new ForbiddenException("You do not have permission to view security audit logs");
            }
        }

        log.debug("Fetching audit logs: isPlatformUser={}, currentOrgId={}, category={}, clientId={}, search={}, action={}",
                isPlatformUser, currentOrgId, filterRequest.getCategory(), filterRequest.getClientId(), filterRequest.getSearch(), filterRequest.getAction());

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
                if (filterRequest.getOrganizationId() != null) {
                    predicates.add(cb.equal(root.get("organizationId"), filterRequest.getOrganizationId()));
                }
            }

            // 2. Category Filter
            if (StringUtils.hasText(filterRequest.getCategory())) {
                String cat = filterRequest.getCategory().trim().toUpperCase();
                if ("SECURITY".equals(cat)) {
                    List<Predicate> secPreds = new ArrayList<>();
                    for (String secAct : SECURITY_ACTIONS) {
                        secPreds.add(cb.equal(cb.upper(root.get("action")), secAct));
                    }
                    secPreds.add(cb.like(cb.upper(root.get("action")), "REFRESH_TOKEN_%"));
                    secPreds.add(cb.like(cb.upper(root.get("action")), "TOKEN_REUSE_%"));
                    secPreds.add(cb.like(cb.upper(root.get("action")), "SESSION_%"));
                    secPreds.add(cb.like(cb.upper(root.get("action")), "SECURITY_%"));
                    predicates.add(cb.or(secPreds.toArray(new Predicate[0])));
                } else if ("PRACTICE_ACTIVITY".equals(cat) || "PRACTICE".equals(cat)) {
                    List<Predicate> secPreds = new ArrayList<>();
                    for (String secAct : SECURITY_ACTIONS) {
                        secPreds.add(cb.equal(cb.upper(root.get("action")), secAct));
                    }
                    secPreds.add(cb.like(cb.upper(root.get("action")), "REFRESH_TOKEN_%"));
                    secPreds.add(cb.like(cb.upper(root.get("action")), "TOKEN_REUSE_%"));
                    secPreds.add(cb.like(cb.upper(root.get("action")), "SESSION_%"));
                    secPreds.add(cb.like(cb.upper(root.get("action")), "SECURITY_%"));
                    predicates.add(cb.not(cb.or(secPreds.toArray(new Predicate[0]))));
                } else if ("ACCESS".equals(cat)) {
                    List<Predicate> accPreds = new ArrayList<>();
                    for (String accAct : ACCESS_ACTIONS) {
                        accPreds.add(cb.equal(cb.upper(root.get("action")), accAct));
                    }
                    accPreds.add(cb.like(cb.upper(root.get("action")), "LOGIN_%"));
                    accPreds.add(cb.like(cb.upper(root.get("action")), "LOGOUT%"));
                    accPreds.add(cb.like(cb.upper(root.get("action")), "PASSWORD_%"));
                    accPreds.add(cb.like(cb.upper(root.get("action")), "CLIENT_PORTAL_%"));
                    predicates.add(cb.or(accPreds.toArray(new Predicate[0])));
                } else if ("SYSTEM".equals(cat)) {
                    List<Predicate> sysPreds = new ArrayList<>();
                    for (String sysAct : SYSTEM_ACTIONS) {
                        sysPreds.add(cb.equal(cb.upper(root.get("action")), sysAct));
                    }
                    sysPreds.add(cb.like(cb.upper(root.get("action")), "SYSTEM_%"));
                    predicates.add(cb.or(sysPreds.toArray(new Predicate[0])));
                } else if ("BUSINESS".equals(cat)) {
                    List<Predicate> excludePreds = new ArrayList<>();
                    for (String secAct : SECURITY_ACTIONS) {
                        excludePreds.add(cb.equal(cb.upper(root.get("action")), secAct));
                    }
                    for (String accAct : ACCESS_ACTIONS) {
                        excludePreds.add(cb.equal(cb.upper(root.get("action")), accAct));
                    }
                    for (String sysAct : SYSTEM_ACTIONS) {
                        excludePreds.add(cb.equal(cb.upper(root.get("action")), sysAct));
                    }
                    excludePreds.add(cb.like(cb.upper(root.get("action")), "REFRESH_TOKEN_%"));
                    excludePreds.add(cb.like(cb.upper(root.get("action")), "TOKEN_REUSE_%"));
                    excludePreds.add(cb.like(cb.upper(root.get("action")), "SESSION_%"));
                    excludePreds.add(cb.like(cb.upper(root.get("action")), "SECURITY_%"));
                    excludePreds.add(cb.like(cb.upper(root.get("action")), "LOGIN_%"));
                    excludePreds.add(cb.like(cb.upper(root.get("action")), "LOGOUT%"));
                    predicates.add(cb.not(cb.or(excludePreds.toArray(new Predicate[0]))));
                }
            } else {
                // If no category specified and caller is a practice user, exclude raw security events by default
                if (!isPlatformUser) {
                    List<Predicate> secPreds = new ArrayList<>();
                    for (String secAct : SECURITY_ACTIONS) {
                        secPreds.add(cb.equal(cb.upper(root.get("action")), secAct));
                    }
                    secPreds.add(cb.like(cb.upper(root.get("action")), "REFRESH_TOKEN_%"));
                    secPreds.add(cb.like(cb.upper(root.get("action")), "TOKEN_REUSE_%"));
                    secPreds.add(cb.like(cb.upper(root.get("action")), "SESSION_%"));
                    secPreds.add(cb.like(cb.upper(root.get("action")), "SECURITY_%"));
                    predicates.add(cb.not(cb.or(secPreds.toArray(new Predicate[0]))));
                }
            }

            // 3. Client ID filter
            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("entityId"), filterRequest.getClientId().toString()));
            }

            // 4. Entity Type filter
            if (StringUtils.hasText(filterRequest.getEntityType())) {
                String typePattern = filterRequest.getEntityType().trim().toUpperCase();
                predicates.add(cb.or(
                        cb.equal(cb.upper(root.get("entityType")), typePattern),
                        cb.equal(cb.upper(root.get("entityName")), typePattern)
                ));
            }

            // 5. Entity ID filter
            if (StringUtils.hasText(filterRequest.getEntityId())) {
                predicates.add(cb.equal(root.get("entityId"), filterRequest.getEntityId().trim()));
            }

            // 6. Action filter
            if (StringUtils.hasText(filterRequest.getAction())) {
                String actionPattern = "%" + filterRequest.getAction().trim().toUpperCase() + "%";
                predicates.add(cb.like(cb.upper(root.get("action")), actionPattern));
            }

            // 7. User ID filter
            if (filterRequest.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), filterRequest.getUserId()));
            }

            // 8. Request / Correlation ID filter
            if (StringUtils.hasText(filterRequest.getRequestId())) {
                predicates.add(cb.equal(root.get("requestId"), filterRequest.getRequestId().trim()));
            }

            // 9. Date Range filter (createdAt)
            if (filterRequest.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filterRequest.getStartDate()));
            }
            if (filterRequest.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filterRequest.getEndDate()));
            }

            // 10. Universal Search keyword filter
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

        Set<UUID> clientCandidateIds = new HashSet<>();
        for (AuditLogEntity logEntity : page.getContent()) {
            if (logEntity.getEntityId() != null) {
                try {
                    clientCandidateIds.add(UUID.fromString(logEntity.getEntityId()));
                } catch (Exception ignored) {}
            }
        }

        Map<UUID, String> orgNames = organizationRepository.findAllById(orgIds).stream()
                .collect(Collectors.toMap(OrganizationEntity::getId, OrganizationEntity::getName, (a, b) -> a));

        Map<UUID, UserEntity> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u, (a, b) -> a));

        Map<UUID, String> clientNames = clientCandidateIds.isEmpty() ? Collections.emptyMap() :
                clientRepository.findAllById(clientCandidateIds).stream()
                        .collect(Collectors.toMap(ClientEntity::getId, ClientEntity::getDisplayName, (a, b) -> a));

        return PagedResponse.of(page, logEntity -> toEnrichedDto(logEntity, orgNames, userMap, clientNames));
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
            case "ROLE_UPDATED" -> "Role updated";
            case "ROLE_DELETED" -> "Role deleted";
            case "EMPLOYEE_ROLE_UPDATED" -> "Employee role updated";
            case "EMPLOYEE_CREATED" -> "Employee onboarded";
            case "EMPLOYEE_UPDATED" -> "Employee profile updated";
            case "EMPLOYEE_STATUS_UPDATED" -> "Employee status changed";
            case "SECURITY_EVENT", "SECURITY_ALERT" -> "Security event detected";
            case "USER_CREATED" -> "New user registered";
            case "USER_UPDATED" -> "User account updated";
            case "USER_STATUS_UPDATED" -> "User status changed";
            case "USER_DISABLED" -> "User disabled";
            case "LOGIN_SUCCESS" -> "User logged in";
            case "LOGIN_FAILURE" -> "Failed login attempt";
            case "LOGOUT", "LOGOUT_SUCCESS" -> "User logged out";
            case "PASSWORD_CHANGED" -> "Password changed";
            case "PASSWORD_RESET_REQUESTED" -> "Password reset requested";
            case "PASSWORD_RESET_COMPLETED" -> "Password reset completed";
            case "CLIENT_PORTAL_USER_INVITED" -> "Client portal invitation sent";
            case "CLIENT_PORTAL_USER_REGISTERED" -> "Client portal user registered";
            case "CLIENT_PORTAL_ACCESS_ENABLED" -> "Client portal access enabled";
            case "CLIENT_PORTAL_ACCESS_DISABLED" -> "Client portal access disabled";
            case "CLIENT_CREATED" -> "Client record created";
            case "CLIENT_UPDATED" -> "Client record updated";
            case "CLIENT_STATUS_UPDATED" -> "Client status changed";
            case "DOCUMENT_UPLOADED" -> "Document uploaded";
            case "DOCUMENT_VERIFIED" -> "Document verified";
            case "DOCUMENT_SHARED" -> "Document shared";
            case "DOCUMENT_DELETED" -> "Document deleted";
            case "TASK_CREATED" -> "Task created";
            case "TASK_STATUS_UPDATED" -> "Task status updated";
            case "TASK_ASSIGNED" -> "Task assigned";
            case "INVOICE_CREATED" -> "Invoice issued";
            case "INVOICE_UPDATED" -> "Invoice status updated";
            case "INVOICE_PAYMENT_RECORDED" -> "Payment recorded";
            case "BILLING_INVOICE_GENERATED" -> "Billing invoice generated";
            case "BILLING_PAYMENT_PROCESSED" -> "Billing payment processed";
            case "GST_PROFILE_UPDATED" -> "GST profile updated";
            case "GST_FILING_SUBMITTED" -> "GST return submitted";
            case "ITR_PROFILE_UPDATED" -> "ITR profile updated";
            case "ITR_RETURN_SUBMITTED" -> "ITR computation prepared";
            case "TDS_RETURN_SUBMITTED" -> "TDS return submitted";
            case "REFRESH_TOKEN_ROTATED" -> "Refresh token rotated";
            case "REFRESH_TOKEN_EXPIRED" -> "Refresh token expired";
            case "REFRESH_TOKEN_REVOKED" -> "Refresh token revoked";
            case "TOKEN_REUSE_DETECTED" -> "Token reuse detected";
            case "SESSION_REVOKED" -> "Session revoked";
            case "ALL_SESSIONS_REVOKED" -> "All sessions revoked";
            case "SESSION_ROTATED" -> "Session rotated";
            case "SESSION_EXPIRED" -> "Session expired";
            case "JWT_VALIDATION_FAILURE" -> "JWT validation failure";
            case "PASSWORD_HASH_UPDATED" -> "Password hash updated";
            default -> action.replace('_', ' ').toLowerCase().replaceFirst("^\\w", String.valueOf(Character.toUpperCase(action.replace('_', ' ').charAt(0))));
        };
    }

    private String formatDisplayEntityType(String entityType) {
        if (entityType == null) return "General";
        return switch (entityType) {
            case "ORGANIZATION", "PRACTICE" -> "Practice";
            case "USER" -> "User";
            case "EMPLOYEE" -> "Employee";
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
            case "TASK" -> "Task";
            case "SESSION", "TOKEN", "AUTH" -> "Security Session";
            default -> entityType.replace('_', ' ').toLowerCase().replaceFirst("^\\w", String.valueOf(Character.toUpperCase(entityType.replace('_', ' ').charAt(0))));
        };
    }

    private String sanitizePayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return payload;
        }
        return SENSITIVE_KEY_PATTERN.matcher(payload).replaceAll("\"$1\": \"***MASKED***\"");
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
                .category(resolveCategory(entity.getAction()))
                .action(entity.getAction())
                .displayAction(formatDisplayAction(entity.getAction()))
                .entityType(entityType)
                .displayEntityType(formatDisplayEntityType(entityType))
                .entityName(entityType)
                .entityId(entity.getEntityId())
                .oldValue(sanitizePayload(entity.getOldValue()))
                .newValue(sanitizePayload(entity.getNewValue()))
                .ipAddress(entity.getIpAddress())
                .requestId(entity.getRequestId())
                .userAgent(entity.getUserAgent())
                .timestamp(entity.getCreatedAt())
                .createdAt(entity.getCreatedAt())
                .status("SUCCESS")
                .severity("INFO")
                .build();
    }

    private AuditLogDto toEnrichedDto(AuditLogEntity entity, Map<UUID, String> orgNames, Map<UUID, UserEntity> userMap, Map<UUID, String> clientNames) {
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
        String category = resolveCategory(action);

        if (action.contains("SUSPEND") || action.contains("FAIL") || action.contains("ALERT") || action.contains("REJECT")) {
            status = "ALERT";
            severity = "WARNING";
        } else if (action.contains("VERIF") || action.contains("RESOLV") || action.contains("CONVERT")) {
            severity = "SUCCESS";
        } else if ("SECURITY".equals(category) || action.contains("SECURITY")) {
            severity = "CRITICAL";
            status = "ALERT";
        }

        UUID entityUuid = null;
        if (entity.getEntityId() != null) {
            try {
                entityUuid = UUID.fromString(entity.getEntityId());
            } catch (Exception ignored) {}
        }

        String clientName = null;
        if (entityUuid != null && clientNames.containsKey(entityUuid)) {
            clientName = clientNames.get(entityUuid);
        }

        String targetDisplayName;
        if (clientName != null) {
            targetDisplayName = clientName;
        } else if ("CLIENT".equalsIgnoreCase(entityType) && entity.getEntityId() != null) {
            targetDisplayName = "Client (" + entity.getEntityId().substring(0, Math.min(8, entity.getEntityId().length())) + ")";
        } else if (orgNames.containsKey(entity.getOrganizationId())) {
            targetDisplayName = orgNames.get(entity.getOrganizationId());
        } else {
            targetDisplayName = orgName;
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
                .category(category)
                .clientName(clientName)
                .action(entity.getAction())
                .displayAction(formatDisplayAction(entity.getAction()))
                .entityType(entityType)
                .displayEntityType(formatDisplayEntityType(entityType))
                .entityName(entityType)
                .entityId(entity.getEntityId())
                .targetDisplayName(targetDisplayName)
                .status(status)
                .severity(severity)
                .description("Action " + formatDisplayAction(entity.getAction()) + " on " + formatDisplayEntityType(entityType))
                .oldValue(sanitizePayload(entity.getOldValue()))
                .newValue(sanitizePayload(entity.getNewValue()))
                .ipAddress(entity.getIpAddress())
                .requestId(entity.getRequestId())
                .userAgent(entity.getUserAgent())
                .timestamp(entity.getCreatedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
