package com.taxoryn.module.gst.service.impl;

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
import com.taxoryn.module.gst.dto.CreateGstRegistrationRequest;
import com.taxoryn.module.gst.dto.GstRegistrationDto;
import com.taxoryn.module.gst.dto.GstRegistrationFilterRequest;
import com.taxoryn.module.gst.dto.UpdateGstRegistrationRequest;
import com.taxoryn.module.gst.entity.GstRegistrationEntity;
import com.taxoryn.module.gst.model.GstFilingFrequency;
import com.taxoryn.module.gst.model.GstRegistrationStatus;
import com.taxoryn.module.gst.model.GstRegistrationType;
import com.taxoryn.module.gst.repository.GstRegistrationRepository;
import com.taxoryn.module.gst.service.GstRegistrationService;
import com.taxoryn.module.organization.entity.LocationEntity;
import com.taxoryn.module.organization.repository.LocationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GstRegistrationServiceImpl implements GstRegistrationService {

    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    private final GstRegistrationRepository gstRegistrationRepository;
    private final ClientRepository clientRepository;
    private final LocationRepository locationRepository;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final AuditService auditService;

    @Override
    @Transactional
    public GstRegistrationDto createRegistration(CreateGstRegistrationRequest request) {
        UUID organizationId = resolveOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        UUID locationId = request.getLocationId() != null ? request.getLocationId() : client.getLocationId();
        validateAccess(client.getId(), locationId);

        String gstin = request.getGstin().trim().toUpperCase();
        if (!GSTIN_PATTERN.matcher(gstin).matches()) {
            throw new IllegalArgumentException("Invalid GSTIN format. Expected standard 15-character Indian GSTIN format.");
        }

        if (gstRegistrationRepository.existsByOrganizationIdAndGstin(organizationId, gstin)) {
            throw new DuplicateResourceException("GST Registration", "gstin", gstin);
        }

        String stateCode = StringUtils.hasText(request.getStateCode())
                ? request.getStateCode().trim()
                : gstin.substring(0, 2);

        String legalName = StringUtils.hasText(request.getLegalName())
                ? request.getLegalName().trim()
                : client.getLegalName();

        String tradeName = StringUtils.hasText(request.getTradeName())
                ? request.getTradeName().trim()
                : client.getDisplayName();

        GstRegistrationEntity entity = GstRegistrationEntity.builder()
                .clientId(client.getId())
                .locationId(locationId)
                .gstin(gstin)
                .legalName(legalName)
                .tradeName(tradeName)
                .registrationType(request.getRegistrationType() != null ? request.getRegistrationType() : GstRegistrationType.REGULAR)
                .registrationStatus(request.getRegistrationStatus() != null ? request.getRegistrationStatus() : GstRegistrationStatus.ACTIVE)
                .registrationDate(request.getRegistrationDate())
                .stateCode(stateCode)
                .jurisdiction(request.getJurisdiction())
                .filingFrequency(request.getFilingFrequency() != null ? request.getFilingFrequency() : GstFilingFrequency.MONTHLY)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        entity.setOrganizationId(organizationId);

        entity = gstRegistrationRepository.save(entity);
        log.info("Created GST Registration: id={}, gstin={}, clientId={}", entity.getId(), entity.getGstin(), entity.getClientId());

        auditService.logEvent("GST_REGISTRATION_CREATED", "GST_REGISTRATION", entity.getId().toString(), null, entity);

        return enrichDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public GstRegistrationDto getRegistrationById(UUID id) {
        UUID organizationId = resolveOrganizationId();
        GstRegistrationEntity entity = gstRegistrationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("GST Registration", "id", id));

        validateAccess(entity.getClientId(), entity.getLocationId());

        return enrichDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<GstRegistrationDto> getRegistrations(GstRegistrationFilterRequest filterRequest) {
        UUID organizationId = resolveOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        Specification<GstRegistrationEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (!scope.isFirmAdmin()) {
                Set<UUID> accessibleLocs = scope.getAccessibleLocationIds();
                if (accessibleLocs != null && !accessibleLocs.isEmpty()) {
                    predicates.add(root.get("locationId").in(accessibleLocs));
                }

                Set<UUID> accessibleClients = securityScopeEvaluator.getAccessibleClientIds(scope);
                if (accessibleClients != null) {
                    if (accessibleClients.isEmpty()) {
                        predicates.add(cb.disjunction());
                    } else {
                        predicates.add(root.get("clientId").in(accessibleClients));
                    }
                }
            }

            if (filterRequest.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), filterRequest.getClientId()));
            }
            if (filterRequest.getLocationId() != null) {
                predicates.add(cb.equal(root.get("locationId"), filterRequest.getLocationId()));
            }
            if (filterRequest.getRegistrationStatus() != null) {
                predicates.add(cb.equal(root.get("registrationStatus"), filterRequest.getRegistrationStatus()));
            }
            if (filterRequest.getRegistrationType() != null) {
                predicates.add(cb.equal(root.get("registrationType"), filterRequest.getRegistrationType()));
            }
            if (filterRequest.getFilingFrequency() != null) {
                predicates.add(cb.equal(root.get("filingFrequency"), filterRequest.getFilingFrequency()));
            }
            if (StringUtils.hasText(filterRequest.getStateCode())) {
                predicates.add(cb.equal(root.get("stateCode"), filterRequest.getStateCode().trim()));
            }
            if (StringUtils.hasText(filterRequest.getSearch())) {
                String searchPattern = "%" + filterRequest.getSearch().trim().toUpperCase() + "%";
                Predicate gstinPred = cb.like(cb.upper(root.get("gstin")), searchPattern);
                Predicate legalPred = cb.like(cb.upper(root.get("legalName")), searchPattern);
                Predicate tradePred = cb.like(cb.upper(root.get("tradeName")), searchPattern);
                predicates.add(cb.or(gstinPred, legalPred, tradePred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<GstRegistrationEntity> page = gstRegistrationRepository.findAll(spec, filterRequest.toPageable());
        List<GstRegistrationDto> dtoList = enrichDtoList(page.getContent());

        return PagedResponse.<GstRegistrationDto>builder()
                .content(dtoList)
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
    public List<GstRegistrationDto> getRegistrationsByClientId(UUID clientId) {
        UUID organizationId = resolveOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        List<GstRegistrationEntity> list = gstRegistrationRepository.findAllByOrganizationIdAndClientId(organizationId, clientId);
        return enrichDtoList(list);
    }

    @Override
    @Transactional
    public GstRegistrationDto updateRegistration(UUID id, UpdateGstRegistrationRequest request) {
        UUID organizationId = resolveOrganizationId();
        GstRegistrationEntity entity = gstRegistrationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("GST Registration", "id", id));

        validateAccess(entity.getClientId(), entity.getLocationId());

        if (request.getLocationId() != null) {
            entity.setLocationId(request.getLocationId());
        }
        if (StringUtils.hasText(request.getLegalName())) {
            entity.setLegalName(request.getLegalName().trim());
        }
        if (StringUtils.hasText(request.getTradeName())) {
            entity.setTradeName(request.getTradeName().trim());
        }
        if (request.getRegistrationType() != null) {
            entity.setRegistrationType(request.getRegistrationType());
        }
        if (request.getRegistrationStatus() != null) {
            entity.setRegistrationStatus(request.getRegistrationStatus());
        }
        if (request.getRegistrationDate() != null) {
            entity.setRegistrationDate(request.getRegistrationDate());
        }
        if (StringUtils.hasText(request.getStateCode())) {
            entity.setStateCode(request.getStateCode().trim());
        }
        if (request.getJurisdiction() != null) {
            entity.setJurisdiction(request.getJurisdiction().trim());
        }
        if (request.getFilingFrequency() != null) {
            entity.setFilingFrequency(request.getFilingFrequency());
        }
        if (request.getEffectiveFrom() != null) {
            entity.setEffectiveFrom(request.getEffectiveFrom());
        }
        if (request.getEffectiveTo() != null) {
            entity.setEffectiveTo(request.getEffectiveTo());
        }
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }

        entity = gstRegistrationRepository.save(entity);
        log.info("Updated GST Registration: id={}, gstin={}", entity.getId(), entity.getGstin());

        auditService.logEvent("GST_REGISTRATION_UPDATED", "GST_REGISTRATION", entity.getId().toString(), null, entity);

        return enrichDto(entity);
    }

    @Override
    @Transactional
    public void deleteRegistration(UUID id) {
        UUID organizationId = resolveOrganizationId();
        GstRegistrationEntity entity = gstRegistrationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("GST Registration", "id", id));

        validateAccess(entity.getClientId(), entity.getLocationId());

        gstRegistrationRepository.delete(entity);
        log.info("Deleted GST Registration: id={}, gstin={}", id, entity.getGstin());

        auditService.logEvent("GST_REGISTRATION_DELETED", "GST_REGISTRATION", id.toString(), null, null);
    }

    // --- Helpers ---

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

    private GstRegistrationDto enrichDto(GstRegistrationEntity entity) {
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

        return GstRegistrationDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(clientName)
                .locationId(entity.getLocationId())
                .locationName(locationName)
                .gstin(entity.getGstin())
                .legalName(entity.getLegalName())
                .tradeName(entity.getTradeName())
                .registrationType(entity.getRegistrationType())
                .registrationStatus(entity.getRegistrationStatus())
                .registrationDate(entity.getRegistrationDate())
                .stateCode(entity.getStateCode())
                .jurisdiction(entity.getJurisdiction())
                .filingFrequency(entity.getFilingFrequency())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    private List<GstRegistrationDto> enrichDtoList(List<GstRegistrationEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        UUID orgId = entities.get(0).getOrganizationId();
        Map<UUID, String> clientNames = clientRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(ClientEntity::getId, ClientEntity::getDisplayName, (a, b) -> a));
        Map<UUID, String> locationNames = locationRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(LocationEntity::getId, LocationEntity::getName, (a, b) -> a));

        return entities.stream().map(e -> GstRegistrationDto.builder()
                .id(e.getId())
                .organizationId(e.getOrganizationId())
                .clientId(e.getClientId())
                .clientName(e.getClientId() != null ? clientNames.get(e.getClientId()) : null)
                .locationId(e.getLocationId())
                .locationName(e.getLocationId() != null ? locationNames.get(e.getLocationId()) : null)
                .gstin(e.getGstin())
                .legalName(e.getLegalName())
                .tradeName(e.getTradeName())
                .registrationType(e.getRegistrationType())
                .registrationStatus(e.getRegistrationStatus())
                .registrationDate(e.getRegistrationDate())
                .stateCode(e.getStateCode())
                .jurisdiction(e.getJurisdiction())
                .filingFrequency(e.getFilingFrequency())
                .effectiveFrom(e.getEffectiveFrom())
                .effectiveTo(e.getEffectiveTo())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build()).collect(Collectors.toList());
    }
}
