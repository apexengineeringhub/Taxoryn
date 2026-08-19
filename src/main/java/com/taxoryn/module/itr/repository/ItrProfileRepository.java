package com.taxoryn.module.itr.repository;

import com.taxoryn.module.itr.entity.ItrProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItrProfileRepository extends JpaRepository<ItrProfileEntity, UUID>, JpaSpecificationExecutor<ItrProfileEntity> {

    Optional<ItrProfileEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ItrProfileEntity> findByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    Optional<ItrProfileEntity> findByOrganizationIdAndPan(UUID organizationId, String pan);

    boolean existsByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    boolean existsByOrganizationIdAndPan(UUID organizationId, String pan);
}
