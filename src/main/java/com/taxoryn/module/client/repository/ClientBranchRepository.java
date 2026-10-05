package com.taxoryn.module.client.repository;

import com.taxoryn.module.client.entity.ClientBranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientBranchRepository extends JpaRepository<ClientBranchEntity, UUID> {

    List<ClientBranchEntity> findAllByOrganizationIdAndClientIdOrderByPrimaryBranchDescCreatedAtAsc(UUID organizationId, UUID clientId);

    List<ClientBranchEntity> findAllByOrganizationIdAndClientIdAndActiveOrderByPrimaryBranchDescCreatedAtAsc(UUID organizationId, UUID clientId, boolean active);

    Optional<ClientBranchEntity> findByOrganizationIdAndClientIdAndId(UUID organizationId, UUID clientId, UUID id);

    Optional<ClientBranchEntity> findByOrganizationIdAndClientIdAndPrimaryBranchTrue(UUID organizationId, UUID clientId);

    long countByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    long countByOrganizationIdAndClientIdAndActive(UUID organizationId, UUID clientId, boolean active);

    @Modifying
    @Query("UPDATE ClientBranchEntity b SET b.primaryBranch = false WHERE b.organizationId = :organizationId AND b.clientId = :clientId AND b.id <> :excludeBranchId")
    void unsetOtherPrimaryBranches(@Param("organizationId") UUID organizationId, @Param("clientId") UUID clientId, @Param("excludeBranchId") UUID excludeBranchId);

    @Modifying
    @Query("UPDATE ClientBranchEntity b SET b.primaryBranch = false WHERE b.organizationId = :organizationId AND b.clientId = :clientId")
    void unsetAllPrimaryBranches(@Param("organizationId") UUID organizationId, @Param("clientId") UUID clientId);
}
