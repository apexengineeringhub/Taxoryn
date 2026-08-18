package com.taxoryn.module.audit.repository;

import com.taxoryn.module.audit.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    Page<AuditLogEntity> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Page<AuditLogEntity> findAllByOrganizationIdAndEntityName(UUID organizationId, String entityName, Pageable pageable);

    Page<AuditLogEntity> findAllByOrganizationIdAndUserId(UUID organizationId, UUID userId, Pageable pageable);
}
