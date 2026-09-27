package com.taxoryn.module.engagement.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.client.repository.ClientServiceRepository;
import com.taxoryn.module.engagement.dto.CreateEngagementRequest;
import com.taxoryn.module.engagement.dto.EngagementDto;
import com.taxoryn.module.engagement.dto.EngagementFilterRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementStatusRequest;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EngagementServiceImpl implements EngagementService {

    private final EngagementRepository engagementRepository;
    private final ClientRepository clientRepository;
    private final ClientServiceRepository clientServiceRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final AuditService auditService;

    @Override
    @Transactional
    public EngagementDto createEngagement(CreateEngagementRequest request) {
        UUID organizationId = resolveOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        UUID locationId = request.getLocationId() != null ? request.getLocationId() : client.getLocationId();

        validateAccess(client.getId(), locationId);

        if (request.getClientServiceId() != null) {
            clientServiceRepository.findByIdAndOrganizationId(request.getClientServiceId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("ClientService", "id", request.getClientServiceId()));
        }

        String engagementCode = StringUtils.hasText(request.getEngagementCode())
                ? request.getEngagementCode().trim()
                : "ENG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        EngagementEntity engagement = EngagementEntity.builder()
                .locationId(locationId)
                .clientId(client.getId())
                .clientServiceId(request.getClientServiceId())
                .engagementCode(engagementCode)
                .name(request.getName().trim())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : EngagementStatus.ACTIVE)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .assignedUserId(request.getAssignedUserId())
                .notes(request.getNotes())
                .build();
        engagement.setOrganizationId(organizationId);

        engagement = engagementRepository.save(engagement);
        log.info("Created Engagement: id={}, name={} for tenant={}", engagement.getId(), engagement.getName(), organizationId);

        auditService.logEvent("ENGAGEMENT_CREATED", "ENGAGEMENT", engagement.getId().toString(), null, engagement);

        return enrichDto(engagement);
    }

    @Override
    @Transactional(readOnly = true)
    public EngagementDto getEngagementById(UUID id) {
        UUID organizationId = resolveOrganizationId();
        EngagementEntity engagement = engagementRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", id));

        validateAccess(engagement.getClientId(), engagement.getLocationId());

        return enrichDto(engagement);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EngagementDto> getEngagements(EngagementFilterRequest filterRequest) {
        UUID organizationId = resolveOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        Specification<EngagementEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (!scope.isFirmAdmin()) {
                Set<UUID> accessibleLocs = scope.getAccessibleLocationIds();
                if (accessibleLocs != null && !accessibleLocs.isEmpty()) {
                    predicates.add(root.get("locationId").in(accessibleLocs));
                }
                Set<UUID> accessibleClients = securityScopeEvaluator.getAccessibleClientIds(scope);
                if (accessibleClients != null) {
                    predicates.add(root.get("clientId").in(accessibleClients));
                }
            }

            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filterRequest.getClientId()));
            }
            if (filterRequest.getLocationId() != null) {
                predicates.add(cb.equal(root.get("locationId"), filterRequest.getLocationId()));
            }
            if (filterRequest.getClientServiceId() != null) {
                predicates.add(cb.equal(root.get("clientServiceId"), filterRequest.getClientServiceId()));
            }
            if (filterRequest.getAssignedUserId() != null) {
                predicates.add(cb.equal(root.get("assignedUserId"), filterRequest.getAssignedUserId()));
            }
            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }
            if (StringUtils.hasText(filterRequest.getSearch())) {
                String pattern = "%" + filterRequest.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("engagementCode")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.fromString(filterRequest.getSortDirection()), filterRequest.getSortBy());
        Pageable pageable = PageRequest.of(filterRequest.getPage(), filterRequest.getSize(), sort);
        Page<EngagementEntity> page = engagementRepository.findAll(spec, pageable);

        List<EngagementDto> content = enrichDtoList(page.getContent());
        return PagedResponse.<EngagementDto>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EngagementDto> getEngagementsByClientId(UUID clientId) {
        UUID organizationId = resolveOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        List<EngagementEntity> list = engagementRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId);
        return enrichDtoList(list);
    }

    @Override
    @Transactional
    public EngagementDto updateEngagement(UUID id, UpdateEngagementRequest request) {
        UUID organizationId = resolveOrganizationId();
        EngagementEntity engagement = engagementRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", id));

        validateAccess(engagement.getClientId(), engagement.getLocationId());

        if (request.getLocationId() != null) {
            engagement.setLocationId(request.getLocationId());
        }
        if (request.getClientServiceId() != null) {
            engagement.setClientServiceId(request.getClientServiceId());
        }
        if (StringUtils.hasText(request.getEngagementCode())) {
            engagement.setEngagementCode(request.getEngagementCode().trim());
        }
        if (StringUtils.hasText(request.getName())) {
            engagement.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            engagement.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            engagement.setStatus(request.getStatus());
        }
        if (request.getStartDate() != null) {
            engagement.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            engagement.setEndDate(request.getEndDate());
        }
        if (request.getAssignedUserId() != null) {
            engagement.setAssignedUserId(request.getAssignedUserId());
        }
        if (request.getNotes() != null) {
            engagement.setNotes(request.getNotes());
        }

        engagement = engagementRepository.save(engagement);
        log.info("Updated Engagement: id={}", engagement.getId());

        auditService.logEvent("ENGAGEMENT_UPDATED", "ENGAGEMENT", engagement.getId().toString(), null, engagement);

        return enrichDto(engagement);
    }

    @Override
    @Transactional
    public EngagementDto updateEngagementStatus(UUID id, UpdateEngagementStatusRequest request) {
        UUID organizationId = resolveOrganizationId();
        EngagementEntity engagement = engagementRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", id));

        validateAccess(engagement.getClientId(), engagement.getLocationId());

        engagement.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            engagement.setNotes(request.getNotes());
        }

        engagement = engagementRepository.save(engagement);
        log.info("Updated status for Engagement: id={}, status={}", engagement.getId(), engagement.getStatus());

        auditService.logEvent("ENGAGEMENT_STATUS_UPDATED", "ENGAGEMENT", engagement.getId().toString(), null, engagement);

        return enrichDto(engagement);
    }

    @Override
    @Transactional
    public void deleteEngagement(UUID id) {
        UUID organizationId = resolveOrganizationId();
        EngagementEntity engagement = engagementRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", id));

        validateAccess(engagement.getClientId(), engagement.getLocationId());

        engagementRepository.delete(engagement);
        log.info("Deleted Engagement: id={}", id);

        auditService.logEvent("ENGAGEMENT_DELETED", "ENGAGEMENT", id.toString(), null, null);
    }

    // --- Helper Methods ---

    private UUID resolveOrganizationId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return SecurityUtils.getCurrentOrganizationId();
    }

    private void validateAccess(UUID clientId, UUID locationId) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        if (scope.isFirmAdmin()) {
            return;
        }

        if (locationId != null) {
            Set<UUID> accessibleLocs = scope.getAccessibleLocationIds();
            if (accessibleLocs != null && !accessibleLocs.isEmpty() && !accessibleLocs.contains(locationId)) {
                throw new AccessDeniedException("Access denied: You do not have permission for this location");
            }
        }

        if (clientId != null) {
            Set<UUID> accessibleClients = securityScopeEvaluator.getAccessibleClientIds(scope);
            if (accessibleClients != null && !accessibleClients.contains(clientId)) {
                throw new AccessDeniedException("Access denied: You do not have permission for this client");
            }
        }
    }

    private EngagementDto enrichDto(EngagementEntity entity) {
        UUID orgId = entity.getOrganizationId();
        String clientName = null;
        if (entity.getClientId() != null) {
            clientName = clientRepository.findByIdAndOrganizationId(entity.getClientId(), orgId)
                    .map(ClientEntity::getDisplayName)
                    .orElse(null);
        }

        String locationName = null;
        if (entity.getLocationId() != null) {
            locationName = locationRepository.findByIdAndOrganizationId(entity.getLocationId(), orgId)
                    .map(LocationEntity::getName)
                    .orElse(null);
        }

        String assignedUserName = null;
        if (entity.getAssignedUserId() != null) {
            assignedUserName = userRepository.findByIdAndOrganizationId(entity.getAssignedUserId(), orgId)
                    .map(UserEntity::getFullName)
                    .orElse(null);
        }

        return EngagementDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .locationId(entity.getLocationId())
                .locationName(locationName)
                .clientId(entity.getClientId())
                .clientName(clientName)
                .clientServiceId(entity.getClientServiceId())
                .engagementCode(entity.getEngagementCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .assignedUserId(entity.getAssignedUserId())
                .assignedUserName(assignedUserName)
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    private List<EngagementDto> enrichDtoList(List<EngagementEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        UUID orgId = entities.get(0).getOrganizationId();
        Map<UUID, String> clientNames = clientRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(ClientEntity::getId, ClientEntity::getDisplayName, (a, b) -> a));
        Map<UUID, String> locationNames = locationRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(LocationEntity::getId, LocationEntity::getName, (a, b) -> a));
        Map<UUID, String> userNames = userRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName, (a, b) -> a));

        return entities.stream().map(e -> EngagementDto.builder()
                .id(e.getId())
                .organizationId(e.getOrganizationId())
                .locationId(e.getLocationId())
                .locationName(e.getLocationId() != null ? locationNames.get(e.getLocationId()) : null)
                .clientId(e.getClientId())
                .clientName(e.getClientId() != null ? clientNames.get(e.getClientId()) : null)
                .clientServiceId(e.getClientServiceId())
                .engagementCode(e.getEngagementCode())
                .name(e.getName())
                .description(e.getDescription())
                .status(e.getStatus())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .assignedUserId(e.getAssignedUserId())
                .assignedUserName(e.getAssignedUserId() != null ? userNames.get(e.getAssignedUserId()) : null)
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build()).collect(Collectors.toList());
    }
}
