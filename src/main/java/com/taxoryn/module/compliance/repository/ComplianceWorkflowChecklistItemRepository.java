package com.taxoryn.module.compliance.repository;

import com.taxoryn.module.compliance.entity.ComplianceWorkflowChecklistItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceWorkflowChecklistItemRepository extends JpaRepository<ComplianceWorkflowChecklistItemEntity, UUID> {

    List<ComplianceWorkflowChecklistItemEntity> findByWorkflowIdAndOrganizationIdOrderBySequenceOrderAsc(UUID workflowId, UUID organizationId);

    Optional<ComplianceWorkflowChecklistItemEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ComplianceWorkflowChecklistItemEntity> findByWorkflowIdAndItemKeyAndOrganizationId(UUID workflowId, String itemKey, UUID organizationId);
}
