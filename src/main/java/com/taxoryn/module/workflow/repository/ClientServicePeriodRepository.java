package com.taxoryn.module.workflow.repository;

import com.taxoryn.module.workflow.entity.ClientServicePeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientServicePeriodRepository extends JpaRepository<ClientServicePeriodEntity, UUID>, JpaSpecificationExecutor<ClientServicePeriodEntity> {

    Optional<ClientServicePeriodEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<ClientServicePeriodEntity> findAllByOrganizationIdAndClientServiceIdOrderByStartDateDesc(UUID organizationId, UUID clientServiceId);

    List<ClientServicePeriodEntity> findAllByOrganizationIdAndClientIdOrderByStartDateDesc(UUID organizationId, UUID clientId);

    Optional<ClientServicePeriodEntity> findByOrganizationIdAndClientServiceIdAndPeriodLabel(UUID organizationId, UUID clientServiceId, String periodLabel);

    boolean existsByOrganizationIdAndClientServiceIdAndPeriodLabel(UUID organizationId, UUID clientServiceId, String periodLabel);
}
