package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.client.dto.ClientContextSummaryDto;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of {@link ClientContextService}.
 * Serves as the authoritative application-level contract for cross-module client context resolution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientContextServiceImpl implements ClientContextService {

    private final ClientRepository clientRepository;
    private final ClientProfileCompletenessEvaluator completenessEvaluator;

    @Override
    public Optional<ClientContextSummaryDto> findClientContext(UUID organizationId, UUID clientId) {
        if (organizationId == null || clientId == null) {
            return Optional.empty();
        }
        return clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .map(this::toSummaryDto);
    }

    @Override
    public Optional<ClientContextSummaryDto> findClientContext(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null || clientId == null) {
            return Optional.empty();
        }
        return findClientContext(organizationId, clientId);
    }

    @Override
    public ClientContextSummaryDto requireClientContext(UUID organizationId, UUID clientId) {
        return findClientContext(organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));
    }

    @Override
    public ClientContextSummaryDto requireClientContext(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new com.taxoryn.core.exception.UnauthorizedException("Authenticated organization context is required");
        }
        return requireClientContext(organizationId, clientId);
    }

    @Override
    public boolean exists(UUID organizationId, UUID clientId) {
        if (organizationId == null || clientId == null) {
            return false;
        }
        return clientRepository.findByIdAndOrganizationId(clientId, organizationId).isPresent();
    }

    @Override
    public boolean isActive(UUID organizationId, UUID clientId) {
        if (organizationId == null || clientId == null) {
            return false;
        }
        return clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .map(c -> c.getStatus() == ClientStatus.ACTIVE)
                .orElse(false);
    }

    private ClientContextSummaryDto toSummaryDto(ClientEntity entity) {
        return ClientContextSummaryDto.builder()
                .clientId(entity.getId())
                .organizationId(entity.getOrganizationId())
                .displayName(entity.getDisplayName())
                .legalName(entity.getLegalName())
                .tradeName(entity.getTradeName())
                .clientCode(entity.getClientCode())
                .clientType(entity.getClientType())
                .status(entity.getStatus())
                .active(entity.getStatus() == ClientStatus.ACTIVE)
                .pan(entity.getPan())
                .gstin(entity.getGstin())
                .tan(entity.getTan())
                .cin(entity.getCin())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .city(entity.getCity())
                .state(entity.getState())
                .stateCode(entity.getStateCode())
                .pincode(entity.getPincode())
                .businessActivity(entity.getBusinessActivity())
                .industry(entity.getIndustry())
                .businessScale(entity.getBusinessScale())
                .completeness(completenessEvaluator.evaluate(entity))
                .locationId(entity.getLocationId())
                .assignedEmployeeId(entity.getAssignedEmployeeId())
                .build();
    }
}
