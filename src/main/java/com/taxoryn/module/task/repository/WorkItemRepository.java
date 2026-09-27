package com.taxoryn.module.task.repository;

import com.taxoryn.module.task.entity.WorkItemEntity;
import com.taxoryn.module.task.model.WorkItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItemEntity, UUID>, JpaSpecificationExecutor<WorkItemEntity> {

    Optional<WorkItemEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<WorkItemEntity> findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(UUID organizationId, UUID clientId);

    List<WorkItemEntity> findAllByOrganizationIdAndWorkflowIdOrderByCreatedAtDesc(UUID organizationId, UUID workflowId);

    List<WorkItemEntity> findAllByOrganizationIdAndNoticeIdOrderByCreatedAtDesc(UUID organizationId, UUID noticeId);

    List<WorkItemEntity> findAllByOrganizationIdAndLocationId(UUID organizationId, UUID locationId);

    List<WorkItemEntity> findAllByOrganizationIdAndAssignedUserId(UUID organizationId, UUID assignedUserId);

    long countByOrganizationIdAndStatus(UUID organizationId, WorkItemStatus status);
}
