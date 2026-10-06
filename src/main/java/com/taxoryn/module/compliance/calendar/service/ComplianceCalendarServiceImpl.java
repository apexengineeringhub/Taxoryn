package com.taxoryn.module.compliance.calendar.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.TenantAccessDeniedException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.calendar.dto.ClientComplianceDeadlineSummaryDto;
import com.taxoryn.module.compliance.calendar.dto.ComplianceCalendarQueryFilter;
import com.taxoryn.module.compliance.calendar.dto.ComplianceDeadlineDto;
import com.taxoryn.module.compliance.calendar.dto.ComplianceDeadlineRadarDto;
import com.taxoryn.module.compliance.calendar.dto.ComplianceDeadlineSummaryDto;
import com.taxoryn.module.compliance.calendar.model.DeadlineStatus;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of Compliance Calendar and Deadline Radar service.
 * Pure query/read layer over ComplianceObligationEntity with zero write side-effects.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplianceCalendarServiceImpl implements ComplianceCalendarService {

    private final ComplianceObligationRepository obligationRepository;
    private final ClientRepository clientRepository;
    private final ComplianceDeadlineClassifier classifier;
    private Clock clock = Clock.systemDefaultZone();

    /**
     * Allows setting a custom clock for testing.
     */
    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    @Override
    public PagedResponse<ComplianceDeadlineDto> getCalendar(ComplianceCalendarQueryFilter filter) {
        UUID orgId = requireTenantId();
        ComplianceCalendarQueryFilter safeFilter = filter != null ? filter : new ComplianceCalendarQueryFilter();
        validateDateRange(safeFilter.getFrom(), safeFilter.getTo());

        LocalDate refDate = safeFilter.getReferenceDate() != null
                ? safeFilter.getReferenceDate()
                : LocalDate.now(clock);

        List<ComplianceObligationEntity> obligations = obligationRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("organizationId"), orgId));

            if (safeFilter.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), safeFilter.getClientId()));
            }
            if (safeFilter.getDomain() != null) {
                predicates.add(cb.equal(root.get("domain"), safeFilter.getDomain()));
            }
            if (safeFilter.getRuleCode() != null && !safeFilter.getRuleCode().isBlank()) {
                predicates.add(cb.equal(root.get("ruleCode"), safeFilter.getRuleCode().trim()));
            }
            if (safeFilter.getObligationStatus() != null) {
                predicates.add(cb.equal(root.get("status"), safeFilter.getObligationStatus()));
            } else if (!Boolean.TRUE.equals(safeFilter.getIncludeTerminal())) {
                predicates.add(root.get("status").in(
                        ComplianceObligationStatus.UPCOMING,
                        ComplianceObligationStatus.READY,
                        ComplianceObligationStatus.IN_PROGRESS,
                        ComplianceObligationStatus.WAITING_FOR_CLIENT,
                        ComplianceObligationStatus.READY_FOR_FILING,
                        ComplianceObligationStatus.FILED,
                        ComplianceObligationStatus.OVERDUE
                ));
            }

            // Date Range filtering (statutoryDueDate prioritized over dueDate)
            if (safeFilter.getFrom() != null) {
                predicates.add(cb.or(
                        cb.greaterThanOrEqualTo(root.get("statutoryDueDate"), safeFilter.getFrom()),
                        cb.and(cb.isNull(root.get("statutoryDueDate")), cb.greaterThanOrEqualTo(root.get("dueDate"), safeFilter.getFrom()))
                ));
            }
            if (safeFilter.getTo() != null) {
                predicates.add(cb.or(
                        cb.lessThanOrEqualTo(root.get("statutoryDueDate"), safeFilter.getTo()),
                        cb.and(cb.isNull(root.get("statutoryDueDate")), cb.lessThanOrEqualTo(root.get("dueDate"), safeFilter.getTo()))
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });

        // Batch load client details to avoid N+1 queries
        Map<UUID, ClientEntity> clientMap = loadClientMap(orgId, obligations);

        // Map and classify
        List<ComplianceDeadlineDto> classified = obligations.stream()
                .map(ob -> mapToDeadlineDto(ob, clientMap.get(ob.getClientId()), refDate))
                .filter(dto -> {
                    if (safeFilter.getDeadlineStatus() != null) {
                        return dto.getDeadlineStatus() == safeFilter.getDeadlineStatus();
                    }
                    return true;
                })
                .filter(dto -> {
                    if (safeFilter.getSearch() != null && !safeFilter.getSearch().isBlank()) {
                        String q = safeFilter.getSearch().toLowerCase().trim();
                        return (dto.getClientDisplayName() != null && dto.getClientDisplayName().toLowerCase().contains(q))
                                || (dto.getRuleCode() != null && dto.getRuleCode().toLowerCase().contains(q))
                                || (dto.getRuleName() != null && dto.getRuleName().toLowerCase().contains(q))
                                || (dto.getPeriodLabel() != null && dto.getPeriodLabel().toLowerCase().contains(q))
                                || (dto.getClientPan() != null && dto.getClientPan().toLowerCase().contains(q));
                    }
                    return true;
                })
                .sorted(buildDeadlineComparator())
                .toList();

        int page = Math.max(0, safeFilter.getPage());
        int size = safeFilter.getSize() > 0 ? Math.min(100, safeFilter.getSize()) : 20;
        int totalElements = classified.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<ComplianceDeadlineDto> pagedItems = classified.subList(fromIndex, toIndex);

        return PagedResponse.<ComplianceDeadlineDto>builder()
                .content(pagedItems)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .isFirst(page == 0)
                .isLast(page >= totalPages - 1 || totalPages == 0)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }

    @Override
    public ComplianceDeadlineRadarDto getPracticeDeadlineRadar(LocalDate referenceDate) {
        UUID orgId = requireTenantId();
        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now(clock);

        List<ComplianceObligationEntity> allOrgObligations = obligationRepository.findAll((root, query, cb) ->
                cb.equal(root.get("organizationId"), orgId)
        );

        Map<UUID, ClientEntity> clientMap = loadClientMap(orgId, allOrgObligations);

        List<ComplianceDeadlineDto> allDeadlines = allOrgObligations.stream()
                .map(ob -> mapToDeadlineDto(ob, clientMap.get(ob.getClientId()), refDate))
                .toList();

        ComplianceDeadlineSummaryDto overallSummary = computeSummary(allDeadlines);

        // Domain Breakdown
        Map<ComplianceRuleDomain, List<ComplianceDeadlineDto>> byDomainMap = allDeadlines.stream()
                .collect(Collectors.groupingBy(d -> d.getDomain() != null ? d.getDomain() : ComplianceRuleDomain.OTHER));

        Map<ComplianceRuleDomain, ComplianceDeadlineSummaryDto> domainBreakdown = new EnumMap<>(ComplianceRuleDomain.class);
        for (ComplianceRuleDomain domain : ComplianceRuleDomain.values()) {
            List<ComplianceDeadlineDto> domainItems = byDomainMap.getOrDefault(domain, Collections.emptyList());
            domainBreakdown.put(domain, computeSummary(domainItems));
        }

        // Top Deadlines for Radar (Active items only, sorted by urgency)
        List<ComplianceDeadlineDto> topDeadlines = allDeadlines.stream()
                .filter(d -> d.getDeadlineStatus() != DeadlineStatus.COMPLETED && d.getDeadlineStatus() != DeadlineStatus.CANCELLED)
                .sorted(buildDeadlineComparator())
                .limit(15)
                .toList();

        return ComplianceDeadlineRadarDto.builder()
                .asOfDate(refDate)
                .summary(overallSummary)
                .domainBreakdown(domainBreakdown)
                .topDeadlines(topDeadlines)
                .build();
    }

    @Override
    public ComplianceDeadlineSummaryDto getDeadlineSummary(LocalDate referenceDate) {
        UUID orgId = requireTenantId();
        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now(clock);

        List<ComplianceObligationEntity> allOrgObligations = obligationRepository.findAll((root, query, cb) ->
                cb.equal(root.get("organizationId"), orgId)
        );

        List<ComplianceDeadlineDto> allDeadlines = allOrgObligations.stream()
                .map(ob -> mapToDeadlineDto(ob, null, refDate))
                .toList();

        return computeSummary(allDeadlines);
    }

    @Override
    public ClientComplianceDeadlineSummaryDto getClientDeadlineSummary(UUID clientId, LocalDate referenceDate) {
        UUID orgId = requireTenantId();
        if (clientId == null) {
            throw new BadRequestException("Client ID is required");
        }

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));

        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now(clock);

        List<ComplianceObligationEntity> clientObligations = obligationRepository.findAll((root, query, cb) ->
                cb.and(
                        cb.equal(root.get("organizationId"), orgId),
                        cb.equal(root.get("clientId"), clientId)
                )
        );

        List<ComplianceDeadlineDto> deadlines = clientObligations.stream()
                .map(ob -> mapToDeadlineDto(ob, client, refDate))
                .toList();

        ComplianceDeadlineSummaryDto summary = computeSummary(deadlines);

        // Next closest active deadline (due today or in future, or closest overdue if none future)
        ComplianceDeadlineDto nextDeadline = deadlines.stream()
                .filter(d -> d.getDeadlineStatus() != DeadlineStatus.COMPLETED && d.getDeadlineStatus() != DeadlineStatus.CANCELLED)
                .filter(d -> d.getStatutoryDueDate() != null || d.getDueDate() != null)
                .min(buildDeadlineComparator())
                .orElse(null);

        List<ComplianceDeadlineDto> upcomingDeadlines = deadlines.stream()
                .filter(d -> d.getDeadlineStatus() != DeadlineStatus.COMPLETED && d.getDeadlineStatus() != DeadlineStatus.CANCELLED)
                .sorted(buildDeadlineComparator())
                .limit(5)
                .toList();

        return ClientComplianceDeadlineSummaryDto.builder()
                .clientId(clientId)
                .clientDisplayName(client.getDisplayName() != null ? client.getDisplayName() : client.getLegalName())
                .summary(summary)
                .nextDeadline(nextDeadline)
                .upcomingDeadlines(upcomingDeadlines)
                .build();
    }

    @Override
    public PagedResponse<ComplianceDeadlineDto> getClientCalendar(UUID clientId, ComplianceCalendarQueryFilter filter) {
        if (clientId == null) {
            throw new BadRequestException("Client ID is required");
        }
        ComplianceCalendarQueryFilter clientFilter = filter != null ? filter : new ComplianceCalendarQueryFilter();
        clientFilter.setClientId(clientId);
        return getCalendar(clientFilter);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private UUID requireTenantId() {
        UUID orgId = TenantContext.getTenantId();
        if (orgId == null) {
            throw new TenantAccessDeniedException("Tenant context is required for compliance calendar operations");
        }
        return orgId;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            if (to.isBefore(from)) {
                throw new BadRequestException("Date range 'to' cannot be earlier than 'from'");
            }
            long days = ChronoUnit.DAYS.between(from, to);
            if (days > 366) {
                throw new BadRequestException("Date range cannot exceed 366 days (requested span: " + days + " days)");
            }
        }
    }

    private Map<UUID, ClientEntity> loadClientMap(UUID orgId, List<ComplianceObligationEntity> obligations) {
        Set<UUID> clientIds = obligations.stream()
                .map(ComplianceObligationEntity::getClientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (clientIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return clientRepository.findAllByOrganizationIdAndIdIn(orgId, clientIds).stream()
                .collect(Collectors.toMap(ClientEntity::getId, Function.identity(), (a, b) -> a));
    }

    private ComplianceDeadlineDto mapToDeadlineDto(ComplianceObligationEntity ob, ClientEntity client, LocalDate refDate) {
        var classification = classifier.classify(ob, refDate);

        LocalDate statDueDate = ob.getStatutoryDueDate();
        LocalDate effDueDate = statDueDate != null ? statDueDate : ob.getDueDate();

        String clientDisplayName = null;
        String clientLegalName = null;
        String clientPan = null;

        if (client != null) {
            clientDisplayName = client.getDisplayName();
            clientLegalName = client.getLegalName();
            clientPan = client.getPan();
        }

        return ComplianceDeadlineDto.builder()
                .obligationId(ob.getId())
                .clientId(ob.getClientId())
                .clientName(clientLegalName != null ? clientLegalName : clientDisplayName)
                .clientDisplayName(clientDisplayName != null ? clientDisplayName : clientLegalName)
                .clientPan(clientPan)
                .ruleCode(ob.getRuleCode())
                .ruleVersion(ob.getRuleVersion())
                .ruleName(ob.getRuleNameSnapshot() != null ? ob.getRuleNameSnapshot() : ob.getTitle())
                .domain(ob.getDomain())
                .periodType(ob.getPeriodType())
                .periodKey(ob.getPeriodKey())
                .periodLabel(ob.getPeriodLabel())
                .statutoryDueDate(statDueDate)
                .dueDate(effDueDate)
                .obligationStatus(ob.getStatus())
                .deadlineStatus(classification.status())
                .daysRemaining(classification.daysRemaining())
                .daysOverdue(classification.daysOverdue())
                .priority(ob.getPriority() != null ? ob.getPriority() : TaskPriority.MEDIUM)
                .dueDateCalculationStatus(ob.getDueDateCalculationStatus())
                .dueDateExplanation(ob.getDueDateExplanation())
                .build();
    }

    private ComplianceDeadlineSummaryDto computeSummary(List<ComplianceDeadlineDto> deadlines) {
        long overdue = 0;
        long dueToday = 0;
        long dueTomorrow = 0;
        long dueWithin3Days = 0;
        long dueThisWeek = 0;
        long upcoming = 0;
        long noDueDate = 0;
        long completed = 0;
        long cancelled = 0;

        for (ComplianceDeadlineDto d : deadlines) {
            switch (d.getDeadlineStatus()) {
                case OVERDUE -> overdue++;
                case DUE_TODAY -> dueToday++;
                case DUE_TOMORROW -> dueTomorrow++;
                case DUE_WITHIN_3_DAYS -> dueWithin3Days++;
                case DUE_THIS_WEEK -> dueThisWeek++;
                case UPCOMING -> upcoming++;
                case NO_DUE_DATE -> noDueDate++;
                case COMPLETED -> completed++;
                case CANCELLED -> cancelled++;
            }
        }

        long totalActive = overdue + dueToday + dueTomorrow + dueWithin3Days + dueThisWeek + upcoming + noDueDate;

        return ComplianceDeadlineSummaryDto.builder()
                .overdue(overdue)
                .dueToday(dueToday)
                .dueTomorrow(dueTomorrow)
                .dueWithin3Days(dueWithin3Days)
                .dueThisWeek(dueThisWeek)
                .upcoming(upcoming)
                .noDueDate(noDueDate)
                .totalActive(totalActive)
                .completed(completed)
                .cancelled(cancelled)
                .build();
    }

    private Comparator<ComplianceDeadlineDto> buildDeadlineComparator() {
        return (a, b) -> {
            // 1. Deadline bucket priority
            int rankA = getDeadlineRank(a.getDeadlineStatus());
            int rankB = getDeadlineRank(b.getDeadlineStatus());
            if (rankA != rankB) {
                return Integer.compare(rankA, rankB);
            }

            // 2. Due Date ascending (nulls last)
            LocalDate dateA = a.getStatutoryDueDate() != null ? a.getStatutoryDueDate() : a.getDueDate();
            LocalDate dateB = b.getStatutoryDueDate() != null ? b.getStatutoryDueDate() : b.getDueDate();
            if (dateA != null && dateB != null && !dateA.isEqual(dateB)) {
                return dateA.compareTo(dateB);
            }
            if (dateA == null && dateB != null) return 1;
            if (dateA != null && dateB == null) return -1;

            // 3. Task Priority descending (CRITICAL > HIGH > MEDIUM > LOW)
            int prioA = getPriorityRank(a.getPriority());
            int prioB = getPriorityRank(b.getPriority());
            if (prioA != prioB) {
                return Integer.compare(prioB, prioA); // higher rank first
            }

            // 4. Client display name ascending
            String nameA = a.getClientDisplayName() != null ? a.getClientDisplayName() : "";
            String nameB = b.getClientDisplayName() != null ? b.getClientDisplayName() : "";
            int nameComp = nameA.compareToIgnoreCase(nameB);
            if (nameComp != 0) {
                return nameComp;
            }

            // 5. Rule Code ascending
            String ruleA = a.getRuleCode() != null ? a.getRuleCode() : "";
            String ruleB = b.getRuleCode() != null ? b.getRuleCode() : "";
            int ruleComp = ruleA.compareToIgnoreCase(ruleB);
            if (ruleComp != 0) {
                return ruleComp;
            }

            // 6. Obligation ID deterministic tie-breaker
            if (a.getObligationId() != null && b.getObligationId() != null) {
                return a.getObligationId().compareTo(b.getObligationId());
            }

            return 0;
        };
    }

    private int getDeadlineRank(DeadlineStatus status) {
        if (status == null) return 10;
        return switch (status) {
            case OVERDUE -> 1;
            case DUE_TODAY -> 2;
            case DUE_TOMORROW -> 3;
            case DUE_WITHIN_3_DAYS -> 4;
            case DUE_THIS_WEEK -> 5;
            case UPCOMING -> 6;
            case NO_DUE_DATE -> 7;
            case COMPLETED -> 8;
            case CANCELLED -> 9;
        };
    }

    private int getPriorityRank(TaskPriority priority) {
        if (priority == null) return 1;
        return switch (priority) {
            case URGENT -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
}
