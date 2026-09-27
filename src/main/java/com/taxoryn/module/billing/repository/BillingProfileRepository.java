package com.taxoryn.module.billing.repository;

import com.taxoryn.module.billing.entity.BillingProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingProfileRepository extends JpaRepository<BillingProfileEntity, UUID>, JpaSpecificationExecutor<BillingProfileEntity> {

    Optional<BillingProfileEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<BillingProfileEntity> findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(UUID organizationId, UUID clientId);

    Optional<BillingProfileEntity> findByOrganizationIdAndClientIdAndEngagementId(UUID organizationId, UUID clientId, UUID engagementId);

    Optional<BillingProfileEntity> findFirstByOrganizationIdAndClientIdAndEngagementIdIsNullAndActiveTrue(UUID organizationId, UUID clientId);
}
