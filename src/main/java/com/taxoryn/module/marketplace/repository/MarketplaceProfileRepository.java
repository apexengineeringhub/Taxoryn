package com.taxoryn.module.marketplace.repository;

import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceProfileRepository extends JpaRepository<MarketplaceProfileEntity, UUID>, JpaSpecificationExecutor<MarketplaceProfileEntity> {

    Optional<MarketplaceProfileEntity> findByOrganizationId(UUID organizationId);

    Optional<MarketplaceProfileEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT p FROM MarketplaceProfileEntity p WHERE p.isPublished = true AND " +
           "(:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:profType IS NULL OR p.professionalType = :profType) AND " +
           "(:specialization IS NULL OR LOWER(p.specializations) LIKE LOWER(CONCAT('%', :specialization, '%'))) AND " +
           "(:verifiedOnly IS NULL OR :verifiedOnly = false OR p.verificationStatus = com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VerificationStatus.VERIFIED) AND " +
           "(:search IS NULL OR LOWER(p.displayName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.headline) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MarketplaceProfileEntity> searchPublicProfiles(
            @Param("city") String city,
            @Param("profType") MarketplaceProfileEntity.ProfessionalType profType,
            @Param("specialization") String specialization,
            @Param("verifiedOnly") Boolean verifiedOnly,
            @Param("search") String search,
            Pageable pageable
    );

    List<MarketplaceProfileEntity> findTop6ByIsPublishedTrueAndIsFeaturedTrueOrderByAverageRatingDesc();

    long countByIsPublishedTrue();

    long countByVerificationStatus(MarketplaceProfileEntity.VerificationStatus status);
}
