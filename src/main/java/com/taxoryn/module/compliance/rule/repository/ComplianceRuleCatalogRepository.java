package com.taxoryn.module.compliance.rule.repository;

import com.taxoryn.module.compliance.rule.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("complianceRuleCatalogRepository")
public interface ComplianceRuleCatalogRepository extends JpaRepository<ComplianceRuleEntity, UUID>, JpaSpecificationExecutor<ComplianceRuleEntity> {

    Optional<ComplianceRuleEntity> findByRuleCodeAndOrganizationId(String ruleCode, UUID organizationId);

    Optional<ComplianceRuleEntity> findByRuleCodeAndOrganizationIdIsNull(String ruleCode);

    boolean existsByRuleCodeAndOrganizationId(String ruleCode, UUID organizationId);

    boolean existsByRuleCodeAndOrganizationIdIsNull(String ruleCode);

    @Query("SELECT r FROM ComplianceRuleCatalogEntity r " +
           "WHERE (r.organizationId = :organizationId OR (r.organizationId IS NULL AND :includeSystem = true)) " +
           "AND (:domain IS NULL OR r.domain = :domain) " +
           "AND (:frequency IS NULL OR r.frequency = :frequency) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:search IS NULL OR " +
           "     LOWER(r.ruleCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(r.ruleName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(r.statutoryFormCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(r.statutoryAct) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY r.domain ASC, r.ruleCode ASC")
    List<ComplianceRuleEntity> findRulesForTenant(
            @Param("organizationId") UUID organizationId,
            @Param("includeSystem") boolean includeSystem,
            @Param("domain") ComplianceRuleDomain domain,
            @Param("frequency") ComplianceRuleFrequency frequency,
            @Param("status") ComplianceRuleStatus status,
            @Param("search") String search
    );

    @Query("SELECT r FROM ComplianceRuleCatalogEntity r WHERE r.organizationId IS NULL ORDER BY r.domain ASC, r.ruleCode ASC")
    List<ComplianceRuleEntity> findAllSystemRules();

    @Query("SELECT r FROM ComplianceRuleCatalogEntity r WHERE r.organizationId = :organizationId ORDER BY r.domain ASC, r.ruleCode ASC")
    List<ComplianceRuleEntity> findAllByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("SELECT COUNT(r) FROM ComplianceRuleCatalogEntity r " +
           "WHERE (r.organizationId = :organizationId OR r.organizationId IS NULL) " +
           "AND r.status = 'ACTIVE'")
    long countActiveRulesForTenant(@Param("organizationId") UUID organizationId);
}
