package com.taxoryn.module.client.repository;

import com.taxoryn.module.client.entity.ClientNoteEntity;
import com.taxoryn.module.client.entity.ClientNoteEntity.NoteType;
import com.taxoryn.module.client.entity.ClientNoteEntity.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ClientCommunicationTimelineRepository extends JpaRepository<ClientNoteEntity, UUID> {
    Optional<ClientNoteEntity> findByIdAndOrganizationIdAndClientId(UUID id, UUID organizationId, UUID clientId);

    @Query("""
            SELECT e FROM ClientNoteEntity e
            WHERE e.organizationId = :organizationId AND e.clientId = :clientId
              AND (:communicationType IS NULL OR e.noteType = :communicationType)
              AND (:dateFrom IS NULL OR e.occurredAt >= :dateFrom)
              AND (:dateTo IS NULL OR e.occurredAt <= :dateTo)
              AND (:followUpRequired IS NULL OR e.followUpRequired = :followUpRequired)
              AND (:visibility IS NULL OR e.visibility = :visibility)
            """)
    Page<ClientNoteEntity> findTimelineEntries(
            @Param("organizationId") UUID organizationId,
            @Param("clientId") UUID clientId,
            @Param("communicationType") NoteType communicationType,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("followUpRequired") Boolean followUpRequired,
            @Param("visibility") Visibility visibility,
            Pageable pageable);
}
