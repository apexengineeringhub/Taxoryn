package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.dto.ClientBranchDto;
import com.taxoryn.module.client.dto.CreateClientBranchRequest;
import com.taxoryn.module.client.dto.UpdateClientBranchRequest;
import com.taxoryn.module.client.entity.ClientBranchEntity;
import com.taxoryn.module.client.entity.ClientBranchType;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.repository.ClientBranchRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientBranchServiceImpl implements ClientBranchService {

    private final ClientBranchRepository branchRepository;
    private final ClientRepository clientRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<ClientBranchDto> getBranches(UUID clientId, Boolean activeOnly) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientExists(organizationId, clientId);

        List<ClientBranchEntity> entities;
        if (Boolean.TRUE.equals(activeOnly)) {
            entities = branchRepository.findAllByOrganizationIdAndClientIdAndActiveOrderByPrimaryBranchDescCreatedAtAsc(organizationId, clientId, true);
        } else {
            entities = branchRepository.findAllByOrganizationIdAndClientIdOrderByPrimaryBranchDescCreatedAtAsc(organizationId, clientId);
        }
        return entities.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientBranchDto getBranchById(UUID clientId, UUID branchId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientExists(organizationId, clientId);

        ClientBranchEntity entity = branchRepository.findByOrganizationIdAndClientIdAndId(organizationId, clientId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Client branch not found with ID: " + branchId));
        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientBranchDto> getPrimaryBranch(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return branchRepository.findByOrganizationIdAndClientIdAndPrimaryBranchTrue(organizationId, clientId)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public ClientBranchDto createBranch(UUID clientId, CreateClientBranchRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = verifyClientActiveForModification(organizationId, clientId);

        boolean isPrimary = Boolean.TRUE.equals(request.getPrimaryBranch());

        ClientBranchEntity entity = ClientBranchEntity.builder()
                .clientId(clientId)
                .branchName(request.getBranchName() != null ? request.getBranchName().trim() : "")
                .branchCode(request.getBranchCode() != null ? request.getBranchCode().trim() : null)
                .branchType(request.getBranchType() != null ? request.getBranchType() : ClientBranchType.BRANCH)
                .addressLine1(request.getAddressLine1() != null ? request.getAddressLine1().trim() : null)
                .addressLine2(request.getAddressLine2() != null ? request.getAddressLine2().trim() : null)
                .city(request.getCity() != null ? request.getCity().trim() : null)
                .state(request.getState() != null ? request.getState().trim() : null)
                .stateCode(request.getStateCode() != null ? request.getStateCode().trim() : null)
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry().trim() : "India")
                .pincode(request.getPincode() != null ? request.getPincode().trim() : null)
                .gstin(request.getGstin() != null ? request.getGstin().trim().toUpperCase() : null)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
                .primaryBranch(isPrimary)
                .active(true)
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build();
        entity.setOrganizationId(organizationId);

        ClientBranchEntity saved = branchRepository.save(entity);

        if (isPrimary) {
            branchRepository.unsetOtherPrimaryBranches(organizationId, clientId, saved.getId());
        }

        auditService.logEvent("CLIENT_BRANCH_CREATED", "CLIENT_BRANCH", saved.getId().toString(), null, saved.getBranchName());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ClientBranchDto updateBranch(UUID clientId, UUID branchId, UpdateClientBranchRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientActiveForModification(organizationId, clientId);

        ClientBranchEntity entity = branchRepository.findByOrganizationIdAndClientIdAndId(organizationId, clientId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Client branch not found with ID: " + branchId));

        if (request.getBranchName() != null) {
            entity.setBranchName(request.getBranchName().trim());
        }
        if (request.getBranchCode() != null) {
            entity.setBranchCode(request.getBranchCode().trim());
        }
        if (request.getBranchType() != null) {
            entity.setBranchType(request.getBranchType());
        }
        if (request.getAddressLine1() != null) {
            entity.setAddressLine1(request.getAddressLine1().trim());
        }
        if (request.getAddressLine2() != null) {
            entity.setAddressLine2(request.getAddressLine2().trim());
        }
        if (request.getCity() != null) {
            entity.setCity(request.getCity().trim());
        }
        if (request.getState() != null) {
            entity.setState(request.getState().trim());
        }
        if (request.getStateCode() != null) {
            entity.setStateCode(request.getStateCode().trim());
        }
        if (request.getCountry() != null) {
            entity.setCountry(request.getCountry().trim());
        }
        if (request.getPincode() != null) {
            entity.setPincode(request.getPincode().trim());
        }
        if (request.getGstin() != null) {
            entity.setGstin(request.getGstin().trim().toUpperCase());
        }
        if (request.getPhone() != null) {
            entity.setPhone(request.getPhone().trim());
        }
        if (request.getEmail() != null) {
            entity.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes().trim());
        }

        if (Boolean.TRUE.equals(request.getPrimaryBranch())) {
            entity.setPrimaryBranch(true);
            branchRepository.unsetOtherPrimaryBranches(organizationId, clientId, entity.getId());
            auditService.logEvent("CLIENT_BRANCH_PRIMARY_CHANGED", "CLIENT_BRANCH", entity.getId().toString(), null, "Designated as primary branch");
        } else if (Boolean.FALSE.equals(request.getPrimaryBranch())) {
            entity.setPrimaryBranch(false);
        }

        ClientBranchEntity saved = branchRepository.save(entity);
        auditService.logEvent("CLIENT_BRANCH_UPDATED", "CLIENT_BRANCH", saved.getId().toString(), null, saved.getBranchName());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ClientBranchDto updateStatus(UUID clientId, UUID branchId, boolean active) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientActiveForModification(organizationId, clientId);

        ClientBranchEntity entity = branchRepository.findByOrganizationIdAndClientIdAndId(organizationId, clientId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Client branch not found with ID: " + branchId));

        entity.setActive(active);
        if (!active && entity.isPrimaryBranch()) {
            entity.setPrimaryBranch(false);
        }

        ClientBranchEntity saved = branchRepository.save(entity);
        if (!active) {
            auditService.logEvent("CLIENT_BRANCH_DEACTIVATED", "CLIENT_BRANCH", saved.getId().toString(), null, "Deactivated branch");
        } else {
            auditService.logEvent("CLIENT_BRANCH_UPDATED", "CLIENT_BRANCH", saved.getId().toString(), null, "Activated branch");
        }
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ClientBranchDto setPrimaryBranch(UUID clientId, UUID branchId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientActiveForModification(organizationId, clientId);

        ClientBranchEntity entity = branchRepository.findByOrganizationIdAndClientIdAndId(organizationId, clientId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Client branch not found with ID: " + branchId));

        if (!entity.isActive()) {
            throw new BadRequestException("Cannot set an inactive branch as the primary branch");
        }

        entity.setPrimaryBranch(true);
        branchRepository.unsetOtherPrimaryBranches(organizationId, clientId, entity.getId());
        ClientBranchEntity saved = branchRepository.save(entity);

        auditService.logEvent("CLIENT_BRANCH_PRIMARY_CHANGED", "CLIENT_BRANCH", entity.getId().toString(), null, "Designated as primary branch");
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteBranch(UUID clientId, UUID branchId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientActiveForModification(organizationId, clientId);

        ClientBranchEntity entity = branchRepository.findByOrganizationIdAndClientIdAndId(organizationId, clientId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Client branch not found with ID: " + branchId));

        entity.setActive(false);
        entity.setPrimaryBranch(false);
        branchRepository.save(entity);
        auditService.logEvent("CLIENT_BRANCH_DEACTIVATED", "CLIENT_BRANCH", entity.getId().toString(), null, "Soft deleted branch");
    }

    @Override
    @Transactional(readOnly = true)
    public long getBranchesCount(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return branchRepository.countByOrganizationIdAndClientId(organizationId, clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveBranchesCount(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return branchRepository.countByOrganizationIdAndClientIdAndActive(organizationId, clientId, true);
    }

    private ClientEntity verifyClientExists(UUID organizationId, UUID clientId) {
        return clientRepository.findByOrganizationIdAndId(organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + clientId));
    }

    private ClientEntity verifyClientActiveForModification(UUID organizationId, UUID clientId) {
        ClientEntity client = verifyClientExists(organizationId, clientId);
        if (client.getStatus() == ClientStatus.ARCHIVED) {
            throw new BadRequestException("Cannot modify branches for an archived client: " + clientId);
        }
        return client;
    }

    private ClientBranchDto mapToDto(ClientBranchEntity entity) {
        return ClientBranchDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .branchName(entity.getBranchName())
                .branchCode(entity.getBranchCode())
                .branchType(entity.getBranchType())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .city(entity.getCity())
                .state(entity.getState())
                .stateCode(entity.getStateCode())
                .country(entity.getCountry())
                .pincode(entity.getPincode())
                .gstin(entity.getGstin())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .primaryBranch(entity.isPrimaryBranch())
                .active(entity.isActive())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
