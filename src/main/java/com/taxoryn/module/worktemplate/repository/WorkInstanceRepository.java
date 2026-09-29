package com.taxoryn.module.worktemplate.repository;

import com.taxoryn.module.worktemplate.entity.WorkInstanceEntity;
import com.taxoryn.module.worktemplate.model.WorkInstanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkInstanceRepository extends JpaRepository<WorkInstanceEntity, UUID>, JpaSpecificationExecutor<WorkInstanceEntity> {

    Optional<WorkInstanceEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<WorkInstanceEntity> findAllByOrganizationIdAndEngagementIdOrderByPeriodStartDesc(UUID organizationId, UUID engagementId);

    Page<WorkInstanceEntity> findAllByOrganizationIdAndEngagementId(UUID organizationId, UUID engagementId, Pageable pageable);

    boolean existsByOrganizationIdAndEngagementIdAndTemplateIdAndPeriodStartAndPeriodEnd(
            UUID organizationId, UUID engagementId, UUID templateId, LocalDate periodStart, LocalDate periodEnd
    );

    long countByOrganizationIdAndEngagementId(UUID organizationId, UUID engagementId);
}
