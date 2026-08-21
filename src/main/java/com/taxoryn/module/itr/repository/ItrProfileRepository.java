package com.taxoryn.module.itr.repository;

import com.taxoryn.module.itr.entity.ItrProfileEntity;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItrProfileRepository extends JpaRepository<ItrProfileEntity, UUID>, JpaSpecificationExecutor<ItrProfileEntity> {

    Optional<ItrProfileEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ItrProfileEntity> findByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    Optional<ItrProfileEntity> findByOrganizationIdAndPan(UUID organizationId, String pan);

    List<ItrProfileEntity> findAllByOrganizationIdAndStatus(UUID organizationId, ItrProfileStatus status);

    List<ItrProfileEntity> findAllByOrganizationId(UUID organizationId);

    boolean existsByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    boolean existsByOrganizationIdAndPan(UUID organizationId, String pan);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT p.clientId) FROM ItrProfileEntity p WHERE p.organizationId = :organizationId")
    long countDistinctClientsByOrganizationId(@org.springframework.data.repository.query.Param("organizationId") UUID organizationId);
}
