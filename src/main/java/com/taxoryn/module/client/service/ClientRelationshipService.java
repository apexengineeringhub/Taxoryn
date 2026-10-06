package com.taxoryn.module.client.service;

import com.taxoryn.module.client.dto.ClientRelationshipDto;
import com.taxoryn.module.client.dto.CreateClientRelationshipRequest;

import java.util.List;
import java.util.UUID;

public interface ClientRelationshipService {

    List<ClientRelationshipDto> getRelationships(UUID clientId);

    ClientRelationshipDto createRelationship(UUID sourceClientId, CreateClientRelationshipRequest request);

    void deleteRelationship(UUID clientId, UUID relationshipId);

    long getRelationshipsCount(UUID clientId);
}
