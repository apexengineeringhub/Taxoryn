package com.taxoryn.module.client.repository;

import com.taxoryn.module.client.entity.ClientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, UUID>, JpaSpecificationExecutor<ClientEntity> {

    Optional<ClientEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ClientEntity> findByOrganizationIdAndPan(UUID organizationId, String pan);

    Optional<ClientEntity> findByOrganizationIdAndGstin(UUID organizationId, String gstin);

    boolean existsByOrganizationIdAndPan(UUID organizationId, String pan);

    boolean existsByOrganizationIdAndGstin(UUID organizationId, String gstin);

    Page<ClientEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    List<ClientEntity> findAllByOrganizationId(UUID organizationId);

    List<ClientEntity> findAllByOrganizationIdAndStatus(UUID organizationId, ClientEntity.ClientStatus status);

    long countByOrganizationId(UUID organizationId);

    @Query("SELECT COUNT(c), " +
           "SUM(CASE WHEN c.status = com.taxoryn.module.client.entity.ClientEntity.ClientStatus.ACTIVE THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN c.status != com.taxoryn.module.client.entity.ClientEntity.ClientStatus.ACTIVE THEN 1L ELSE 0L END) " +
           "FROM ClientEntity c WHERE c.organizationId = :organizationId")
    List<Object[]> getClientDashboardStats(@Param("organizationId") UUID organizationId);
}
