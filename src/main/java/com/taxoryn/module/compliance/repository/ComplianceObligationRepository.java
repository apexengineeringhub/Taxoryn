package com.taxoryn.module.compliance.repository;

import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceObligationRepository extends JpaRepository<ComplianceObligationEntity, UUID>, JpaSpecificationExecutor<ComplianceObligationEntity> {

    Optional<ComplianceObligationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ComplianceObligationEntity> findByOrganizationIdAndClientIdAndPeriodAndRuleId(
            UUID organizationId, UUID clientId, String period, UUID ruleId);

    Optional<ComplianceObligationEntity> findByOrganizationIdAndGstFilingId(UUID organizationId, UUID gstFilingId);

    Optional<ComplianceObligationEntity> findByOrganizationIdAndItrReturnId(UUID organizationId, UUID itrReturnId);

    Optional<ComplianceObligationEntity> findByOrganizationIdAndTdsReturnId(UUID organizationId, UUID tdsReturnId);

    Optional<ComplianceObligationEntity> findByOrganizationIdAndClientIdAndPeriodAndComplianceType(
            UUID organizationId, UUID clientId, String period, com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType complianceType);

    List<ComplianceObligationEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    boolean existsByOrganizationIdAndClientIdAndPeriodAndRuleId(
            UUID organizationId, UUID clientId, String period, UUID ruleId);

    List<ComplianceObligationEntity> findAllByOrganizationIdAndDueDateBetween(
            UUID organizationId, LocalDate startDate, LocalDate endDate);

    List<ComplianceObligationEntity> findAllByOrganizationIdAndDueDate(
            UUID organizationId, LocalDate dueDate);

    List<ComplianceObligationEntity> findAllByOrganizationIdAndStatus(
            UUID organizationId, ComplianceStatus status);

    long countByOrganizationIdAndStatus(UUID organizationId, ComplianceStatus status);
}
