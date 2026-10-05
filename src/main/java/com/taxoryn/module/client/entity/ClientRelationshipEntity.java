package com.taxoryn.module.client.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "client_relationships")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientRelationshipEntity extends TenantAuditableEntity {

    @Column(name = "source_client_id", nullable = false)
    private UUID sourceClientId;

    @Column(name = "target_client_id", nullable = false)
    private UUID targetClientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 50)
    @Builder.Default
    private ClientRelationshipType relationshipType = ClientRelationshipType.RELATED_ENTITY;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
