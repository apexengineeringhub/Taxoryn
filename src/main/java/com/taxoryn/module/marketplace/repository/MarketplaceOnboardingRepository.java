package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.MarketplaceOnboardingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceOnboardingRepository extends JpaRepository<MarketplaceOnboardingEntity, UUID>, JpaSpecificationExecutor<MarketplaceOnboardingEntity> {

    Optional<MarketplaceOnboardingEntity> findByAccessToken(String accessToken);

    Optional<MarketplaceOnboardingEntity> findByLeadId(UUID leadId);

    Optional<MarketplaceOnboardingEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByOrganizationIdAndOnboardingStatus(UUID organizationId, MarketplaceOnboardingEntity.OnboardingStatus status);
}
