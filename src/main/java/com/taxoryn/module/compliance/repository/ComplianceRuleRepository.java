package com.taxoryn.module.compliance.repository;

import com.taxoryn.module.compliance.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceRuleRepository extends JpaRepository<ComplianceRuleEntity, UUID>, JpaSpecificationExecutor<ComplianceRuleEntity> {

    @Query("SELECT r FROM ComplianceRuleEntity r WHERE (r.organizationId = :organizationId OR r.organizationId IS NULL) AND r.active = true")
    List<ComplianceRuleEntity> findActiveRulesForOrganization(@Param("organizationId") UUID organizationId);

    @Query("SELECT r FROM ComplianceRuleEntity r WHERE (r.organizationId = :organizationId OR r.organizationId IS NULL) AND r.complianceType = :complianceType AND r.active = true")
    List<ComplianceRuleEntity> findActiveRulesForOrganizationAndType(@Param("organizationId") UUID organizationId, @Param("complianceType") ComplianceType complianceType);

    @Query("SELECT r FROM ComplianceRuleEntity r WHERE r.id = :id AND (r.organizationId = :organizationId OR r.organizationId IS NULL)")
    Optional<ComplianceRuleEntity> findByIdAndOrganizationIdOrSystem(@Param("id") UUID id, @Param("organizationId") UUID organizationId);

    Optional<ComplianceRuleEntity> findByRuleCode(String ruleCode);

    boolean existsByOrganizationIdAndRuleCode(UUID organizationId, String ruleCode);
}
