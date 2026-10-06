package com.taxoryn.module.gov.repository;

import com.taxoryn.module.gov.entity.GovConnectionEntity;
import com.taxoryn.module.gov.model.GovProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GovConnectionRepository extends JpaRepository<GovConnectionEntity, UUID> {

    Optional<GovConnectionEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<GovConnectionEntity> findAllByOrganizationId(UUID organizationId);

    List<GovConnectionEntity> findAllByOrganizationIdAndProviderType(UUID organizationId, GovProviderType providerType);

    boolean existsByOrganizationIdAndDisplayName(UUID organizationId, String displayName);
}
