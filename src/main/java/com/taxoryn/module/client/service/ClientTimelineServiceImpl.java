package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.entity.AuditLogEntity;
import com.taxoryn.module.audit.repository.AuditLogRepository;
import com.taxoryn.module.client.dto.ClientTimelineFilterRequest;
import com.taxoryn.module.client.dto.ClientTimelineItemDto;
import com.taxoryn.module.client.entity.TimelineEventCategory;
import com.taxoryn.module.client.entity.TimelineEventSeverity;
import com.taxoryn.module.client.repository.ClientBranchRepository;
import com.taxoryn.module.client.repository.ClientContactRepository;
import com.taxoryn.module.client.repository.ClientNoteRepository;
import com.taxoryn.module.client.repository.ClientRelationshipRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientTimelineServiceImpl implements ClientTimelineService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ClientRepository clientRepository;
    private final AuditLogRepository auditLogRepository;
    private final ClientServiceRepository clientServiceRepository;
    private final ClientContactRepository clientContactRepository;
    private final ClientBranchRepository clientBranchRepository;
    private final ClientRelationshipRepository clientRelationshipRepository;
    private final ClientNoteRepository clientNoteRepository;
    private final UserRepository userRepository;

    @Override
    public PagedResponse<ClientTimelineItemDto> getClientTimeline(UUID clientId, ClientTimelineFilterRequest filter) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required");
        }

        // Validate client exists and belongs to organization
        clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        final ClientTimelineFilterRequest actualFilter = filter != null ? filter : ClientTimelineFilterRequest.builder().build();

        Set<String> allEntityIds = collectAllRelatedEntityIds(organizationId, clientId);

        int pageNum = actualFilter.getPage() != null && actualFilter.getPage() >= 0 ? actualFilter.getPage() : 0;
        int pageSize = actualFilter.getSize() != null && actualFilter.getSize() > 0 ? Math.min(actualFilter.getSize(), MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        PageRequest pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Order.desc("createdAt")));

        Specification<AuditLogEntity> spec = createSpecification(organizationId, allEntityIds, actualFilter);
        Page<AuditLogEntity> logPage = auditLogRepository.findAll(spec, pageable);

        // Fetch user names in batch for the page
        Map<UUID, String> userNames = resolveUserNames(organizationId, logPage.getContent());

        List<ClientTimelineItemDto> dtoList = logPage.getContent().stream()
                .map(log -> toDto(log, clientId, userNames))
                .filter(dto -> actualFilter.getCategory() == null || dto.getEventCategory() == actualFilter.getCategory())
                .collect(Collectors.toList());

        return PagedResponse.<ClientTimelineItemDto>builder()
                .content(dtoList)
                .pageNumber(logPage.getNumber())
                .pageSize(logPage.getSize())
                .totalElements(logPage.getTotalElements())
                .totalPages(logPage.getTotalPages())
                .isFirst(logPage.isFirst())
                .isLast(logPage.isLast())
                .hasNext(logPage.hasNext())
                .hasPrevious(logPage.hasPrevious())
                .build();
    }

    @Override
    public List<ClientTimelineItemDto> getRecentTimeline(UUID organizationId, UUID clientId, int limit) {
        if (organizationId == null || clientId == null) {
            return Collections.emptyList();
        }

        Set<String> allEntityIds = collectAllRelatedEntityIds(organizationId, clientId);
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        PageRequest pageable = PageRequest.of(0, boundedLimit, Sort.by(Sort.Order.desc("createdAt")));

        Specification<AuditLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));
            if (!allEntityIds.isEmpty()) {
                predicates.add(root.get("entityId").in(allEntityIds));
            } else {
                predicates.add(cb.equal(root.get("entityId"), clientId.toString()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLogEntity> logPage = auditLogRepository.findAll(spec, pageable);
        Map<UUID, String> userNames = resolveUserNames(organizationId, logPage.getContent());

        return logPage.getContent().stream()
                .map(log -> toDto(log, clientId, userNames))
                .collect(Collectors.toList());
    }

    private Set<String> collectAllRelatedEntityIds(UUID organizationId, UUID clientId) {
        Set<String> ids = new HashSet<>();
        ids.add(clientId.toString());

        if (clientServiceRepository != null) {
            clientServiceRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId)
                    .forEach(s -> ids.add(s.getId().toString()));
        }
        if (clientContactRepository != null) {
            clientContactRepository.findAllByOrganizationIdAndClientIdOrderByPrimaryContactDescCreatedAtAsc(organizationId, clientId)
                    .forEach(c -> ids.add(c.getId().toString()));
        }
        if (clientBranchRepository != null) {
            clientBranchRepository.findAllByOrganizationIdAndClientIdOrderByPrimaryBranchDescCreatedAtAsc(organizationId, clientId)
                    .forEach(b -> ids.add(b.getId().toString()));
        }
        if (clientRelationshipRepository != null) {
            clientRelationshipRepository.findAllForClient(organizationId, clientId)
                    .forEach(r -> ids.add(r.getId().toString()));
        }
        if (clientNoteRepository != null) {
            clientNoteRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId)
                    .forEach(n -> ids.add(n.getId().toString()));
        }

        return ids;
    }

    private Specification<AuditLogEntity> createSpecification(UUID organizationId, Set<String> allEntityIds, ClientTimelineFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (allEntityIds != null && !allEntityIds.isEmpty()) {
                predicates.add(root.get("entityId").in(allEntityIds));
            } else {
                predicates.add(cb.equal(root.get("entityId"), ""));
            }

            if (StringUtils.hasText(filter.getEventType())) {
                predicates.add(cb.equal(root.get("action"), filter.getEventType().trim()));
            }

            if (filter.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFrom()));
            }

            if (filter.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Map<UUID, String> resolveUserNames(UUID organizationId, List<AuditLogEntity> logs) {
        Set<UUID> userIds = logs.stream()
                .map(AuditLogEntity::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, String> map = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> {
            String name = StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getEmail();
            map.put(user.getId(), name);
        });
        return map;
    }

    private ClientTimelineItemDto toDto(AuditLogEntity log, UUID clientId, Map<UUID, String> userNames) {
        TimelineEventCategory category = resolveCategory(log.getAction(), log.getEntityType());
        TimelineEventSeverity severity = resolveSeverity(log.getAction(), log.getNewValue());
        String actorName = log.getUserId() != null ? userNames.getOrDefault(log.getUserId(), "Practice User") : "System";

        Map<String, Object> meta = new HashMap<>();
        if (StringUtils.hasText(log.getIpAddress())) meta.put("ipAddress", log.getIpAddress());
        if (StringUtils.hasText(log.getRequestId())) meta.put("requestId", log.getRequestId());
        if (StringUtils.hasText(log.getOldValue())) meta.put("oldValue", log.getOldValue());
        if (StringUtils.hasText(log.getNewValue())) meta.put("newValue", log.getNewValue());

        return ClientTimelineItemDto.builder()
                .id(log.getId())
                .clientId(clientId)
                .organizationId(log.getOrganizationId())
                .eventType(log.getAction())
                .eventCategory(category)
                .title(formatTitle(log.getAction()))
                .description(formatDescription(log))
                .occurredAt(log.getCreatedAt())
                .actorId(log.getUserId())
                .actorName(actorName)
                .sourceModule(resolveSourceModule(category))
                .severity(severity)
                .relatedEntityId(log.getEntityId())
                .relatedEntityType(log.getEntityType())
                .metadata(meta)
                .build();
    }

    public static TimelineEventCategory resolveCategory(String action, String entityType) {
        if (action == null && entityType == null) return TimelineEventCategory.SYSTEM;
        String act = action != null ? action.toUpperCase() : "";
        String ent = entityType != null ? entityType.toUpperCase() : "";

        if (act.contains("PROFILE") || ent.contains("PROFILE")) return TimelineEventCategory.PROFILE;
        if (act.contains("SERVICE") || ent.contains("SERVICE")) return TimelineEventCategory.SERVICE;
        if (act.contains("CONTACT") || ent.contains("CONTACT")) return TimelineEventCategory.CONTACT;
        if (act.contains("BRANCH") || ent.contains("BRANCH") || act.contains("LOCATION") || ent.contains("LOCATION")) return TimelineEventCategory.BRANCH;
        if (act.contains("RELATIONSHIP") || ent.contains("RELATIONSHIP") || act.contains("GROUP")) return TimelineEventCategory.RELATIONSHIP;
        if (act.contains("ENGAGEMENT") || ent.contains("ENGAGEMENT")) return TimelineEventCategory.ENGAGEMENT;
        if (act.contains("NOTE") || act.contains("COMMUNICATION") || ent.contains("COMMUNICATION") || ent.contains("NOTE")) return TimelineEventCategory.COMMUNICATION;
        if (act.contains("DOC") || ent.contains("DOC")) return TimelineEventCategory.DOCUMENT;
        if (act.contains("BILL") || act.contains("INVOICE") || ent.contains("INVOICE") || ent.contains("BILLING")) return TimelineEventCategory.BILLING;
        if (act.contains("PAYMENT") || ent.contains("PAYMENT")) return TimelineEventCategory.PAYMENT;
        if (act.contains("NOTICE") || act.contains("FILING") || act.contains("RETURN") || act.contains("COMPLIANCE") || act.contains("GST") || act.contains("ITR") || act.contains("TDS")) return TimelineEventCategory.COMPLIANCE;
        if (act.contains("CLIENT") || ent.contains("CLIENT")) return TimelineEventCategory.CLIENT;
        return TimelineEventCategory.SYSTEM;
    }

    public static TimelineEventSeverity resolveSeverity(String action, String newValue) {
        if (action == null) return TimelineEventSeverity.INFO;
        String act = action.toUpperCase();
        if (act.contains("ERROR") || act.contains("FAILED") || act.contains("SUSPEND") || act.contains("TERMINAT") || act.contains("CANCEL") || act.contains("DELETE") || act.contains("REMOVED")) {
            return TimelineEventSeverity.WARNING;
        }
        if (act.contains("CREATE") || act.contains("ACTIVAT") || act.contains("APPROV") || act.contains("FIL") || act.contains("PAID") || act.contains("RESUM") || act.contains("ASSIGNED") || act.contains("PRIMARY")) {
            return TimelineEventSeverity.SUCCESS;
        }
        return TimelineEventSeverity.INFO;
    }

    public static String resolveSourceModule(TimelineEventCategory category) {
        if (category == null) return "CLIENT";
        switch (category) {
            case SERVICE:
            case ENGAGEMENT:
                return "CLIENT_SERVICE";
            case CONTACT:
            case BRANCH:
            case RELATIONSHIP:
            case PROFILE:
            case CLIENT:
                return "CLIENT";
            case COMMUNICATION:
                return "CLIENT_COMMUNICATION";
            case DOCUMENT:
                return "DOCUMENT";
            case BILLING:
            case PAYMENT:
                return "BILLING";
            case COMPLIANCE:
            case GOVERNMENT:
                return "COMPLIANCE";
            default:
                return "SYSTEM";
        }
    }

    public static String formatTitle(String action) {
        if (!StringUtils.hasText(action)) return "Activity Event";
        String[] parts = action.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    public static String formatDescription(AuditLogEntity log) {
        if (StringUtils.hasText(log.getNewValue())) {
            return log.getNewValue();
        }
        if (StringUtils.hasText(log.getOldValue())) {
            return "Previous value: " + log.getOldValue();
        }
        return "Action " + formatTitle(log.getAction()) + " executed on " + (log.getEntityType() != null ? log.getEntityType().toLowerCase() : "client record");
    }
}
