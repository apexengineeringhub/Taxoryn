package com.taxoryn.module.client.repository;

import com.taxoryn.module.client.entity.ClientNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientNoteRepository extends JpaRepository<ClientNoteEntity, UUID> {

    List<ClientNoteEntity> findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(UUID organizationId, UUID clientId);

    List<ClientNoteEntity> findTop10ByOrganizationIdAndClientIdOrderByCreatedAtDesc(UUID organizationId, UUID clientId);
}
