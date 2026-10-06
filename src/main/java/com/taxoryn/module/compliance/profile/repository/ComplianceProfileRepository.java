package com.taxoryn.module.compliance.profile.repository;

import com.taxoryn.module.compliance.profile.entity.ComplianceProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceProfileRepository extends JpaRepository<ComplianceProfileEntity, UUID> {

    Optional<ComplianceProfileEntity> findByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    boolean existsByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    void deleteByOrganizationIdAndClientId(UUID organizationId, UUID clientId);
}
