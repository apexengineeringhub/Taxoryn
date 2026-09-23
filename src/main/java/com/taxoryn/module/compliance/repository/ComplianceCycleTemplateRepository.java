package com.taxoryn.module.compliance.repository;

import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.compliance.entity.ComplianceCycleTemplateEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceCycleTemplateRepository extends JpaRepository<ComplianceCycleTemplateEntity, UUID> {

    @Query("""
        SELECT c FROM ComplianceCycleTemplateEntity c
        WHERE (c.organizationId = :orgId OR c.organizationId IS NULL)
          AND c.isActive = true
        ORDER BY c.serviceType, c.name
    """)
    List<ComplianceCycleTemplateEntity> findActiveTemplatesForOrganization(@Param("orgId") UUID orgId);

    @Query("""
        SELECT c FROM ComplianceCycleTemplateEntity c
        WHERE (c.organizationId = :orgId OR c.organizationId IS NULL)
          AND c.serviceType = :serviceType
          AND c.isActive = true
        ORDER BY c.isSystem DESC, c.createdAt DESC
    """)
    List<ComplianceCycleTemplateEntity> findByServiceType(
            @Param("orgId") UUID orgId,
            @Param("serviceType") ClientServiceType serviceType
    );

    @Query("""
        SELECT c FROM ComplianceCycleTemplateEntity c
        WHERE (c.organizationId = :orgId OR c.organizationId IS NULL)
          AND c.serviceType = :serviceType
          AND c.obligationType = :obligationType
          AND c.isActive = true
        ORDER BY c.isSystem DESC, c.createdAt DESC
    """)
    List<ComplianceCycleTemplateEntity> findByServiceTypeAndObligationType(
            @Param("orgId") UUID orgId,
            @Param("serviceType") ClientServiceType serviceType,
            @Param("obligationType") ComplianceObligationType obligationType
    );

    Optional<ComplianceCycleTemplateEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
