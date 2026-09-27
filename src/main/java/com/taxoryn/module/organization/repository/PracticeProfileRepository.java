package com.taxoryn.module.organization.repository;

import com.taxoryn.module.organization.entity.PracticeProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PracticeProfileRepository extends JpaRepository<PracticeProfileEntity, UUID> {

    Optional<PracticeProfileEntity> findByOrganizationId(UUID organizationId);

    boolean existsByOrganizationId(UUID organizationId);
}
