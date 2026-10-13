package com.taxoryn.module.compliance.qc.repository;

import com.taxoryn.module.compliance.qc.entity.ComplianceQcAuthorizationRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceQcAuthorizationRecordRepository extends JpaRepository<ComplianceQcAuthorizationRecordEntity, UUID> {

    List<ComplianceQcAuthorizationRecordEntity> findByOrganizationIdAndWorkflowIdOrderByCreatedAtDesc(
            UUID organizationId, UUID workflowId);

    Optional<ComplianceQcAuthorizationRecordEntity> findTopByOrganizationIdAndWorkflowIdOrderByCreatedAtDesc(
            UUID organizationId, UUID workflowId);
}
