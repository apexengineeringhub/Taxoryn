package com.taxoryn.module.workflow.repository;

import com.taxoryn.module.workflow.entity.ClientServiceWorkflowEntity;
import com.taxoryn.module.workflow.model.ServiceWorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientServiceWorkflowRepository extends JpaRepository<ClientServiceWorkflowEntity, UUID>, JpaSpecificationExecutor<ClientServiceWorkflowEntity> {

    Optional<ClientServiceWorkflowEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ClientServiceWorkflowEntity> findByOrganizationIdAndClientServiceIdAndPeriodId(UUID organizationId, UUID clientServiceId, UUID periodId);

    List<ClientServiceWorkflowEntity> findAllByOrganizationIdAndClientServiceIdOrderByCreatedAtDesc(UUID organizationId, UUID clientServiceId);

    List<ClientServiceWorkflowEntity> findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(UUID organizationId, UUID clientId);

    List<ClientServiceWorkflowEntity> findAllByOrganizationIdAndAssignedEmployeeId(UUID organizationId, UUID assignedEmployeeId);

    long countByOrganizationIdAndStatus(UUID organizationId, ServiceWorkflowStatus status);

    boolean existsByOrganizationIdAndClientServiceIdAndPeriodId(UUID organizationId, UUID clientServiceId, UUID periodId);
}
