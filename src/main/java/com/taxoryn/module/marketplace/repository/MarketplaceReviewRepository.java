package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.MarketplaceReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketplaceReviewRepository extends JpaRepository<MarketplaceReviewEntity, UUID> {

    List<MarketplaceReviewEntity> findByMarketplaceProfileIdAndStatusOrderByCreatedAtDesc(
            UUID marketplaceProfileId,
            MarketplaceReviewEntity.ReviewStatus status
    );

    List<MarketplaceReviewEntity> findAllByOrganizationId(UUID organizationId);

    List<MarketplaceReviewEntity> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    long countByCustomerId(UUID customerId);
}
