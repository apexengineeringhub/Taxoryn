package com.taxoryn.module.compliance.repository;

import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
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

    Optional<ComplianceObligationEntity> findByOrganizationIdAndClientServiceIdAndServicePeriodIdAndObligationType(
            UUID organizationId, UUID clientServiceId, UUID servicePeriodId, ComplianceObligationType obligationType);

    boolean existsByOrganizationIdAndClientServiceIdAndServicePeriodIdAndObligationType(
            UUID organizationId, UUID clientServiceId, UUID servicePeriodId, ComplianceObligationType obligationType);

    Optional<ComplianceObligationEntity> findByOrganizationIdAndWorkflowId(UUID organizationId, UUID workflowId);

    Optional<ComplianceObligationEntity> findByOrganizationIdAndClientIdAndPeriodAndRuleId(
            UUID organizationId, UUID clientId, String period, UUID ruleId);

    boolean existsByOrganizationIdAndClientIdAndPeriodAndRuleId(
            UUID organizationId, UUID clientId, String period, UUID ruleId);

    Optional<ComplianceObligationEntity> findByOrganizationIdAndGstFilingId(UUID organizationId, UUID gstFilingId);

    Optional<ComplianceObligationEntity> findByOrganizationIdAndItrReturnId(UUID organizationId, UUID itrReturnId);

    Optional<ComplianceObligationEntity> findByOrganizationIdAndClientIdAndRuleCodeAndRuleVersionAndPeriodTypeAndPeriodKey(
            UUID organizationId, UUID clientId, String ruleCode, Integer ruleVersion, com.taxoryn.module.compliance.rule.model.CompliancePeriodType periodType, String periodKey);

    Optional<ComplianceObligationEntity> findByOrganizationIdAndClientIdAndRuleCodeAndPeriodKey(
            UUID organizationId, UUID clientId, String ruleCode, String periodKey);

    List<ComplianceObligationEntity> findByOrganizationIdAndClientIdAndPeriodTypeAndPeriodKey(
            UUID organizationId, UUID clientId, com.taxoryn.module.compliance.rule.model.CompliancePeriodType periodType, String periodKey);

    long countByOrganizationIdAndClientIdAndStatus(UUID organizationId, UUID clientId, ComplianceObligationStatus status);

    long countByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    List<ComplianceObligationEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId);

    List<ComplianceObligationEntity> findAllByOrganizationIdAndClientId(UUID organizationId, UUID clientId, org.springframework.data.domain.Sort sort);

    List<ComplianceObligationEntity> findAllByOrganizationIdAndClientServiceId(UUID organizationId, UUID clientServiceId);

    List<ComplianceObligationEntity> findAllByOrganizationIdAndServicePeriodId(UUID organizationId, UUID servicePeriodId);

    List<ComplianceObligationEntity> findAllByOrganizationIdAndStatutoryDueDateBetween(
            UUID organizationId, LocalDate startDate, LocalDate endDate);

    List<ComplianceObligationEntity> findAllByOrganizationIdAndStatutoryDueDate(
            UUID organizationId, LocalDate statutoryDueDate);

    List<ComplianceObligationEntity> findAllByOrganizationIdAndStatus(
            UUID organizationId, ComplianceObligationStatus status);

    List<ComplianceObligationEntity> findByOrganizationIdAndClientIdOrderByStatutoryDueDateAsc(
            UUID organizationId, UUID clientId);

    List<ComplianceObligationEntity> findByOrganizationIdAndClientServiceIdOrderByStatutoryDueDateAsc(
            UUID organizationId, UUID clientServiceId);

    long countByOrganizationIdAndStatus(UUID organizationId, ComplianceObligationStatus status);

    // Legacy query methods
    List<ComplianceObligationEntity> findAllByOrganizationIdAndDueDateBetween(
            UUID organizationId, LocalDate fromDate, LocalDate toDate);

    Optional<ComplianceObligationEntity> findByOrganizationIdAndClientIdAndPeriodAndComplianceType(
            UUID organizationId, UUID clientId, String period, com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType complianceType);
}

