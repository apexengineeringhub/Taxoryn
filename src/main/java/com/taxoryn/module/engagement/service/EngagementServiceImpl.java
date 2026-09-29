package com.taxoryn.module.engagement.service;

import com.taxoryn.core.exception.BusinessValidationException;
import com.taxoryn.core.exception.DuplicateResourceException;
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
import com.taxoryn.module.engagement.dto.UpdateEngagementAssignmentRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementRequest;
import com.taxoryn.module.engagement.dto.UpdateEngagementStatusRequest;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.model.EngagementPriority;
import com.taxoryn.module.engagement.model.EngagementStatus;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.repository.LocationRepository;
import com.taxoryn.module.service.entity.ServiceEntity;
import com.taxoryn.module.service.repository.ServiceRepository;
import com.taxoryn.module.service.service.ServiceCatalogService;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EngagementServiceImpl implements EngagementService {

    private final EngagementRepository engagementRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final ClientServiceRepository clientServiceRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final AuditService auditService;

    @Override
    @Transactional
    public EngagementDto createEngagement(CreateEngagementRequest request) {
        UUID organizationId = resolveOrganizationId();

        // 1. Client Ownership & Existence Validation
        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        UUID locationId = request.getLocationId() != null ? request.getLocationId() : client.getLocationId();
        validateAccess(client.getId(), locationId);

        // 2. Service Entitlement & Availability Check
        ServiceEntity service = null;
        if (request.getServiceId() != null) {
            service = serviceRepository.findAccessibleServiceById(request.getServiceId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.getServiceId()));

            if (!serviceCatalogService.isServiceAvailableForPractice(organizationId, service)) {
                throw new BusinessValidationException(
                        "SERVICE_NOT_AVAILABLE: The requested service '" + service.getServiceName() +
                                "' is not enabled or entitled under your practice's subscription plan."
                );
            }
        }

        // 3. User Assignment Validation (Must belong to same organization)
        if (request.getAssignedUserId() != null) {
            userRepository.findByIdAndOrganizationId(request.getAssignedUserId(), organizationId)
                    .orElseThrow(() -> new BusinessValidationException("Assigned preparer/owner does not belong to this organization."));
        }
        if (request.getReviewerUserId() != null) {
            userRepository.findByIdAndOrganizationId(request.getReviewerUserId(), organizationId)
                    .orElseThrow(() -> new BusinessValidationException("Reviewer user does not belong to this organization."));
        }

        // 4. Date Range Validation
        if (request.getStartDate() != null && request.getEndDate() != null && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessValidationException("Engagement start date cannot be after end date.");
        }

        // 5. Backend Generated Unique Engagement Code
        String engagementCode;
        if (StringUtils.hasText(request.getEngagementCode())) {
            engagementCode = request.getEngagementCode().trim().toUpperCase();
            if (engagementRepository.existsByOrganizationIdAndEngagementCode(organizationId, engagementCode)) {
                throw new DuplicateResourceException("Engagement", "engagementCode", engagementCode);
            }
        } else {
            engagementCode = generateEngagementCode(organizationId);
        }

        EngagementStatus status = request.getStatus() != null ? request.getStatus() : EngagementStatus.ACTIVE;
        EngagementPriority priority = request.getPriority() != null ? request.getPriority() : EngagementPriority.MEDIUM;

        EngagementEntity engagement = EngagementEntity.builder()
                .locationId(locationId)
                .clientId(client.getId())
                .serviceId(service != null ? service.getId() : null)
                .clientServiceId(request.getClientServiceId())
                .engagementCode(engagementCode)
                .name(request.getName().trim())
                .description(request.getDescription())
                .status(status)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .assignedUserId(request.getAssignedUserId())
                .reviewerUserId(request.getReviewerUserId())
                .priority(priority)
                .notes(request.getNotes())
                .build();
        engagement.setOrganizationId(organizationId);

        engagement = engagementRepository.save(engagement);
        log.info("Created Engagement: id={}, code={}, name={} for tenant={}",
                engagement.getId(), engagement.getEngagementCode(), engagement.getName(), organizationId);

        // Audit Logging
        auditService.logEvent("ENGAGEMENT_CREATED", "ENGAGEMENT", engagement.getId().toString(), null, engagement);
        if (engagement.getStatus() == EngagementStatus.ACTIVE) {
            auditService.logEvent("ENGAGEMENT_ACTIVATED", "ENGAGEMENT", engagement.getId().toString(), null, engagement);
        }
        if (engagement.getAssignedUserId() != null) {
            auditService.logEvent("ENGAGEMENT_ASSIGNED", "ENGAGEMENT", engagement.getId().toString(), null, engagement);
        }

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
            if (filterRequest.getServiceId() != null) {
                predicates.add(cb.equal(root.get("serviceId"), filterRequest.getServiceId()));
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
            if (filterRequest.getReviewerUserId() != null) {
                predicates.add(cb.equal(root.get("reviewerUserId"), filterRequest.getReviewerUserId()));
            }
            if (filterRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filterRequest.getStatus()));
            }
            if (filterRequest.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filterRequest.getPriority()));
            }
            if (filterRequest.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), filterRequest.getStartDate()));
            }
            if (filterRequest.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), filterRequest.getEndDate()));
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

        if (request.getServiceId() != null) {
            ServiceEntity service = serviceRepository.findAccessibleServiceById(request.getServiceId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.getServiceId()));

            if (!serviceCatalogService.isServiceAvailableForPractice(organizationId, service)) {
                throw new BusinessValidationException(
                        "SERVICE_NOT_AVAILABLE: The requested service '" + service.getServiceName() +
                                "' is not enabled or entitled under your practice's subscription plan."
                );
            }
            engagement.setServiceId(service.getId());
        }

        if (request.getLocationId() != null) {
            engagement.setLocationId(request.getLocationId());
        }
        if (request.getClientServiceId() != null) {
            engagement.setClientServiceId(request.getClientServiceId());
        }
        if (StringUtils.hasText(request.getEngagementCode())) {
            String newCode = request.getEngagementCode().trim().toUpperCase();
            if (!newCode.equalsIgnoreCase(engagement.getEngagementCode())
                    && engagementRepository.existsByOrganizationIdAndEngagementCode(organizationId, newCode)) {
                throw new DuplicateResourceException("Engagement", "engagementCode", newCode);
            }
            engagement.setEngagementCode(newCode);
        }
        if (StringUtils.hasText(request.getName())) {
            engagement.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            engagement.setDescription(request.getDescription());
        }
        if (request.getStatus() != null && request.getStatus() != engagement.getStatus()) {
            engagement.getStatus().validateTransition(request.getStatus());
            engagement.setStatus(request.getStatus());
        }
        if (request.getStartDate() != null) {
            engagement.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            engagement.setEndDate(request.getEndDate());
        }
        if (engagement.getStartDate() != null && engagement.getEndDate() != null
                && engagement.getStartDate().isAfter(engagement.getEndDate())) {
            throw new BusinessValidationException("Engagement start date cannot be after end date.");
        }

        if (request.getAssignedUserId() != null) {
            userRepository.findByIdAndOrganizationId(request.getAssignedUserId(), organizationId)
                    .orElseThrow(() -> new BusinessValidationException("Assigned preparer/owner does not belong to this organization."));
            engagement.setAssignedUserId(request.getAssignedUserId());
        }
        if (request.getReviewerUserId() != null) {
            userRepository.findByIdAndOrganizationId(request.getReviewerUserId(), organizationId)
                    .orElseThrow(() -> new BusinessValidationException("Reviewer user does not belong to this organization."));
            engagement.setReviewerUserId(request.getReviewerUserId());
        }
        if (request.getPriority() != null) {
            engagement.setPriority(request.getPriority());
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

        EngagementStatus previousStatus = engagement.getStatus();
        EngagementStatus targetStatus = request.getStatus();

        previousStatus.validateTransition(targetStatus);
        engagement.setStatus(targetStatus);

        if (request.getNotes() != null) {
            engagement.setNotes(request.getNotes());
        }

        engagement = engagementRepository.save(engagement);
        log.info("Updated status for Engagement: id={}, previous={}, new={}", engagement.getId(), previousStatus, targetStatus);

        // Audit Logging based on lifecycle event
        auditService.logEvent("ENGAGEMENT_STATUS_CHANGED", "ENGAGEMENT", engagement.getId().toString(), previousStatus, targetStatus);
        if (targetStatus == EngagementStatus.ACTIVE && previousStatus != EngagementStatus.ACTIVE) {
            auditService.logEvent("ENGAGEMENT_ACTIVATED", "ENGAGEMENT", engagement.getId().toString(), null, engagement);
        } else if (targetStatus == EngagementStatus.COMPLETED) {
            auditService.logEvent("ENGAGEMENT_COMPLETED", "ENGAGEMENT", engagement.getId().toString(), null, engagement);
        } else if (targetStatus == EngagementStatus.CANCELLED) {
            auditService.logEvent("ENGAGEMENT_CANCELLED", "ENGAGEMENT", engagement.getId().toString(), null, engagement);
        }

        return enrichDto(engagement);
    }

    @Override
    @Transactional
    public EngagementDto updateEngagementAssignment(UUID id, UpdateEngagementAssignmentRequest request) {
        UUID organizationId = resolveOrganizationId();
        EngagementEntity engagement = engagementRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", id));

        validateAccess(engagement.getClientId(), engagement.getLocationId());

        UUID previousAssignedUser = engagement.getAssignedUserId();
        UUID previousReviewer = engagement.getReviewerUserId();

        if (request.getAssignedUserId() != null) {
            userRepository.findByIdAndOrganizationId(request.getAssignedUserId(), organizationId)
                    .orElseThrow(() -> new BusinessValidationException("Assigned user does not belong to this organization."));
            engagement.setAssignedUserId(request.getAssignedUserId());
        }
        if (request.getReviewerUserId() != null) {
            userRepository.findByIdAndOrganizationId(request.getReviewerUserId(), organizationId)
                    .orElseThrow(() -> new BusinessValidationException("Reviewer user does not belong to this organization."));
            engagement.setReviewerUserId(request.getReviewerUserId());
        }
        if (request.getNotes() != null) {
            engagement.setNotes(request.getNotes());
        }

        engagement = engagementRepository.save(engagement);
        log.info("Updated assignment for Engagement: id={}, assignedUser={}, reviewerUser={}",
                engagement.getId(), engagement.getAssignedUserId(), engagement.getReviewerUserId());

        if (!Objects.equals(previousAssignedUser, engagement.getAssignedUserId())
                || !Objects.equals(previousReviewer, engagement.getReviewerUserId())) {
            String auditAction = previousAssignedUser == null ? "ENGAGEMENT_ASSIGNED" : "ENGAGEMENT_REASSIGNED";
            auditService.logEvent(auditAction, "ENGAGEMENT", engagement.getId().toString(), previousAssignedUser, engagement.getAssignedUserId());
        }

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

    private String generateEngagementCode(UUID organizationId) {
        int year = LocalDate.now().getYear();
        long count = engagementRepository.countByOrganizationId(organizationId) + 1;
        String code;
        do {
            code = String.format("ENG-%d-%06d", year, count++);
        } while (engagementRepository.existsByOrganizationIdAndEngagementCode(organizationId, code));
        return code;
    }

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
        String clientCode = null;
        if (entity.getClientId() != null) {
            ClientEntity client = clientRepository.findByIdAndOrganizationId(entity.getClientId(), orgId).orElse(null);
            if (client != null) {
                clientName = client.getDisplayName();
                clientCode = client.getClientCode();
            }
        }

        String serviceCode = null;
        String serviceName = null;
        com.taxoryn.module.service.model.ServiceCategory serviceCategory = null;
        if (entity.getServiceId() != null) {
            ServiceEntity s = serviceRepository.findAccessibleServiceById(entity.getServiceId(), orgId).orElse(null);
            if (s != null) {
                serviceCode = s.getServiceCode();
                serviceName = s.getServiceName();
                serviceCategory = s.getCategory();
            }
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

        String reviewerUserName = null;
        if (entity.getReviewerUserId() != null) {
            reviewerUserName = userRepository.findByIdAndOrganizationId(entity.getReviewerUserId(), orgId)
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
                .clientCode(clientCode)
                .serviceId(entity.getServiceId())
                .serviceCode(serviceCode)
                .serviceName(serviceName)
                .serviceCategory(serviceCategory)
                .clientServiceId(entity.getClientServiceId())
                .engagementCode(entity.getEngagementCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .assignedUserId(entity.getAssignedUserId())
                .assignedUserName(assignedUserName)
                .reviewerUserId(entity.getReviewerUserId())
                .reviewerUserName(reviewerUserName)
                .priority(entity.getPriority())
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
        Map<UUID, ClientEntity> clientMap = clientRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(ClientEntity::getId, c -> c, (a, b) -> a));
        Map<UUID, ServiceEntity> serviceMap = serviceRepository.findAll().stream()
                .collect(Collectors.toMap(ServiceEntity::getId, s -> s, (a, b) -> a));
        Map<UUID, String> locationNames = locationRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(LocationEntity::getId, LocationEntity::getName, (a, b) -> a));
        Map<UUID, String> userNames = userRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName, (a, b) -> a));

        return entities.stream().map(e -> {
            ClientEntity c = e.getClientId() != null ? clientMap.get(e.getClientId()) : null;
            ServiceEntity s = e.getServiceId() != null ? serviceMap.get(e.getServiceId()) : null;

            return EngagementDto.builder()
                    .id(e.getId())
                    .organizationId(e.getOrganizationId())
                    .locationId(e.getLocationId())
                    .locationName(e.getLocationId() != null ? locationNames.get(e.getLocationId()) : null)
                    .clientId(e.getClientId())
                    .clientName(c != null ? c.getDisplayName() : null)
                    .clientCode(c != null ? c.getClientCode() : null)
                    .serviceId(e.getServiceId())
                    .serviceCode(s != null ? s.getServiceCode() : null)
                    .serviceName(s != null ? s.getServiceName() : null)
                    .serviceCategory(s != null ? s.getCategory() : null)
                    .clientServiceId(e.getClientServiceId())
                    .engagementCode(e.getEngagementCode())
                    .name(e.getName())
                    .description(e.getDescription())
                    .status(e.getStatus())
                    .startDate(e.getStartDate())
                    .endDate(e.getEndDate())
                    .assignedUserId(e.getAssignedUserId())
                    .assignedUserName(e.getAssignedUserId() != null ? userNames.get(e.getAssignedUserId()) : null)
                    .reviewerUserId(e.getReviewerUserId())
                    .reviewerUserName(e.getReviewerUserId() != null ? userNames.get(e.getReviewerUserId()) : null)
                    .priority(e.getPriority())
                    .notes(e.getNotes())
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .createdBy(e.getCreatedBy())
                    .updatedBy(e.getUpdatedBy())
                    .build();
        }).collect(Collectors.toList());
    }
}
