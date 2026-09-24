package com.taxoryn.module.compliance.repository;

import com.taxoryn.module.compliance.entity.ComplianceReminderRuleEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplianceReminderRuleRepository extends JpaRepository<ComplianceReminderRuleEntity, UUID> {

    @Query("""
        SELECT r FROM ComplianceReminderRuleEntity r
        WHERE (r.organizationId = :orgId OR r.organizationId IS NULL)
          AND r.isActive = true
        ORDER BY r.obligationType, r.daysOffset
    """)
    List<ComplianceReminderRuleEntity> findActiveRulesForOrganization(@Param("orgId") UUID orgId);

    @Query("""
        SELECT r FROM ComplianceReminderRuleEntity r
        WHERE (r.organizationId = :orgId OR r.organizationId IS NULL)
          AND r.obligationType = :obligationType
          AND r.isActive = true
        ORDER BY r.daysOffset
    """)
    List<ComplianceReminderRuleEntity> findByObligationType(
            @Param("orgId") UUID orgId,
            @Param("obligationType") ComplianceObligationType obligationType
    );
}
