package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.dto.ClientContactDto;
import com.taxoryn.module.client.dto.CreateClientContactRequest;
import com.taxoryn.module.client.dto.UpdateClientContactRequest;
import com.taxoryn.module.client.entity.ClientContactEntity;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ContactRole;
import com.taxoryn.module.client.repository.ClientContactRepository;
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
public class ClientContactServiceImpl implements ClientContactService {

    private final ClientContactRepository contactRepository;
    private final ClientRepository clientRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<ClientContactDto> getContacts(UUID clientId, Boolean activeOnly) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientExists(organizationId, clientId);

        List<ClientContactEntity> entities;
        if (Boolean.TRUE.equals(activeOnly)) {
            entities = contactRepository.findAllByOrganizationIdAndClientIdAndActiveOrderByPrimaryContactDescCreatedAtAsc(organizationId, clientId, true);
        } else {
            entities = contactRepository.findAllByOrganizationIdAndClientIdOrderByPrimaryContactDescCreatedAtAsc(organizationId, clientId);
        }
        return entities.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientContactDto getContactById(UUID clientId, UUID contactId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientExists(organizationId, clientId);

        ClientContactEntity entity = contactRepository.findByOrganizationIdAndClientIdAndId(organizationId, clientId, contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Client contact not found with ID: " + contactId));
        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientContactDto> getPrimaryContact(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return contactRepository.findByOrganizationIdAndClientIdAndPrimaryContactTrue(organizationId, clientId)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public ClientContactDto createContact(UUID clientId, CreateClientContactRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        ClientEntity client = verifyClientActiveForModification(organizationId, clientId);

        boolean isPrimary = Boolean.TRUE.equals(request.getPrimaryContact());

        ClientContactEntity entity = ClientContactEntity.builder()
                .clientId(clientId)
                .firstName(request.getFirstName() != null ? request.getFirstName().trim() : "")
                .lastName(request.getLastName() != null ? request.getLastName().trim() : null)
                .displayName(request.getDisplayName() != null ? request.getDisplayName().trim() : null)
                .designation(request.getDesignation() != null ? request.getDesignation().trim() : null)
                .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .altPhone(request.getAltPhone() != null ? request.getAltPhone().trim() : null)
                .contactRole(request.getContactRole() != null ? request.getContactRole() : ContactRole.OTHER)
                .primaryContact(isPrimary)
                .active(true)
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build();
        entity.setOrganizationId(organizationId);

        if (entity.getDisplayName() == null || entity.getDisplayName().isBlank()) {
            entity.setDisplayName(entity.getComputedDisplayName());
        }

        ClientContactEntity saved = contactRepository.save(entity);

        if (isPrimary) {
            contactRepository.unsetOtherPrimaryContacts(organizationId, clientId, saved.getId());
        }

        auditService.logEvent("CLIENT_CONTACT_CREATED", "CLIENT_CONTACT", saved.getId().toString(), null, saved.getDisplayName());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ClientContactDto updateContact(UUID clientId, UUID contactId, UpdateClientContactRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientActiveForModification(organizationId, clientId);

        ClientContactEntity entity = contactRepository.findByOrganizationIdAndClientIdAndId(organizationId, clientId, contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Client contact not found with ID: " + contactId));

        if (request.getFirstName() != null) {
            entity.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            entity.setLastName(request.getLastName().trim());
        }
        if (request.getDisplayName() != null) {
            entity.setDisplayName(request.getDisplayName().trim());
        } else if (request.getFirstName() != null || request.getLastName() != null) {
            entity.setDisplayName(entity.getComputedDisplayName());
        }
        if (request.getDesignation() != null) {
            entity.setDesignation(request.getDesignation().trim());
        }
        if (request.getEmail() != null) {
            entity.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (request.getPhone() != null) {
            entity.setPhone(request.getPhone().trim());
        }
        if (request.getAltPhone() != null) {
            entity.setAltPhone(request.getAltPhone().trim());
        }
        if (request.getContactRole() != null) {
            entity.setContactRole(request.getContactRole());
        }
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes().trim());
        }

        if (Boolean.TRUE.equals(request.getPrimaryContact())) {
            entity.setPrimaryContact(true);
            contactRepository.unsetOtherPrimaryContacts(organizationId, clientId, entity.getId());
            auditService.logEvent("CLIENT_CONTACT_PRIMARY_CHANGED", "CLIENT_CONTACT", entity.getId().toString(), null, "Designated as primary contact");
        } else if (Boolean.FALSE.equals(request.getPrimaryContact())) {
            entity.setPrimaryContact(false);
        }

        ClientContactEntity saved = contactRepository.save(entity);
        auditService.logEvent("CLIENT_CONTACT_UPDATED", "CLIENT_CONTACT", saved.getId().toString(), null, saved.getDisplayName());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ClientContactDto updateStatus(UUID clientId, UUID contactId, boolean active) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientActiveForModification(organizationId, clientId);

        ClientContactEntity entity = contactRepository.findByOrganizationIdAndClientIdAndId(organizationId, clientId, contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Client contact not found with ID: " + contactId));

        entity.setActive(active);
        if (!active && entity.isPrimaryContact()) {
            entity.setPrimaryContact(false);
        }

        ClientContactEntity saved = contactRepository.save(entity);
        if (!active) {
            auditService.logEvent("CLIENT_CONTACT_DEACTIVATED", "CLIENT_CONTACT", saved.getId().toString(), null, "Deactivated contact");
        } else {
            auditService.logEvent("CLIENT_CONTACT_UPDATED", "CLIENT_CONTACT", saved.getId().toString(), null, "Activated contact");
        }
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ClientContactDto setPrimaryContact(UUID clientId, UUID contactId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientActiveForModification(organizationId, clientId);

        ClientContactEntity entity = contactRepository.findByOrganizationIdAndClientIdAndId(organizationId, clientId, contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Client contact not found with ID: " + contactId));

        if (!entity.isActive()) {
            throw new BadRequestException("Cannot set an inactive contact as the primary contact");
        }

        entity.setPrimaryContact(true);
        contactRepository.unsetOtherPrimaryContacts(organizationId, clientId, entity.getId());
        ClientContactEntity saved = contactRepository.save(entity);

        auditService.logEvent("CLIENT_CONTACT_PRIMARY_CHANGED", "CLIENT_CONTACT", entity.getId().toString(), null, "Designated as primary contact");
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteContact(UUID clientId, UUID contactId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientActiveForModification(organizationId, clientId);

        ClientContactEntity entity = contactRepository.findByOrganizationIdAndClientIdAndId(organizationId, clientId, contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Client contact not found with ID: " + contactId));

        entity.setActive(false);
        entity.setPrimaryContact(false);
        contactRepository.save(entity);
        auditService.logEvent("CLIENT_CONTACT_DEACTIVATED", "CLIENT_CONTACT", entity.getId().toString(), null, "Soft deleted contact");
    }

    @Override
    @Transactional(readOnly = true)
    public long getContactsCount(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return contactRepository.countByOrganizationIdAndClientId(organizationId, clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveContactsCount(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return contactRepository.countByOrganizationIdAndClientIdAndActive(organizationId, clientId, true);
    }

    private ClientEntity verifyClientExists(UUID organizationId, UUID clientId) {
        return clientRepository.findByOrganizationIdAndId(organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + clientId));
    }

    private ClientEntity verifyClientActiveForModification(UUID organizationId, UUID clientId) {
        ClientEntity client = verifyClientExists(organizationId, clientId);
        if (client.getStatus() == ClientStatus.ARCHIVED) {
            throw new BadRequestException("Cannot modify contacts for an archived client: " + clientId);
        }
        return client;
    }

    private ClientContactDto mapToDto(ClientContactEntity entity) {
        String displayName = entity.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = entity.getComputedDisplayName();
        }
        return ClientContactDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .displayName(displayName)
                .designation(entity.getDesignation())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .altPhone(entity.getAltPhone())
                .contactRole(entity.getContactRole())
                .primaryContact(entity.isPrimaryContact())
                .active(entity.isActive())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
