package com.taxoryn.module.client.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.dto.ClientRelationshipDto;
import com.taxoryn.module.client.dto.CreateClientRelationshipRequest;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;
import com.taxoryn.module.client.entity.ClientRelationshipEntity;
import com.taxoryn.module.client.repository.ClientRelationshipRepository;
import com.taxoryn.module.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientRelationshipServiceImpl implements ClientRelationshipService {

    private final ClientRelationshipRepository relationshipRepository;
    private final ClientRepository clientRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<ClientRelationshipDto> getRelationships(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientExists(organizationId, clientId);

        List<ClientRelationshipEntity> entities = relationshipRepository.findAllForClient(organizationId, clientId);
        if (entities.isEmpty()) {
            return List.of();
        }

        // Fetch referenced clients in batch to avoid N+1 queries
        Set<UUID> clientIds = entities.stream()
                .flatMap(e -> java.util.stream.Stream.of(e.getSourceClientId(), e.getTargetClientId()))
                .collect(Collectors.toSet());

        Map<UUID, ClientEntity> clientMap = clientRepository.findAllByOrganizationIdAndIdIn(organizationId, clientIds).stream()
                .collect(Collectors.toMap(ClientEntity::getId, Function.identity()));

        return entities.stream().map(e -> {
            ClientEntity source = clientMap.get(e.getSourceClientId());
            ClientEntity target = clientMap.get(e.getTargetClientId());

            return ClientRelationshipDto.builder()
                    .id(e.getId())
                    .organizationId(e.getOrganizationId())
                    .sourceClientId(e.getSourceClientId())
                    .sourceClientDisplayName(source != null ? source.getDisplayName() : null)
                    .targetClientId(e.getTargetClientId())
                    .targetClientDisplayName(target != null ? target.getDisplayName() : null)
                    .targetClientLegalName(target != null ? target.getLegalName() : null)
                    .targetClientCode(target != null ? target.getClientCode() : null)
                    .targetClientType(target != null ? target.getClientType() : null)
                    .targetClientPan(target != null ? target.getPan() : null)
                    .relationshipType(e.getRelationshipType())
                    .active(e.isActive())
                    .notes(e.getNotes())
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClientRelationshipDto createRelationship(UUID sourceClientId, CreateClientRelationshipRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();

        if (sourceClientId.equals(request.getTargetClientId())) {
            throw new BadRequestException("Cannot create a relationship between a client and itself");
        }

        ClientEntity sourceClient = verifyClientActiveForModification(organizationId, sourceClientId);
        ClientEntity targetClient = verifyClientExists(organizationId, request.getTargetClientId());

        if (relationshipRepository.existsByOrganizationIdAndSourceClientIdAndTargetClientIdAndRelationshipType(
                organizationId, sourceClientId, request.getTargetClientId(), request.getRelationshipType())) {
            throw new BadRequestException("Relationship already exists between these clients with type: " + request.getRelationshipType());
        }

        ClientRelationshipEntity entity = ClientRelationshipEntity.builder()
                .sourceClientId(sourceClientId)
                .targetClientId(request.getTargetClientId())
                .relationshipType(request.getRelationshipType())
                .active(true)
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build();
        entity.setOrganizationId(organizationId);

        ClientRelationshipEntity saved = relationshipRepository.save(entity);

        auditService.logEvent("CLIENT_RELATIONSHIP_CREATED", "CLIENT_RELATIONSHIP", saved.getId().toString(),
                null, "Created relationship " + saved.getRelationshipType() + " between " + sourceClient.getDisplayName() + " and " + targetClient.getDisplayName());

        return ClientRelationshipDto.builder()
                .id(saved.getId())
                .organizationId(saved.getOrganizationId())
                .sourceClientId(saved.getSourceClientId())
                .sourceClientDisplayName(sourceClient.getDisplayName())
                .targetClientId(saved.getTargetClientId())
                .targetClientDisplayName(targetClient.getDisplayName())
                .targetClientLegalName(targetClient.getLegalName())
                .targetClientCode(targetClient.getClientCode())
                .targetClientType(targetClient.getClientType())
                .targetClientPan(targetClient.getPan())
                .relationshipType(saved.getRelationshipType())
                .active(saved.isActive())
                .notes(saved.getNotes())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteRelationship(UUID clientId, UUID relationshipId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        verifyClientActiveForModification(organizationId, clientId);

        ClientRelationshipEntity entity = relationshipRepository.findByOrganizationIdAndId(organizationId, relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Client relationship not found with ID: " + relationshipId));

        if (!entity.getSourceClientId().equals(clientId) && !entity.getTargetClientId().equals(clientId)) {
            throw new ResourceNotFoundException("Client relationship not found for client: " + clientId);
        }

        relationshipRepository.delete(entity);
        auditService.logEvent("CLIENT_RELATIONSHIP_DELETED", "CLIENT_RELATIONSHIP", relationshipId.toString(), null, "Deleted relationship");
    }

    @Override
    @Transactional(readOnly = true)
    public long getRelationshipsCount(UUID clientId) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return relationshipRepository.countAllForClient(organizationId, clientId);
    }

    private ClientEntity verifyClientExists(UUID organizationId, UUID clientId) {
        return clientRepository.findByOrganizationIdAndId(organizationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + clientId));
    }

    private ClientEntity verifyClientActiveForModification(UUID organizationId, UUID clientId) {
        ClientEntity client = verifyClientExists(organizationId, clientId);
        if (client.getStatus() == ClientStatus.ARCHIVED) {
            throw new BadRequestException("Cannot modify relationships for an archived client: " + clientId);
        }
        return client;
    }
}
