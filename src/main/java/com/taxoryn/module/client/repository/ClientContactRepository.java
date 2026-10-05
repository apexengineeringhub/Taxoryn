package com.taxoryn.module.client.repository;

import com.taxoryn.module.client.entity.ClientContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientContactRepository extends JpaRepository<ClientContactEntity, UUID> {

    List<ClientContactEntity> findAllByOrganizationIdAndClientIdOrderByPrimaryContactDescCreatedAtAsc(UUID organizationId, UUID clientId);

    List<ClientContactEntity> findAllByOrganizationIdAndClientIdAndActiveOrderByPrimaryContactDescCreatedAtAsc(UUID organizationId, UUID clientId, boolean active);

    Optional<ClientContactEntity> findByOrganizationIdAndClientIdAndId(UUID organizationId, UUID clientId, UUID id);

    Optional<ClientContactEntity> findByOrganizationIdAndClientIdAndPrimaryContactTrue(UUID organizationId, UUID clientId);

    long countByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    long countByOrganizationIdAndClientIdAndActive(UUID organizationId, UUID clientId, boolean active);

    @Modifying
    @Query("UPDATE ClientContactEntity c SET c.primaryContact = false WHERE c.organizationId = :organizationId AND c.clientId = :clientId AND c.id <> :excludeContactId")
    void unsetOtherPrimaryContacts(@Param("organizationId") UUID organizationId, @Param("clientId") UUID clientId, @Param("excludeContactId") UUID excludeContactId);

    @Modifying
    @Query("UPDATE ClientContactEntity c SET c.primaryContact = false WHERE c.organizationId = :organizationId AND c.clientId = :clientId")
    void unsetAllPrimaryContacts(@Param("organizationId") UUID organizationId, @Param("clientId") UUID clientId);
}
