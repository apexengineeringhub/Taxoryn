package com.taxoryn.module.worktemplate.repository;

import com.taxoryn.module.worktemplate.entity.EngagementWorkTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EngagementWorkTemplateRepository extends JpaRepository<EngagementWorkTemplateEntity, UUID> {

    List<EngagementWorkTemplateEntity> findAllByOrganizationIdAndEngagementId(UUID organizationId, UUID engagementId);

    Optional<EngagementWorkTemplateEntity> findByOrganizationIdAndEngagementIdAndTemplateId(
            UUID organizationId, UUID engagementId, UUID templateId
    );

    boolean existsByOrganizationIdAndEngagementIdAndTemplateId(
            UUID organizationId, UUID engagementId, UUID templateId
    );

    void deleteAllByOrganizationIdAndEngagementId(UUID organizationId, UUID engagementId);
}
