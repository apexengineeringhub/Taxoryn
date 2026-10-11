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

    org.springframework.data.domain.Page<MarketplaceReviewEntity> findByMarketplaceProfileIdAndStatusOrderByCreatedAtDesc(
            UUID marketplaceProfileId,
            MarketplaceReviewEntity.ReviewStatus status,
            org.springframework.data.domain.Pageable pageable
    );

    org.springframework.data.domain.Page<MarketplaceReviewEntity> findByStatusOrderByCreatedAtDesc(
            MarketplaceReviewEntity.ReviewStatus status,
            org.springframework.data.domain.Pageable pageable
    );

    org.springframework.data.domain.Page<MarketplaceReviewEntity> findAllByOrderByCreatedAtDesc(
            org.springframework.data.domain.Pageable pageable
    );

    List<MarketplaceReviewEntity> findAllByOrganizationId(UUID organizationId);

    List<MarketplaceReviewEntity> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    java.util.Optional<MarketplaceReviewEntity> findByLeadId(UUID leadId);

    boolean existsByLeadId(UUID leadId);

    boolean existsByCustomerIdAndMarketplaceProfileId(UUID customerId, UUID marketplaceProfileId);

    long countByCustomerId(UUID customerId);

    long countByMarketplaceProfileIdAndStatus(UUID marketplaceProfileId, MarketplaceReviewEntity.ReviewStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM MarketplaceReviewEntity r WHERE r.marketplaceProfileId = :profileId AND r.status = :status")
    Double getAverageRatingByProfileIdAndStatus(
            @org.springframework.data.repository.query.Param("profileId") UUID profileId,
            @org.springframework.data.repository.query.Param("status") MarketplaceReviewEntity.ReviewStatus status
    );

    @org.springframework.data.jpa.repository.Query("SELECT r.rating, COUNT(r) FROM MarketplaceReviewEntity r WHERE r.marketplaceProfileId = :profileId AND r.status = :status GROUP BY r.rating")
    List<Object[]> getRatingBreakdownByProfileIdAndStatus(
            @org.springframework.data.repository.query.Param("profileId") UUID profileId,
            @org.springframework.data.repository.query.Param("status") MarketplaceReviewEntity.ReviewStatus status
    );
}
