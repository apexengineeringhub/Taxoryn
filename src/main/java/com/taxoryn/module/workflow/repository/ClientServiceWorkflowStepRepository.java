package com.taxoryn.module.workflow.repository;

import com.taxoryn.module.workflow.entity.ClientServiceWorkflowStepEntity;
import com.taxoryn.module.workflow.model.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientServiceWorkflowStepRepository extends JpaRepository<ClientServiceWorkflowStepEntity, UUID> {

    Optional<ClientServiceWorkflowStepEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<ClientServiceWorkflowStepEntity> findAllByOrganizationIdAndWorkflowIdOrderBySequenceAsc(UUID organizationId, UUID workflowId);

    List<ClientServiceWorkflowStepEntity> findAllByOrganizationIdAndAssignedEmployeeId(UUID organizationId, UUID assignedEmployeeId);

    long countByOrganizationIdAndWorkflowIdAndStatus(UUID organizationId, UUID workflowId, StepStatus status);
}
