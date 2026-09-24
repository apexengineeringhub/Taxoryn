package com.taxoryn.module.workflow.repository;

import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.workflow.entity.ServiceWorkflowTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceWorkflowTemplateRepository extends JpaRepository<ServiceWorkflowTemplateEntity, UUID> {

    @Query("SELECT t FROM ServiceWorkflowTemplateEntity t WHERE (t.organizationId = :orgId OR (t.organizationId IS NULL AND t.isSystemDefault = true)) AND t.serviceType = :serviceType AND t.active = true ORDER BY t.organizationId DESC NULLS LAST")
    List<ServiceWorkflowTemplateEntity> findEffectiveTemplatesForService(@Param("orgId") UUID orgId, @Param("serviceType") ClientServiceType serviceType);

    default Optional<ServiceWorkflowTemplateEntity> findBestTemplate(UUID orgId, ClientServiceType serviceType) {
        List<ServiceWorkflowTemplateEntity> list = findEffectiveTemplatesForService(orgId, serviceType);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    List<ServiceWorkflowTemplateEntity> findAllByOrganizationIdOrOrganizationIdIsNull(UUID organizationId);

    Optional<ServiceWorkflowTemplateEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
