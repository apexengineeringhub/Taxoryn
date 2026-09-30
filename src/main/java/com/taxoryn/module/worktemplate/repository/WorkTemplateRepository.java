package com.taxoryn.module.worktemplate.repository;

import com.taxoryn.module.service.model.ServiceCategory;
import com.taxoryn.module.worktemplate.entity.WorkTemplateEntity;
import com.taxoryn.module.worktemplate.model.WorkTemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkTemplateRepository extends JpaRepository<WorkTemplateEntity, UUID>, JpaSpecificationExecutor<WorkTemplateEntity> {

    @Query("SELECT t FROM WorkTemplateEntity t WHERE t.id = :id AND (t.organizationId = :orgId OR (t.organizationId IS NULL AND t.isSystemDefault = true))")
    Optional<WorkTemplateEntity> findAccessibleById(@Param("id") UUID id, @Param("orgId") UUID orgId);

    Optional<WorkTemplateEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("SELECT t FROM WorkTemplateEntity t WHERE (t.organizationId = :orgId OR (t.organizationId IS NULL AND t.isSystemDefault = true)) AND t.serviceId = :serviceId AND t.status = 'ACTIVE'")
    List<WorkTemplateEntity> findActiveTemplatesForService(@Param("serviceId") UUID serviceId, @Param("orgId") UUID orgId);

    @Query("SELECT t FROM WorkTemplateEntity t WHERE (t.organizationId = :orgId OR (t.organizationId IS NULL AND t.isSystemDefault = true)) AND t.status = 'ACTIVE'")
    List<WorkTemplateEntity> findAllActiveTemplates(@Param("orgId") UUID orgId);

    @Query("SELECT COUNT(t) > 0 FROM WorkTemplateEntity t WHERE (t.organizationId = :orgId OR (t.organizationId IS NULL AND :orgId IS NULL)) AND UPPER(t.templateCode) = UPPER(:code)")
    boolean existsByOrganizationIdAndTemplateCodeIgnoreCase(@Param("orgId") UUID orgId, @Param("code") String code);

    long countByOrganizationId(UUID organizationId);
}
